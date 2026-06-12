package com.dailycost.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record DeviceCostSnapshot(
        String id,
        String name,
        BigDecimal basePrice,
        LocalDate purchaseDate,
        long baseUsedDays,
        BigDecimal baseDailyCost,
        BigDecimal accessoriesInvestment,
        BigDecimal accessoriesDailyCost,
        BigDecimal totalInvestment,
        BigDecimal currentDailyCost,
        BigDecimal weightedUsedDays,
        BigDecimal targetDailyCost,
        TargetPlanSnapshot targetPlan,
        boolean replaced,
        LocalDate replacementDate,
        List<AccessoryCostSnapshot> accessories
) {
}
