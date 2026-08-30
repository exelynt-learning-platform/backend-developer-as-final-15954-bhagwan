package com.example.booking.service;

import com.example.booking.api.dto.ResourceDtos.*;
import com.example.booking.domain.Resource;
import com.example.booking.repository.ResourceRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ResourceService {
    private final ResourceRepository resources;
    public ResourceService(ResourceRepository resources) { this.resources = resources; }
    public List<ResourceResponse> all() { return resources.findAll().stream().map(this::response).toList(); }
    public ResourceResponse get(Long id) { return response(resources.findById(id).orElseThrow(() -> new EntityNotFoundException("Resource not found: " + id))); }
    public ResourceResponse create(ResourceRequest request) { return response(resources.save(new Resource(request.name(), request.description(), request.price(), request.available()))); }
    public ResourceResponse update(Long id, ResourceRequest request) { Resource resource = resources.findById(id).orElseThrow(() -> new EntityNotFoundException("Resource not found: " + id)); resource.update(request.name(), request.description(), request.price(), request.available()); return response(resources.save(resource)); }
    public void delete(Long id) { if (!resources.existsById(id)) throw new EntityNotFoundException("Resource not found: " + id); resources.deleteById(id); }
    private ResourceResponse response(Resource r) { return new ResourceResponse(r.getId(), r.getName(), r.getDescription(), r.getPrice(), r.isAvailable()); }
}