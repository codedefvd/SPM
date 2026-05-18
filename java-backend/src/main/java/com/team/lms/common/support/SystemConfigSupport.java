package com.team.lms.common.support;

import com.team.lms.entity.SystemConfig;
import com.team.lms.mapper.SystemConfigMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
public class SystemConfigSupport {

    private final SystemConfigMapper systemConfigMapper;

    public int getBorrowPeriodDays() {
        return getIntValue("BORROW_PERIOD_DAYS", 30);
    }

    public int getBorrowLimit() {
        return getIntValue("BORROW_LIMIT", 5);
    }

    public BigDecimal getOverdueFinePerDay() {
        return getDecimalValue("OVERDUE_FINE_PER_DAY", new BigDecimal("1.00"));
    }

    public int getReturnReminderLeadDays() {
        return getIntValue("RETURN_REMINDER_LEAD_DAYS", 3);
    }

    private int getIntValue(String key, int defaultValue) {
        SystemConfig config = systemConfigMapper.selectByKey(key);
        if (config == null || config.getConfigValue() == null || config.getConfigValue().isBlank()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(config.getConfigValue().trim());
        } catch (NumberFormatException ignored) {
            return defaultValue;
        }
    }

    private BigDecimal getDecimalValue(String key, BigDecimal defaultValue) {
        SystemConfig config = systemConfigMapper.selectByKey(key);
        if (config == null || config.getConfigValue() == null || config.getConfigValue().isBlank()) {
            return defaultValue;
        }
        try {
            return new BigDecimal(config.getConfigValue().trim());
        } catch (NumberFormatException ignored) {
            return defaultValue;
        }
    }
}
