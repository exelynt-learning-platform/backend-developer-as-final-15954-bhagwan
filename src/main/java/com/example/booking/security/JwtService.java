package com.example.booking.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import java.util.Date;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Service
public class JwtService {
    private final Algorithm algorithm;
    private final long expirationMs;

    public JwtService(@Value("${jwt.secret}") String secret, @Value("${jwt.expiration-ms}") long expirationMs) {
        this.algorithm = Algorithm.HMAC256(secret);
        this.expirationMs = expirationMs;
    }

    public String generateToken(UserDetails user) {
        return JWT.create()
            .withSubject(user.getUsername())
            .withClaim("role", user.getAuthorities().iterator().next().getAuthority())
            .withIssuedAt(new Date())
            .withExpiresAt(new Date(System.currentTimeMillis() + expirationMs))
            .sign(algorithm);
    }

    public String username(String token) {
        return decoded(token).getSubject();
    }

    public boolean valid(String token, UserDetails user) {
        try {
            DecodedJWT decoded = decoded(token);
            return user.getUsername().equals(decoded.getSubject())
                && decoded.getExpiresAt().after(new Date());
        } catch (RuntimeException ex) {
            return false;
        }
    }

    private DecodedJWT decoded(String token) {
        return JWT.require(algorithm).build().verify(token);
    }
}