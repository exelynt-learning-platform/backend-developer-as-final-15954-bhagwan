package com.example.booking.api.dto;

import com.example.booking.domain.ReservationStatus;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

public final class ReservationDtos {
    private ReservationDtos() {}
    public record ReservationRequest(@NotNull Long resourceId, @NotNull @FutureOrPresent OffsetDateTime startTime, @NotNull @Future OffsetDateTime endTime, @NotNull @PositiveOrZero BigDecimal price) {}
    public record ReservationUpdate(@NotNull @FutureOrPresent OffsetDateTime startTime, @NotNull OffsetDateTime endTime, @NotNull @PositiveOrZero BigDecimal price, @NotNull ReservationStatus status) {}
    public record ReservationResponse(Long id, Long resourceId, String resourceName, String username, OffsetDateTime startTime, OffsetDateTime endTime, BigDecimal price, ReservationStatus status) {}
}