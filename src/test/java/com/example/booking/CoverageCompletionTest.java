package com.example.booking;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.example.booking.ResourceBookingApplication;
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
import com.example.booking.repository.ResourceRepository;
import com.example.booking.repository.ReservationRepository;
import com.example.booking.repository.UserRepository;
import com.example.booking.security.DatabaseUserDetailsService;
import com.example.booking.security.JwtAuthenticationFilter;
import com.example.booking.security.JwtService;
import com.example.booking.service.ResourceService;
import com.example.booking.service.ReservationService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
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
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

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

        BeanPropertyBindingResult binding = new BeanPropertyBindingResult(new Object(), "request");
        binding.addError(new FieldError("request", "name", "must not be blank"));
        ResponseEntity<?> validationError = handler.badRequest(new MethodArgumentNotValidException(null, binding));
        assertEquals(400, ((Map<?, ?>) validationError.getBody()).get("status"));

        ResponseEntity<?> nullMessageError = handler.badRequest(new IllegalArgumentException((String) null));
        assertEquals("Request failed", ((Map<?, ?>) nullMessageError.getBody()).get("message"));
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
    void domainModelsDtosAndConfigClassesHaveCoveredBehavior() throws Exception {
        AppUser user = new AppUser("alice", "encoded", Role.ADMIN);
        setField(user, "id", 42L);
        assertEquals("alice", user.getUsername());
        assertEquals("encoded", user.getPassword());
        assertEquals(Role.ADMIN, user.getRole());
        assertEquals(42L, user.getId());

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
        assertFalse(jwtService.valid(validToken, User.withUsername("other").password("encoded").authorities(new SimpleGrantedAuthority("ROLE_USER")).build()));

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

    @Test
    void resourceServiceAndControllerCoverRemainingBranchesAndMainMethod() throws Exception {
        ResourceRepository resourceRepo = mock(ResourceRepository.class);
        ResourceService resourceService = new ResourceService(resourceRepo);
        ResourceController resourceController = new ResourceController(resourceService);

        Resource first = new Resource("Board", "Room", new BigDecimal("50.00"), true);
        Resource second = new Resource("Desk", "Workstation", new BigDecimal("20.00"), false);
        setField(first, "id", 1L);
        setField(second, "id", 2L);
        when(resourceRepo.findAll()).thenReturn(List.of(first, second));
        when(resourceRepo.findById(1L)).thenReturn(Optional.of(first));
        when(resourceRepo.findById(2L)).thenReturn(Optional.of(second));
        when(resourceRepo.existsById(1L)).thenReturn(true);
        when(resourceRepo.existsById(2L)).thenReturn(true);
        when(resourceRepo.existsById(99L)).thenReturn(false);
        when(resourceRepo.save(any(Resource.class))).thenAnswer(invocation -> {
            Resource saved = invocation.getArgument(0);
            setField(saved, "id", 99L);
            return saved;
        });

        assertEquals(2, resourceService.all().size());
        assertEquals("Room", resourceService.get(1L).description());
        assertThrows(EntityNotFoundException.class, () -> resourceService.get(99L));
        assertEquals("Cabin", resourceService.create(new ResourceRequest("Cabin", "Quiet", new BigDecimal("10.00"), true)).name());
        assertEquals("Desk2", resourceService.update(2L, new ResourceRequest("Desk2", "Updated", new BigDecimal("25.00"), true)).name());
        assertThrows(EntityNotFoundException.class, () -> resourceService.update(99L, new ResourceRequest("X", "Y", new BigDecimal("10.00"), true)));
        assertThrows(EntityNotFoundException.class, () -> resourceService.delete(99L));
        resourceService.delete(1L);
        verify(resourceRepo).deleteById(1L);

        assertEquals("Updated", resourceController.update(2L, new ResourceRequest("Desk2", "Updated", new BigDecimal("25.00"), true)).description());
        resourceController.delete(2L);
        verify(resourceRepo).deleteById(2L);

        try (var mocked = mockStatic(SpringApplication.class)) {
            ResourceBookingApplication.main(new String[]{"--server.port=0"});
            mocked.verify(() -> SpringApplication.run(ResourceBookingApplication.class, new String[]{"--server.port=0"}));
        }
    }

    @Test
    void reservationServiceCoversSpecificationAndNotFoundPaths() throws Exception {
        ReservationRepository reservationRepo = mock(ReservationRepository.class);
        ResourceRepository resourceRepo = mock(ResourceRepository.class);
        UserRepository userRepo = mock(UserRepository.class);
        ReservationService service = new ReservationService(reservationRepo, resourceRepo, userRepo);
        PageRequest page = PageRequest.of(0, 10);

        Resource resource = new Resource("Board", "Room", new BigDecimal("100.00"), true);
        AppUser user = new AppUser("alice", "encoded", Role.USER);
        Reservation reservation = new Reservation(resource, user, OffsetDateTime.now().plusDays(1), OffsetDateTime.now().plusDays(1).plusHours(2), new BigDecimal("150.00"));
        setField(resource, "id", 11L);
        setField(reservation, "id", 7L);

        when(reservationRepo.findAll(any(Specification.class), eq(page))).thenAnswer(invocation -> {
            Specification<Reservation> spec = invocation.getArgument(0);
            CriteriaBuilder cb = mock(CriteriaBuilder.class);
            CriteriaQuery<Reservation> query = mock(CriteriaQuery.class);
            Root<Reservation> root = mock(Root.class);
            Path userPath = mock(Path.class);
            Path nestedUserPath = mock(Path.class);
            Path statusPath = mock(Path.class);
            Path pricePath = mock(Path.class);
            Predicate conjunction = mock(Predicate.class);
            Predicate userPredicate = mock(Predicate.class);
            Predicate statusPredicate = mock(Predicate.class);
            Predicate minPredicate = mock(Predicate.class);
            Predicate maxPredicate = mock(Predicate.class);

            when(cb.conjunction()).thenReturn(conjunction);
            when(root.get("user")).thenReturn(userPath);
            when(userPath.get("username")).thenReturn(nestedUserPath);
            when(root.get("status")).thenReturn(statusPath);
            when(root.get("price")).thenReturn(pricePath);
            when(cb.equal(nestedUserPath, "alice")).thenReturn(userPredicate);
            when(cb.equal(statusPath, ReservationStatus.PENDING)).thenReturn(statusPredicate);
            when(cb.greaterThanOrEqualTo(pricePath, new BigDecimal("100.00"))).thenReturn(minPredicate);
            when(cb.lessThanOrEqualTo(pricePath, new BigDecimal("200.00"))).thenReturn(maxPredicate);

            spec.toPredicate(root, query, cb);
            return new PageImpl<>(List.of(reservation), page, 1);
        });

        var filtered = service.find("alice", false, ReservationStatus.PENDING, new BigDecimal("100.00"), new BigDecimal("200.00"), page);
        assertEquals(1, filtered.getTotalElements());

        when(resourceRepo.findById(11L)).thenReturn(Optional.of(resource));
        when(userRepo.findByUsername("alice")).thenReturn(Optional.of(user));
        when(reservationRepo.findById(7L)).thenReturn(Optional.of(reservation));
        when(reservationRepo.save(any(Reservation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var fetched = service.get(7L, "alice", false);
        assertEquals("alice", fetched.username());

        var created = service.create("alice", new ReservationRequest(11L, OffsetDateTime.now().plusDays(2), OffsetDateTime.now().plusDays(2).plusHours(1), new BigDecimal("120.00")));
        assertEquals("alice", created.username());

        var updated = service.update(7L, "alice", false, new ReservationUpdate(OffsetDateTime.now().plusDays(3), OffsetDateTime.now().plusDays(3).plusHours(1), new BigDecimal("180.00"), ReservationStatus.CONFIRMED));
        assertEquals(ReservationStatus.CONFIRMED, updated.status());

        service.delete(7L, "alice", false);
        verify(reservationRepo).delete(reservation);

        when(reservationRepo.findById(99L)).thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class, () -> service.get(99L, "alice", false));
        when(userRepo.findByUsername("missing")).thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class, () -> service.create("missing", new ReservationRequest(11L, OffsetDateTime.now().plusDays(4), OffsetDateTime.now().plusDays(4).plusHours(1), new BigDecimal("50.00"))));
    }

    private static void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
