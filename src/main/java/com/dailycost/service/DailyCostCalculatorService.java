package com.dailycost.service;

import com.dailycost.model.Accessory;
import com.dailycost.model.AccessoryCostSnapshot;
import com.dailycost.model.AppData;
import com.dailycost.model.Device;
import com.dailycost.model.DeviceCostSnapshot;
import com.dailycost.model.SummarySnapshot;
import com.dailycost.model.TargetPlanSnapshot;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class DailyCostCalculatorService {
    private static final int SCALE = 10;
    private static final BigDecimal MONTH_DAYS = new BigDecimal("30.4167");
    private static final BigDecimal YEAR_DAYS = new BigDecimal("365");
    private static final BigDecimal THIRTY_DAYS = new BigDecimal("30");
    private static final BigDecimal ONE_DAY = BigDecimal.ONE;
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

    public long calculateUsedDays(LocalDate purchaseDate, LocalDate currentDate) {
        LocalDate safeCurrentDate = currentDate == null ? LocalDate.now() : currentDate;
        LocalDate safePurchaseDate = purchaseDate == null ? safeCurrentDate : purchaseDate;
        long days = ChronoUnit.DAYS.between(safePurchaseDate, safeCurrentDate) + 1;
        return Math.max(1, days);
    }

    public BigDecimal calculateDailyCost(BigDecimal price, long usedDays) {
        BigDecimal safePrice = price == null ? BigDecimal.ZERO : price;
        long safeDays = Math.max(1, usedDays);
        return safePrice.divide(BigDecimal.valueOf(safeDays), SCALE, RoundingMode.HALF_UP);
    }

    public AccessoryCostSnapshot calculateAccessory(Accessory accessory, LocalDate currentDate) {
        long usedDays = calculateUsedDays(accessory.getPurchaseDate(), currentDate);
        BigDecimal dailyCost = calculateDailyCost(accessory.getPrice(), usedDays);
        return new AccessoryCostSnapshot(
                accessory.getId(),
                accessory.getName(),
                accessory.getPrice(),
                accessory.getPurchaseDate(),
                usedDays,
                dailyCost
        );
    }

    public DeviceCostSnapshot calculateDevice(Device device, LocalDate currentDate) {
        long baseUsedDays = calculateUsedDays(device.getPurchaseDate(), currentDate);
        BigDecimal baseDailyCost = calculateDailyCost(device.getBasePrice(), baseUsedDays);

        List<AccessoryCostSnapshot> accessorySnapshots = new ArrayList<>();
        BigDecimal accessoriesInvestment = BigDecimal.ZERO;
        BigDecimal accessoriesDailyCost = BigDecimal.ZERO;
        for (Accessory accessory : device.getAccessories()) {
            AccessoryCostSnapshot snapshot = calculateAccessory(accessory, currentDate);
            accessorySnapshots.add(snapshot);
            accessoriesInvestment = accessoriesInvestment.add(snapshot.price());
            accessoriesDailyCost = accessoriesDailyCost.add(snapshot.dailyCost());
        }

        BigDecimal totalInvestment = device.getBasePrice().add(accessoriesInvestment);
        BigDecimal currentDailyCost = baseDailyCost.add(accessoriesDailyCost);
        BigDecimal weightedUsedDays = divideOrZero(totalInvestment, currentDailyCost);
        TargetPlanSnapshot targetPlan = calculateTargetPlan(
                totalInvestment,
                currentDailyCost,
                weightedUsedDays,
                device.getTargetDailyCost(),
                currentDate
        );

        return new DeviceCostSnapshot(
                device.getId(),
                device.getName(),
                device.getBasePrice(),
                device.getPurchaseDate(),
                baseUsedDays,
                baseDailyCost,
                accessoriesInvestment,
                accessoriesDailyCost,
                totalInvestment,
                currentDailyCost,
                weightedUsedDays,
                device.getTargetDailyCost(),
                targetPlan,
                List.copyOf(accessorySnapshots)
        );
    }

    public TargetPlanSnapshot calculateTargetPlan(
            BigDecimal totalInvestment,
            BigDecimal currentDailyCost,
            BigDecimal weightedUsedDays,
            BigDecimal targetDailyCost,
            LocalDate currentDate
    ) {
        BigDecimal safeTarget = positiveOrDefault(targetDailyCost, BigDecimal.TEN);
        BigDecimal safeTotal = totalInvestment == null ? BigDecimal.ZERO : totalInvestment;
        BigDecimal safeCurrent = currentDailyCost == null ? BigDecimal.ZERO : currentDailyCost;
        BigDecimal safeWeightedDays = weightedUsedDays == null ? BigDecimal.ZERO : weightedUsedDays;
        LocalDate safeDate = currentDate == null ? LocalDate.now() : currentDate;

        boolean achieved = safeCurrent.compareTo(BigDecimal.ZERO) <= 0 || safeCurrent.compareTo(safeTarget) <= 0;
        BigDecimal targetRequiredDays = divideOrZero(safeTotal, safeTarget);
        BigDecimal remainingDays = achieved ? BigDecimal.ZERO : targetRequiredDays.subtract(safeWeightedDays).max(BigDecimal.ZERO);
        LocalDate estimatedDate = safeDate.plusDays(ceilToLong(remainingDays));

        BigDecimal thirtyDayFutureDailyCost = divideOrZero(safeTotal, safeWeightedDays.add(THIRTY_DAYS));
        BigDecimal thirtyDayDecrease = safeCurrent.subtract(thirtyDayFutureDailyCost).max(BigDecimal.ZERO);
        BigDecimal tomorrowDailyCost = divideOrZero(safeTotal, safeWeightedDays.add(ONE_DAY));
        BigDecimal marginalDecrease = safeCurrent.subtract(tomorrowDailyCost).max(BigDecimal.ZERO);
        int replacementIndex = calculateReplacementIndex(safeCurrent, safeTarget);
        String advice = buildAdvice(achieved, replacementIndex);

        return new TargetPlanSnapshot(
                safeTarget,
                remainingDays,
                estimatedDate,
                thirtyDayDecrease,
                marginalDecrease,
                replacementIndex,
                achieved,
                advice
        );
    }

    public SummarySnapshot calculateSummary(AppData appData, LocalDate currentDate) {
        List<DeviceCostSnapshot> devices = appData.getDevices().stream()
                .sorted(Comparator.comparingInt(Device::getSortOrder))
                .map(device -> calculateDevice(device, currentDate))
                .toList();

        BigDecimal totalInvestment = BigDecimal.ZERO;
        BigDecimal totalDailyCost = BigDecimal.ZERO;
        int accessoryCount = 0;
        int achievedTargetCount = 0;
        for (DeviceCostSnapshot device : devices) {
            totalInvestment = totalInvestment.add(device.totalInvestment());
            totalDailyCost = totalDailyCost.add(device.currentDailyCost());
            accessoryCount += device.accessories().size();
            if (device.targetPlan().achieved()) {
                achievedTargetCount++;
            }
        }

        return new SummarySnapshot(
                devices.size(),
                accessoryCount,
                totalInvestment,
                totalDailyCost,
                totalDailyCost.multiply(MONTH_DAYS),
                totalDailyCost.multiply(YEAR_DAYS),
                achievedTargetCount,
                devices
        );
    }

    public int calculateReplacementIndex(BigDecimal currentDailyCost, BigDecimal targetDailyCost) {
        BigDecimal safeCurrent = currentDailyCost == null ? BigDecimal.ZERO : currentDailyCost;
        BigDecimal safeTarget = positiveOrDefault(targetDailyCost, BigDecimal.TEN);
        if (safeCurrent.compareTo(BigDecimal.ZERO) <= 0 || safeCurrent.compareTo(safeTarget) <= 0) {
            return 100;
        }
        BigDecimal score = safeTarget.multiply(ONE_HUNDRED)
                .divide(safeCurrent, SCALE, RoundingMode.HALF_UP)
                .setScale(0, RoundingMode.FLOOR);
        int value = score.intValue();
        return Math.max(1, Math.min(99, value));
    }

    private BigDecimal divideOrZero(BigDecimal dividend, BigDecimal divisor) {
        if (dividend == null || divisor == null || divisor.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return dividend.divide(divisor, SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal positiveOrDefault(BigDecimal value, BigDecimal fallback) {
        return value == null || value.compareTo(BigDecimal.ZERO) <= 0 ? fallback : value;
    }

    private long ceilToLong(BigDecimal value) {
        return value.max(BigDecimal.ZERO).setScale(0, RoundingMode.CEILING).longValue();
    }

    private String buildAdvice(boolean achieved, int replacementIndex) {
        if (achieved) {
            return "已达到目标，可考虑换新";
        }
        if (replacementIndex >= 70) {
            return "接近目标，可观望换新";
        }
        return "继续摊薄更划算";
    }
}
