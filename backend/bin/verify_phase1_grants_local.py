"""Test the approved phase-one role grants only in the owned local restore."""
from __future__ import annotations

import json
import socket
import subprocess
import sys
import tempfile
import time
from pathlib import Path

from verify_downloaded_backup_restore import BIN, PORT, run, safe_error


def count(mysql: list[str]) -> int:
    sql = ("SELECT COUNT(*) FROM wsnet_websiteplus.wp_rol_permisos rp "
           "JOIN wsnet_websiteplus.wp_permisos p ON p.id_permiso=rp.id_permiso "
           "WHERE (rp.id_rol=1 AND p.codigo IN ('dashboard.admin','auth.perfil')) "
           "OR (rp.id_rol IN (5,6) AND p.codigo IN ('auth.perfil','kpi.ver','kpi.entregar'))")
    result = run(mysql + ["-e", sql])
    if result.returncode:
        raise RuntimeError("Permission count failed: " + safe_error(result))
    return int(result.stdout.strip())


def main() -> None:
    if len(sys.argv) != 2:
        raise SystemExit("Usage: python verify_phase1_grants_local.py OWNED_LAB_DIR")
    root = Path(sys.argv[1]).resolve()
    if not root.name.startswith("KPIWeb_restore_owned_") or root.parent != Path(tempfile.gettempdir()).resolve():
        raise SystemExit("Not an owned isolated restore")
    owned = json.loads((root / "owned-lab.json").read_text(encoding="utf-8"))
    data_dir = (root / "mysql-data").resolve()
    if owned["port"] != PORT or Path(owned["data_dir"]).resolve() != data_dir:
        raise SystemExit("Owned laboratory marker mismatch")
    with socket.socket() as probe:
        probe.settimeout(0.4)
        if probe.connect_ex(("127.0.0.1", PORT)) == 0:
            raise SystemExit("Laboratory port already occupied")
    mysql = [str(BIN / "mysql.exe"), "--no-defaults", "--protocol=TCP", "--host=127.0.0.1",
             f"--port={PORT}", "--user=root", "--batch", "--raw", "--skip-column-names"]
    shutdown = [str(BIN / "mysqladmin.exe"), "--no-defaults", "--protocol=TCP", "--host=127.0.0.1",
                f"--port={PORT}", "--user=root", "shutdown"]
    log = (root / "phase1-server.log").open("wb")
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
        versions = run(mysql + ["-e", "SELECT COUNT(*) FROM wsnet_websiteplus.wp_migraciones "
                        "WHERE version IN ('V015','V016','V017','V018','V019')"])
        if versions.returncode or versions.stdout.strip() != b"5":
            raise RuntimeError("Expected the five new migrations before grant test")
        before = count(mysql)
        if before != 0:
            raise RuntimeError("Expected no baseline role grants in the local restore")
        roles = run(mysql + ["--database=wsnet_bd_asistencia", "-e",
                             "INSERT INTO roles (id_rol,nombre) VALUES "
                             "(1,'Administrador'),(5,'Colaborador'),(6,'Practicante')"])
        if roles.returncode:
            raise RuntimeError("Synthetic role fixture failed: " + safe_error(roles))
        proposal = Path(__file__).resolve().parents[2] / "docs/PERMISOS_FASE1_APROBADOS_2026-11-14.sql"
        for attempt in (1, 2):
            result = run(mysql + ["--database=wsnet_websiteplus"], data=proposal.read_bytes())
            if result.returncode:
                raise RuntimeError("Approved SQL failed in laboratory: " + safe_error(result))
            if count(mysql) != 8:
                raise RuntimeError("Expected exactly eight grants after attempt " + str(attempt))
        print("PHASE1_GRANTS_OK eight grants; second application idempotent")
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
        print("PHASE1_GRANTS_FAILED " + str(exc), file=sys.stderr)
        raise SystemExit(1)
