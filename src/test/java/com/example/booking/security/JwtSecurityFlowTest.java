package com.example.booking.security;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.example.booking.domain.*;
import com.example.booking.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.web.authentication.WebAuthenticationDetails;

class JwtSecurityFlowTest {
    @Test
    void jwtServiceGeneratesAndValidatesToken() {
        JwtService service = new JwtService("a-secret-key-that-is-at-least-32-characters-long", 60_000);
        var user = User.withUsername("user").password("encoded").roles("USER").build();

        String token = service.generateToken(user);

        assertEquals("user", service.username(token));
        assertTrue(service.valid(token, user));
    }

    @Test
    void userDetailsServiceLoadsUserByUsername() {
        UserRepository users = mock(UserRepository.class);
        DatabaseUserDetailsService service = new DatabaseUserDetailsService(users);
        AppUser appUser = new AppUser("admin", "encoded", Role.ADMIN);

        when(users.findByUsername("admin")).thenReturn(Optional.of(appUser));

        var principal = service.loadUserByUsername("admin");

        assertEquals("admin", principal.getUsername());
        assertTrue(principal.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));
    }

    @Test
    void jwtAuthenticationFilterSetsAuthenticatedContextWhenTokenIsValid() throws Exception {
        JwtService jwtService = new JwtService("a-secret-key-that-is-at-least-32-characters-long", 60_000);
        UserRepository users = mock(UserRepository.class);
        DatabaseUserDetailsService userDetailsService = new DatabaseUserDetailsService(users);
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService, userDetailsService);
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        AppUser appUser = new AppUser("admin", "encoded", Role.ADMIN);
        when(users.findByUsername("admin")).thenReturn(Optional.of(appUser));
        when(request.getHeader("Authorization")).thenReturn("Bearer " + jwtService.generateToken(User.withUsername("admin").password("encoded").roles("ADMIN").build()));
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        SecurityContextHolder.clearContext();
        filter.doFilterInternal(request, response, chain);

        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals("admin", SecurityContextHolder.getContext().getAuthentication().getName());
        verify(chain).doFilter(request, response);
    }

    @Test
    void jwtAuthenticationFilterSkipsInvalidToken() throws Exception {
        JwtService jwtService = new JwtService("a-secret-key-that-is-at-least-32-characters-long", 60_000);
        UserRepository users = mock(UserRepository.class);
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService, new DatabaseUserDetailsService(users));
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        when(request.getHeader("Authorization")).thenReturn("Bearer invalid-token");

        SecurityContextHolder.clearContext();
        filter.doFilterInternal(request, response, chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(chain).doFilter(request, response);
    }
}
