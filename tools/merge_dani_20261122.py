from pathlib import Path
import shutil

ROOT = Path(r'C:\Users\Jair L.G\AndroidStudioProjects\KPIWeb-integracion')
INCOMING = Path(r'C:\Users\Jair L.G\Documents\KPIWeb_DANI_EXTRACT_20261122\app\src')
TARGET = ROOT / 'app' / 'src'

# Estas clases contienen la conexión de producción y correcciones posteriores
# al punto de partida de Daniela. Se fusionan a mano después de copiar la UI.
KEEP = {
    'ApiClient.java', 'ApiService.java', 'SessionManager.java',
    'CollaboratorAttendanceActivity.java', 'KpiAttendanceRepository.java',
    'KpiDailyReminderReceiver.java', 'KpiFormRecovery.java',
    'KpiFormSyncManager.java', 'KpiFormFragment.java',
    'KpiReportsEditorFragment.java', 'KpiReportsHubFragment.java',
    'KpiRoleSelectorFragment.java', 'KpiServerReportBrowser.java',
    'MainActivity.java', 'CollaboratorKpiIntegrationActivity.java',
    'AndroidManifest.xml', 'TestApiService.java',
    'kpi_fragment_reports_editor.xml', 'kpi_fragment_reports_hub.xml',
    'kpi_fragment_role_selector.xml',
}

copied_new = copied_changed = skipped = 0
for src in INCOMING.rglob('*'):
    if not src.is_file():
        continue
    rel = src.relative_to(INCOMING)
    dst = TARGET / rel
    if src.name in KEEP and dst.exists():
        skipped += 1
        continue
    if dst.exists() and src.read_bytes() == dst.read_bytes():
        continue
    was_existing = dst.exists()
    dst.parent.mkdir(parents=True, exist_ok=True)
    shutil.copy2(src, dst)
    if was_existing:
        copied_changed += 1
    else:
        copied_new += 1
print(f'new={copied_new} changed={copied_changed} preserved={skipped}')
