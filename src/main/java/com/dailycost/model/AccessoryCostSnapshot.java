package com.dailycost.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AccessoryCostSnapshot(
        String id,
        String name,
        BigDecimal price,
        LocalDate purchaseDate,
        long usedDays,
        BigDecimal dailyCost
) {
}
