#!/usr/bin/env bash
# Create or update the Light Phone III AVD (API 34, default/test-keys image, 1080x1240).
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
# shellcheck source=/dev/null
source "$ROOT/scripts/android-env.sh"

AVD_NAME="${AVD_NAME:-LightPhoneIII}"
if [[ $# -gt 0 ]]; then
  if [[ $# -eq 1 && ( "$1" == --help || "$1" == -h ) ]]; then
    echo "Usage: AVD_NAME=LightPhoneIII [SYSTEM_IMAGE=...] $0"
    exit 0
  fi
  echo "Unexpected arguments. Use AVD_NAME and SYSTEM_IMAGE environment variables." >&2
  exit 2
fi
if [[ -z "${SYSTEM_IMAGE:-}" ]]; then
  case "$(uname -m)" in
    arm64|aarch64) IMAGE_ARCH=arm64-v8a ;;
    x86_64|amd64) IMAGE_ARCH=x86_64 ;;
    *) echo "Unsupported host architecture; set SYSTEM_IMAGE explicitly." >&2; exit 1 ;;
  esac
  SYSTEM_IMAGE="system-images;android-34;default;$IMAGE_ARCH"
fi

if ! avdmanager list avd -c | grep -Fxq "$AVD_NAME"; then
  if ! avdmanager create avd -n "$AVD_NAME" -k "$SYSTEM_IMAGE" -d pixel_5 <<<no; then
    echo "AVD creation failed. Ensure the image is installed: sdkmanager '$SYSTEM_IMAGE'" >&2
    exit 1
  fi
fi

AVD_ROOT="${ANDROID_AVD_HOME:-${ANDROID_USER_HOME:-$HOME/.android}/avd}"
python3 - "$AVD_ROOT" "$AVD_NAME" <<'PY'
from pathlib import Path
import sys

avd_root, name = Path(sys.argv[1]), sys.argv[2]
pointer = avd_root / f"{name}.ini"
directory = avd_root / f"{name}.avd"
if pointer.is_file():
    for line in pointer.read_text(encoding="utf-8").splitlines():
        if line.startswith("path="):
            directory = Path(line.partition("=")[2])
config = directory / "config.ini"
if not config.is_file():
    raise SystemExit(f"AVD config was not created: {config}")
updates = {
    "hw.lcd.width": "1080",
    "hw.lcd.height": "1240",
    "hw.lcd.density": "420",
    "hw.initialOrientation": "portrait",
    "skin.name": "1080x1240",
    "skin.path": "_no_skin",
}
lines = []
for line in config.read_text(encoding="utf-8").splitlines():
    key = line.partition("=")[0].strip()
    if key not in updates:
        lines.append(line)
lines.extend(f"{key}={value}" for key, value in updates.items())
config.write_text("\n".join(lines) + "\n", encoding="utf-8")
print(f"Configured 1080 x 1240 display at 420 dpi: {config}")
PY

echo "AVD ready: $AVD_NAME"
echo "Boot with: AVD_NAME='$AVD_NAME' ./scripts/start-emulator.sh"
