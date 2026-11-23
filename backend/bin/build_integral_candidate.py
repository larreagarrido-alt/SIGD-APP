"""Build one reviewed deployment ZIP from an unchanged biometric v6 baseline.

Usage: python build_integral_candidate.py PATH_TO_V6_ZIP OUTPUT_ZIP
No database, network, credential, or production access.
"""
from __future__ import annotations

import hashlib
import sys
import zipfile
from pathlib import Path


EXPECTED_V6 = "C8715D48B642E6B856C9306A76DFBDCD7C88DDACAAE0B56CAD3AC6E471B26CC1"
EXTRA = [
    "websiteplus_backend/src/Auth/TokenService.php",
    "websiteplus_backend/src/Controllers/KpiFormsController.php",
    "websiteplus_backend/src/Controllers/KpiDraftsController.php",
    "database/migrations/V018__kpi_formularios_detalle.up.sql",
    "database/migrations/V018__kpi_formularios_detalle.down.sql",
    "database/migrations/V019__kpi_borradores_admin.up.sql",
    "database/migrations/V019__kpi_borradores_admin.down.sql",
]
CHANGED_FROM_V6 = {
    "websiteplus_backend/src/Controllers/AuthController.php",
    "websiteplus_backend/src/Controllers/DashboardController.php",
    "public_html/websiteplus-api/index.php",
}


def digest(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest().upper()


def source(root: Path, name: str) -> Path:
    if name.startswith("websiteplus_backend/"):
        path = root / "backend" / name.removeprefix("websiteplus_backend/")
    elif name == "public_html/websiteplus-api/index.php":
        path = root / "backend/public/index.php"
    elif name.startswith("database/migrations/"):
        path = root / name
    else:
        raise ValueError(f"Unexpected package path: {name}")
    if not path.resolve().is_relative_to(root):
        raise ValueError("Path escapes workspace")
    return path


def main() -> None:
    if len(sys.argv) != 3:
        raise SystemExit("Usage: python build_integral_candidate.py V6_ZIP OUTPUT_ZIP")
    root = Path(__file__).resolve().parents[2]
    v6 = Path(sys.argv[1]).resolve()
    output = Path(sys.argv[2]).resolve()
    if not v6.is_file() or digest(v6.read_bytes()) != EXPECTED_V6:
        raise SystemExit("Biometric v6 candidate does not match its reviewed SHA-256")
    with zipfile.ZipFile(v6) as baseline:
        base_names = baseline.namelist()
        if len(base_names) != 18 or len(set(base_names)) != 18:
            raise SystemExit("Unexpected v6 package contents")
        entries = {}
        for name in base_names + EXTRA:
            if name in entries:
                raise SystemExit(f"Duplicate entry: {name}")
            path = source(root, name)
            if not path.is_file():
                raise SystemExit(f"Missing local dependency: {path}")
            data = path.read_bytes()
            if name in base_names and name not in CHANGED_FROM_V6 and data != baseline.read(name):
                raise SystemExit(f"Reviewed biometric v6 file was changed: {name}")
            entries[name] = data
    manifest = "\n".join(f"{digest(data)}  {name}" for name, data in sorted(entries.items())) + "\n"
    readme = (
        "KPIWeb deployment candidate. NOT PUBLISHED.\n"
        "Backend paths in this ZIP are relative to the cPanel account home.\n"
        "The database/migrations files are manual review artifacts, not web files.\n"
        "Review docs/GUIA_PUBLICACION_INTEGRAL_KPI_2026-11-14.md in the local workspace.\n"
        "Verify recent server backup and isolated restoration before any production change.\n"
        "Apply only reviewed up migrations in order V015, V016, V017, V018, V019.\n"
        "Keep V012 absent. Copy PHP controllers/services, including TokenService.php, then public index.php last.\n"
        "Do not deploy without approved cargo mappings and authorized staging tests.\n"
        "Do not run down scripts automatically; preserve all biometric and KPI records.\n"
    )
    output.parent.mkdir(parents=True, exist_ok=True)
    with zipfile.ZipFile(output, "w", compression=zipfile.ZIP_DEFLATED) as candidate:
        for name, data in sorted(entries.items()):
            candidate.writestr(name, data)
        candidate.writestr("MANIFEST_SHA256.txt", manifest)
        candidate.writestr("README_PUBLICACION.txt", readme)
    with zipfile.ZipFile(output) as candidate:
        for name, data in entries.items():
            if candidate.read(name) != data:
                raise SystemExit(f"Archive verification failed: {name}")
    print(f"FILES={len(entries)} ZIP_SHA256={digest(output.read_bytes())} OUTPUT={output}")


if __name__ == "__main__":
    main()
