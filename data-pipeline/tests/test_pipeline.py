import sys
from pathlib import Path

import pandas as pd
import pytest

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import extract_profile as pipeline


def test_mismatch_fails_pipeline_reconciliation():
    with pytest.raises(ValueError, match="Source/output row mismatch"):
        pipeline.validate_reconciliation(5, 4)


def test_snapshot_view_contracts_and_csv_outputs():
    latest = (pipeline.OUTPUT / "LATEST").read_text().strip()
    run_dir = pipeline.OUTPUT / latest
    assert run_dir.is_dir()
    assert not list(pipeline.OUTPUT.rglob("*.parquet"))
    for name, (_, columns) in pipeline.DATASETS.items():
        frame = pd.read_csv(run_dir / f"{name}.csv", dtype=str, keep_default_na=False)
        assert list(frame.columns) == columns
        assert len(frame) == int(pd.read_csv(run_dir / "manifest.csv").set_index("dataset").loc[name, "source_count"])
        sensitive = [column for column in frame.columns if any(token in column.lower() for token in ("password", "hash", "session", "notes", "reason", "payload", "ip_address", "deleted_at", "deleted_by")) and column != "quarantine_reason"]
        assert not sensitive
        if "quarantine_reason" in frame.columns:
            assert (frame.quarantine_reason == "").all()
    quality = pd.read_csv(run_dir / "telemetry_quality_report.csv", dtype=str, keep_default_na=False)
    total = quality[quality.shipment_id == "TOTAL"].iloc[0]
    assert int(total.raw) == int(total.clean) + int(total.duplicate) + int(total.quarantined)
    manifest = pd.read_csv(run_dir / "manifest.csv", dtype=str, keep_default_na=False).set_index("dataset")
    assert manifest.loc["event_identity", "status"] == "PASS"
    assert manifest.loc["event_identity", "difference"] == "0"
    assert (run_dir / "manifest.csv").is_file()
    assert (run_dir / "kpi_summary.csv").is_file()
    kpis = pd.read_csv(run_dir / "kpi_summary.csv", dtype=str, keep_default_na=False)
    assert list(kpis.columns) == pipeline.KPI_COLUMNS


def test_seeded_duplicate_late_and_missing_gap_flags():
    latest = (pipeline.OUTPUT / "LATEST").read_text().strip()
    events = pd.read_csv(pipeline.OUTPUT / latest / "fact_sensor_event.csv", dtype=str, keep_default_na=False)
    assert (events.validation_status == "DUPLICATE").any()
    assert (events.validation_status == "QUARANTINED").any()
    clean = events[events.validation_status == "CLEAN"]
    assert (clean.data_quality == "LATE").any()
    assert (clean.missing_interval == "true").any()
    report = pd.read_csv(pipeline.OUTPUT / latest / "telemetry_quality_report.csv", dtype=str, keep_default_na=False)
    gaps = set(clean.loc[clean.missing_interval == "true", "shipment_id"])
    assert any(float(report.set_index("shipment_id").loc[sid, "unknown_minutes"]) > 0 for sid in gaps)
    for sid in gaps:
        event_in_range = int(((clean.shipment_id == sid) & (clean.condition == "NORMAL")).sum())
        assert int(report.set_index("shipment_id").loc[sid, "in_range_readings"]) == event_in_range
    late_row = clean[clean.data_quality == "LATE"].iloc[0]
    shipment = pd.read_csv(pipeline.OUTPUT / latest / "dim_shipment.csv", dtype=str, keep_default_na=False).set_index("shipment_id").loc[late_row.shipment_id]
    assert (late_row.is_late == "true") == (float(late_row.lateness_minutes) > float(shipment.allowed_lateness_minutes))


def test_deterministic_rows_sorted_and_profile_ranges_are_per_shipment():
    latest = (pipeline.OUTPUT / "LATEST").read_text().strip()
    run_dir = pipeline.OUTPUT / latest
    shipments = pd.read_csv(run_dir / "dim_shipment.csv", dtype=str, keep_default_na=False)
    assert shipments.shipment_id.tolist() == sorted(shipments.shipment_id.tolist())
    profiles = {row.storage_profile.lower(): (row.target_min_c, row.target_max_c) for row in shipments.itertuples(index=False)}
    assert "refrigerated" in profiles
    assert profiles["refrigerated"] == ("2.00", "8.00")
    controlled = next((v for k, v in profiles.items() if "controlled" in k), None)
    assert controlled == ("15.00", "25.00")
    events = pd.read_csv(run_dir / "fact_sensor_event.csv", dtype=str, keep_default_na=False)
    clean = events[events.validation_status == "CLEAN"].merge(shipments, on="shipment_id", suffixes=("", "_shipment"))
    for profile in ("Refrigerated", "Controlled Room / Ambient"):
        rows = clean[clean.storage_profile == profile]
        assert (rows.condition == "NORMAL").any()
        assert (rows.condition == "EXCURSION").any()
        normal_temps = rows.loc[rows.condition == "NORMAL", "temperature_c"].astype(float)
        assert ((normal_temps >= float(rows.target_min_c.iloc[0])) & (normal_temps <= float(rows.target_max_c.iloc[0]))).all()


def test_manifest_mismatch_cannot_be_marked_pass():
    with pytest.raises(ValueError):
        pipeline.validate_reconciliation(10, 11)
