package com.example.mroads.config;

import com.example.mroads.security.JwsUtils;
import com.example.mroads.user.User;
import com.example.mroads.user.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CustomOAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final JwsUtils jwsUtils;

    // ✅ Frontend URL from properties (NO hardcoding)
    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) throws IOException {

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");
        String providerId = oAuth2User.getName();

        String finalEmail = (email != null) ? email : providerId;
        String finalName = (name != null) ? name : finalEmail;

        User user = userRepository.findByEmail(finalEmail).orElseGet(() -> {
            User u = new User();
            u.setEmail(finalEmail);
            u.setUserName(finalName);
            u.setProvider("GOOGLE");
            u.setProviderId(providerId);
            u.setUserUuid(UUID.randomUUID().toString());
            return userRepository.save(u);
        });

        // Backward compatibility
        if (user.getUserUuid() == null) {
            user.setUserUuid(UUID.randomUUID().toString());
            userRepository.save(user);
        }

        // Generate JWT
        String token = jwsUtils.generateToken(user.getUserUuid());

        // ✅ Redirect to correct frontend (DEV / LOCAL) as well as app
        String redirectUri = request.getParameter("redirect_uri");

        String redirectUrl;

        if (redirectUri != null && redirectUri.startsWith("moveapp://")) {
            // 📱 Mobile app (Expo / APK)
            redirectUrl = redirectUri + "?token=" +
                    URLEncoder.encode(token, StandardCharsets.UTF_8);
        } else {
            // 🌐 Web fallback
            redirectUrl = frontendUrl + "/oauth-success?token=" +
                    URLEncoder.encode(token, StandardCharsets.UTF_8);
        }

        response.sendRedirect(redirectUrl);
    }
}
