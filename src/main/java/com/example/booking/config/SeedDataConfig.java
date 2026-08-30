package com.example.booking.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.example.booking.domain.AppUser;
import com.example.booking.domain.Role;
import com.example.booking.repository.UserRepository;

@Configuration
public class SeedDataConfig {
    @Bean CommandLineRunner seedUsers(UserRepository users, PasswordEncoder encoder) { 
        return args -> { 
            if (users.count() == 0) { 
                users.save(new AppUser("admin", encoder.encode("Admin@123"), Role.ADMIN)); 
                users.save(new AppUser("user", encoder.encode("User@123"), Role.USER)); 
            } 
        };
             }
}