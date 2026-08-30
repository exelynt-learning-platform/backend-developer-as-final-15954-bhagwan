package com.example.booking.service;

import com.example.booking.api.dto.ReservationDtos.*;
import com.example.booking.domain.*;
import com.example.booking.repository.*;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

@Service
public class ReservationService {
    private final ReservationRepository reservations;
    private final ResourceRepository resources;
    private final UserRepository users;

    public ReservationService(ReservationRepository reservations, ResourceRepository resources, UserRepository users) {
        this.reservations = reservations;
        this.resources = resources;
        this.users = users;
    }

    public Page<ReservationResponse> find(
        String username,
        boolean admin,
        ReservationStatus status,
        BigDecimal min,
        BigDecimal max,
        Pageable page) {
        Specification<Reservation> spec = (root, query, cb) -> cb.conjunction();
        if (!admin) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("user").get("username"), username));
        }
        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }
        if (min != null) {
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("price"), min));
        }
        if (max != null) {
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("price"), max));
        }
        return reservations.findAll(spec, page).map(this::response);
    }

    public ReservationResponse get(Long id, String username, boolean admin) {
        Reservation reservation = findEntity(id);
        checkOwner(reservation, username, admin);
        return response(reservation);
    }

    public ReservationResponse create(String username, ReservationRequest request) {
        validateTimes(request.startTime(), request.endTime());
        Resource resource = resources.findById(request.resourceId())
            .orElseThrow(() -> new EntityNotFoundException("Resource not found: " + request.resourceId()));
        if (!resource.isAvailable()) {
            throw new IllegalArgumentException("Resource is unavailable");
        }
        AppUser user = users.findByUsername(username)
            .orElseThrow(() -> new EntityNotFoundException("User not found"));
        return response(reservations.save(new Reservation(resource, user, request.startTime(), request.endTime(), request.price())));
    }

    public ReservationResponse update(Long id, String username, boolean admin, ReservationUpdate request) {
        validateTimes(request.startTime(), request.endTime());
        Reservation reservation = findEntity(id);
        checkOwner(reservation, username, admin);
        reservation.update(request.startTime(), request.endTime(), request.price(), request.status());
        return response(reservations.save(reservation));
    }

    public void delete(Long id, String username, boolean admin) {
        Reservation reservation = findEntity(id);
        checkOwner(reservation, username, admin);
        reservations.delete(reservation);
    }

    private Reservation findEntity(Long id) {
        return reservations.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Reservation not found: " + id));
    }

    private void checkOwner(Reservation reservation, String username, boolean admin) {
        if (!admin && !reservation.getUser().getUsername().equals(username)) {
            throw new AccessDeniedException("Reservation does not belong to the current user");
        }
    }

    private void validateTimes(OffsetDateTime start, OffsetDateTime end) {
        if (!end.isAfter(start)) {
            throw new IllegalArgumentException("endTime must be after startTime");
        }
    }

    private ReservationResponse response(Reservation reservation) {
        return new ReservationResponse(
            reservation.getId(),
            reservation.getResource().getId(),
            reservation.getResource().getName(),
            reservation.getUser().getUsername(),
            reservation.getStartTime(),
            reservation.getEndTime(),
            reservation.getPrice(),
            reservation.getStatus());
    }
}