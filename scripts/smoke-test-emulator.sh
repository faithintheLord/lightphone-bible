#!/usr/bin/env bash
# Smoke-test Bible app on a running emulator/device via adb.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
# shellcheck source=/dev/null
source "$ROOT/scripts/android-env.sh"

PACKAGE="com.lightphone.bible"
ACTIVITY="com.thelightphone.sdk.LightActivity"

if ! adb get-state >/dev/null 2>&1; then
  echo "No adb device. Start the emulator first." >&2
  exit 1
fi

echo "=== Launch Bible app ==="
adb logcat -c
adb shell am start -n "$PACKAGE/$ACTIVITY" -W 2>&1 | tail -5

echo "=== Wait for startup (15s) ==="
sleep 15

echo "=== Recent logcat (errors) ==="
adb logcat -d -t 200 | rg -i "FATAL|AndroidRuntime|lightphone.bible|BibleSync|Exception" | tail -40 || true

echo "=== Process running? ==="
adb shell pidof "$PACKAGE" && echo "App process alive" || echo "App not running"

echo "=== Trigger sync job via broadcast/work (if enqueued on launch) ==="
sleep 30

echo "=== Check database file after sync window ==="
adb shell run-as "$PACKAGE" ls -la databases/ 2>&1 || adb shell "ls -la /data/data/$PACKAGE/databases/" 2>&1 || true

echo "=== Package info ==="
adb shell dumpsys package "$PACKAGE" | rg "versionName|versionCode|firstInstall" | head -5

echo "Smoke test complete. Review logcat above for crashes."
