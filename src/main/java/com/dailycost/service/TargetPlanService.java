package com.dailycost.service;

import com.dailycost.model.AppData;
import com.dailycost.model.Device;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class TargetPlanService {
    public void applyTargetDailyCostToAll(AppData appData, BigDecimal targetDailyCost) {
        if (targetDailyCost == null || targetDailyCost.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("目标日均必须大于 0");
        }
        LocalDateTime now = LocalDateTime.now();
        for (Device device : appData.getDevices()) {
            device.setTargetDailyCost(targetDailyCost);
            device.setUpdatedAt(now);
        }
    }
}
