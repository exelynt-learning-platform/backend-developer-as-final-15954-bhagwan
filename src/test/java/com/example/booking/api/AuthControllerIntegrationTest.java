package com.example.booking.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.booking.domain.AppUser;
import com.example.booking.domain.Role;
import com.example.booking.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerIntegrationTest {
    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository users;
    @Autowired private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        users.deleteAll();
        users.save(new AppUser("admin", passwordEncoder.encode("Admin@123"), Role.ADMIN));
        users.save(new AppUser("user", passwordEncoder.encode("User@123"), Role.USER));
    }

    @Test
    void loginShouldReturnTokenForValidUser() throws Exception {
        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"user\",\"password\":\"User@123\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.username").value("user"))
            .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    void loginShouldRejectBadCredentials() throws Exception {
        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"user\",\"password\":\"wrong\"}"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void authLoginEndpointAndUserPersistenceAreWorking() {
        assertThat(users.findByUsername("admin")).isPresent();
        assertThat(users.findByUsername("user")).isPresent();
    }
}
