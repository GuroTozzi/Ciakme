package com.ciakme.domain.auth;

import com.ciakme.domain.user.UserRole;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuthResponse {
    private String token;
    private String email;
    private UserRole role;
}