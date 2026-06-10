package com.dailycost.model;

import java.math.BigDecimal;

public class AppSettings {
    public static final String THEME_DARK_PURPLE = "暗夜紫";
    public static final int SAVE_FORMAT_VERSION = 4;

    private String themeName;
    private BigDecimal defaultTargetDailyCost;
    private int saveFormatVersion;
    private String lastAiAnalysis;
    private String lastAiAnalysisAt;

    public AppSettings() {
        this.themeName = THEME_DARK_PURPLE;
        this.defaultTargetDailyCost = BigDecimal.TEN;
        this.saveFormatVersion = SAVE_FORMAT_VERSION;
    }

    public String getThemeName() {
        return themeName == null || themeName.isBlank() ? THEME_DARK_PURPLE : themeName;
    }

    public void setThemeName(String themeName) {
        this.themeName = themeName;
    }

    public BigDecimal getDefaultTargetDailyCost() {
        return defaultTargetDailyCost == null ? BigDecimal.TEN : defaultTargetDailyCost;
    }

    public void setDefaultTargetDailyCost(BigDecimal defaultTargetDailyCost) {
        this.defaultTargetDailyCost = defaultTargetDailyCost == null ? BigDecimal.TEN : defaultTargetDailyCost;
    }

    public int getSaveFormatVersion() {
        return saveFormatVersion == 0 ? SAVE_FORMAT_VERSION : saveFormatVersion;
    }

    public void setSaveFormatVersion(int saveFormatVersion) {
        this.saveFormatVersion = saveFormatVersion;
    }

    public String getLastAiAnalysis() {
        return lastAiAnalysis == null ? "" : lastAiAnalysis;
    }

    public void setLastAiAnalysis(String lastAiAnalysis) {
        this.lastAiAnalysis = lastAiAnalysis == null ? "" : lastAiAnalysis;
    }

    public String getLastAiAnalysisAt() {
        return lastAiAnalysisAt == null ? "" : lastAiAnalysisAt;
    }

    public void setLastAiAnalysisAt(String lastAiAnalysisAt) {
        this.lastAiAnalysisAt = lastAiAnalysisAt == null ? "" : lastAiAnalysisAt;
    }
}
