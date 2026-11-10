package com.coldchain.platform.excursion.dto;

import com.coldchain.platform.excursion.model.StorageProfile;
import java.math.BigDecimal;

public class StorageProfileDto {
    private String id;
    private String name;
    private BigDecimal targetMinC;
    private BigDecimal targetMaxC;
    private BigDecimal minorMinC;
    private BigDecimal minorMaxC;
    private BigDecimal majorMinC;
    private BigDecimal majorMaxC;
    private BigDecimal criticalMinC;
    private BigDecimal criticalMaxC;
    private Integer maxMinorDurationMinutes;
    private String ruleVersion;

    public StorageProfileDto() {}

    public static StorageProfileDto fromEntity(StorageProfile profile) {
        StorageProfileDto dto = new StorageProfileDto();
        dto.setId(profile.getId());
        dto.setName(profile.getName());
        dto.setTargetMinC(profile.getTargetMinC());
        dto.setTargetMaxC(profile.getTargetMaxC());
        dto.setMinorMinC(profile.getMinorMinC());
        dto.setMinorMaxC(profile.getMinorMaxC());
        dto.setMajorMinC(profile.getMajorMinC());
        dto.setMajorMaxC(profile.getMajorMaxC());
        dto.setCriticalMinC(profile.getCriticalMinC());
        dto.setCriticalMaxC(profile.getCriticalMaxC());
        dto.setMaxMinorDurationMinutes(profile.getMaxMinorDurationMinutes());
        dto.setRuleVersion(profile.getRuleVersion());
        return dto;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public BigDecimal getTargetMinC() { return targetMinC; }
    public void setTargetMinC(BigDecimal targetMinC) { this.targetMinC = targetMinC; }

    public BigDecimal getTargetMaxC() { return targetMaxC; }
    public void setTargetMaxC(BigDecimal targetMaxC) { this.targetMaxC = targetMaxC; }

    public BigDecimal getMinorMinC() { return minorMinC; }
    public void setMinorMinC(BigDecimal minorMinC) { this.minorMinC = minorMinC; }

    public BigDecimal getMinorMaxC() { return minorMaxC; }
    public void setMinorMaxC(BigDecimal minorMaxC) { this.minorMaxC = minorMaxC; }

    public BigDecimal getMajorMinC() { return majorMinC; }
    public void setMajorMinC(BigDecimal majorMinC) { this.majorMinC = majorMinC; }

    public BigDecimal getMajorMaxC() { return majorMaxC; }
    public void setMajorMaxC(BigDecimal majorMaxC) { this.majorMaxC = majorMaxC; }

    public BigDecimal getCriticalMinC() { return criticalMinC; }
    public void setCriticalMinC(BigDecimal criticalMinC) { this.criticalMinC = criticalMinC; }

    public BigDecimal getCriticalMaxC() { return criticalMaxC; }
    public void setCriticalMaxC(BigDecimal criticalMaxC) { this.criticalMaxC = criticalMaxC; }

    public Integer getMaxMinorDurationMinutes() { return maxMinorDurationMinutes; }
    public void setMaxMinorDurationMinutes(Integer maxMinorDurationMinutes) { this.maxMinorDurationMinutes = maxMinorDurationMinutes; }

    public String getRuleVersion() { return ruleVersion; }
    public void setRuleVersion(String ruleVersion) { this.ruleVersion = ruleVersion; }
}
