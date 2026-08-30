package com.example.booking.api;

import com.example.booking.api.dto.ReservationDtos.*;
import com.example.booking.domain.ReservationStatus;
import com.example.booking.service.ReservationService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/reservations") @SecurityRequirement(name = "bearerAuth")
public class ReservationController {
    private final ReservationService service;
    public ReservationController(ReservationService service) { this.service = service; }
    @GetMapping public Page<ReservationResponse> find(Authentication auth, @RequestParam(required = false) ReservationStatus status, @RequestParam(required = false) BigDecimal minPrice, @RequestParam(required = false) BigDecimal maxPrice, @PageableDefault(size = 20, sort = "startTime") Pageable pageable) { return service.find(auth.getName(), isAdmin(auth), status, minPrice, maxPrice, pageable); }
    @GetMapping("/{id}") public ReservationResponse get(Authentication auth, @PathVariable Long id) { return service.get(id, auth.getName(), isAdmin(auth)); }
    @PostMapping @ResponseStatus(HttpStatus.CREATED) public ReservationResponse create(Authentication auth, @Valid @RequestBody ReservationRequest request) { return service.create(auth.getName(), request); }
    @PutMapping("/{id}") public ReservationResponse update(Authentication auth, @PathVariable Long id, @Valid @RequestBody ReservationUpdate request) { return service.update(id, auth.getName(), isAdmin(auth), request); }
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void delete(Authentication auth, @PathVariable Long id) { service.delete(id, auth.getName(), isAdmin(auth)); }
    private boolean isAdmin(Authentication auth) { return auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")); }
}