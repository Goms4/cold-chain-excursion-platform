#!/usr/bin/env python3
"""Offline validation of the latest pipeline manifest; performs no API calls."""
import sys
from pathlib import Path

import pandas as pd

root = Path(__file__).resolve().parent
try:
    run_id = (root / "output" / "LATEST").read_text(encoding="utf-8").strip()
    manifest = pd.read_csv(root / "output" / run_id / "manifest.csv", dtype=str, keep_default_na=False)
    print(manifest.to_csv(index=False, lineterminator="\n"), end="")
    if not (manifest.status == "PASS").all():
        raise ValueError("Latest manifest contains a failed dataset")
except Exception as exc:
    print(f"Manifest validation failed: {exc}", file=sys.stderr)
    sys.exit(1)
