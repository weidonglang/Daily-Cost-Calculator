package com.dailycost.service;

import com.dailycost.model.AppData;
import com.dailycost.model.Device;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CsvImportExportServiceTest {
    private final CsvImportExportService service = new CsvImportExportService();

    @Test
    void parsesDeviceAndAccessoryRows() {
        AppData appData = new AppData();
        String csv = """
                类型,名称/所属设备,价格/配件名称,购买日期/价格,配件购买日期或目标日均
                设备,vivo x200 ultra,4999,2026-03-07,10
                配件,vivo x200 ultra,手机壳,39,2026-03-08
                """;

        CsvImportExportService.ImportResult result = service.parse(csv, appData);
        service.applyImport(appData, result);

        assertEquals(1, appData.getDevices().size());
        assertEquals("vivo x200 ultra", appData.getDevices().getFirst().getName());
        assertEquals(1, appData.getDevices().getFirst().getAccessories().size());
        assertEquals("手机壳", appData.getDevices().getFirst().getAccessories().getFirst().getName());
    }

    @Test
    void rejectsInvalidMoney() {
        AppData appData = new AppData();
        CsvImportException exception = assertThrows(CsvImportException.class,
                () -> service.parse("设备,Phone,0,2026-03-07,10", appData));

        assertTrue(exception.getMessage().contains("必须大于 0"));
    }

    @Test
    void rejectsInvalidDate() {
        AppData appData = new AppData();
        CsvImportException exception = assertThrows(CsvImportException.class,
                () -> service.parse("设备,Phone,100,bad-date,10", appData));

        assertTrue(exception.getMessage().contains("yyyy-MM-dd"));
    }

    @Test
    void rejectsAccessoryWithoutOwner() {
        AppData appData = new AppData();
        CsvImportException exception = assertThrows(CsvImportException.class,
                () -> service.parse("配件,Missing,Case,39,2026-03-08", appData));

        assertTrue(exception.getMessage().contains("所属设备不存在"));
    }

    @Test
    void parseDoesNotMutateExistingDataBeforeApply() {
        AppData appData = new AppData();
        appData.getDevices().add(new Device("Phone", new BigDecimal("1000"), LocalDate.of(2026, 6, 1), BigDecimal.TEN));

        CsvImportExportService.ImportResult result = service.parse("配件,Phone,Case,39,2026-06-02", appData);

        assertEquals(0, appData.getDevices().getFirst().getAccessories().size());
        service.applyImport(appData, result);
        assertEquals(1, appData.getDevices().getFirst().getAccessories().size());
    }

    @Test
    void exportsCsvDetails() {
        AppData appData = new AppData();
        appData.getDevices().add(new Device("Phone", new BigDecimal("1000"), LocalDate.of(2026, 6, 1), BigDecimal.TEN));

        String csv = service.exportDetails(appData, LocalDate.of(2026, 6, 10));

        assertTrue(csv.contains("类型,设备名,配件名,价格,购买日期,已使用天数,当前日均,目标日均"));
        assertTrue(csv.contains("设备,Phone,,1000.00,2026-06-01,10,100.00,10.00"));
    }
}
