# Snowflake Analytical Model

Keep the warehouse model small.

## Dimensions
DIM_DATE
DIM_SHIPMENT
DIM_LOT
DIM_SENSOR

## Facts
FACT_SENSOR_EVENT
FACT_EXCURSION
FACT_INVESTIGATION
FACT_DECISION

## KPI view
V_COLD_CHAIN_KPI:
- total shipments
- active excursions
- critical excursions
- affected lots
- duplicate events
- late events
- missing/unknown intervals
- investigation SLA breaches
- average investigation duration

Python extracts PostgreSQL data, profiles/reconciles it, creates analytical
datasets and loads Snowflake. Snowflake is for analytics, not live decisions.
Never commit credentials.
