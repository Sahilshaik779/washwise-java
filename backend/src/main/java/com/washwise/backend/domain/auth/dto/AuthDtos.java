package com.washwise.backend.domain.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AuthDtos {

    public record RegisterRequest(
        @NotBlank String username,
        @Email @NotBlank String email,
        @NotBlank @Size(min = 6) String password,
        String role,     
        String staffSecret 
    ) {}
    
    public record LoginRequest(@NotBlank String username, @NotBlank String password) {}
    public record TokenResponse(String access_token, String role, String user_id, String token_type) {}
    public record PasswordResetRequest(@Email @NotBlank String email) {}
    public record PasswordResetConfirmRequest(@NotBlank String token, @NotBlank String newPassword) {}
}