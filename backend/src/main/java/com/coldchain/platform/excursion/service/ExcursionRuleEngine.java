package com.coldchain.platform.excursion.service;

import com.coldchain.platform.excursion.model.ExcursionSeverity;
import com.coldchain.platform.excursion.model.StorageProfile;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class ExcursionRuleEngine {

    public ExcursionSeverity evaluateTemperature(BigDecimal temperature, StorageProfile profile, int currentDurationMinutes) {
        if (temperature == null || profile == null) {
            return ExcursionSeverity.NORMAL;
        }

        // 1. Check if within target compliant range
        if (temperature.compareTo(profile.getTargetMinC()) >= 0 && temperature.compareTo(profile.getTargetMaxC()) <= 0) {
            return ExcursionSeverity.NORMAL;
        }

        // 2. Check Critical bounds
        if (profile.getCriticalMinC() != null && temperature.compareTo(profile.getCriticalMinC()) >= 0) {
            return ExcursionSeverity.CRITICAL;
        }
        // Critical freeze condition: if temperature falls significantly below target (e.g. below 0°C for room temp/refrigerated)
        if (temperature.compareTo(profile.getTargetMinC().subtract(BigDecimal.valueOf(5))) < 0) {
            return ExcursionSeverity.CRITICAL;
        }

        // 3. Check Major bounds
        if (profile.getMajorMinC() != null && temperature.compareTo(profile.getMajorMinC()) >= 0
                && (profile.getMajorMaxC() == null || temperature.compareTo(profile.getMajorMaxC()) <= 0)) {
            return ExcursionSeverity.MAJOR;
        }

        // 4. Check Minor bounds
        if (profile.getMinorMinC() != null && temperature.compareTo(profile.getMinorMinC()) >= 0
                && (profile.getMinorMaxC() == null || temperature.compareTo(profile.getMinorMaxC()) <= 0)) {
            // Check duration elevation
            int maxMinor = profile.getMaxMinorDurationMinutes() != null ? profile.getMaxMinorDurationMinutes() : 240;
            if (currentDurationMinutes > maxMinor) {
                return ExcursionSeverity.MAJOR;
            }
            return ExcursionSeverity.MINOR;
        }

        // 5. Fallback if outside target but not explicitly matched in minor/major/critical
        if (temperature.compareTo(profile.getTargetMaxC()) > 0) {
            return ExcursionSeverity.MAJOR;
        } else {
            return ExcursionSeverity.CRITICAL; // Freeze excursion
        }
    }
}
