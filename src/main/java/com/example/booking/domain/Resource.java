package com.example.booking.domain;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "resources")
public class Resource {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) 
    private Long id;
    @Column(nullable = false) private 
    String name;
    @Column(nullable = false, length = 2000) 
    private String description;
    @Column(nullable = false, precision = 12, scale = 2) 
    private BigDecimal price;
    @Column(nullable = false)
     private boolean available = true;
    protected Resource() {}
    public Resource(String name, String description, BigDecimal price, boolean available) { 
        this.name = name; 
        this.description = description; 
        this.price = price; 
        this.available = available; }
    public Long getId() { 
        return id; 
    }
    public String getName() { 
        return name; 
    }
    public String getDescription() { 
        return description; 
    }
    public BigDecimal getPrice() { 
        return price; 
    }
    public boolean isAvailable() { 
        return available;
     }
    public void update(String name, String description, BigDecimal price, boolean available) { 
        this.name = name; 
        this.description = description; 
        this.price = price; 
        this.available = available; }
}