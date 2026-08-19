package com.ciakme.domain.user;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class UserResponse {

    private UUID id;
    private String email;
    private UserRole role;
    private String locale;
    private boolean verified;
    private LocalDateTime createdAt;

    // Converte l'entità User nel DTO di risposta
    // Nota: non esponiamo mai passwordHash nella risposta
    public static UserResponse from(User user){
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .role(user.getRole())
                .locale(user.getLocale())
                .verified(user.isVerified())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
