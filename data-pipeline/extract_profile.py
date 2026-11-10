#!/usr/bin/env python3
"""
Cold-Chain Excursion Platform - Data Extraction and Profiling Pipeline
Extracts operational telemetry, profiles data quality, and generates analytical CSV exports.
"""

import os
import sys
import json
import csv
import urllib.request
import urllib.error
from datetime import datetime

OUTPUT_DIR = os.path.join(os.path.dirname(__file__), "output")
API_BASE_URL = os.getenv("API_BASE_URL", "http://localhost:8080/api")

def fetch_json(endpoint):
    url = f"{API_BASE_URL}/{endpoint.lstrip('/')}"
    try:
        req = urllib.request.Request(url, headers={"Accept": "application/json"})
        with urllib.request.urlopen(req, timeout=10) as response:
            return json.loads(response.read().decode("utf-8"))
    except Exception as e:
        print(f"[WARN] Could not fetch from {url}: {e}")
        return []

def write_csv(filename, fieldnames, rows):
    os.makedirs(OUTPUT_DIR, exist_ok=True)
    filepath = os.path.join(OUTPUT_DIR, filename)
    with open(filepath, "w", newline="", encoding="utf-8") as f:
        writer = csv.DictWriter(f, fieldnames=fieldnames)
        writer.writeheader()
        for r in rows:
            writer.writerow({k: r.get(k, "") for k in fieldnames})
    print(f"[INFO] Exported {len(rows)} records to {filepath}")
    return filepath

def main():
    print("=" * 60)
    print("Cold-Chain Excursion Platform - Analytical Extraction & Profiling")
    print("=" * 60)

    # 1. Fetch data
    shipments = fetch_json("/shipments")
    excursions = fetch_json("/excursions")
    reconciliation = fetch_json("/reconciliation/summary")

    print(f"\n[PROFILING SUMMARY]")
    print(f"Total Shipments:          {len(shipments)}")
    print(f"Total Excursions:         {len(excursions)}")
    print(f"Reconciliation Status:    {reconciliation.get('explanation', 'N/A')}")
    print(f"Raw Events in Ledger:     {reconciliation.get('totalRawEvents', 0)}")
    print(f"Duplicates Suppressed:    {reconciliation.get('totalDuplicateEvents', 0)}")
    print(f"Quarantined Events:       {reconciliation.get('totalQuarantinedEvents', 0)}")
    print(f"Missing (UNKNOWN) Gaps:   {reconciliation.get('totalMissingIntervals', 0)}")

    # 2. Export analytical tables
    if shipments:
        write_csv(
            "dim_shipment.csv",
            ["id", "trackingNumber", "origin", "destination", "storageProfileId", "status", "expectedCadenceMinutes", "allowedLatenessMinutes"],
            shipments
        )

    if excursions:
        write_csv(
            "fact_excursion.csv",
            ["id", "shipmentId", "ruleVersion", "severity", "status", "startEventTime", "endEventTime", "peakTemperatureC", "durationMinutes"],
            excursions
        )

    print("\nExtraction & Profiling completed successfully.")

if __name__ == "__main__":
    main()
