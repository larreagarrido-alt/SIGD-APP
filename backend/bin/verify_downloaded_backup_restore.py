"""Restore a downloaded SQL copy into a NEW loopback-only MariaDB instance.

No production connection, no cleanup, no row values printed. The owned lab remains
on disk for inspection. Run only with authorization to handle the real export.
Usage: python verify_downloaded_backup_restore.py PATH_TO_SQL
"""
from __future__ import annotations

import json
import re
import socket
import subprocess
import sys
import tempfile
import time
from pathlib import Path

PORT = 13318
BIN = Path("C:/xampp/mysql/bin")
PHP_TEST_FIXTURE = Path(__file__).resolve().parents[1] / "tests/fixtures/attendance_schema.sql"


def run(args: list[str], *, data: bytes | None = None) -> subprocess.CompletedProcess[bytes]:
    return subprocess.run(args, input=data, stdout=subprocess.PIPE, stderr=subprocess.PIPE, timeout=120)


def safe_error(result: subprocess.CompletedProcess[bytes]) -> str:
    raw = result.stderr.decode("utf-8", "replace")
    match = re.search(r"ERROR\s+\d+\s+\([^)]*\)\s+at line\s+\d+", raw)
    return match.group(0) if match else f"process exit {result.returncode}; details suppressed"


def main() -> None:
    if len(sys.argv) != 2:
        raise SystemExit("Usage: python verify_downloaded_backup_restore.py PATH_TO_SQL")
    source = Path(sys.argv[1]).resolve()
    if not source.is_file() or source.stat().st_size == 0:
        raise SystemExit("Downloaded backup is missing or empty")
    content = source.read_bytes()
    if re.search(rb"\b(?:CREATE|DROP)\s+DATABASE\b|\bUSE\s+`?[A-Za-z0-9_]+`?\s*;", content, re.I):
        raise SystemExit("Backup contains database selection or creation; manual review required")
    for exe in ("mysql_install_db.exe", "mysqld.exe", "mysql.exe", "mysqladmin.exe"):
        if not (BIN / exe).is_file():
            raise SystemExit(f"Missing local MariaDB tool: {exe}")
    with socket.socket() as probe:
        probe.settimeout(0.4)
        if probe.connect_ex(("127.0.0.1", PORT)) == 0:
            raise SystemExit("Reserved laboratory port is occupied; nothing imported")
    root = Path(tempfile.mkdtemp(prefix="KPIWeb_restore_owned_"))
    data_dir = root / "mysql-data"
    init = run([str(BIN / "mysql_install_db.exe"), f"--datadir={data_dir}", f"--port={PORT}", "--silent"])
    if init.returncode:
        raise SystemExit(f"Local MariaDB initialization failed: {safe_error(init)}")
    ini = data_dir / "my.ini"
    ini.write_text("[mysqld]\n" + f"datadir={data_dir.as_posix()}\nport={PORT}\n"
                   + "bind-address=127.0.0.1\nskip-log-bin\n"
                   + f"[client]\nport={PORT}\n", encoding="utf-8")
    (root / "owned-lab.json").write_text(json.dumps({"port": PORT, "data_dir": str(data_dir),
                                                      "purpose": "isolated backup restoration"}), encoding="utf-8")
    mysql = [str(BIN / "mysql.exe"), "--no-defaults", "--protocol=TCP", "--host=127.0.0.1",
             f"--port={PORT}", "--user=root", "--batch", "--raw", "--skip-column-names"]
    shutdown = [str(BIN / "mysqladmin.exe"), "--no-defaults", "--protocol=TCP", "--host=127.0.0.1",
                f"--port={PORT}", "--user=root", "shutdown"]
    log = (root / "server.log").open("wb")
    server = subprocess.Popen([str(BIN / "mysqld.exe"), f"--defaults-file={ini}", "--console"],
                              stdout=log, stderr=subprocess.STDOUT,
                              creationflags=getattr(subprocess, "CREATE_NO_WINDOW", 0))
    try:
        for _ in range(80):
            if server.poll() is not None:
                raise RuntimeError("Isolated MariaDB stopped before it became ready")
            ping = run(mysql + ["-e", "SELECT @@datadir"])
            if ping.returncode == 0:
                actual = Path(ping.stdout.decode("utf-8", "replace").strip()).resolve()
                if actual != data_dir.resolve():
                    raise RuntimeError("Laboratory ownership check failed; import refused")
                break
            time.sleep(0.25)
        else:
            raise RuntimeError("Isolated MariaDB did not become ready")
        create = run(mysql + ["-e", "CREATE DATABASE `wsnet_bd_asistencia` CHARACTER SET utf8mb4; "
                            "CREATE DATABASE `wsnet_websiteplus` CHARACTER SET utf8mb4;"])
        if create.returncode:
            raise RuntimeError("Cannot create isolated schemas: " + safe_error(create))
        fixture = run(mysql + ["--database=wsnet_bd_asistencia"], data=PHP_TEST_FIXTURE.read_bytes())
        if fixture.returncode:
            raise RuntimeError("Attendance DDL fixture failed: " + safe_error(fixture))
        with source.open("rb") as stream:
            import_result = subprocess.run(mysql + ["--database=wsnet_websiteplus"], stdin=stream,
                                           stdout=subprocess.PIPE, stderr=subprocess.PIPE, timeout=180)
        if import_result.returncode:
            raise RuntimeError("Backup import failed: " + safe_error(import_result))
        query = ("SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='wsnet_websiteplus' "
                 "AND table_type='BASE TABLE'; "
                 "SELECT COUNT(*) FROM information_schema.views WHERE table_schema='wsnet_websiteplus'; "
                 "SELECT COUNT(*) FROM wsnet_websiteplus.wp_migraciones; "
                 "SELECT COUNT(*) FROM wsnet_websiteplus.wp_kpi_definiciones;")
        result = run(mysql + ["-e", query])
        if result.returncode:
            raise RuntimeError("Restored schema checks failed: " + safe_error(result))
        counts = [int(v) for v in result.stdout.decode("ascii").split()]
        if len(counts) != 4:
            raise RuntimeError("Unexpected check result shape")
        print("RESTORE_OK base_tables=%d views=%d migration_rows=%d kpi_definitions=%d"
              % tuple(counts))
        print("OWNED_LAB=" + str(root))
        print("Real backup and isolated database were preserved; no production access")
    finally:
        if server.poll() is None:
            owned = run(mysql + ["-e", "SELECT @@datadir"])
            if owned.returncode == 0 and Path(owned.stdout.decode().strip()).resolve() == data_dir.resolve():
                run(shutdown)
        server.wait(timeout=20)
        log.close()


if __name__ == "__main__":
    try:
        main()
    except Exception as exc:
        print("RESTORE_FAILED " + str(exc), file=sys.stderr)
        raise SystemExit(1)
