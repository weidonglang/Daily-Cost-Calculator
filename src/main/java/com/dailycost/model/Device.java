package com.dailycost.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Device {
    private String id;
    private String name;
    private BigDecimal basePrice;
    private LocalDate purchaseDate;
    private BigDecimal targetDailyCost;
    private List<Accessory> accessories;
    private int sortOrder;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Device() {
        this.id = UUID.randomUUID().toString();
        this.name = "";
        this.basePrice = BigDecimal.ZERO;
        this.purchaseDate = LocalDate.now();
        this.targetDailyCost = BigDecimal.TEN;
        this.accessories = new ArrayList<>();
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }

    public Device(String name, BigDecimal basePrice, LocalDate purchaseDate, BigDecimal targetDailyCost) {
        this();
        this.name = name;
        this.basePrice = basePrice == null ? BigDecimal.ZERO : basePrice;
        this.purchaseDate = purchaseDate == null ? LocalDate.now() : purchaseDate;
        this.targetDailyCost = targetDailyCost == null ? BigDecimal.TEN : targetDailyCost;
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

    public BigDecimal getBasePrice() {
        return basePrice == null ? BigDecimal.ZERO : basePrice;
    }

    public void setBasePrice(BigDecimal basePrice) {
        this.basePrice = basePrice == null ? BigDecimal.ZERO : basePrice;
    }

    public LocalDate getPurchaseDate() {
        return purchaseDate == null ? LocalDate.now() : purchaseDate;
    }

    public void setPurchaseDate(LocalDate purchaseDate) {
        this.purchaseDate = purchaseDate == null ? LocalDate.now() : purchaseDate;
    }

    public BigDecimal getTargetDailyCost() {
        return targetDailyCost == null ? BigDecimal.TEN : targetDailyCost;
    }

    public void setTargetDailyCost(BigDecimal targetDailyCost) {
        this.targetDailyCost = targetDailyCost == null ? BigDecimal.TEN : targetDailyCost;
    }

    public List<Accessory> getAccessories() {
        if (accessories == null) {
            accessories = new ArrayList<>();
        }
        return accessories;
    }

    public void setAccessories(List<Accessory> accessories) {
        this.accessories = accessories == null ? new ArrayList<>() : accessories;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
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
