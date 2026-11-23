from pathlib import Path
import hashlib
import shutil
import zipfile

ROOT = Path(r'C:\Users\Jair L.G\AndroidStudioProjects\KPIWeb-integracion')
OUT = Path(r'C:\Users\Jair L.G\Downloads\KPIWeb_INTEGRADO_DANI_20261122')

def copy_file(relative: str, destination: Path) -> None:
    source = ROOT / relative
    destination.parent.mkdir(parents=True, exist_ok=True)
    shutil.copy2(source, destination)

copy_file('app/build/outputs/apk/debug/app-debug.apk', OUT / 'APP' / 'KPIWeb-integrado-20261122-debug.apk')
copy_file('backend/public/index.php', OUT / 'CPANEL' / 'public_html' / 'websiteplus-api' / 'index.php')
publish_php = [
    'Auth/KpiLaborCalendar.php', 'Auth/LaborPolicy.php',
    'Controllers/AttendanceController.php', 'Controllers/KpiController.php',
    'Controllers/KpiFormsController.php', 'Controllers/PersonalController.php',
    'Controllers/KpiOfficialAdminController.php',
    'Controllers/ScheduleController.php',
    'Controllers/WellnessController.php', 'Controllers/WellnessAdminController.php',
    'Kpi/KpiPointsPolicy.php', 'Kpi/KpiFormCompletion.php',
]
for relative in publish_php:
    copy_file('backend/src/' + relative, OUT / 'CPANEL' / 'websiteplus_backend' / 'src' / relative)
copy_file('database/migrations/V028__bienestar_actividades_publicadas.up.sql',
          OUT / 'SQL' / 'V028__bienestar_actividades_publicadas.up.sql')
copy_file('database/proposals/VERIFICAR_PERMISOS_ENTREGA_20261122.sql',
          OUT / 'SQL' / 'VERIFICAR_PERMISOS_ENTREGA_20261122.sql')
copy_file('docs/ENTREGA_INTEGRACION_DANI_2026-11-22.md', OUT / 'LEEME_PUBLICACION.md')
shutil.copy2(Path(r'C:\Users\Jair L.G\Documents\KPIWeb_DANI_EXTRACT_20261122\CAMBIOS_INTEGRACION_2026-11-22.md'),
             OUT / 'REFERENCIA_CAMBIOS_DANIELA.md')

source_zip = OUT / 'FUENTE' / 'KPIWeb-integracion-20261122.zip'
source_zip.parent.mkdir(parents=True, exist_ok=True)
with zipfile.ZipFile(source_zip, 'w', compression=zipfile.ZIP_DEFLATED, compresslevel=6) as archive:
    individual = ['build.gradle.kts', 'settings.gradle.kts', 'gradle.properties',
                  'gradlew', 'gradlew.bat', 'README.md', 'app/build.gradle.kts',
                  'docs/ENTREGA_INTEGRACION_DANI_2026-11-22.md']
    for relative in individual:
        archive.write(ROOT / relative, relative)
    for folder in ['gradle', 'app/src', 'backend/src', 'backend/public', 'database/migrations']:
        for path in (ROOT / folder).rglob('*'):
            if path.is_file():
                archive.write(path, path.relative_to(ROOT).as_posix())

for file in [OUT / 'APP' / 'KPIWeb-integrado-20261122-debug.apk', source_zip]:
    print(file.name, file.stat().st_size, hashlib.sha256(file.read_bytes()).hexdigest())
print('php_files', len(publish_php))
