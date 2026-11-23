"""Apply V021 and the reviewed own-scope grant to an owned, isolated restore.

No cPanel connection, no row values in output, no deletion. The lab is preserved.
Usage: python verify_restored_bienestar.py OWNED_LAB_DIR
"""
from __future__ import annotations

import json
import socket
import subprocess
import sys
import tempfile
import time
from pathlib import Path

from verify_downloaded_backup_restore import BIN, PORT, run, safe_error

REPO = Path(__file__).resolve().parents[2]


def scalar(mysql: list[str], statement: str) -> int:
    result = run(mysql + ["-e", statement])
    if result.returncode:
        raise RuntimeError("Read-only check failed: " + safe_error(result))
    return int(result.stdout.strip())


def main() -> None:
    if len(sys.argv) not in (2, 3) or (len(sys.argv) == 3 and sys.argv[2] != "--inspect"):
        raise SystemExit("Usage: python verify_restored_bienestar.py OWNED_LAB_DIR [--inspect]")
    inspect_only = len(sys.argv) == 3
    root = Path(sys.argv[1]).resolve()
    marker = root / "owned-lab.json"
    if not root.name.startswith("KPIWeb_restore_owned_") or root.parent != Path(tempfile.gettempdir()).resolve() or not marker.is_file():
        raise SystemExit("Not a verified owned restore directory")
    owned = json.loads(marker.read_text(encoding="utf-8"))
    data_dir = Path(owned["data_dir"]).resolve()
    if owned["port"] != PORT or data_dir != (root / "mysql-data").resolve():
        raise SystemExit("Owned laboratory marker mismatch")
    with socket.socket() as probe:
        probe.settimeout(0.4)
        if probe.connect_ex(("127.0.0.1", PORT)) == 0:
            raise SystemExit("Laboratory port occupied; no import attempted")
    mysql = [str(BIN / "mysql.exe"), "--no-defaults", "--protocol=TCP", "--host=127.0.0.1",
             f"--port={PORT}", "--user=root", "--batch", "--raw", "--skip-column-names"]
    shutdown = [str(BIN / "mysqladmin.exe"), "--no-defaults", "--protocol=TCP", "--host=127.0.0.1",
                f"--port={PORT}", "--user=root", "shutdown"]
    log = (root / "bienestar-server.log").open("wb")
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
        before = [
            scalar(mysql, "SELECT COUNT(*) FROM wsnet_websiteplus.wp_migraciones WHERE version='V020'"),
            scalar(mysql, "SELECT COUNT(*) FROM wsnet_websiteplus.wp_migraciones WHERE version='V021'"),
            scalar(mysql, "SELECT COUNT(*) FROM wsnet_websiteplus.wp_bienestar_tests WHERE codigo IN ('DANI_SEMANAL','DANI_MENSUAL')"),
            scalar(mysql, "SELECT COUNT(*) FROM wsnet_websiteplus.wp_bienestar_preguntas"),
            scalar(mysql, "SELECT COUNT(*) FROM wsnet_websiteplus.wp_bienestar_opciones"),
            scalar(mysql, "SELECT COUNT(*) FROM wsnet_websiteplus.wp_rol_permisos rp JOIN wsnet_websiteplus.wp_permisos p ON p.id_permiso=rp.id_permiso WHERE p.codigo='bienestar.responder'"),
        ]
        if not inspect_only and before != [1, 0, 0, 0, 0, 0]:
            raise RuntimeError("Restored schema differs from the reviewed preflight; no import attempted")
        if not inspect_only:
            source_roles = scalar(mysql, "SELECT COUNT(*) FROM wsnet_bd_asistencia.roles")
            if source_roles == 0:
                # The websiteplus export does not contain the separate attendance database.
                # These three rows exist only in this owned local lab for grant validation.
                fixture_roles = run(mysql + ["--database=wsnet_bd_asistencia", "-e",
                    "INSERT INTO roles (id_rol,nombre) VALUES (1,'Administrador'),(5,'Colaborador'),(6,'Practicante')"])
                if fixture_roles.returncode:
                    raise RuntimeError("Synthetic attendance role fixture failed: " + safe_error(fixture_roles))
            migration = (REPO / "database/migrations/V021__bienestar_dani_cuestionarios.up.sql").read_bytes()
            result = run(mysql + ["--database=wsnet_websiteplus"], data=migration)
            if result.returncode:
                raise RuntimeError("V021 failed: " + safe_error(result))
            grant = (REPO / "database/proposals/bienestar_responder_roles_1_5_6.sql").read_bytes()
            result = run(mysql + ["--database=wsnet_websiteplus"], data=grant)
            if result.returncode:
                raise RuntimeError("Own-scope grant failed: " + safe_error(result))
        after = [
            scalar(mysql, "SELECT COUNT(*) FROM wsnet_websiteplus.wp_migraciones WHERE version='V021'"),
            scalar(mysql, "SELECT COUNT(*) FROM wsnet_websiteplus.wp_bienestar_tests WHERE codigo IN ('DANI_SEMANAL','DANI_MENSUAL') AND activo=1"),
            scalar(mysql, "SELECT COUNT(*) FROM wsnet_websiteplus.wp_bienestar_preguntas"),
            scalar(mysql, "SELECT COUNT(*) FROM wsnet_websiteplus.wp_bienestar_opciones"),
            scalar(mysql, "SELECT COUNT(*) FROM wsnet_websiteplus.wp_rol_permisos rp JOIN wsnet_websiteplus.wp_permisos p ON p.id_permiso=rp.id_permiso WHERE p.codigo='bienestar.responder' AND rp.id_rol IN (1,5,6) AND rp.alcance='PROPIO'"),
            scalar(mysql, "SELECT COUNT(*) FROM wsnet_websiteplus.wp_rol_permisos rp JOIN wsnet_websiteplus.wp_permisos p ON p.id_permiso=rp.id_permiso WHERE p.codigo='bienestar.ver_privado'"),
        ]
        if after != [1, 2, 16, 80, 3, 0]:
            raise RuntimeError("Unexpected post-import counts " + str(after) + "; inspect the preserved isolated lab")
        print("BIENESTAR_RESTORE_OK v021=1 questionnaires=2 questions=16 options=80 own_grants=3 private_grants=0")
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
        print("BIENESTAR_RESTORE_FAILED " + str(exc), file=sys.stderr)
        raise SystemExit(1)
