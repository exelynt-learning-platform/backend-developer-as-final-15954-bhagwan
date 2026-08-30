package com.example.booking.repository;

import com.example.booking.domain.Reservation;
import com.example.booking.domain.ReservationStatus;
import java.math.BigDecimal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ReservationRepository extends JpaRepository<Reservation, Long>, JpaSpecificationExecutor<Reservation> {
    Page<Reservation> findByUserUsername(String username, Pageable pageable);
    Page<Reservation> findByStatusAndPriceGreaterThanEqualAndPriceLessThanEqual(ReservationStatus status, BigDecimal min, BigDecimal max, Pageable pageable);
}