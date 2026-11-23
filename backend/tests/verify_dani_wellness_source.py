"""Check that the proposed SQL uses the original Dani questionnaire text."""
import pathlib
import re

root = pathlib.Path(__file__).resolve().parents[2]
java = (root / "app/src/main/java/pe/net/ws/kpiweb/features/collaborator/wellness/CollaboratorWellnessActivity.java").read_text(encoding="utf-8")
sql = (root / "database/migrations/V021__bienestar_dani_cuestionarios.up.sql").read_text(encoding="utf-8")

for array, code, expected in [
    ("WEEKLY_QUESTIONS", "weekly", 6),
    ("MONTHLY_QUESTIONS", "monthly", 10),
]:
    match = re.search(r"private static final String\[\] " + array + r" = new String\[\]\{(.*?)\};", java, re.S)
    assert match, array
    source = re.findall(r'"([^"]+)"', match.group(1))
    saved = re.findall(r"\(@dani_" + code + r"_id,\d+,'([^']+)','OPCION_MULTIPLE'\)", sql)
    assert len(source) == expected and source == saved, f"{array} differs from SQL"

for array, code, expected in [
    ("MOOD_OPTIONS", "weekly", ["Muy bien", "Bien", "Neutral", "Mal", "Muy mal"]),
    ("MONTHLY_MOOD_OPTIONS", "monthly", ["Muy mal", "Mal", "Regular", "Bien", "Muy bien"]),
]:
    match = re.search(r"private static final MoodOption\[\] " + array + r" = new MoodOption\[\]\{(.*?)\};", java, re.S)
    assert match, array
    source = re.findall(r'new MoodOption\(\d+, "([^"]+)"', match.group(1))
    assert source == expected, f"{array} changed"
    options = sql.split("SET @dani_" + code + "_id = LAST_INSERT_ID();", 1)[1]
    options = options.split("INSERT INTO wp_bienestar_tests", 1)[0]
    saved = re.findall(r"SELECT \d+(?: AS orden)?,'([^']+)'", options)
    assert saved == expected, f"{array} option labels differ from SQL"

print("PASS: 16 question texts and both five-option lists match Dani's Java source")
