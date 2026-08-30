package com.example.booking.security;

import com.example.booking.repository.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

@Service
public class DatabaseUserDetailsService implements UserDetailsService {
    private final UserRepository users;
    public DatabaseUserDetailsService(UserRepository users) { this.users = users; }
    @Override public UserDetails loadUserByUsername(String username) { return users.findByUsername(username).map(user -> User.withUsername(user.getUsername()).password(user.getPassword()).authorities(new SimpleGrantedAuthority("ROLE_" + user.getRole())).build()).orElseThrow(() -> new UsernameNotFoundException(username)); }
}