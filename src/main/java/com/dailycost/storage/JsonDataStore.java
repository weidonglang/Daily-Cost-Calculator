package com.dailycost.storage;

import com.dailycost.model.AppData;
import com.dailycost.util.AppPaths;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class JsonDataStore {
    private final Path dataFile;
    private final ObjectMapper objectMapper;

    public JsonDataStore() {
        this(AppPaths.dataFile());
    }

    public JsonDataStore(Path dataFile) {
        this.dataFile = dataFile;
        this.objectMapper = createObjectMapper();
    }

    public Path getDataFile() {
        return dataFile;
    }

    public AppData load() {
        ensureDataDirectory();
        if (Files.notExists(dataFile)) {
            AppData defaultData = new AppData();
            save(defaultData);
            return defaultData;
        }

        try {
            AppData appData = objectMapper.readValue(dataFile.toFile(), AppData.class);
            validate(appData);
            return appData;
        } catch (IOException | IllegalArgumentException e) {
            throw new StorageException("读取数据文件失败，请检查 data.json 是否损坏：" + dataFile, e);
        }
    }

    public void save(AppData appData) {
        ensureDataDirectory();
        try {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(dataFile.toFile(), appData == null ? new AppData() : appData);
        } catch (IOException e) {
            throw new StorageException("保存数据文件失败：" + dataFile, e);
        }
    }

    public void exportBackup(Path targetFile, AppData appData) {
        if (targetFile == null) {
            throw new StorageException("导出路径不能为空");
        }
        try {
            Path parent = targetFile.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(targetFile.toFile(), appData == null ? new AppData() : appData);
        } catch (IOException e) {
            throw new StorageException("导出 JSON 备份失败：" + targetFile, e);
        }
    }

    public AppData importBackup(Path sourceFile) {
        if (sourceFile == null || Files.notExists(sourceFile)) {
            throw new StorageException("导入文件不存在：" + sourceFile);
        }
        try {
            AppData imported = objectMapper.readValue(sourceFile.toFile(), AppData.class);
            validate(imported);
            save(imported);
            return imported;
        } catch (IOException | IllegalArgumentException e) {
            throw new StorageException("导入 JSON 备份失败，请确认文件格式正确：" + sourceFile, e);
        }
    }

    private void ensureDataDirectory() {
        try {
            Path parent = dataFile.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
        } catch (IOException e) {
            throw new StorageException("创建数据目录失败：" + dataFile.getParent(), e);
        }
    }

    private void validate(AppData appData) {
        if (appData == null) {
            throw new IllegalArgumentException("数据内容为空");
        }
        if (appData.getVersion() <= 0) {
            throw new IllegalArgumentException("version 必须大于 0");
        }
        appData.getSettings();
        appData.getDevices();
    }

    private ObjectMapper createObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        return mapper;
    }
}
