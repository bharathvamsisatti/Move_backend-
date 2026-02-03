package com.example.mroads.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

	private final JwsUtils jwsUtils;

	@Override
protected boolean shouldNotFilter(HttpServletRequest request) {
    String path = request.getServletPath();

    return "OPTIONS".equalsIgnoreCase(request.getMethod())
        || path.startsWith("/api/auth")
        || path.startsWith("/oauth2")
        || path.startsWith("/login/oauth2");
}


	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws IOException, ServletException {

		String header = request.getHeader("Authorization");

		if (header != null && header.startsWith("Bearer ")) {
			String token = header.substring(7);

			try {
				Claims claims = Jwts.parserBuilder().setSigningKey(jwsUtils.getSecretKey()).build()
						.parseClaimsJws(token).getBody();

				// ✅ UUID FROM SUBJECT
				String userUuid = claims.getSubject();

				UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(userUuid, // ⭐
																														// PRINCIPAL
																														// =
																														// UUID
						null, Collections.emptyList());

				SecurityContextHolder.getContext().setAuthentication(authentication);

			} catch (JwtException ex) {
				response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
				response.setContentType("application/json");
				response.getWriter().write("{\"error\":\"Invalid or expired JWT\"}");
				return;
			}
		}

		chain.doFilter(request, response);
	}
}
