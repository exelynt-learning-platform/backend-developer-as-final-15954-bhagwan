package com.example.booking.api;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.booking.api.dto.AuthDtos.*;
import com.example.booking.api.dto.AuthDtos.LoginRequest;
import com.example.booking.api.dto.AuthDtos.LoginResponse;
import com.example.booking.security.JwtService;

import jakarta.validation.Valid;

@RestController 
@RequestMapping("/auth")
public class AuthController {
    private final AuthenticationManager manager;
     private final JwtService jwt;
    public AuthController(AuthenticationManager manager, JwtService jwt) { 
        this.manager = manager;
         this.jwt = jwt; }
    @PostMapping("/login") public LoginResponse login(@Valid @RequestBody LoginRequest request) { Authentication auth = manager.authenticate(new UsernamePasswordAuthenticationToken(request.username(), request.password())); String role = auth.getAuthorities().iterator().next().getAuthority().replace("ROLE_", ""); return new LoginResponse(jwt.generateToken((org.springframework.security.core.userdetails.UserDetails) auth.getPrincipal()), auth.getName(), role); }
}