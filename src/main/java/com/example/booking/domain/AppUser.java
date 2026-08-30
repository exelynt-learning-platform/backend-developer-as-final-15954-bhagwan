package com.example.booking.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "app_users")
public class AppUser {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false, unique = true) private String username;
    @Column(nullable = false) private String password;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private Role role;
    @Column(nullable = false) private Instant createdAt = Instant.now();
    protected AppUser() {}
    public AppUser(String username, String password, Role role) { this.username = username; this.password = password; this.role = role; }
    public Long getId() { return id; }
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public Role getRole() { return role; }
}