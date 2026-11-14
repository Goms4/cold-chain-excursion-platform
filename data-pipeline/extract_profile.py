#!/usr/bin/env python3
"""Repeatable-read PostgreSQL analytics export to deterministic CSV files."""
from __future__ import annotations

import hashlib
import os
import sys
from datetime import date, datetime, timezone
from decimal import Decimal
from pathlib import Path
from uuid import uuid4

import pandas as pd
import psycopg

ROOT = Path(__file__).resolve().parent
OUTPUT = ROOT / "output"
DATASETS = {
    "dim_shipment": ("analytics.v_shipments", ["shipment_id", "tracking_number", "shipment_reference", "origin", "destination", "status", "storage_profile_id", "storage_profile", "target_min_c", "target_max_c", "expected_cadence_minutes", "allowed_lateness_minutes", "created_at", "updated_at", "expected_delivery_at", "archived_at"]),
    "dim_lot": ("analytics.v_lots", ["lot_id", "shipment_id", "product_name", "batch_number", "quantity", "risk_level", "disposition_status", "created_at", "expiry_date", "archived_at", "unloaded_at"]),
    "fact_sensor_event": ("analytics.v_sensor_events", ["raw_record_id", "event_id", "sensor_id", "shipment_id", "lot_id", "sequence_no", "event_time", "ingest_time", "temperature_c", "source", "condition", "data_quality", "validation_status", "duplicate_of_event_id", "quarantine_reason", "previous_event_time", "gap_minutes", "missing_interval", "lateness_minutes", "is_late"]),
    "fact_excursion": ("analytics.v_excursions", ["excursion_id", "shipment_id", "severity", "peak_temperature_c", "start_event_time", "end_event_time", "duration_minutes", "reading_count", "rule_version", "status", "flagged_at", "affected_lot_ids"]),
    "fact_investigation": ("analytics.v_investigations", ["investigation_id", "excursion_id", "shipment_id", "status", "opened_at", "sla_due_at", "closed_at", "has_evidence", "contradictory_evidence"]),
    "fact_decision": ("analytics.v_decisions", ["decision_record_id", "investigation_id", "excursion_id", "shipment_id", "decision_value", "decided_at", "rule_version", "actor_role", "affected_lot_ids", "investigation_status", "investigation_opened_at", "contradictory_evidence"]),
}
SOURCE_COLUMNS = {
    "fact_sensor_event": ["raw_record_id", "event_id", "sensor_id", "shipment_id", "lot_id", "sequence_no", "event_time", "ingest_time", "temperature_c", "source", "condition", "data_quality", "validation_status", "duplicate_of_event_id", "quarantine_reason"],
}
ORDER_BY = {
    "fact_sensor_event": ["shipment_id", "event_time", "sensor_id", "event_id", "raw_record_id"],
    "fact_decision": ["shipment_id", "decided_at", "decision_record_id"],
}
QUALITY_COLUMNS = ["shipment_id", "raw", "clean", "duplicate", "late_accepted", "quarantined", "expected_readings", "received_readings", "missing_intervals", "unknown_minutes", "in_range_readings", "out_of_range_readings", "completeness_pct"]
KPI_COLUMNS = ["kpi", "severity", "value", "unit", "sla_target_hours"]
TIMESTAMP_COLUMNS = {"created_at", "updated_at", "expected_delivery_at", "archived_at", "unloaded_at", "event_time", "ingest_time", "previous_event_time", "start_event_time", "end_event_time", "flagged_at", "opened_at", "sla_due_at", "closed_at", "decided_at"}
TEMPERATURE_COLUMNS = {"target_min_c", "target_max_c", "temperature_c", "peak_temperature_c"}


def connect():
    return psycopg.connect(
        host=os.getenv("PGHOST", "localhost"), port=os.getenv("PGPORT", "5432"),
        dbname=os.getenv("PGDATABASE", "cold_chain"), user=os.getenv("PGUSER", "coldchain_analytics"),
        password=os.getenv("PGPASSWORD", ""), application_name="coldlatch-csv-pipeline",
    )


def fetch_frame(conn, sql: str) -> pd.DataFrame:
    with conn.cursor() as cur:
        cur.execute(sql)
        names = [column.name for column in cur.description]
        return pd.DataFrame.from_records(cur.fetchall(), columns=names)


def count_rows(conn, view: str) -> int:
    with conn.cursor() as cur:
        cur.execute(f"SELECT count(*) FROM {view}")
        return int(cur.fetchone()[0])


def iso_utc(value):
    if value is None or pd.isna(value):
        return ""
    if isinstance(value, pd.Timestamp):
        value = value.to_pydatetime()
    if isinstance(value, datetime):
        if value.tzinfo is None:
            value = value.replace(tzinfo=timezone.utc)
        return value.astimezone(timezone.utc).isoformat(timespec="microseconds").replace("+00:00", "Z")
    if isinstance(value, date):
        return value.isoformat()
    return str(value)


def serialize_frame(frame: pd.DataFrame, columns: list[str]) -> pd.DataFrame:
    frame = frame.reindex(columns=columns).copy()
    for column in frame.columns:
        if column in TIMESTAMP_COLUMNS:
            frame[column] = frame[column].map(iso_utc)
        elif column in TEMPERATURE_COLUMNS:
            frame[column] = frame[column].map(lambda v: "" if v is None or pd.isna(v) else f"{Decimal(str(v)):.2f}")
        else:
            def plain(v):
                if v is None:
                    return ""
                if isinstance(v, list):
                    return "{" + ",".join(str(item) for item in v) + "}"
                if pd.isna(v):
                    return ""
                return str(v).lower() if isinstance(v, bool) else str(v)
            frame[column] = frame[column].map(plain)
    return frame


def event_analytics(events: pd.DataFrame, shipments: pd.DataFrame):
    clean = events[events.validation_status == "CLEAN"].copy()
    clean["event_time"] = pd.to_datetime(clean["event_time"], utc=True)
    clean["ingest_time"] = pd.to_datetime(clean["ingest_time"], utc=True)
    cadence = shipments.set_index("shipment_id")["expected_cadence_minutes"].to_dict()
    lateness = shipments.set_index("shipment_id")["allowed_lateness_minutes"].to_dict()
    clean = clean.sort_values(["shipment_id", "sensor_id", "event_time", "raw_record_id"], kind="mergesort")
    group = clean.groupby(["shipment_id", "sensor_id"], sort=False)
    clean["previous_event_time"] = group.event_time.shift(1)
    clean["gap_minutes"] = (clean.event_time - clean.previous_event_time).dt.total_seconds() / 60
    clean["missing_interval"] = clean.apply(lambda r: bool(pd.notna(r.gap_minutes) and r.gap_minutes > cadence[r.shipment_id]), axis=1)
    clean["lateness_minutes"] = (clean.ingest_time - clean.event_time).dt.total_seconds() / 60
    clean["is_late"] = clean.apply(lambda r: bool(r.lateness_minutes > lateness[r.shipment_id]), axis=1)
    return clean


def enrich_events(events: pd.DataFrame, shipments: pd.DataFrame) -> pd.DataFrame:
    result = events.copy()
    derived = event_analytics(events, shipments)
    fields = ["raw_record_id", "previous_event_time", "gap_minutes", "missing_interval", "lateness_minutes", "is_late"]
    result = result.merge(derived[fields], on="raw_record_id", how="left", validate="one_to_one")
    return result


def build_quality(shipments, events):
    rows = []
    clean = event_analytics(events, shipments)
    for shipment in shipments.sort_values("shipment_id").itertuples(index=False):
        sid = shipment.shipment_id
        se = events[events.shipment_id == sid]
        ce = clean[clean.shipment_id == sid]
        missing = 0
        unknown_minutes = 0.0
        span = 0.0
        expected = 0
        declared_end = shipment.expected_delivery_at
        if declared_end is not None and not pd.isna(declared_end):
            declared_end = pd.Timestamp(declared_end)
            if declared_end.tzinfo is None:
                declared_end = declared_end.tz_localize("UTC")
        for _, sensor_events in ce.groupby("sensor_id", sort=True):
            sensor_events = sensor_events.sort_values(["event_time", "raw_record_id"], kind="mergesort")
            event_end = sensor_events.event_time.max()
            end = max(event_end, declared_end) if declared_end is not None and not pd.isna(declared_end) else event_end
            span += max(0.0, (end - sensor_events.event_time.min()).total_seconds() / 60)
            cadence = shipment.expected_cadence_minutes
            expected += max(1, int((max(0.0, (end - sensor_events.event_time.min()).total_seconds() / 60) + cadence - 1) // cadence) + 1)
            gaps = (sensor_events.event_time.diff().dt.total_seconds() / 60).dropna()
            long_gaps = gaps[gaps > cadence]
            missing += len(long_gaps)
            unknown_minutes += float((long_gaps - cadence).sum())
            trailing = (end - event_end).total_seconds() / 60
            if trailing > cadence:
                missing += 1
                unknown_minutes += trailing - cadence
        received = len(ce)
        rows.append({
            "shipment_id": sid, "raw": len(se), "clean": int((se.validation_status == "CLEAN").sum()),
            "duplicate": int((se.validation_status == "DUPLICATE").sum()),
            "late_accepted": int(((se.validation_status == "CLEAN") & (se.data_quality == "LATE")).sum()),
            "quarantined": int((se.validation_status == "QUARANTINED").sum()),
            "expected_readings": expected, "received_readings": received,
            "missing_intervals": missing, "unknown_minutes": round(unknown_minutes, 2),
            "in_range_readings": int((ce.condition == "NORMAL").sum()),
            "out_of_range_readings": int((ce.condition == "EXCURSION").sum()),
            "completeness_pct": round((received / expected * 100) if expected else 0.0, 2),
            "_span_minutes": span,
        })
    detail = pd.DataFrame(rows)
    total = {"shipment_id": "TOTAL"}
    for col in QUALITY_COLUMNS[1:-1]:
        total[col] = round(float(detail[col].sum()), 2) if col == "unknown_minutes" else int(detail[col].sum())
    total["completeness_pct"] = round((detail.received_readings.sum() / detail.expected_readings.sum() * 100) if detail.expected_readings.sum() else 0.0, 2)
    output = pd.concat([detail, pd.DataFrame([total])], ignore_index=True)
    return output, detail


def build_kpis(shipments, excursions, investigations, decisions, quality_detail):
    values = []
    def add(kpi, value, unit, definition, severity="", target=""):
        values.append({"kpi": kpi, "severity": severity, "value": value, "unit": unit, "sla_target_hours": target})
    shipment_count = len(shipments)
    add("excursion_rate", round(shipments.shipment_id.isin(excursions.shipment_id).sum() / shipment_count, 6) if shipment_count else 0, "ratio", "Shipments with at least one incident divided by active shipments.")
    detect = (pd.to_datetime(excursions.flagged_at, utc=True) - pd.to_datetime(excursions.start_event_time, utc=True)).dt.total_seconds() / 60 if len(excursions) else pd.Series(dtype=float)
    add("mean_time_to_detect", round(float(detect.mean()), 2) if len(detect) else "", "minutes", "Mean flagged_at minus the incident's earliest event_time.")
    incident_created = excursions.set_index("excursion_id").flagged_at.to_dict()
    if len(decisions):
        elapsed = [(pd.Timestamp(row.decided_at) - pd.Timestamp(incident_created[row.excursion_id])).total_seconds() / 60 for row in decisions.itertuples(index=False) if row.excursion_id in incident_created]
    else:
        elapsed = []
    add("mean_time_to_disposition", round(sum(elapsed) / len(elapsed), 2) if elapsed else "", "minutes", "Mean decision decided_at minus incident created_at (flagged_at).")
    merged = investigations.merge(excursions[["excursion_id", "severity"]], on="excursion_id", how="inner")
    for severity in sorted(excursions.severity.dropna().unique()):
        group = merged[(merged.severity == severity) & merged.closed_at.notna()]
        within = sum(pd.Timestamp(row.closed_at) <= pd.Timestamp(row.sla_due_at) for row in group.itertuples(index=False) if row.sla_due_at is not None)
        pct = round(within / len(group) * 100, 2) if len(group) else ""
        add("sla_compliance_pct", pct, "percent", "Closed investigations decided by sla_due_at; open investigations are excluded.", severity, 24)
    total_span = float(quality_detail._span_minutes.sum())
    unknown = float(quality_detail.unknown_minutes.sum())
    add("data_completeness_pct", round(max(0.0, 1 - unknown / total_span) * 100, 2) if total_span else "", "percent", "100 × (1 − unknown gap minutes / observed shipment minutes); no wall-clock end is used.")
    open_excursions = set(investigations.loc[investigations.status.str.upper().eq("OPEN"), "excursion_id"])
    at_risk = set()
    for row in excursions[excursions.excursion_id.isin(open_excursions)].affected_lot_ids:
        if isinstance(row, list): at_risk.update(row)
        elif row: at_risk.update(str(row).strip("{}").split(","))
    add("lots_at_risk", len(at_risk), "lots", "Distinct active lots linked to incidents with an OPEN investigation.")
    return pd.DataFrame(values, columns=KPI_COLUMNS)


def sha256(path: Path) -> str:
    h = hashlib.sha256()
    with path.open("rb") as stream:
        for block in iter(lambda: stream.read(1024 * 1024), b""):
            h.update(block)
    return h.hexdigest()


def write_csv(frame: pd.DataFrame, path: Path, columns: list[str]):
    serialized = serialize_frame(frame, columns)
    serialized.to_csv(path, index=False, encoding="utf-8", lineterminator="\n", na_rep="")
    reread = pd.read_csv(path, dtype=str, keep_default_na=False)
    if list(reread.columns) != columns:
        raise ValueError(f"CSV column mismatch for {path.name}")
    return len(reread), sha256(path)


def validate_reconciliation(source_count: int, output_count: int) -> int:
    difference = output_count - source_count
    if difference != 0:
        raise ValueError(f"Source/output row mismatch: {source_count} != {output_count}")
    return difference


def run():
    run_id = datetime.now(timezone.utc).strftime("%Y%m%dT%H%M%S.%fZ")
    target = OUTPUT / run_id
    target.mkdir(parents=True, exist_ok=False)
    with connect() as conn:
        with conn.transaction():
            conn.execute("SET TRANSACTION ISOLATION LEVEL REPEATABLE READ, READ ONLY")
            frames, source_counts = {}, {}
            for name, (view, columns) in DATASETS.items():
                source_counts[name] = count_rows(conn, view)
                selected = SOURCE_COLUMNS.get(name, columns)
                ordering = ORDER_BY.get(name, selected[:2])
                frames[name] = fetch_frame(conn, f"SELECT {', '.join(selected)} FROM {view} ORDER BY {', '.join(ordering)}")
                if len(frames[name]) != source_counts[name]:
                    raise ValueError(f"Snapshot count mismatch for {name}")
            shipments, events = frames["dim_shipment"], frames["fact_sensor_event"]
            frames["fact_sensor_event"] = enrich_events(events, shipments)
            quality, quality_detail = build_quality(shipments, events)
            kpis = build_kpis(shipments, frames["fact_excursion"], frames["fact_investigation"], frames["fact_decision"], quality_detail)
            tables = {**frames, "telemetry_quality_report": quality, "kpi_summary": kpis}
            column_map = {name: contract[1] for name, contract in DATASETS.items()}
            column_map.update({"telemetry_quality_report": QUALITY_COLUMNS, "kpi_summary": KPI_COLUMNS})
            manifest_rows = []
            for name, frame in tables.items():
                filename = name + ".csv"
                row_count, digest = write_csv(frame, target / filename, column_map[name])
                if name in source_counts:
                    source = source_counts[name]
                    try:
                        diff, status = validate_reconciliation(source, row_count), "PASS"
                    except ValueError:
                        diff, status = row_count - source, "FAIL"
                else:
                    source, diff, status = "", "", "PASS"
                event_times = frames["fact_sensor_event"].event_time if name == "fact_sensor_event" and len(events) else []
                minimum = iso_utc(pd.to_datetime(event_times, utc=True).min()) if len(event_times) else ""
                maximum = iso_utc(pd.to_datetime(event_times, utc=True).max()) if len(event_times) else ""
                manifest_rows.append({"dataset": name, "source_count": source, "output_count": row_count, "difference": diff, "status": status, "sha256": digest, "min_event_time": minimum, "max_event_time": maximum})
            counts = quality[["raw", "clean", "duplicate", "quarantined"]].iloc[-1]
            identity_difference = int(counts.clean + counts.duplicate + counts.quarantined - counts.raw)
            manifest_rows.append({
                "dataset": "event_identity", "source_count": int(counts.raw),
                "output_count": int(counts.clean + counts.duplicate + counts.quarantined),
                "difference": identity_difference, "status": "PASS" if identity_difference == 0 else "FAIL",
                "sha256": "", "min_event_time": "", "max_event_time": "",
            })
            manifest = pd.DataFrame(manifest_rows, columns=["dataset", "source_count", "output_count", "difference", "status", "sha256", "min_event_time", "max_event_time"])
            manifest.to_csv(target / "manifest.csv", index=False, encoding="utf-8", lineterminator="\n", na_rep="")
            if (manifest.status != "PASS").any():
                raise ValueError("Manifest contains a failed reconciliation, including raw event identity")
    latest_tmp = OUTPUT / f".LATEST-{uuid4().hex}"
    latest_tmp.write_text(run_id + "\n", encoding="utf-8")
    latest_tmp.replace(OUTPUT / "LATEST")
    print(f"Run: {run_id}")
    print("dataset,source_count,output_count,difference,status,sha256")
    for row in manifest.itertuples(index=False):
        print(f"{row.dataset},{row.source_count},{row.output_count},{row.difference},{row.status},{row.sha256}")
    print(f"Raw identity: {int(counts.raw)} = {int(counts.clean)} + {int(counts.duplicate)} + {int(counts.quarantined)}")
    print(f"Output: {target}")


if __name__ == "__main__":
    try:
        run()
    except Exception as exc:
        print(f"Pipeline failed: {exc}", file=sys.stderr)
        sys.exit(1)
