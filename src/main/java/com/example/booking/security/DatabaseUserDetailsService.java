package com.example.booking.security;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.example.booking.repository.UserRepository;

@Service
public class DatabaseUserDetailsService implements UserDetailsService {
    private final UserRepository users;
    public DatabaseUserDetailsService(UserRepository users) { 
        this.users = users; }
    @Override public UserDetails loadUserByUsername(String username) { 
        return users.findByUsername(username)
        .map(user -> User.withUsername(user.getUsername())
        .password(user.getPassword())
        .authorities(new SimpleGrantedAuthority("ROLE_" + user.getRole()))
        .build()).orElseThrow(() -> new UsernameNotFoundException(username));
     }
}