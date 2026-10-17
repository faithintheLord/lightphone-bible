#!/usr/bin/env bash
# Install (if needed) and launch the Bible app on a running emulator.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
# shellcheck source=/dev/null
source "$ROOT/scripts/android-env.sh"

LIGHTOS_EMULATOR=false
SERIAL="${ANDROID_SERIAL:-}"
while [[ $# -gt 0 ]]; do
  case "$1" in
    --lightos-emulator) LIGHTOS_EMULATOR=true; shift ;;
    --serial)
      [[ $# -ge 2 && -n "$2" ]] || { echo "--serial requires a device serial" >&2; exit 2; }
      SERIAL="$2"; shift 2 ;;
    --help|-h)
      echo "Usage: $0 [--lightos-emulator] [--serial SERIAL]"
      exit 0 ;;
    *) echo "Unknown argument: $1" >&2; exit 2 ;;
  esac
done
ADB=(adb)
if [[ -n "$SERIAL" ]]; then
  ADB+=(-s "$SERIAL")
  export ANDROID_SERIAL="$SERIAL"
fi
if ! "${ADB[@]}" get-state >/dev/null 2>&1; then
  echo "No selected adb device. Start an emulator or select one with --serial SERIAL." >&2
  exit 1
fi

cd "$ROOT"
METADATA="$ROOT/tool/lighttool.toml"
METADATA_BACKUP=""
BACKUP_READY=false
restore_metadata() {
  local status=$?
  trap - EXIT
  if "$BACKUP_READY"; then
    if ! cp -p "$METADATA_BACKUP" "$METADATA"; then
      echo "Could not restore $METADATA; original retained at $METADATA_BACKUP" >&2
      exit 1
    fi
  fi
  if [[ -n "$METADATA_BACKUP" ]]; then rm -f "$METADATA_BACKUP"; fi
  exit "$status"
}
trap restore_metadata EXIT
trap 'exit 129' HUP
trap 'exit 130' INT
trap 'exit 143' TERM

GRADLE_ARGS=(:tool:installDebug --no-daemon)
if "$LIGHTOS_EMULATOR"; then
  if ! "${ADB[@]}" shell pm path com.thelightphone.sdk.emulator | grep -q '^package:'; then
    echo "Install the LightOS emulator first: ./scripts/setup-lightos-emulator.sh" >&2
    exit 1
  fi
  METADATA_BACKUP="$(mktemp "${TMPDIR:-/tmp}/lightphone-metadata.XXXXXX")"
  cp -p "$METADATA" "$METADATA_BACKUP"
  BACKUP_READY=true
  python3 - "$METADATA" <<'PY'
from pathlib import Path
import re
import sys

path = Path(sys.argv[1])
text, count = re.subn(
    r'^serverPackage\s*=\s*"[^"]*"',
    'serverPackage = "com.thelightphone.sdk.emulator"',
    path.read_text(encoding="utf-8"),
    flags=re.MULTILINE,
)
if count != 1:
    raise SystemExit("Expected exactly one serverPackage in tool/lighttool.toml")
path.write_text(text, encoding="utf-8")
PY
  GRADLE_ARGS+=(-DlightSdk.allowAltServerPackage=true)
fi
./gradlew "${GRADLE_ARGS[@]}"

if ! LAUNCH_OUTPUT="$("${ADB[@]}" shell am start -W -n com.lightphone.bible/com.thelightphone.sdk.LightActivity 2>&1)"; then
  echo "$LAUNCH_OUTPUT" >&2
  exit 1
fi
echo "$LAUNCH_OUTPUT"
if [[ "$LAUNCH_OUTPUT" != *"Status: ok"* || "$LAUNCH_OUTPUT" == *"Error:"* ]]; then
  echo "Bible activity did not launch successfully." >&2
  exit 1
fi
echo "Bible app launched."
