package com.dailycost.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TargetPlanSnapshot(
        BigDecimal targetDailyCost,
        BigDecimal remainingDays,
        LocalDate estimatedDate,
        BigDecimal thirtyDayDecrease,
        BigDecimal marginalDecreasePerDay,
        int replacementIndex,
        boolean achieved,
        String advice
) {
}
