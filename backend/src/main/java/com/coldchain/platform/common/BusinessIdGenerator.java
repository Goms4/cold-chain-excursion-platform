package com.coldchain.platform.common;

import jakarta.annotation.PostConstruct;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class BusinessIdGenerator {
    private static final Map<String, Long> SEQUENCES = Map.of(
            "shipment", 10001L,
            "lot", 10001L,
            "sensor", 1L,
            "issue", 10001L,
            "investigation", 10001L
    );

    private final JdbcTemplate jdbcTemplate;
    private final Environment environment;

    public BusinessIdGenerator(JdbcTemplate jdbcTemplate, Environment environment) {
        this.jdbcTemplate = jdbcTemplate;
        this.environment = environment;
    }

    @PostConstruct
    void initializeTestSequences() {
        if (java.util.Arrays.asList(environment.getActiveProfiles()).contains("test")) {
            SEQUENCES.forEach((name, start) -> jdbcTemplate.execute(
                    "CREATE SEQUENCE IF NOT EXISTS " + name + "_business_id_seq START WITH " + start + " INCREMENT BY 1"));
        }
    }

    public long nextShipmentNumber() { return next("shipment"); }
    public String shipmentId(long number) { return "SHP-%05d".formatted(number); }
    public String trackingNumber(long number) { return "TRK-2026-%06d".formatted(number - 10000); }
    public String nextShipmentId() { return shipmentId(nextShipmentNumber()); }
    public String nextLotId() { return "LOT-%05d".formatted(next("lot")); }
    public String nextSensorId() { return "SEN-%03d".formatted(next("sensor")); }
    public String nextIssueId() { return "ISS-%05d".formatted(next("issue")); }
    public String nextInvestigationId() { return "INV-%05d".formatted(next("investigation")); }

    private long next(String name) {
        Long value = jdbcTemplate.queryForObject("SELECT nextval('" + name + "_business_id_seq')", Long.class);
        if (value == null) throw new IllegalStateException("Could not allocate " + name + " business ID");
        return value;
    }
}
