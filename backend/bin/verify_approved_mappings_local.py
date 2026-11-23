"""Test the exact approved cargo SQL in the owned local restore, with synthetic cargos."""
from __future__ import annotations

import json
import socket
import subprocess
import sys
import tempfile
import time
from pathlib import Path

from verify_downloaded_backup_restore import BIN, PORT, run, safe_error

NAMES = {
    1: "Gerente General", 2: "Jefe de Agencia", 3: "Creativa/Diseño",
    4: "Programador Web", 5: "Ejecutivo Comercial", 6: "Programador Junior",
    7: "Diseñador Junior", 13: "Community Manager", 14: "Diseñador Senior",
    15: "Community Manager Junior", 16: "Community Manager Senior",
    17: "Creativo / Videógrafo",
}


def scalar(mysql: list[str], statement: str) -> int:
    result = run(mysql + ["-e", statement])
    if result.returncode:
        raise RuntimeError("Local read failed: " + safe_error(result))
    return int(result.stdout.strip())


def main() -> None:
    if len(sys.argv) != 3:
        raise SystemExit("Usage: python verify_approved_mappings_local.py OWNED_LAB_DIR APPROVED_SQL")
    root = Path(sys.argv[1]).resolve()
    if not root.name.startswith("KPIWeb_restore_owned_") or root.parent != Path(tempfile.gettempdir()).resolve():
        raise SystemExit("Not an owned isolated restore")
    owned = json.loads((root / "owned-lab.json").read_text(encoding="utf-8"))
    data_dir = (root / "mysql-data").resolve()
    if owned["port"] != PORT or Path(owned["data_dir"]).resolve() != data_dir:
        raise SystemExit("Owned laboratory marker mismatch")
    sql = Path(sys.argv[2]).resolve().read_bytes()
    if sql.count(b"INSERT INTO wp_kpi_formulario_cargos") != 11 or b"WHERE c.id_cargo=2 " in sql:
        raise SystemExit("Expected exactly 11 approved mappings and no cargo 2")
    with socket.socket() as probe:
        probe.settimeout(0.4)
        if probe.connect_ex(("127.0.0.1", PORT)) == 0:
            raise SystemExit("Laboratory port already occupied")
    mysql = [str(BIN / "mysql.exe"), "--no-defaults", "--protocol=TCP", "--host=127.0.0.1",
             f"--port={PORT}", "--user=root", "--batch", "--raw", "--skip-column-names"]
    shutdown = [str(BIN / "mysqladmin.exe"), "--no-defaults", "--protocol=TCP", "--host=127.0.0.1",
                f"--port={PORT}", "--user=root", "shutdown"]
    log = (root / "mappings-server.log").open("wb")
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
                    raise RuntimeError("Laboratory ownership check failed")
                break
            time.sleep(0.25)
        else:
            raise RuntimeError("Owned MariaDB did not become ready")
        before = scalar(mysql, "SELECT COUNT(*) FROM wsnet_websiteplus.wp_kpi_formulario_cargos")
        if before:
            raise RuntimeError("Expected no cargo mappings in the restored backup")
        rows = ",".join(f"({i},'{name}',0)" for i, name in NAMES.items())
        fixture = run(mysql + ["--database=wsnet_bd_asistencia", "-e",
                               "INSERT INTO cargos (id_cargo,nombre,sueldo_base) VALUES " + rows])
        if fixture.returncode:
            raise RuntimeError("Synthetic cargo fixture failed: " + safe_error(fixture))
        for attempt in (1, 2):
            result = run(mysql + ["--database=wsnet_websiteplus"], data=sql)
            if result.returncode:
                raise RuntimeError("Approved mapping SQL failed: " + safe_error(result))
            count = scalar(mysql, "SELECT COUNT(*) FROM wsnet_websiteplus.wp_kpi_formulario_cargos")
            cargo2 = scalar(mysql, "SELECT COUNT(*) FROM wsnet_websiteplus.wp_kpi_formulario_cargos WHERE id_cargo=2")
            if count != 11 or cargo2:
                raise RuntimeError("Unexpected mapping state after attempt " + str(attempt))
        print("APPROVED_MAPPINGS_OK eleven mappings; cargo 2 excluded; second application idempotent")
        print("No production access; no real account or password created")
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
        print("APPROVED_MAPPINGS_FAILED " + str(exc), file=sys.stderr)
        raise SystemExit(1)
