package com.dailycost.service;

import com.dailycost.model.Accessory;
import com.dailycost.model.AppData;
import com.dailycost.model.Device;
import com.dailycost.model.DeviceCostSnapshot;
import com.dailycost.util.FormatUtil;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class CsvImportExportService {
    private final DailyCostCalculatorService calculatorService = new DailyCostCalculatorService();

    public ImportResult parse(String csvText, AppData existingData) {
        if (csvText == null || csvText.isBlank()) {
            throw new CsvImportException("导入内容不能为空");
        }

        AppData parsed = new AppData();
        List<PendingAccessory> pendingAccessories = new ArrayList<>();
        Map<String, Device> existingDeviceByName = new HashMap<>();
        Map<String, Device> importedDeviceByName = new HashMap<>();
        for (Device device : existingData.getDevices()) {
            existingDeviceByName.put(device.getName(), device);
        }

        List<String> lines = csvText.lines().toList();
        int importedDevices = 0;
        int importedAccessories = 0;
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            int lineNumber = i + 1;
            if (line.isBlank()) {
                continue;
            }
            List<String> fields = parseLine(line);
            if (!fields.isEmpty() && "类型".equals(fields.getFirst())) {
                continue;
            }
            if (fields.size() < 5) {
                throw new CsvImportException("第 " + lineNumber + " 行字段不足");
            }

            String type = fields.get(0).trim();
            if ("设备".equals(type)) {
                Device device = parseDevice(fields, lineNumber);
                device.setSortOrder(existingData.getDevices().size() + parsed.getDevices().size());
                parsed.getDevices().add(device);
                importedDeviceByName.put(device.getName(), device);
                importedDevices++;
            } else if ("配件".equals(type)) {
                Accessory accessory = parseAccessory(fields, lineNumber);
                String ownerName = fields.get(1).trim();
                Device importedOwner = importedDeviceByName.get(ownerName);
                if (importedOwner != null) {
                    importedOwner.getAccessories().add(accessory);
                } else if (existingDeviceByName.containsKey(ownerName)) {
                    pendingAccessories.add(new PendingAccessory(ownerName, accessory));
                } else {
                    throw new CsvImportException("第 " + lineNumber + " 行配件所属设备不存在：" + ownerName);
                }
                importedAccessories++;
            } else {
                throw new CsvImportException("第 " + lineNumber + " 行类型必须是 设备 或 配件");
            }
        }
        return new ImportResult(parsed.getDevices(), pendingAccessories, importedDevices, importedAccessories);
    }

    public void applyImport(AppData appData, ImportResult result) {
        appData.getDevices().addAll(result.devices());
        for (PendingAccessory pendingAccessory : result.pendingAccessories()) {
            Device owner = appData.getDevices().stream()
                    .filter(device -> device.getName().equals(pendingAccessory.ownerName()))
                    .findFirst()
                    .orElseThrow(() -> new CsvImportException("配件所属设备不存在：" + pendingAccessory.ownerName()));
            owner.getAccessories().add(pendingAccessory.accessory());
        }
        normalizeSortOrder(appData);
    }

    public String exportDetails(AppData appData, LocalDate currentDate) {
        StringBuilder builder = new StringBuilder();
        builder.append('\ufeff');
        builder.append("类型,设备名,配件名,价格,购买日期,已使用天数,当前日均,目标日均\n");
        for (Device device : appData.getDevices()) {
            DeviceCostSnapshot snapshot = calculatorService.calculateDevice(device, currentDate);
            builder.append(csv("设备")).append(',')
                    .append(csv(device.getName())).append(',')
                    .append(',')
                    .append(csv(FormatUtil.money(device.getBasePrice()))).append(',')
                    .append(csv(FormatUtil.date(device.getPurchaseDate()))).append(',')
                    .append(snapshot.baseUsedDays()).append(',')
                    .append(csv(FormatUtil.money(snapshot.baseDailyCost()))).append(',')
                    .append(csv(FormatUtil.money(device.getTargetDailyCost()))).append('\n');
            for (Accessory accessory : device.getAccessories()) {
                Optional<com.dailycost.model.AccessoryCostSnapshot> accessorySnapshot = snapshot.accessories().stream()
                        .filter(item -> item.id().equals(accessory.getId()))
                        .findFirst();
                builder.append(csv("配件")).append(',')
                        .append(csv(device.getName())).append(',')
                        .append(csv(accessory.getName())).append(',')
                        .append(csv(FormatUtil.money(accessory.getPrice()))).append(',')
                        .append(csv(FormatUtil.date(accessory.getPurchaseDate()))).append(',')
                        .append(accessorySnapshot.map(item -> Long.toString(item.usedDays())).orElse("")).append(',')
                        .append(csv(accessorySnapshot.map(item -> FormatUtil.money(item.dailyCost())).orElse(""))).append(',')
                        .append(csv(FormatUtil.money(device.getTargetDailyCost()))).append('\n');
            }
        }
        return builder.toString();
    }

    private Device parseDevice(List<String> fields, int lineNumber) {
        String name = fields.get(1).trim();
        if (name.isBlank()) {
            throw new CsvImportException("第 " + lineNumber + " 行设备名称不能为空");
        }
        BigDecimal price = parsePositiveDecimal(fields.get(2), lineNumber, "设备价格");
        LocalDate purchaseDate = parseDate(fields.get(3), lineNumber);
        BigDecimal target = parsePositiveDecimal(fields.get(4), lineNumber, "目标日均");
        return new Device(name, price, purchaseDate, target);
    }

    private Accessory parseAccessory(List<String> fields, int lineNumber) {
        String name = fields.get(2).trim();
        if (name.isBlank()) {
            throw new CsvImportException("第 " + lineNumber + " 行配件名称不能为空");
        }
        BigDecimal price = parsePositiveDecimal(fields.get(3), lineNumber, "配件价格");
        LocalDate purchaseDate = parseDate(fields.get(4), lineNumber);
        return new Accessory(name, price, purchaseDate);
    }

    private BigDecimal parsePositiveDecimal(String value, int lineNumber, String fieldName) {
        try {
            BigDecimal number = new BigDecimal(value.trim());
            if (number.compareTo(BigDecimal.ZERO) <= 0) {
                throw new CsvImportException("第 " + lineNumber + " 行" + fieldName + "必须大于 0");
            }
            return number;
        } catch (NumberFormatException e) {
            throw new CsvImportException("第 " + lineNumber + " 行" + fieldName + "不是有效数字");
        }
    }

    private LocalDate parseDate(String value, int lineNumber) {
        try {
            return LocalDate.parse(value.trim(), FormatUtil.DATE_FORMATTER);
        } catch (DateTimeParseException e) {
            throw new CsvImportException("第 " + lineNumber + " 行日期格式必须是 yyyy-MM-dd");
        }
    }

    private List<String> parseLine(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (ch == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (ch == ',' && !inQuotes) {
                fields.add(current.toString());
                current.setLength(0);
            } else {
                current.append(ch);
            }
        }
        fields.add(current.toString());
        return fields;
    }

    private String csv(String value) {
        String safeValue = value == null ? "" : value;
        if (safeValue.contains(",") || safeValue.contains("\"") || safeValue.contains("\n")) {
            return '"' + safeValue.replace("\"", "\"\"") + '"';
        }
        return safeValue;
    }

    private void normalizeSortOrder(AppData appData) {
        for (int i = 0; i < appData.getDevices().size(); i++) {
            appData.getDevices().get(i).setSortOrder(i);
        }
    }

    public record ImportResult(List<Device> devices, List<PendingAccessory> pendingAccessories, int deviceCount, int accessoryCount) {
    }

    public record PendingAccessory(String ownerName, Accessory accessory) {
    }
}
