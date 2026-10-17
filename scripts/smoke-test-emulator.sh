#!/usr/bin/env bash
# Smoke-test Bible app on a running emulator/device via adb.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
# shellcheck source=/dev/null
source "$ROOT/scripts/android-env.sh"

PACKAGE="com.lightphone.bible"
ACTIVITY="com.thelightphone.sdk.LightActivity"
SERIAL="${ANDROID_SERIAL:-}"
while [[ $# -gt 0 ]]; do
  case "$1" in
    --serial)
      [[ $# -ge 2 && -n "$2" ]] || { echo "--serial requires a device serial" >&2; exit 2; }
      SERIAL="$2"; shift 2 ;;
    --help|-h)
      echo "Usage: $0 [--serial SERIAL]"
      echo "Checks a fresh launch and process survival for 45 seconds. Install the app first."
      exit 0 ;;
    *) echo "Unknown argument: $1" >&2; exit 2 ;;
  esac
done
ADB=(adb)
if [[ -n "$SERIAL" ]]; then ADB+=(-s "$SERIAL"); fi

if ! "${ADB[@]}" get-state >/dev/null 2>&1; then
  echo "No selected adb device. Start an emulator or select one with --serial SERIAL." >&2
  exit 1
fi

fail_with_logs() {
  echo "$1" >&2
  "${ADB[@]}" logcat -b crash -d -t 100 >&2 || true
  exit 1
}

echo "Launching Bible on $("${ADB[@]}" get-serialno)..."
"${ADB[@]}" shell am force-stop "$PACKAGE"
START_TIME="$("${ADB[@]}" shell "date '+%m-%d %H:%M:%S.000'" | tr -d '\r')"
if ! LAUNCH_OUTPUT="$("${ADB[@]}" shell am start -W -n "$PACKAGE/$ACTIVITY" 2>&1)"; then
  echo "$LAUNCH_OUTPUT" >&2
  fail_with_logs "Activity launch command failed."
fi
echo "$LAUNCH_OUTPUT"
if [[ "$LAUNCH_OUTPUT" != *"Status: ok"* || "$LAUNCH_OUTPUT" == *"Error:"* ]]; then
  fail_with_logs "Bible activity did not launch successfully."
fi

echo "Waiting 15 seconds for startup..."
sleep 15
"${ADB[@]}" shell pidof "$PACKAGE" >/dev/null || fail_with_logs "Bible process exited during startup."

echo "Allowing 30 seconds for any background work already enqueued by the app..."
sleep 30
"${ADB[@]}" shell pidof "$PACKAGE" >/dev/null || fail_with_logs "Bible process exited during the smoke test."
CRASH_OUTPUT="$("${ADB[@]}" shell "logcat -b crash -d -T '$START_TIME'")"
if [[ "$CRASH_OUTPUT" == *"$PACKAGE"* ]]; then
  echo "$CRASH_OUTPUT" >&2
  fail_with_logs "A Bible crash was recorded during the smoke test."
fi

echo "Recent app diagnostics:"
"${ADB[@]}" logcat -d -t 200 | grep -E 'AndroidRuntime|lightphone.bible|BibleSync|Exception' | tail -40 || true

echo "Database files (available for debuggable builds):"
"${ADB[@]}" shell run-as "$PACKAGE" ls -la databases/ 2>&1 || true

echo "Installed package:"
"${ADB[@]}" shell dumpsys package "$PACKAGE" | grep -E 'versionName|versionCode|firstInstall' | head -5

echo "Smoke test passed: launch succeeded, process survived, and no Bible crash was recorded."
