"""Exercise the reversible prototype-form suspension on the owned MariaDB lab."""
from __future__ import annotations

import json
import socket
import subprocess
import sys
import tempfile
import time
from pathlib import Path

from verify_downloaded_backup_restore import BIN, PORT, run, safe_error


def main() -> None:
    if len(sys.argv) != 4:
        raise SystemExit("Usage: verify_mapping_suspension_local.py OWNED_LAB_DIR SUSPEND_SQL RESTORE_SQL")
    root, suspend_path, restore_path = (Path(arg).resolve() for arg in sys.argv[1:])
    if root.parent != Path(tempfile.gettempdir()).resolve() or not root.name.startswith("KPIWeb_restore_owned_"):
        raise SystemExit("Not an owned isolated restore")
    data_dir = (root / "mysql-data").resolve()
    owned = json.loads((root / "owned-lab.json").read_text(encoding="utf-8"))
    if owned["port"] != PORT or Path(owned["data_dir"]).resolve() != data_dir:
        raise SystemExit("Owned laboratory marker mismatch")
    suspend_sql = suspend_path.read_bytes()
    restore_sql = restore_path.read_bytes()
    if suspend_sql.count(b"(1, 'GERENCIA')") != 1 or suspend_sql.count(b"AND aprobado_en IS NOT NULL") != 1:
        raise SystemExit("Unexpected suspension SQL")
    with socket.socket() as probe:
        probe.settimeout(0.4)
        if probe.connect_ex(("127.0.0.1", PORT)) == 0:
            raise SystemExit("Laboratory port already occupied")
    mysql = [str(BIN / "mysql.exe"), "--no-defaults", "--protocol=TCP", "--host=127.0.0.1",
             f"--port={PORT}", "--user=root", "--batch", "--raw", "--skip-column-names"]
    shutdown = [str(BIN / "mysqladmin.exe"), "--no-defaults", "--protocol=TCP", "--host=127.0.0.1",
                f"--port={PORT}", "--user=root", "shutdown"]
    log = (root / "suspension-server.log").open("wb")
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

        def counts() -> tuple[int, int, int]:
            result = run(mysql + ["--database=wsnet_websiteplus", "-e",
                "SELECT COUNT(*),SUM(aprobado_en IS NOT NULL),"
                "(SELECT COUNT(*) FROM wp_kpi_formularios_detalle) FROM wp_kpi_formulario_cargos"])
            if result.returncode:
                raise RuntimeError("Local read failed: " + safe_error(result))
            return tuple(map(int, result.stdout.decode().strip().split("\t")))

        before = counts()
        if before != (11, 11, 0):
            raise RuntimeError("Unexpected local preflight counts")
        result = run(mysql + ["--database=wsnet_websiteplus"], data=suspend_sql)
        if result.returncode:
            raise RuntimeError("Suspension failed: " + safe_error(result))
        suspended = counts()
        if suspended != (11, 0, 0):
            raise RuntimeError("Suspension changed unexpected local state")
        result = run(mysql + ["--database=wsnet_websiteplus"], data=restore_sql)
        if result.returncode:
            raise RuntimeError("Local reversal failed: " + safe_error(result))
        if counts() != (11, 11, 0):
            raise RuntimeError("Local reversal did not restore approvals")
        print("MAPPING_SUSPENSION_OK 11 preserved, 0 active; local reversal restored 11")
        print("No production access; no real account or report changed")
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
        print("MAPPING_SUSPENSION_FAILED " + str(exc), file=sys.stderr)
        raise SystemExit(1)
