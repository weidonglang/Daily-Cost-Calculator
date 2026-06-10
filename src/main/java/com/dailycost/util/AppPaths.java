package com.dailycost.util;

import java.awt.Desktop;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class AppPaths {
    public static final String APP_DIRECTORY_NAME = "DailyCostCalculator";
    public static final String DATA_FILE_NAME = "data.json";

    private AppPaths() {
    }

    public static Path appDataDirectory() {
        String appData = System.getenv("APPDATA");
        Path roamingDirectory;
        if (appData != null && !appData.isBlank()) {
            roamingDirectory = Paths.get(appData);
        } else {
            roamingDirectory = Paths.get(System.getProperty("user.home"), "AppData", "Roaming");
        }
        return roamingDirectory.resolve(APP_DIRECTORY_NAME);
    }

    public static Path dataFile() {
        return appDataDirectory().resolve(DATA_FILE_NAME);
    }

    public static Path ensureAppDataDirectory() {
        Path directory = appDataDirectory();
        try {
            Files.createDirectories(directory);
            return directory;
        } catch (IOException e) {
            throw new IllegalStateException("创建数据目录失败：" + directory, e);
        }
    }

    public static void openDataDirectory() {
        Path directory = ensureAppDataDirectory();
        if (!Desktop.isDesktopSupported()) {
            throw new IllegalStateException("当前环境不支持打开数据目录");
        }
        try {
            Desktop.getDesktop().open(directory.toFile());
        } catch (IOException e) {
            throw new IllegalStateException("打开数据目录失败：" + directory, e);
        }
    }

    public static void openWindowsAppsSettings() {
        try {
            new ProcessBuilder("cmd", "/c", "start", "", "ms-settings:appsfeatures").start();
        } catch (IOException e) {
            try {
                new ProcessBuilder("control", "appwiz.cpl").start();
            } catch (IOException fallback) {
                throw new IllegalStateException("打开卸载入口失败", fallback);
            }
        }
    }
}
