package com.example.booking.security;

import java.io.IOException;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtService jwt; private final DatabaseUserDetailsService users;
    public JwtAuthenticationFilter(JwtService jwt, DatabaseUserDetailsService users) { 
        this.jwt = jwt; 
        this.users = users; }
    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ") && SecurityContextHolder.getContext().getAuthentication() == null) {
            try { String username = jwt.username(header.substring(7)); 
                UserDetails user = users.loadUserByUsername(username); 
                if (jwt.valid(header.substring(7), user)) { 
                var auth = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()); 
                auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request)); 
                SecurityContextHolder.getContext().setAuthentication(auth); 
            } 
        } catch (RuntimeException ignored) { }
        }
        chain.doFilter(request, response);
    }
}