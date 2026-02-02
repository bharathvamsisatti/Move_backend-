package com.example.mroads.auth;

import com.example.mroads.security.JwsUtils;
import com.example.mroads.user.User;
import com.example.mroads.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final OtpService otpService;
    private final JwsUtils jwsUtils;

    // =====================
    // REGISTER
    // =====================
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {

        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Email already in use");
        }

        User user = User.builder()
                .userName(request.getUserName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .provider("LOCAL")
                .userUuid(UUID.randomUUID().toString())
                .build();

        userRepository.save(user);

        String token = jwsUtils.generateToken(user.getUserUuid());
        return ResponseEntity.status(HttpStatus.CREATED).body(token);
    }

    // =====================
    // PASSWORD LOGIN
    // =====================
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {

        Optional<User> opt = userRepository.findByEmail(request.getEmail());
        if (opt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid email or password");
        }

        User user = opt.get();

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid email or password");
        }

        String token = jwsUtils.generateToken(user.getUserUuid());
        return ResponseEntity.ok(token);
    }

    // =====================
    // LOGIN WITH OTP (NEW)
    // =====================
    @PostMapping("/login/send-otp")
    public ResponseEntity<?> sendLoginOtp(@RequestParam String email) {

        Optional<User> opt = userRepository.findByEmail(email);
        if (opt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not registered");
        }

        otpService.generateAndSend(email);
        return ResponseEntity.ok("OTP sent to registered email");
    }

    @PostMapping("/login/verify-otp")
    public ResponseEntity<?> loginWithOtp(@RequestBody VerifyOtpRequest request) {

        boolean valid = otpService.verify(request.getEmail(), request.getOtp());
        if (!valid) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid or expired OTP");
        }

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        String token = jwsUtils.generateToken(user.getUserUuid());

        otpService.clear(request.getEmail());

        Map<String, Object> res = new HashMap<>();
        res.put("token", token);
        res.put("user", user);

        return ResponseEntity.ok(res);
    }

    // =====================
    // FORGOT PASSWORD
    // =====================
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody ForgotPasswordRequest request) {

        Optional<User> opt = userRepository.findByEmail(request.getEmail());
        if (opt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not registered");
        }

        otpService.generateAndSend(opt.get().getEmail());
        return ResponseEntity.ok("OTP sent successfully");
    }

    // =====================
    // VERIFY OTP (FOR RESET)
    // =====================
    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(@RequestBody VerifyOtpRequest request) {

        boolean valid = otpService.verify(request.getEmail(), request.getOtp());
        if (!valid) {
            return ResponseEntity.badRequest().body("Invalid or expired OTP");
        }

        return ResponseEntity.ok("OTP verified");
    }

    // =====================
    // RESET PASSWORD
    // =====================
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody ResetPasswordRequest request) {

        boolean valid = otpService.verify(request.getEmail(), request.getOtp());
        if (!valid) {
            return ResponseEntity.badRequest().body("Invalid or expired OTP");
        }

        if (request.getNewPassword().length() < 8) {
            return ResponseEntity.badRequest().body("Password must be at least 8 characters");
        }

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        otpService.clear(request.getEmail());
        return ResponseEntity.ok("Password updated successfully");
    }
}
