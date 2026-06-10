package com.dailycost.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public class Accessory {
    private String id;
    private String name;
    private BigDecimal price;
    private LocalDate purchaseDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Accessory() {
        this.id = UUID.randomUUID().toString();
        this.name = "";
        this.price = BigDecimal.ZERO;
        this.purchaseDate = LocalDate.now();
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }

    public Accessory(String name, BigDecimal price, LocalDate purchaseDate) {
        this();
        this.name = name;
        this.price = price == null ? BigDecimal.ZERO : price;
        this.purchaseDate = purchaseDate == null ? LocalDate.now() : purchaseDate;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BigDecimal getPrice() {
        return price == null ? BigDecimal.ZERO : price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price == null ? BigDecimal.ZERO : price;
    }

    public LocalDate getPurchaseDate() {
        return purchaseDate == null ? LocalDate.now() : purchaseDate;
    }

    public void setPurchaseDate(LocalDate purchaseDate) {
        this.purchaseDate = purchaseDate == null ? LocalDate.now() : purchaseDate;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
