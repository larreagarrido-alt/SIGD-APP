"""Exercise the published wellness submit path on the owned, isolated SQL restore.

Uses a synthetic worker and synthetic answers. The production export is never changed.
"""
from __future__ import annotations

import json
import socket
import subprocess
import sys
import tempfile
import time
from pathlib import Path

from verify_downloaded_backup_restore import BIN, PORT, run

REPO = Path(__file__).resolve().parents[2]


def main() -> None:
    if len(sys.argv) not in (2, 3) or (len(sys.argv) == 3 and sys.argv[2] not in ("--ansi", "--limited", "--partial")):
        raise SystemExit("Usage: python repro_restored_wellness.py OWNED_LAB_DIR [--ansi|--limited|--partial]")
    partial = len(sys.argv) == 3 and sys.argv[2] == "--partial"
    limited = len(sys.argv) == 3 and sys.argv[2] in ("--limited", "--partial")
    ansi = len(sys.argv) == 3 and sys.argv[2] == "--ansi"
    root = Path(sys.argv[1]).resolve()
    marker = root / "owned-lab.json"
    if root.parent != Path(tempfile.gettempdir()).resolve() or not root.name.startswith("KPIWeb_restore_owned_") or not marker.is_file():
        raise SystemExit("Not an owned isolated restore")
    owned = json.loads(marker.read_text(encoding="utf-8"))
    data_dir = (root / "mysql-data").resolve()
    if owned["port"] != PORT or Path(owned["data_dir"]).resolve() != data_dir:
        raise SystemExit("Owned lab marker mismatch")
    with socket.socket() as probe:
        if probe.connect_ex(("127.0.0.1", PORT)) == 0:
            raise SystemExit("Lab port already in use")

    mysql = [str(BIN / "mysql.exe"), "--no-defaults", "--protocol=TCP", "--host=127.0.0.1",
             f"--port={PORT}", "--user=root", "--batch", "--raw", "--skip-column-names"]
    log = (root / "wellness-repro-server.log").open("wb")
    server = subprocess.Popen([str(BIN / "mysqld.exe"), f"--defaults-file={data_dir / 'my.ini'}", "--console"],
                              stdout=log, stderr=subprocess.STDOUT,
                              creationflags=getattr(subprocess, "CREATE_NO_WINDOW", 0))
    try:
        for _ in range(80):
            if server.poll() is not None:
                raise RuntimeError("Owned MariaDB stopped")
            ready = run(mysql + ["-e", "SELECT @@datadir"])
            if ready.returncode == 0:
                if Path(ready.stdout.decode().strip()).resolve() != data_dir:
                    raise RuntimeError("Unexpected MariaDB data directory")
                break
            time.sleep(.25)
        else:
            raise RuntimeError("Owned MariaDB did not become ready")
        if limited:
            grant = run(mysql + ["-e", "CREATE USER IF NOT EXISTS 'wellness_limited'@'127.0.0.1' IDENTIFIED BY 'synthetic-only-local'; GRANT SELECT, INSERT, UPDATE ON wsnet_websiteplus.* TO 'wellness_limited'@'127.0.0.1'; FLUSH PRIVILEGES"])
            if grant.returncode:
                raise RuntimeError("Could not create limited synthetic lab account")
        if partial:
            fixture = run(mysql + ["--database=wsnet_websiteplus", "-e", "INSERT INTO wp_bienestar_evaluaciones (id_test,id_trabajador,periodo_clave,estado) SELECT id_test,999995,DATE_FORMAT(CURRENT_DATE(),'%x-W%v'),'EN_PROCESO' FROM wp_bienestar_tests WHERE codigo='DANI_SEMANAL'; INSERT INTO wp_bienestar_respuestas (id_evaluacion_bienestar,id_pregunta,id_opcion_seleccionada) SELECT e.id_evaluacion_bienestar,q.id_pregunta,MIN(o.id_opcion) FROM wp_bienestar_evaluaciones e JOIN wp_bienestar_preguntas q ON q.id_test=e.id_test JOIN wp_bienestar_opciones o ON o.id_pregunta=q.id_pregunta WHERE e.id_trabajador=999995 GROUP BY e.id_evaluacion_bienestar,q.id_pregunta LIMIT 1"])
            if fixture.returncode:
                raise RuntimeError("Could not prepare synthetic partial evaluation")
        php = r'''<?php
declare(strict_types=1);
require $argv[1].'/backend/src/Core/Autoloader.php';
\WebSitePlus\Core\Autoloader::register();
$limited=($argv[3] ?? '') === 'limited';
$db = new PDO('mysql:host=127.0.0.1;port=' . $argv[2] . ';dbname=wsnet_websiteplus;charset=utf8mb4', $limited ? 'wellness_limited' : 'root', $limited ? 'synthetic-only-local' : '', [PDO::ATTR_ERRMODE=>PDO::ERRMODE_EXCEPTION, PDO::ATTR_EMULATE_PREPARES=>false]);
if (($argv[3] ?? '') === 'ansi') $db->exec("SET SESSION sql_mode='ANSI_QUOTES'");
\WebSitePlus\Core\Database::setConnection($db);
$test = $db->query("SELECT id_test FROM wp_bienestar_tests WHERE codigo='DANI_SEMANAL' AND activo=1")->fetchColumn();
$q = $db->prepare('SELECT q.id_pregunta,MIN(o.id_opcion) AS id_opcion FROM wp_bienestar_preguntas q JOIN wp_bienestar_opciones o ON o.id_pregunta=q.id_pregunta WHERE q.id_test=? GROUP BY q.id_pregunta');
$q->execute([$test]);
$answers=[];
foreach($q as $row) $answers[]=['id_pregunta'=>(int)$row['id_pregunta'],'id_opcion_seleccionada'=>(int)$row['id_opcion']];
if(count($answers)!==6) throw new RuntimeException('Synthetic questionnaire count mismatch');
$request = new \WebSitePlus\Core\Request();
$property = new ReflectionProperty($request, 'body');
$property->setValue($request, ['id_test'=>(int)$test, 'respuestas'=>$answers]);
$syntheticId=(int)$argv[4];
$session=['id_usuario'=>$syntheticId,'id_trabajador'=>$syntheticId,'id_rol'=>5,'username'=>'synthetic'];
try { (new \WebSitePlus\Controllers\WellnessController())->submit($request,$session); }
catch(\WebSitePlus\Core\HttpException $e) { if (($argv[3] ?? '') === 'partial') { echo json_encode(['status'=>'conflict','code'=>$e->status]); exit(0); } fwrite(STDERR,'REPRO_ERROR HttpException'.PHP_EOL); exit(2); }
catch(Throwable $e) { fwrite(STDERR,'REPRO_ERROR '.get_class($e).' '.basename($e->getFile()).':'.$e->getLine().PHP_EOL); exit(2); }
'''
        script = root / "wellness-submit-synthetic.php"
        script.write_text(php, encoding="utf-8")
        synthetic_id = 999997 if ansi else (999995 if partial else (999996 if limited else 999998))
        mode = "ansi" if ansi else ("partial" if partial else ("limited" if limited else "normal"))
        result = run([r"C:\xampp\php\php.exe", str(script), str(REPO), str(PORT), mode, str(synthetic_id)])
        if result.returncode:
            raise RuntimeError(result.stderr.decode(errors="replace").splitlines()[0][:220])
        payload = json.loads(result.stdout)
        count = run(mysql + ["-e", f"SELECT COUNT(*) FROM wsnet_websiteplus.wp_bienestar_evaluaciones WHERE id_trabajador={synthetic_id} AND estado='COMPLETADA'"])
        if count.returncode:
            raise RuntimeError("Synthetic persistence check failed")
        if partial:
            intact = run(mysql + ["-e", "SELECT COUNT(*) FROM wsnet_websiteplus.wp_bienestar_respuestas r JOIN wsnet_websiteplus.wp_bienestar_evaluaciones e ON e.id_evaluacion_bienestar=r.id_evaluacion_bienestar WHERE e.id_trabajador=999995 AND e.estado='EN_PROCESO'"])
            if payload.get("status") != "conflict" or payload.get("code") != 409 or intact.returncode or intact.stdout.strip() != b"1":
                raise RuntimeError("Existing partial answers were not preserved")
            print("RESTORED_WELLNESS_PARTIAL_OK conflict=409 previous_answers=1")
        elif ansi:
            if payload.get("status") != "error" or not payload.get("error_code", "").startswith("WELLNESS_WRITE_1054_L") or count.stdout.strip() != b"0":
                raise RuntimeError("Expected safe diagnostic and rollback under ANSI_QUOTES")
            print("RESTORED_WELLNESS_DIAGNOSTIC_OK sql_error=1054 persisted=0")
        else:
            if payload.get("status") != "success" or payload.get("data", {}).get("respuestas_guardadas") != 6 or count.stdout.strip() != b"1":
                raise RuntimeError("Synthetic submit did not persist six answers")
            print("RESTORED_WELLNESS_SUBMIT_OK synthetic_answers=6 persisted=1 delete_grant=" + ("0" if limited else "1"))
    finally:
        if server.poll() is None:
            ready = run(mysql + ["-e", "SELECT @@datadir"])
            if ready.returncode == 0 and Path(ready.stdout.decode().strip()).resolve() == data_dir:
                run([str(BIN / "mysqladmin.exe"), "--no-defaults", "--protocol=TCP", "--host=127.0.0.1",
                     f"--port={PORT}", "--user=root", "shutdown"])
        server.wait(timeout=20)
        log.close()


if __name__ == "__main__":
    try:
        main()
    except Exception as exc:
        print("RESTORED_WELLNESS_SUBMIT_FAILED " + str(exc), file=sys.stderr)
        raise SystemExit(1)
