package com.dailycost.service;

import com.dailycost.model.AppData;
import com.dailycost.model.SummarySnapshot;

import java.time.LocalDate;

public class StatisticsService {
    private final DailyCostCalculatorService calculatorService;

    public StatisticsService() {
        this(new DailyCostCalculatorService());
    }

    public StatisticsService(DailyCostCalculatorService calculatorService) {
        this.calculatorService = calculatorService;
    }

    public SummarySnapshot summarize(AppData appData, LocalDate currentDate) {
        return calculatorService.calculateSummary(appData, currentDate);
    }
}
