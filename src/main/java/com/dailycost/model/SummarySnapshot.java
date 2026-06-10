package com.dailycost.model;

import java.math.BigDecimal;
import java.util.List;

public record SummarySnapshot(
        int deviceCount,
        int accessoryCount,
        BigDecimal totalInvestment,
        BigDecimal totalDailyCost,
        BigDecimal equivalentMonthlyCost,
        BigDecimal equivalentAnnualCost,
        int achievedTargetCount,
        List<DeviceCostSnapshot> devices
) {
}
