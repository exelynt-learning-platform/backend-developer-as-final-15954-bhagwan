package com.example.booking.security;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;

class JwtServiceTest {
    @Test
    void generatedTokenValidatesForItsUser() {
        JwtService service = new JwtService("a-secret-key-that-is-at-least-32-characters-long", 60_000);
        var user = User.withUsername("user").password("encoded").roles("USER").build();
        String token = service.generateToken(user);
        assertEquals("user", service.username(token));
        assertTrue(service.valid(token, user));
    }
}