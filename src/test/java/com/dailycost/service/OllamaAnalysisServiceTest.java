package com.dailycost.service;

import com.dailycost.model.AppData;
import com.dailycost.model.Device;
import com.dailycost.model.SummarySnapshot;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertTrue;

class OllamaAnalysisServiceTest {
    @Test
    void buildPromptContainsSummaryAndDeviceDetails() {
        AppData appData = new AppData();
        appData.getDevices().add(new Device("Phone", new BigDecimal("1000"), LocalDate.of(2026, 6, 1), BigDecimal.TEN));
        SummarySnapshot summary = new DailyCostCalculatorService().calculateSummary(appData, LocalDate.of(2026, 6, 10));

        String prompt = new OllamaAnalysisService().buildPrompt(summary);

        assertTrue(prompt.contains("设备数：1"));
        assertTrue(prompt.contains("Phone"));
        assertTrue(prompt.contains("换新优先级"));
    }
}
