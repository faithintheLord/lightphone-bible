#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
if [[ -d "$ROOT/tool/src/main" ]]; then
    OUT="$ROOT/tool/src/main/assets/strongs"
else
    OUT="$ROOT/bible/src/main/assets/strongs"
fi
# Update the commit and expected hashes together after reviewing upstream changes.
STRONGS_COMMIT="0acd2f251c2d35ff8db2dece4e0593979d3ac223"
BASE="https://raw.githubusercontent.com/openscriptures/strongs/$STRONGS_COMMIT"
WORK_DIR="$(mktemp -d "${TMPDIR:-/tmp}/lightphone-strongs.XXXXXX")"
trap 'rm -rf "$WORK_DIR"' EXIT

echo "Downloading Strong's dictionaries from openscriptures/strongs @ $STRONGS_COMMIT"

for language in greek hebrew; do
    curl --fail --silent --show-error --location --retry 3 \
        --connect-timeout 15 --max-time 120 --proto '=https' --tlsv1.2 \
        "$BASE/$language/strongs-$language-dictionary.js" \
        -o "$WORK_DIR/strongs-$language.js"
done

python3 - "$OUT" "$WORK_DIR" "$STRONGS_COMMIT" <<'PY'
import hashlib
import json
import os
from pathlib import Path
import re
import sys
import tempfile

out, work = map(Path, sys.argv[1:3])
commit = sys.argv[3]
repository = "https://github.com/openscriptures/strongs"
specs = {
    "greek": ("strongsGreekDictionary", "G", 5523,
              "7624ee738ae47e80f1a352223e28a26d011c9cd4898822cee52f47a010c04efd"),
    "hebrew": ("strongsHebrewDictionary", "H", 8674,
               "5ce6aeed551c709f49bcfa341cadf2f34bc7599b85d9de9e6ac2ecbf60fc3739"),
}

def unique_object(pairs):
    result = {}
    for key, value in pairs:
        if key in result:
            raise ValueError(f"Duplicate JSON key: {key}")
        result[key] = value
    return result

generated = {}
notices = []
sources = []
for language, (variable, prefix, expected_count, expected_hash) in specs.items():
    raw = (work / f"strongs-{language}.js").read_bytes()
    digest = hashlib.sha256(raw).hexdigest()
    if digest != expected_hash:
        raise ValueError(f"Unexpected {language} source SHA-256: {digest}")
    text = raw.decode("utf-8")
    declaration = re.search(rf"\bvar\s+{variable}\s*=\s*", text)
    if declaration is None:
        raise ValueError(f"Could not find {variable}")
    entries, end = json.JSONDecoder(object_pairs_hook=unique_object).raw_decode(
        text[declaration.end():]
    )
    if not text[declaration.end() + end:].lstrip().startswith(";"):
        raise ValueError(f"Invalid {language} dictionary declaration")
    if not isinstance(entries, dict) or len(entries) != expected_count:
        raise ValueError(f"Expected {expected_count} {language} entries")
    for key, entry in entries.items():
        if not re.fullmatch(prefix + r"[1-9][0-9]*", key):
            raise ValueError(f"Invalid dictionary key: {key}")
        if not isinstance(entry, dict) or not entry.get("lemma"):
            raise ValueError(f"Missing lemma for {key}")
        if not (entry.get("strongs_def") or entry.get("kjv_def")):
            raise ValueError(f"Missing definition for {key}")
        if not all(isinstance(value, str) for value in entry.values()):
            raise ValueError(f"Unexpected field type for {key}")
    output_bytes = json.dumps(entries, ensure_ascii=False, separators=(",", ":")).encode("utf-8")
    generated[f"{language}.json"] = output_bytes
    notices.append(f"{language.title()} upstream notice\n{'=' * 24}\n" + "\n".join(line.rstrip() for line in text[:declaration.start()].strip().splitlines()))
    sources.append({
        "language": language,
        "sourcePath": f"{language}/strongs-{language}-dictionary.js",
        "sourceUrl": f"https://raw.githubusercontent.com/openscriptures/strongs/{commit}/{language}/strongs-{language}-dictionary.js",
        "sourceSha256": digest,
        "asset": f"{language}.json",
        "assetSha256": hashlib.sha256(output_bytes).hexdigest(),
        "entries": len(entries),
    })

provenance = {
    "schemaVersion": 1,
    "repository": repository,
    "commit": commit,
    "licenseNotice": "CC-BY-SA; the upstream dictionary headers do not specify a version",
    "transformation": "Removed JavaScript variable/export wrapper and serialized the unchanged dictionary objects as compact UTF-8 JSON. No entry text was edited.",
    "reproduce": "Run scripts/prepare-strongs.sh from this repository.",
    "sources": sources,
}
generated["provenance.json"] = (json.dumps(provenance, ensure_ascii=False, indent=2) + "\n").encode("utf-8")
generated["NOTICE.txt"] = (
    "Strong's Dictionaries of Hebrew and Greek\n"
    f"Source: {repository}\nCommit: {commit}\n\n"
    "The following copyright, attribution, and CC-BY-SA notices are preserved from the upstream source files. "
    "Those headers do not specify a Creative Commons license version. "
    "The dictionary data retains its upstream CC-BY-SA terms.\n\n"
    "Lightphone Bible changes: removed the JavaScript wrapper and compacted JSON formatting; "
    "dictionary entries and text are unchanged. See provenance.json for source and asset checksums.\n\n"
    + "\n\n".join(notices) + "\n"
).encode("utf-8")

# Validate both dictionaries before replacing any assets. Stage on the same
# filesystem so each destination is replaced atomically.
out.mkdir(parents=True, exist_ok=True)
with tempfile.TemporaryDirectory(prefix=".strongs-stage-", dir=out) as staging:
    stage = Path(staging)
    for name, data in generated.items():
        (stage / name).write_bytes(data)
    for name in generated:
        os.replace(stage / name, out / name)
print("Verified and wrote 5,523 Greek and 8,674 Hebrew entries, notices, and provenance.")
PY

echo "Done."
