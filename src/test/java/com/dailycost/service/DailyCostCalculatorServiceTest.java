package com.dailycost.service;

import com.dailycost.model.Accessory;
import com.dailycost.model.AppData;
import com.dailycost.model.Device;
import com.dailycost.model.DeviceCostSnapshot;
import com.dailycost.model.SummarySnapshot;
import com.dailycost.model.TargetPlanSnapshot;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DailyCostCalculatorServiceTest {
    private final DailyCostCalculatorService service = new DailyCostCalculatorService();
    private final LocalDate today = LocalDate.of(2026, 6, 10);

    @Test
    void calculateUsedDaysCountsPurchaseDayAsFirstDay() {
        assertEquals(1, service.calculateUsedDays(today, today));
        assertEquals(10, service.calculateUsedDays(LocalDate.of(2026, 6, 1), today));
        assertEquals(1, service.calculateUsedDays(LocalDate.of(2026, 6, 12), today));
        assertEquals(1, service.calculateUsedDays(null, today));
    }

    @Test
    void calculateBaseDailyCostUsesBigDecimalDivision() {
        Device device = new Device("Phone", new BigDecimal("1000"), LocalDate.of(2026, 6, 1), BigDecimal.TEN);
        DeviceCostSnapshot snapshot = service.calculateDevice(device, today);

        assertDecimalEquals("100.0000000000", snapshot.baseDailyCost());
    }

    @Test
    void calculateAccessoryDailyCostAndDeviceTotalDailyCost() {
        Device device = new Device("Phone", new BigDecimal("1000"), LocalDate.of(2026, 6, 1), BigDecimal.TEN);
        device.getAccessories().add(new Accessory("Case", new BigDecimal("100"), LocalDate.of(2026, 6, 6)));

        DeviceCostSnapshot snapshot = service.calculateDevice(device, today);

        assertDecimalEquals("20.0000000000", snapshot.accessoriesDailyCost());
        assertDecimalEquals("120.0000000000", snapshot.currentDailyCost());
    }

    @Test
    void calculateTotalInvestment() {
        Device device = new Device("Phone", new BigDecimal("1000"), LocalDate.of(2026, 6, 1), BigDecimal.TEN);
        device.getAccessories().add(new Accessory("Case", new BigDecimal("100"), LocalDate.of(2026, 6, 6)));
        device.getAccessories().add(new Accessory("Film", new BigDecimal("50"), LocalDate.of(2026, 6, 6)));

        DeviceCostSnapshot snapshot = service.calculateDevice(device, today);

        assertDecimalEquals("1150", snapshot.totalInvestment());
    }

    @Test
    void calculateSummaryTotalsMonthlyAndAnnualCosts() {
        AppData appData = new AppData();
        appData.getDevices().add(new Device("A", new BigDecimal("1000"), LocalDate.of(2026, 6, 1), BigDecimal.TEN));
        appData.getDevices().add(new Device("B", new BigDecimal("500"), LocalDate.of(2026, 6, 6), BigDecimal.TEN));

        SummarySnapshot summary = service.calculateSummary(appData, today);

        assertDecimalEquals("200.0000000000", summary.totalDailyCost());
        assertDecimalEquals("6083.34000000000000", summary.equivalentMonthlyCost());
        assertDecimalEquals("73000.0000000000", summary.equivalentAnnualCost());
    }

    @Test
    void replacedDeviceKeepsInvestmentButIsExcludedFromActiveDailyCost() {
        AppData appData = new AppData();
        Device active = new Device("Active", new BigDecimal("1000"), LocalDate.of(2026, 6, 1), BigDecimal.TEN);
        Device replaced = new Device("Old", new BigDecimal("500"), LocalDate.of(2026, 6, 6), BigDecimal.TEN);
        replaced.setReplaced(true);
        replaced.setReplacementDate(today);
        appData.getDevices().add(active);
        appData.getDevices().add(replaced);

        SummarySnapshot summary = service.calculateSummary(appData, today);

        assertDecimalEquals("1500", summary.totalInvestment());
        assertDecimalEquals("100.0000000000", summary.totalDailyCost());
        assertEquals(0, summary.achievedTargetCount());
        assertTrue(summary.devices().stream().anyMatch(DeviceCostSnapshot::replaced));
    }

    @Test
    void calculateTargetPlanDateAndRemainingDays() {
        Device device = new Device("Phone", new BigDecimal("1000"), LocalDate.of(2026, 6, 1), new BigDecimal("10"));
        DeviceCostSnapshot snapshot = service.calculateDevice(device, today);
        TargetPlanSnapshot targetPlan = snapshot.targetPlan();

        assertDecimalEquals("90.0000000000", targetPlan.remainingDays());
        assertEquals(LocalDate.of(2026, 9, 8), targetPlan.estimatedDate());
    }

    @Test
    void calculateThirtyDayDecreaseAndMarginalDecrease() {
        Device device = new Device("Phone", new BigDecimal("1000"), LocalDate.of(2026, 6, 1), BigDecimal.TEN);
        DeviceCostSnapshot snapshot = service.calculateDevice(device, today);

        assertDecimalEquals("75.0000000000", snapshot.targetPlan().thirtyDayDecrease());
        assertDecimalEquals("9.0909090909", snapshot.targetPlan().marginalDecreasePerDay());
    }

    @Test
    void calculateReplacementIndexClampsAndMarksAchieved() {
        assertEquals(20, service.calculateReplacementIndex(new BigDecimal("50"), new BigDecimal("10")));
        assertEquals(1, service.calculateReplacementIndex(new BigDecimal("1000"), new BigDecimal("1")));
        assertEquals(99, service.calculateReplacementIndex(new BigDecimal("101"), new BigDecimal("100")));
        assertEquals(100, service.calculateReplacementIndex(new BigDecimal("10"), new BigDecimal("10")));
    }

    @Test
    void deviceWithTargetReachedHasZeroRemainingDays() {
        Device device = new Device("Phone", new BigDecimal("100"), today, new BigDecimal("100"));
        DeviceCostSnapshot snapshot = service.calculateDevice(device, today);

        assertTrue(snapshot.targetPlan().achieved());
        assertDecimalEquals("0", snapshot.targetPlan().remainingDays());
        assertEquals("已达到目标，可考虑换新", snapshot.targetPlan().advice());
    }

    private void assertDecimalEquals(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual), "expected " + expected + " but was " + actual);
    }
}
