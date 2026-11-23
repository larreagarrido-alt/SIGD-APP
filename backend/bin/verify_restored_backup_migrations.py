"""Apply only V015..V019 from the reviewed ZIP to the owned local restore.

No historical migrator, no production connection, no row values printed.
Usage: python verify_restored_backup_migrations.py OWNED_LAB_DIR CANDIDATE_ZIP
"""
from __future__ import annotations

import hashlib
import json
import socket
import subprocess
import sys
import tempfile
import time
import zipfile
from pathlib import Path

from verify_downloaded_backup_restore import BIN, PORT, run, safe_error

EXPECTED_ZIP_SHA = "A1E2019A2844575BDE82333C7B17B4AD94AEA029B5F001C6EAD815D0BB8A6D78"
MIGRATIONS = {
    "V015": "V015__kpi_registros_android.up.sql",
    "V016": "V016__biometria_por_dispositivo.up.sql",
    "V017": "V017__rostro_por_dispositivo.up.sql",
    "V018": "V018__kpi_formularios_detalle.up.sql",
    "V019": "V019__kpi_borradores_admin.up.sql",
}


def scalar(mysql: list[str], statement: str) -> int:
    result = run(mysql + ["-e", statement])
    if result.returncode:
        raise RuntimeError("Read-only check failed: " + safe_error(result))
    return int(result.stdout.strip())


def main() -> None:
    if len(sys.argv) != 3:
        raise SystemExit("Usage: python verify_restored_backup_migrations.py OWNED_LAB_DIR CANDIDATE_ZIP")
    root = Path(sys.argv[1]).resolve()
    marker = root / "owned-lab.json"
    if not root.name.startswith("KPIWeb_restore_owned_") or root.parent != Path(tempfile.gettempdir()).resolve() or not marker.is_file():
        raise SystemExit("Not a verified owned restore directory")
    owned = json.loads(marker.read_text(encoding="utf-8"))
    data_dir = Path(owned["data_dir"]).resolve()
    if owned["port"] != PORT or data_dir != (root / "mysql-data").resolve():
        raise SystemExit("Owned laboratory marker mismatch")
    candidate = Path(sys.argv[2]).resolve()
    if hashlib.sha256(candidate.read_bytes()).hexdigest().upper() != EXPECTED_ZIP_SHA:
        raise SystemExit("Deployment candidate SHA-256 mismatch")
    with socket.socket() as probe:
        probe.settimeout(0.4)
        if probe.connect_ex(("127.0.0.1", PORT)) == 0:
            raise SystemExit("Laboratory port already occupied; no migration attempted")
    mysql = [str(BIN / "mysql.exe"), "--no-defaults", "--protocol=TCP", "--host=127.0.0.1",
             f"--port={PORT}", "--user=root", "--batch", "--raw", "--skip-column-names"]
    shutdown = [str(BIN / "mysqladmin.exe"), "--no-defaults", "--protocol=TCP", "--host=127.0.0.1",
                f"--port={PORT}", "--user=root", "shutdown"]
    log = (root / "migration-server.log").open("wb")
    server = subprocess.Popen([str(BIN / "mysqld.exe"), f"--defaults-file={data_dir / 'my.ini'}", "--console"],
                              stdout=log, stderr=subprocess.STDOUT,
                              creationflags=getattr(subprocess, "CREATE_NO_WINDOW", 0))
    try:
        for _ in range(80):
            if server.poll() is not None:
                raise RuntimeError("Owned MariaDB stopped during startup")
            ping = run(mysql + ["-e", "SELECT @@datadir"])
            if ping.returncode == 0:
                if Path(ping.stdout.decode().strip()).resolve() != data_dir:
                    raise RuntimeError("Laboratory ownership check failed; no migration attempted")
                break
            time.sleep(0.25)
        else:
            raise RuntimeError("Owned MariaDB did not become ready")
        before = scalar(mysql, "SELECT COUNT(*) FROM wsnet_websiteplus.wp_migraciones "
                        "WHERE version IN ('V012','V015','V016','V017','V018','V019')")
        if before:
            raise RuntimeError("Expected V012 and V015..V019 absent in restored backup")
        with zipfile.ZipFile(candidate) as release:
            for version, filename in MIGRATIONS.items():
                sql = release.read("database/migrations/" + filename)
                result = run(mysql + ["--database=wsnet_websiteplus"], data=sql)
                if result.returncode:
                    raise RuntimeError(version + " failed: " + safe_error(result))
                found = scalar(mysql, "SELECT COUNT(*) FROM wsnet_websiteplus.wp_migraciones "
                               f"WHERE version='{version}'")
                if found != 1:
                    raise RuntimeError(version + " did not register exactly once")
                print(version + "_OK")
        result = run(mysql + ["-e", "SELECT COUNT(*) FROM information_schema.tables "
                      "WHERE table_schema='wsnet_websiteplus' AND table_type='BASE TABLE'; "
                      "SELECT COUNT(*) FROM information_schema.views "
                      "WHERE table_schema='wsnet_websiteplus'; "
                      "SELECT COUNT(*) FROM wsnet_websiteplus.wp_migraciones "
                      "WHERE version IN ('V015','V016','V017','V018','V019'); "
                      "SELECT COUNT(*) FROM wsnet_websiteplus.wp_migraciones WHERE version='V012';"])
        if result.returncode:
            raise RuntimeError("Post-migration check failed: " + safe_error(result))
        counts = [int(v) for v in result.stdout.decode("ascii").split()]
        if len(counts) != 4 or counts[2:] != [5, 0]:
            raise RuntimeError("Unexpected migration version state")
        print("MIGRATIONS_OK base_tables=%d views=%d new_versions=%d v012=%d" % tuple(counts))
        print("Owned local restore preserved; no production access")
    finally:
        if server.poll() is None:
            check = run(mysql + ["-e", "SELECT @@datadir"])
            if check.returncode == 0 and Path(check.stdout.decode().strip()).resolve() == data_dir:
                run(shutdown)
        server.wait(timeout=20)
        log.close()


if __name__ == "__main__":
    try:
        main()
    except Exception as exc:
        print("MIGRATION_CHECK_FAILED " + str(exc), file=sys.stderr)
        raise SystemExit(1)
