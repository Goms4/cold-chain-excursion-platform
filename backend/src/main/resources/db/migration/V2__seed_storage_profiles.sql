-- V2__seed_storage_profiles.sql
-- Seed baseline storage profiles defined in docs/BUSINESS_RULES.md

INSERT INTO storage_profiles (
    id, name, target_min_c, target_max_c,
    minor_min_c, minor_max_c, major_min_c, major_max_c,
    critical_min_c, critical_max_c, max_minor_duration_minutes, rule_version
) VALUES
(
    'PROFILE-CRT',
    'Controlled Room / Ambient',
    15.0, 25.0,
    25.1, 30.0,
    30.1, 40.0,
    40.1, 99.9,
    240,
    'v1.0.0'
),
(
    'PROFILE-COOL',
    'Cool',
    8.0, 15.0,
    15.1, 20.0,
    20.1, 25.0,
    25.1, 99.9,
    180,
    'v1.0.0'
),
(
    'PROFILE-REF',
    'Refrigerated',
    2.0, 8.0,
    8.1, 12.0,
    12.1, 20.0,
    20.1, 99.9,
    120,
    'v1.0.0'
),
(
    'PROFILE-FRZ',
    'Frozen',
    -25.0, -10.0,
    -9.9, -5.0,
    -4.9, 0.0,
    0.1, 99.9,
    60,
    'v1.0.0'
);
