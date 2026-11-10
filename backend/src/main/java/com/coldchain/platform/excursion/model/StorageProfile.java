package com.coldchain.platform.excursion.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "storage_profiles")
public class StorageProfile {

    @Id
    @Column(name = "id", length = 64)
    private String id;

    @Column(name = "name", nullable = false, length = 64)
    private String name;

    @Column(name = "target_min_c", nullable = false, precision = 5, scale = 2)
    private BigDecimal targetMinC;

    @Column(name = "target_max_c", nullable = false, precision = 5, scale = 2)
    private BigDecimal targetMaxC;

    @Column(name = "minor_min_c", precision = 5, scale = 2)
    private BigDecimal minorMinC;

    @Column(name = "minor_max_c", precision = 5, scale = 2)
    private BigDecimal minorMaxC;

    @Column(name = "major_min_c", precision = 5, scale = 2)
    private BigDecimal majorMinC;

    @Column(name = "major_max_c", precision = 5, scale = 2)
    private BigDecimal majorMaxC;

    @Column(name = "critical_min_c", precision = 5, scale = 2)
    private BigDecimal criticalMinC;

    @Column(name = "critical_max_c", precision = 5, scale = 2)
    private BigDecimal criticalMaxC;

    @Column(name = "max_minor_duration_minutes")
    private Integer maxMinorDurationMinutes = 240;

    @Column(name = "rule_version", nullable = false, length = 32)
    private String ruleVersion = "v1.0.0";

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public StorageProfile() {}

    public StorageProfile(String id, String name, BigDecimal targetMinC, BigDecimal targetMaxC,
                          BigDecimal minorMinC, BigDecimal minorMaxC, BigDecimal majorMinC, BigDecimal majorMaxC,
                          BigDecimal criticalMinC, BigDecimal criticalMaxC, Integer maxMinorDurationMinutes, String ruleVersion) {
        this.id = id;
        this.name = name;
        this.targetMinC = targetMinC;
        this.targetMaxC = targetMaxC;
        this.minorMinC = minorMinC;
        this.minorMaxC = minorMaxC;
        this.majorMinC = majorMinC;
        this.majorMaxC = majorMaxC;
        this.criticalMinC = criticalMinC;
        this.criticalMaxC = criticalMaxC;
        this.maxMinorDurationMinutes = maxMinorDurationMinutes;
        this.ruleVersion = ruleVersion;
        this.createdAt = Instant.now();
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

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
