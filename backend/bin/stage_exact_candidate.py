"""Build a private laboratory tree from published PHP snapshots plus the deployment ZIP.

Usage: python stage_exact_candidate.py SRC_ZIP API_ZIP CANDIDATE_ZIP
Copies only reviewed source/test dependencies. Never opens a database or network.
"""
from __future__ import annotations

import hashlib
import json
import sys
import tempfile
import zipfile
from pathlib import Path


def safe_write(root: Path, name: str, content: bytes) -> None:
    dest = (root / name).resolve()
    if not dest.is_relative_to(root) or name.endswith("/config.php"):
        raise RuntimeError(f"Unsafe stage path: {name}")
    dest.parent.mkdir(parents=True, exist_ok=True)
    dest.write_bytes(content)


def main() -> None:
    if len(sys.argv) != 4:
        raise SystemExit("Usage: python stage_exact_candidate.py SRC_ZIP API_ZIP CANDIDATE_ZIP")
    repo = Path(__file__).resolve().parents[2]
    root = Path(tempfile.mkdtemp(prefix="kpiweb_exact_candidate_", dir=Path(tempfile.gettempdir()))).resolve()
    manifest = json.loads((repo / "backend/tests/repro_manifest.json").read_text(encoding="utf-8"))
    for name in manifest["files"]:
        safe_write(root, name, (repo / name).read_bytes())
    # The archived published sources form the baseline; the release ZIP is overlaid last.
    with zipfile.ZipFile(sys.argv[1]) as source:
        for name in source.namelist():
            if name.startswith("src/") and name.endswith(".php"):
                safe_write(root, "backend/src/" + name.removeprefix("src/"), source.read(name))
    with zipfile.ZipFile(sys.argv[2]) as public:
        for name in ("websiteplus-api/index.php", "websiteplus-api/.htaccess"):
            safe_write(root, "backend/public/" + name.rsplit("/", 1)[1], public.read(name))
    with zipfile.ZipFile(sys.argv[3]) as candidate:
        files = {name: candidate.read(name) for name in candidate.namelist() if name not in ("MANIFEST_SHA256.txt", "README_PUBLICACION.txt")}
        lines = candidate.read("MANIFEST_SHA256.txt").decode("ascii").splitlines()
        verified = set()
        for line in lines:
            digest, name = line.split("  ", 1)
            if name not in files or hashlib.sha256(files[name]).hexdigest().upper() != digest:
                raise RuntimeError(f"Candidate manifest mismatch: {name}")
            verified.add(name)
        if verified != set(files) or len(verified) != 25:
            raise RuntimeError("Candidate file count or manifest is incomplete")
        for name, content in files.items():
            if name.startswith("websiteplus_backend/"):
                dest = "backend/" + name.removeprefix("websiteplus_backend/")
            elif name == "public_html/websiteplus-api/index.php":
                dest = "backend/public/index.php"
            elif name.startswith("database/migrations/"):
                dest = name
            else:
                raise RuntimeError(f"Unexpected deployment path: {name}")
            safe_write(root, dest, content)
    print(f"STAGED={root}")
    print(f"DEPLOYMENT_FILES={len(verified)} MANIFEST_OK=YES")


if __name__ == "__main__":
    main()
