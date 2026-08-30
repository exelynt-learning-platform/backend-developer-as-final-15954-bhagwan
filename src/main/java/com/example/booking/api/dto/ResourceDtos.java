package com.example.booking.api.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public final class ResourceDtos {
    private ResourceDtos() {}
    public record ResourceRequest(@NotBlank String name, @NotBlank String description, @NotNull @PositiveOrZero BigDecimal price, boolean available) {}
    public record ResourceResponse(Long id, String name, String description, BigDecimal price, boolean available) {}
}