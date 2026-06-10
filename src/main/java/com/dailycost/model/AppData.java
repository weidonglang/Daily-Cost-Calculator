package com.dailycost.model;

import java.util.ArrayList;
import java.util.List;

public class AppData {
    public static final int CURRENT_VERSION = 4;

    private int version;
    private AppSettings settings;
    private List<Device> devices;

    public AppData() {
        this.version = CURRENT_VERSION;
        this.settings = new AppSettings();
        this.devices = new ArrayList<>();
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public AppSettings getSettings() {
        if (settings == null) {
            settings = new AppSettings();
        }
        return settings;
    }

    public void setSettings(AppSettings settings) {
        this.settings = settings == null ? new AppSettings() : settings;
    }

    public List<Device> getDevices() {
        if (devices == null) {
            devices = new ArrayList<>();
        }
        return devices;
    }

    public void setDevices(List<Device> devices) {
        this.devices = devices == null ? new ArrayList<>() : devices;
    }
}
