package com.dailycost.storage;

import com.dailycost.model.Accessory;
import com.dailycost.model.AppData;
import com.dailycost.model.Device;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsonDataStoreTest {
    @TempDir
    Path tempDir;

    @Test
    void loadCreatesDefaultDataWhenFileDoesNotExist() {
        Path dataFile = tempDir.resolve("nested").resolve("data.json");
        JsonDataStore store = new JsonDataStore(dataFile);

        AppData appData = store.load();

        assertTrue(Files.exists(dataFile));
        assertEquals(AppData.CURRENT_VERSION, appData.getVersion());
        assertTrue(appData.getDevices().isEmpty());
    }

    @Test
    void saveAndLoadRoundTrip() {
        Path dataFile = tempDir.resolve("data.json");
        JsonDataStore store = new JsonDataStore(dataFile);
        AppData appData = new AppData();
        Device device = new Device("Phone", new BigDecimal("4999"), LocalDate.of(2026, 3, 7), new BigDecimal("3.5"));
        device.getAccessories().add(new Accessory("Case", new BigDecimal("39"), LocalDate.of(2026, 3, 8)));
        appData.getDevices().add(device);

        store.save(appData);
        AppData loaded = store.load();

        assertEquals(1, loaded.getDevices().size());
        assertEquals("Phone", loaded.getDevices().getFirst().getName());
        assertEquals(1, loaded.getDevices().getFirst().getAccessories().size());
        assertEquals("Case", loaded.getDevices().getFirst().getAccessories().getFirst().getName());
    }

    @Test
    void loadDamagedJsonThrowsReadableException() throws Exception {
        Path dataFile = tempDir.resolve("data.json");
        Files.writeString(dataFile, "{bad json");
        JsonDataStore store = new JsonDataStore(dataFile);

        StorageException exception = assertThrows(StorageException.class, store::load);

        assertTrue(exception.getMessage().contains("读取数据文件失败"));
    }

    @Test
    void exportAndImportBackup() {
        Path dataFile = tempDir.resolve("data.json");
        Path backupFile = tempDir.resolve("backup").resolve("backup.json");
        JsonDataStore store = new JsonDataStore(dataFile);
        AppData appData = new AppData();
        appData.getDevices().add(new Device("Laptop", new BigDecimal("8999"), LocalDate.of(2025, 6, 1), BigDecimal.TEN));

        store.exportBackup(backupFile, appData);
        AppData imported = store.importBackup(backupFile);

        assertTrue(Files.exists(backupFile));
        assertEquals(1, imported.getDevices().size());
        assertEquals("Laptop", imported.getDevices().getFirst().getName());
        assertEquals("Laptop", store.load().getDevices().getFirst().getName());
    }
}
