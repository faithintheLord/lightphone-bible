#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
OUT="$ROOT/bible/src/main/assets/strongs"
mkdir -p "$OUT"
BASE="https://raw.githubusercontent.com/openscriptures/strongs/master"
STRONGS_COMMIT="${STRONGS_COMMIT:-master}"

echo "Downloading Strong's dictionaries from openscriptures/strongs @ $STRONGS_COMMIT"

curl -fsSL "$BASE/greek/strongs-greek-dictionary.js" -o /tmp/strongs-greek.js
curl -fsSL "$BASE/hebrew/strongs-hebrew-dictionary.js" -o /tmp/strongs-hebrew.js

python3 - "$OUT" <<'PY'
import json, re, sys

def extract(path, var_name):
    text = open(path).read()
    m = re.search(rf'var {re.escape(var_name)} = (\{{.*\}});', text, re.DOTALL)
    if not m:
        sys.exit(f"Could not parse {path}")
    return json.loads(m.group(1))

greek = extract("/tmp/strongs-greek.js", "strongsGreekDictionary")
hebrew = extract("/tmp/strongs-hebrew.js", "strongsHebrewDictionary")
out = sys.argv[1]
json.dump(greek, open(f"{out}/greek.json", "w"), ensure_ascii=False, separators=(",", ":"))
json.dump(hebrew, open(f"{out}/hebrew.json", "w"), ensure_ascii=False, separators=(",", ":"))
print(f"Wrote {len(greek)} Greek and {len(hebrew)} Hebrew entries")
PY

echo "Done."
