package com.example.booking.api;

import com.example.booking.api.dto.ResourceDtos.*;
import com.example.booking.service.ResourceService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/resources") @SecurityRequirement(name = "bearerAuth")
public class ResourceController {
    private final ResourceService service;
    public ResourceController(ResourceService service) { this.service = service; }
    @GetMapping public List<ResourceResponse> all() { return service.all(); }
    @GetMapping("/{id}") public ResourceResponse get(@PathVariable Long id) { return service.get(id); }
    @PostMapping @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("hasRole('ADMIN')") public ResourceResponse create(@Valid @RequestBody ResourceRequest request) { return service.create(request); }
    @PutMapping("/{id}") @PreAuthorize("hasRole('ADMIN')") public ResourceResponse update(@PathVariable Long id, @Valid @RequestBody ResourceRequest request) { return service.update(id, request); }
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) @PreAuthorize("hasRole('ADMIN')") public void delete(@PathVariable Long id) { service.delete(id); }
}