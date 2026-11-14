package com.coldchain.platform.common;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Business identifiers presented by APIs; technical IDs remain in the database. */
public final class BusinessIds {
    private static final Pattern SHIPMENT_SCENARIO = Pattern.compile("SHP-SCENARIO-(\\d+)");
    private static final Pattern LOT_SCENARIO = Pattern.compile("LOT-SCEN-(\\d+)");
    private static final Pattern SENSOR_SCENARIO = Pattern.compile("S-SCEN-(\\d+)");
    private static final Pattern TRACKING_SCENARIO = Pattern.compile("TRK-SHP-SCENARIO-(\\d+)");

    private BusinessIds() {}

    public static String shipment(String id) { return scenario(id, SHIPMENT_SCENARIO, "SHP", 10000); }
    public static String lot(String id) { return scenario(id, LOT_SCENARIO, "LOT", 10000); }
    public static String sensor(String id) { return scenario(id, SENSOR_SCENARIO, "SEN", 0); }
    public static String tracking(String trackingNumber) {
        if (trackingNumber == null) return null;
        Matcher matcher = TRACKING_SCENARIO.matcher(trackingNumber);
        return matcher.matches() ? "TRK-2026-%06d".formatted(Integer.parseInt(matcher.group(1))) : trackingNumber;
    }
    public static String event(Long databaseId) { return databaseId == null ? null : "EVT-%06d".formatted(databaseId); }
    public static String decision(Long databaseId) { return databaseId == null ? null : "DEC-%05d".formatted(databaseId); }
    public static String investigation(String internalId) {
        if (internalId == null) return null;
        if (internalId.startsWith("INV-EXC-")) return "INV-" + internalId.substring("INV-EXC-".length());
        return internalId;
    }

    public static String internalShipment(String id) { return internal(id, "SHP", "SHP-SCENARIO-"); }
    public static String internalLot(String id) { return internal(id, "LOT", "LOT-SCEN-"); }

    private static String scenario(String id, Pattern pattern, String prefix, int offset) {
        if (id == null) return null;
        Matcher matcher = pattern.matcher(id);
        if (!matcher.matches()) return id;
        int n = Integer.parseInt(matcher.group(1));
        return offset == 0 ? "%s-%03d".formatted(prefix, n) : "%s-%05d".formatted(prefix, offset + n);
    }

    private static String internal(String id, String prefix, String technicalPrefix) {
        if (id == null || !id.matches(prefix + "-\\d{5}")) return id;
        int n = Integer.parseInt(id.substring(prefix.length() + 1));
        if (prefix.equals("SHP") && n >= 10001 && n <= 10099) return technicalPrefix + (n - 10000);
        if (prefix.equals("LOT") && n >= 10001 && n <= 10099) return technicalPrefix + (n - 10000);
        return id;
    }
}
