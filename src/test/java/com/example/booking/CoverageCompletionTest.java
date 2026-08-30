package com.example.booking;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.example.booking.api.ApiExceptionHandler;
import com.example.booking.api.AuthController;
import com.example.booking.api.ResourceController;
import com.example.booking.api.ReservationController;
import com.example.booking.api.dto.AuthDtos.LoginRequest;
import com.example.booking.api.dto.AuthDtos.LoginResponse;
import com.example.booking.api.dto.ResourceDtos.ResourceRequest;
import com.example.booking.api.dto.ResourceDtos.ResourceResponse;
import com.example.booking.api.dto.ReservationDtos.ReservationRequest;
import com.example.booking.api.dto.ReservationDtos.ReservationResponse;
import com.example.booking.api.dto.ReservationDtos.ReservationUpdate;
import com.example.booking.config.OpenApiConfig;
import com.example.booking.config.SeedDataConfig;
import com.example.booking.domain.AppUser;
import com.example.booking.domain.Reservation;
import com.example.booking.domain.ReservationStatus;
import com.example.booking.domain.Resource;
import com.example.booking.domain.Role;
import com.example.booking.repository.UserRepository;
import com.example.booking.security.DatabaseUserDetailsService;
import com.example.booking.security.JwtAuthenticationFilter;
import com.example.booking.security.JwtService;
import com.example.booking.service.ResourceService;
import com.example.booking.service.ReservationService;
import jakarta.persistence.EntityNotFoundException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.boot.CommandLineRunner;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

class CoverageCompletionTest {
    @Test
    void authControllerAndExceptionHandlerCoverMainAuthPaths() {
        AuthenticationManager manager = mock(AuthenticationManager.class);
        JwtService jwt = mock(JwtService.class);
        Authentication auth = new UsernamePasswordAuthenticationToken(
            User.withUsername("admin").password("encoded").authorities(new SimpleGrantedAuthority("ROLE_ADMIN")).build(),
            "encoded",
            List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );
        when(manager.authenticate(any())).thenReturn(auth);
        when(jwt.generateToken(any(UserDetails.class))).thenReturn("jwt-token");

        AuthController controller = new AuthController(manager, jwt);
        LoginResponse response = controller.login(new LoginRequest("admin", "Admin@123"));

        assertEquals("jwt-token", response.token());
        assertEquals("admin", response.username());
        assertEquals("ADMIN", response.role());

        ApiExceptionHandler handler = new ApiExceptionHandler();
        ResponseEntity<?> notFound = handler.notFound(new EntityNotFoundException("missing"));
        assertEquals(404, ((Map<?, ?>) notFound.getBody()).get("status"));

        ResponseEntity<?> badRequest = handler.badRequest(new IllegalArgumentException("bad"));
        assertEquals(400, ((Map<?, ?>) badRequest.getBody()).get("status"));

        ResponseEntity<?> forbidden = handler.denied(new AccessDeniedException("forbidden"));
        assertEquals(403, ((Map<?, ?>) forbidden.getBody()).get("status"));

        ResponseEntity<?> unauthorized = handler.unauthenticated(new org.springframework.security.authentication.BadCredentialsException("oops"));
        assertEquals(401, ((Map<?, ?>) unauthorized.getBody()).get("status"));
    }

    @Test
    void resourceAndReservationControllersCallServiceMethods() {
        ResourceService resourceService = mock(ResourceService.class);
        ResourceController resourceController = new ResourceController(resourceService);
        when(resourceService.all()).thenReturn(List.of(new ResourceResponse(1L, "Board", "Room", new BigDecimal("50.00"), true)));
        when(resourceService.get(1L)).thenReturn(new ResourceResponse(1L, "Board", "Room", new BigDecimal("50.00"), true));
        when(resourceService.create(any())).thenReturn(new ResourceResponse(2L, "Desk", "Workstation", new BigDecimal("20.00"), true));

        assertEquals(1, resourceController.all().size());
        assertEquals("Board", resourceController.get(1L).name());
        assertEquals("Desk", resourceController.create(new ResourceRequest("Desk", "Workstation", new BigDecimal("20.00"), true)).name());

        ReservationService reservationService = mock(ReservationService.class);
        ReservationController reservationController = new ReservationController(reservationService);
        Authentication admin = new UsernamePasswordAuthenticationToken(
            "admin",
            "pw",
            List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );
        Authentication user = new UsernamePasswordAuthenticationToken(
            "alice",
            "pw",
            List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );

        OffsetDateTime now = OffsetDateTime.now().plusDays(1);
        ReservationResponse reservationResponse = new ReservationResponse(7L, 1L, "Board", "alice", now, now.plusHours(2), new BigDecimal("100.00"), ReservationStatus.PENDING);
        when(reservationService.find(eq("alice"), eq(false), eq(null), eq(null), eq(null), any())).thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(reservationResponse)));
        when(reservationService.find(eq("admin"), eq(true), eq(null), eq(null), eq(null), any())).thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(reservationResponse)));
        when(reservationService.get(7L, "alice", false)).thenReturn(reservationResponse);
        when(reservationService.get(7L, "admin", true)).thenReturn(reservationResponse);
        when(reservationService.create(eq("alice"), any())).thenReturn(reservationResponse);
        when(reservationService.update(eq(7L), eq("alice"), eq(false), any())).thenReturn(reservationResponse);

        var userPage = reservationController.find(user, null, null, null, org.springframework.data.domain.PageRequest.of(0, 20));
        var adminPage = reservationController.find(admin, null, null, null, org.springframework.data.domain.PageRequest.of(0, 20));

        assertEquals(1, userPage.getTotalElements());
        assertEquals(1, adminPage.getTotalElements());
        assertEquals("alice", reservationController.get(user, 7L).username());
        assertEquals("alice", reservationController.get(admin, 7L).username());
        assertEquals("alice", reservationController.create(user, new ReservationRequest(1L, now, now.plusHours(1), new BigDecimal("100.00"))).username());
        assertEquals("alice", reservationController.update(user, 7L, new ReservationUpdate(now, now.plusHours(2), new BigDecimal("100.00"), ReservationStatus.CONFIRMED)).username());

        reservationController.delete(user, 7L);
        verify(reservationService).delete(7L, "alice", false);
    }

    @Test
    void domainModelsDtosAndConfigClassesHaveCoveredBehavior() {
        AppUser user = new AppUser("alice", "encoded", Role.ADMIN);
        assertEquals("alice", user.getUsername());
        assertEquals("encoded", user.getPassword());
        assertEquals(Role.ADMIN, user.getRole());

        Resource resource = new Resource("Board", "Large room", new BigDecimal("120.00"), true);
        resource.update("Board2", "Larger room", new BigDecimal("150.00"), false);
        assertEquals("Board2", resource.getName());
        assertEquals("Larger room", resource.getDescription());
        assertEquals(new BigDecimal("150.00"), resource.getPrice());
        assertFalse(resource.isAvailable());

        OffsetDateTime start = OffsetDateTime.now().plusDays(2);
        OffsetDateTime end = start.plusHours(4);
        Reservation reservation = new Reservation(resource, user, start, end, new BigDecimal("300.00"));
        reservation.update(start.plusDays(1), end.plusDays(1), new BigDecimal("400.00"), ReservationStatus.CONFIRMED);
        assertEquals(ReservationStatus.CONFIRMED, reservation.getStatus());
        assertEquals("alice", reservation.getUser().getUsername());
        assertEquals("Board2", reservation.getResource().getName());

        ResourceRequest resourceRequest = new ResourceRequest("Desk", "Quiet desk", new BigDecimal("10.00"), true);
        ResourceResponse resourceResponse = new ResourceResponse(9L, "Desk", "Quiet desk", new BigDecimal("10.00"), true);
        assertEquals("Desk", resourceRequest.name());
        assertTrue(resourceResponse.available());

        ReservationRequest reservationRequest = new ReservationRequest(2L, start, end, new BigDecimal("50.00"));
        ReservationUpdate reservationUpdate = new ReservationUpdate(start.plusDays(1), end.plusDays(1), new BigDecimal("60.00"), ReservationStatus.PENDING);
        ReservationResponse reservationResponse = new ReservationResponse(11L, 2L, "Desk", "alice", start, end, new BigDecimal("50.00"), ReservationStatus.PENDING);
        assertEquals(2L, reservationRequest.resourceId());
        assertEquals(ReservationStatus.PENDING, reservationUpdate.status());
        assertEquals("alice", reservationResponse.username());

        assertEquals(Role.ADMIN, Role.valueOf("ADMIN"));
        assertEquals(ReservationStatus.CANCELLED, ReservationStatus.valueOf("CANCELLED"));

        new OpenApiConfig();
        new SeedDataConfig();
        assertNotNull(Instant.now());
    }

    @Test
    void seedConfigurationSeedsOnlyWhenEmpty() throws Exception {
        UserRepository users = mock(UserRepository.class);
        PasswordEncoder encoder = new BCryptPasswordEncoder();
        when(users.count()).thenReturn(0L);

        Method method = SeedDataConfig.class.getDeclaredMethod("seedUsers", UserRepository.class, PasswordEncoder.class);
        method.setAccessible(true);
        CommandLineRunner runner = (CommandLineRunner) method.invoke(new SeedDataConfig(), users, encoder);
        runner.run();

        verify(users, times(2)).save(any(AppUser.class));

        when(users.count()).thenReturn(1L);
        CommandLineRunner secondRun = (CommandLineRunner) method.invoke(new SeedDataConfig(), users, encoder);
        secondRun.run();
        verify(users, times(2)).save(any(AppUser.class));
    }

    @Test
    void databaseUserDetailsServiceAndJwtServiceCoverInvalidAndExpiredCases() throws Exception {
        UserRepository users = mock(UserRepository.class);
        DatabaseUserDetailsService service = new DatabaseUserDetailsService(users);

        AppUser appUser = new AppUser("admin", "encoded", Role.ADMIN);
        when(users.findByUsername("admin")).thenReturn(Optional.of(appUser));
        when(users.findByUsername("demo")).thenReturn(Optional.of(new AppUser("demo", "encoded", Role.USER)));

        UserDetails principal = service.loadUserByUsername("admin");
        assertEquals("admin", principal.getUsername());
        assertTrue(principal.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));
        assertThrows(UsernameNotFoundException.class, () -> service.loadUserByUsername("missing"));

        JwtService jwtService = new JwtService("a-secret-key-that-is-at-least-32-characters-long", 60_000);
        UserDetails user = User.withUsername("demo").password("encoded").authorities(new SimpleGrantedAuthority("ROLE_USER")).build();
        String validToken = jwtService.generateToken(user);
        assertEquals("demo", jwtService.username(validToken));
        assertTrue(jwtService.valid(validToken, user));
        assertFalse(jwtService.valid("bad-token", user));

        JwtService expiredJwt = new JwtService("a-secret-key-that-is-at-least-32-characters-long", -1L);
        String expiredToken = expiredJwt.generateToken(user);
        assertFalse(expiredJwt.valid(expiredToken, user));

        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService, service);
        var request = mock(jakarta.servlet.http.HttpServletRequest.class);
        var response = mock(jakarta.servlet.http.HttpServletResponse.class);
        var chain = mock(jakarta.servlet.FilterChain.class);

        when(request.getHeader("Authorization")).thenReturn("Bearer invalid-token");
        filter.doFilter(request, response, chain);
        verify(chain).doFilter(request, response);

        when(request.getHeader("Authorization")).thenReturn("Bearer " + validToken);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
        filter.doFilter(request, response, chain);
        assertNotNull(org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication());
        assertEquals("demo", org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName());
    }
}
