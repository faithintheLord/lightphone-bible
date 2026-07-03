#!/usr/bin/env bash
# Create or update the Light Phone III AVD (API 34, default/test-keys image, 1080x1240).
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
# shellcheck source=/dev/null
source "$ROOT/scripts/android-env.sh"

AVD_NAME="${AVD_NAME:-LightPhoneIII}"
SYSTEM_IMAGE="system-images;android-34;default;arm64-v8a"

if ! avdmanager list avd 2>/dev/null | grep -q "Name: ${AVD_NAME}"; then
  echo no | avdmanager create avd -n "$AVD_NAME" -k "$SYSTEM_IMAGE" -d pixel_5
fi

CONFIG="${HOME}/.android/avd/${AVD_NAME}.avd/config.ini"
if [[ -f "$CONFIG" ]]; then
  sed -i '' 's/^hw\.lcd\.width=.*/hw.lcd.width=1080/' "$CONFIG"
  sed -i '' 's/^hw\.lcd\.height=.*/hw.lcd.height=1240/' "$CONFIG"
  sed -i '' 's/^hw\.lcd\.density=.*/hw.lcd.density=420/' "$CONFIG"
fi

echo "AVD ready: $AVD_NAME"
echo "Boot with: emulator -avd $AVD_NAME -writable-system"
