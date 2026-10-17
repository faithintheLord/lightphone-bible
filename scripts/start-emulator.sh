#!/usr/bin/env bash
# Start LightPhoneIII emulator and keep it running (for interactive testing).
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
# shellcheck source=/dev/null
source "$ROOT/scripts/android-env.sh"

AVD="${AVD_NAME:-LightPhoneIII}"
SERIAL="${ANDROID_SERIAL:-emulator-${EMULATOR_PORT:-5554}}"
while [[ $# -gt 0 ]]; do
  case "$1" in
    --serial)
      [[ $# -ge 2 && -n "$2" ]] || { echo "--serial requires emulator-PORT" >&2; exit 2; }
      SERIAL="$2"; shift 2 ;;
    --help|-h)
      echo "Usage: AVD_NAME=LightPhoneIII $0 [--serial emulator-5554]"
      echo "ANDROID_SERIAL or EMULATOR_PORT can also select the emulator port."
      exit 0 ;;
    *) echo "Unknown argument: $1" >&2; exit 2 ;;
  esac
done
if [[ ! "$SERIAL" =~ ^emulator-([0-9]+)$ ]]; then
  echo "Select an emulator serial such as emulator-5554; got: $SERIAL" >&2
  exit 2
fi
PORT=$((10#${BASH_REMATCH[1]}))
if (( PORT < 5554 || PORT > 5682 || PORT % 2 != 0 )); then
  echo "Emulator port must be an even number from 5554 through 5682." >&2
  exit 2
fi
SERIAL="emulator-$PORT"
ADB=(adb -s "$SERIAL")
EMULATOR_PID=""
LOG=""

if adb devices | awk -v serial="$SERIAL" '$1 == serial { found = 1 } END { exit !found }'; then
  echo "Waiting for existing $SERIAL to finish booting..."
else
  if ! emulator -list-avds | grep -Fxq "$AVD"; then
    echo "AVD '$AVD' does not exist. Run ./scripts/create-avd.sh first." >&2
    exit 1
  fi
  LOG="$(mktemp "${TMPDIR:-/tmp}/lightphone-emulator.XXXXXX")"
  echo "Starting $AVD as $SERIAL (logs: $LOG)..."
  nohup emulator -avd "$AVD" -port "$PORT" -writable-system -no-snapshot-load </dev/null >>"$LOG" 2>&1 &
  EMULATOR_PID=$!
fi

BOOT_COMPLETED=false
for (( attempt=0; attempt<120; attempt++ )); do
  if [[ -n "$EMULATOR_PID" ]] && ! kill -0 "$EMULATOR_PID" 2>/dev/null; then
    echo "Emulator exited before boot. Last log lines:" >&2
    tail -40 "$LOG" >&2
    exit 1
  fi
  if "${ADB[@]}" get-state >/dev/null 2>&1; then
    boot="$("${ADB[@]}" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r' || true)"
    if [[ "$boot" == 1 ]]; then BOOT_COMPLETED=true; break; fi
  fi
  sleep 2
done
if ! "$BOOT_COMPLETED"; then
  echo "Timed out waiting for $SERIAL to boot.${LOG:+ Logs: $LOG}" >&2
  exit 1
fi
RUNNING_AVD="$("${ADB[@]}" emu avd name | tr -d '\r' | head -1)"
if [[ "$RUNNING_AVD" != "$AVD" ]]; then
  echo "$SERIAL is running '$RUNNING_AVD', not requested AVD '$AVD'. Select another --serial port." >&2
  exit 1
fi

echo "Emulator ready: $SERIAL ($AVD)"
echo "Use --serial '$SERIAL' with launch-bible.sh and smoke-test-emulator.sh."
