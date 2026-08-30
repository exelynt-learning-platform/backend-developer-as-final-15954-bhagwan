package com.example.booking.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.example.booking.api.dto.ReservationDtos.*;
import com.example.booking.domain.*;
import com.example.booking.repository.*;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;

class ReservationServiceTest {
    private final ReservationRepository reservations = mock(ReservationRepository.class);
    private final ResourceRepository resources = mock(ResourceRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final ReservationService service = new ReservationService(reservations, resources, users);

    @Test
    void rejectsAnEndTimeBeforeStartTime() {
        OffsetDateTime start = OffsetDateTime.now().plusDays(1);
        ReservationRequest request = new ReservationRequest(1L, start, start.minusHours(1), BigDecimal.TEN);
        assertThrows(IllegalArgumentException.class, () -> service.create("user", request));
    }

    @Test
    void findReturnsFilteredResultsForOwner() {
        OffsetDateTime start = OffsetDateTime.now().plusDays(2);
        Resource resource = new Resource("Meeting Room", "Large room", new BigDecimal("120.00"), true);
        AppUser user = new AppUser("alice", "encoded", Role.USER);
        Reservation reservation = new Reservation(resource, user, start, start.plusHours(2), new BigDecimal("240.00"));
        PageRequest page = PageRequest.of(0, 20);

        when(reservations.findAll(any(Specification.class), eq(page))).thenReturn(new PageImpl<>(List.of(reservation), page, 1));

        Page<ReservationResponse> result = service.find("alice", false, ReservationStatus.PENDING, new BigDecimal("200.00"), new BigDecimal("300.00"), page);

        assertEquals(1, result.getTotalElements());
        assertEquals("alice", result.getContent().get(0).username());
        assertEquals(ReservationStatus.PENDING, result.getContent().get(0).status());
    }

    @Test
    void getRejectsReservationThatDoesNotBelongToUser() {
        OffsetDateTime start = OffsetDateTime.now().plusDays(3);
        Resource resource = new Resource("Desk", "Quiet workstation", new BigDecimal("30.00"), true);
        AppUser user = new AppUser("bob", "encoded", Role.USER);
        Reservation reservation = new Reservation(resource, user, start, start.plusHours(1), new BigDecimal("30.00"));

        when(reservations.findById(7L)).thenReturn(Optional.of(reservation));

        assertThrows(AccessDeniedException.class, () -> service.get(7L, "alice", false));
    }

    @Test
    void createSavesReservationWhenResourceAndUserExist() {
        OffsetDateTime start = OffsetDateTime.now().plusDays(4);
        Resource resource = new Resource("Desk", "Quiet workstation", new BigDecimal("30.00"), true);
        AppUser user = new AppUser("alice", "encoded", Role.USER);
        ReservationRequest request = new ReservationRequest(5L, start, start.plusHours(2), new BigDecimal("60.00"));

        when(resources.findById(5L)).thenReturn(Optional.of(resource));
        when(users.findByUsername("alice")).thenReturn(Optional.of(user));
        when(reservations.save(any(Reservation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ReservationResponse response = service.create("alice", request);

        assertEquals("alice", response.username());
        assertEquals("Desk", response.resourceName());
        assertEquals(new BigDecimal("60.00"), response.price());
    }

    @Test
    void createRejectsUnavailableResource() {
        OffsetDateTime start = OffsetDateTime.now().plusDays(5);
        Resource resource = new Resource("Desk", "Quiet workstation", new BigDecimal("30.00"), false);
        ReservationRequest request = new ReservationRequest(8L, start, start.plusHours(1), new BigDecimal("30.00"));

        when(resources.findById(8L)).thenReturn(Optional.of(resource));

        assertThrows(IllegalArgumentException.class, () -> service.create("alice", request));
    }

    @Test
    void updatePersistsGuestReservationChanges() {
        OffsetDateTime start = OffsetDateTime.now().plusDays(6);
        Resource resource = new Resource("Board", "Board room", new BigDecimal("80.00"), true);
        AppUser user = new AppUser("alice", "encoded", Role.USER);
        Reservation reservation = new Reservation(resource, user, start, start.plusHours(1), new BigDecimal("80.00"));
        ReservationUpdate update = new ReservationUpdate(start.plusDays(1), start.plusDays(1).plusHours(2), new BigDecimal("200.00"), ReservationStatus.CONFIRMED);

        when(reservations.findById(10L)).thenReturn(Optional.of(reservation));
        when(reservations.save(reservation)).thenReturn(reservation);

        ReservationResponse response = service.update(10L, "alice", false, update);

        assertEquals(ReservationStatus.CONFIRMED, response.status());
        assertEquals(new BigDecimal("200.00"), response.price());
    }

    @Test
    void deleteRemovesOwnedReservation() {
        OffsetDateTime start = OffsetDateTime.now().plusDays(7);
        Resource resource = new Resource("Cabin", "Private cabin", new BigDecimal("90.00"), true);
        AppUser user = new AppUser("alice", "encoded", Role.USER);
        Reservation reservation = new Reservation(resource, user, start, start.plusHours(3), new BigDecimal("270.00"));

        when(reservations.findById(11L)).thenReturn(Optional.of(reservation));

        assertDoesNotThrow(() -> service.delete(11L, "alice", false));
        verify(reservations).delete(reservation);
    }

    @Test
    void missingResourceThrowsNotFoundDuringCreate() {
        OffsetDateTime start = OffsetDateTime.now().plusDays(8);
        ReservationRequest request = new ReservationRequest(99L, start, start.plusHours(1), new BigDecimal("50.00"));

        when(resources.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> service.create("alice", request));
    }
}