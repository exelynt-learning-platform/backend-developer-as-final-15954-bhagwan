package com.example.booking.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "reservations")
public class Reservation {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(optional = false, fetch = FetchType.LAZY) private Resource resource;
    @ManyToOne(optional = false, fetch = FetchType.LAZY) private AppUser user;
    @Column(nullable = false) private OffsetDateTime startTime;
    @Column(nullable = false) private OffsetDateTime endTime;
    @Column(nullable = false, precision = 12, scale = 2) private BigDecimal price;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private ReservationStatus status = ReservationStatus.PENDING;
    protected Reservation() {}
    public Reservation(Resource resource, AppUser user, OffsetDateTime startTime, OffsetDateTime endTime, BigDecimal price) { this.resource = resource; this.user = user; this.startTime = startTime; this.endTime = endTime; this.price = price; }
    public Long getId() { return id; }
    public Resource getResource() { return resource; }
    public AppUser getUser() { return user; }
    public OffsetDateTime getStartTime() { return startTime; }
    public OffsetDateTime getEndTime() { return endTime; }
    public BigDecimal getPrice() { return price; }
    public ReservationStatus getStatus() { return status; }
    public void update(OffsetDateTime startTime, OffsetDateTime endTime, BigDecimal price, ReservationStatus status) { this.startTime = startTime; this.endTime = endTime; this.price = price; this.status = status; }
}