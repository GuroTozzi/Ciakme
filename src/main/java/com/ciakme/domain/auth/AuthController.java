package com.ciakme.domain.auth;

import com.ciakme.domain.user.User;
import com.ciakme.domain.user.UserService;
import com.ciakme.security.JwtTokenProvider;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request) {

        // Cerca l'utente per email
        User user = userService.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Credenziali non valide"));

        // Verifica la password
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new RuntimeException("Credenziali non valide");
        }

        // Genera il token JWT
        String token = jwtTokenProvider.generateToken(
                user.getEmail(),
                user.getRole().name()
        );

        return ResponseEntity.ok(AuthResponse.builder()
                .token(token)
                .email(user.getEmail())
                .role(user.getRole())
                .build());
    }
}