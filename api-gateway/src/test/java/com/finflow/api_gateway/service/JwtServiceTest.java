package com.finflow.api_gateway.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Date;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

class JwtServiceTest {

    private static final String SECRET = "12345678901234567890123456789012";

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secret", SECRET);
    }

    @Test
    void validateTokenShouldReturnTrueForValidToken() {
        String token = createToken("user@finflow.com", "ADMIN");

        assertTrue(jwtService.validateToken(token));
    }

    @Test
    void validateTokenShouldReturnFalseForInvalidToken() {
        assertFalse(jwtService.validateToken("invalid-token"));
    }

    @Test
    void extractEmailShouldReturnTokenSubject() {
        String token = createToken("user@finflow.com", "USER");

        assertEquals("user@finflow.com", jwtService.extractEmail(token));
    }

    @Test
    void extractRoleShouldReturnRoleClaimWhenPresent() {
        String token = createToken("user@finflow.com", "ADMIN");

        assertEquals("ADMIN", jwtService.extractRole(token));
    }

    @Test
    void extractRoleShouldReturnNullWhenRoleClaimMissing() {
        String token = Jwts.builder()
                .setSubject("user@finflow.com")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 60000))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes()))
                .compact();

        assertNull(jwtService.extractRole(token));
    }

    private String createToken(String email, String role) {
        return Jwts.builder()
                .setSubject(email)
                .claim("role", role)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 60000))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes()))
                .compact();
    }
}
