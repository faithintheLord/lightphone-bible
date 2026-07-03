#!/usr/bin/env bash
# Patch vendored light-sdk for bible app build (until upstream merges applicationContext).
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
SDK="$ROOT/vendor/light-sdk"
FILE="$SDK/sdk/client/src/main/kotlin/com/thelightphone/sdk/LightActivity.kt"

if [[ ! -f "$FILE" ]]; then
  echo "Missing $FILE" >&2
  exit 1
fi

if grep -q 'val applicationContext: Context' "$FILE"; then
  echo "light-sdk already patched (applicationContext present)."
  exit 0
fi

python3 - <<PY
from pathlib import Path
path = Path("$FILE")
text = path.read_text()
old = "class SealedLightContext(internal val androidContext: Context) {\n    val dataStore:"
new = """class SealedLightContext(internal val androidContext: Context) {
    val applicationContext: Context
        get() = androidContext.applicationContext
    val dataStore:"""
if old not in text:
    raise SystemExit("Could not find SealedLightContext patch point in LightActivity.kt")
path.write_text(text.replace(old, new, 1))
print("Patched SealedLightContext.applicationContext in light-sdk vendor.")
PY
