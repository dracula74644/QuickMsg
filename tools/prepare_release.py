#!/usr/bin/env python3
"""Package an already-built release APK and its update manifest for GitHub."""
import hashlib
import json
from pathlib import Path
import re
import shutil

root = Path(__file__).resolve().parents[1]
output = root / "app/build/outputs/apk/release"
metadata = json.loads((output / "output-metadata.json").read_text())
assert metadata["applicationId"] == "com.quickmsg.app"
assert len(metadata["elements"]) == 1
apk = metadata["elements"][0]
version = apk["versionName"]
assert re.fullmatch(r"\d+\.\d+(?:\.\d+)?", version), "Use a numeric release version"
name = f"QuickMsg-v{version}.apk"
release = root / "build/release" / f"v{version}"
release.mkdir(parents=True, exist_ok=True)
shutil.copy2(output / apk["outputFile"], release / name)
manifest = {
    "versionCode": apk["versionCode"],
    "versionName": version,
    "apkUrl": f"https://github.com/dracula74644/QuickMsg/releases/download/v{version}/{name}",
    "sha256": hashlib.sha256((release / name).read_bytes()).hexdigest(),
}
(release / "update.json").write_text(json.dumps(manifest, indent=2) + "\n")
print(release)
