package com.dailycost.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public final class FormatUtil {
    public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private FormatUtil() {
    }

    public static String money(BigDecimal value) {
        BigDecimal safeValue = value == null ? BigDecimal.ZERO : value;
        return safeValue.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    public static String yuan(BigDecimal value) {
        return money(value) + " 元";
    }

    public static String yuanPerDay(BigDecimal value) {
        return money(value) + " 元/天";
    }

    public static String date(LocalDate value) {
        return value == null ? "" : DATE_FORMATTER.format(value);
    }
}
