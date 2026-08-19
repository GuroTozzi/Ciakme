package com.ciakme.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Slf4j
@Component
public class JwtTokenProvider {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration-ms}")
    private long jwtExpirationMs;

    private SecretKey secretKey;

    // Viene chiamato da Spring dopo l'injection delle proprietà
    // Converte la stringa jwtSecret in una chiave crittografica
    @PostConstruct
    public void init() {
        this.secretKey = Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }

    // Genera un token JWT per l'utente autenticato
    public String generateToken(String email, String role) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + jwtExpirationMs);

        return Jwts.builder()
                .subject(email)           // chi è il token
                .claim("role", role)      // ruolo dell'utente
                .issuedAt(now)            // quando è stato emesso
                .expiration(expiry)       // quando scade
                .signWith(secretKey)      // firma con la chiave segreta
                .compact();
    }

    // Estrae l'email dal token
    public String getEmailFromToken(String token) {
        return parseClaims(token).getSubject();
    }

    // Estrae il ruolo dal token
    public String getRoleFromToken(String token) {
        return parseClaims(token).get("role", String.class);
    }

    // Verifica che il token sia valido e non scaduto
    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException e) {
            log.warn("Token JWT non valido: {}", e.getMessage());
            return false;
        }
    }

    // Parsa il token e restituisce il payload (Claims)
    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}