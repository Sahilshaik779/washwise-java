package com.washwise.backend.domain.auth;

import com.washwise.backend.domain.auth.dto.AuthDtos.*;
import com.washwise.backend.domain.notification.NotificationService;
import com.washwise.backend.domain.user.User;
import com.washwise.backend.domain.user.UserRepository;
import com.washwise.backend.security.JwtTokenProvider;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.ZonedDateTime;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final NotificationService notificationService;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> register(@Valid @RequestBody RegisterRequest request) {
        
        // 1. Check if username or email already exists
        if (userRepository.existsByUsername(request.username())) {
            throw new IllegalArgumentException("Username is already taken");
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Email is already registered");
        }

        // 2. Create the new user
        String assignedRole = "customer"; // Default to customer
        
        if ("serviceman".equalsIgnoreCase(request.role())) {
            // Require a secret passcode to register as a staff member!
            if ("WASHWISE_STAFF_2026".equals(request.staffSecret())) {
                assignedRole = "serviceman";
            } else {
                throw new IllegalArgumentException("Invalid staff secret key");
            }
        }

        // Create the new user with the correct role
        User newUser = User.builder()
                .username(request.username())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .role(assignedRole) 
                .build();

        userRepository.save(newUser);

        // 3. Return success message
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "message", "User registered successfully",
                "userId", newUser.getId()
        ));
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        User user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new IllegalArgumentException("Incorrect username or password"));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String token = jwtTokenProvider.createAccessToken(user.getUsername(), user.getRole(), user.getId());
        return ResponseEntity.ok(new TokenResponse(token, user.getRole(), user.getId(), "bearer"));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(@Valid @RequestBody PasswordResetRequest request) {
        userRepository.findByEmail(request.email()).ifPresent(user -> {
            String token = UUID.randomUUID().toString().replace("-", "");
            user.setResetToken(token);
            user.setResetTokenExpiry(ZonedDateTime.now().plusMinutes(15));
            userRepository.save(user);

            String resetLink = frontendUrl + "/reset-password?token=" + token;
            String subject = "Your WashWise Password Reset Request";
            String body = "<p>Hi " + user.getUsername() + ",</p>" +
                          "<p>Click <a href='" + resetLink + "'>here</a> to reset your password.</p>";
            
            // Runs synchronously here, but should be marked @Async in NotificationService
            notificationService.sendEmail(user.getEmail(), subject, body);
        });

        return ResponseEntity.ok(Map.of("message", "If an account exists, a link has been sent."));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(@Valid @RequestBody PasswordResetConfirmRequest request) {
        User user = userRepository.findByResetToken(request.token())
                .orElseThrow(() -> new IllegalArgumentException("Invalid token"));

        if (user.getResetTokenExpiry() == null || user.getResetTokenExpiry().isBefore(ZonedDateTime.now())) {
            throw new IllegalArgumentException("Token expired");
        }

        user.setPassword(passwordEncoder.encode(request.newPassword()));
        user.setResetToken(null);
        user.setResetTokenExpiry(null);
        userRepository.save(user);

        return ResponseEntity.ok(Map.of("message", "Password has been reset successfully."));
    }
    
    // Note: The Google OAuth logic (@router.get("/login/google")) translates perfectly to 
    // Spring Security's native OAuth2 Login module rather than a manual REST Controller mapping.
    // By adding `spring-boot-starter-oauth2-client` to your pom.xml, Spring automatically 
    // generates the redirect URL and handles the callback via standard security filters.
}