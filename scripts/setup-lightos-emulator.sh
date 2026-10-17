#!/usr/bin/env bash
# One-time LightOS emulator setup per vendor/light-sdk/docs/system_app/README.md
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
# Keep this aligned with the SDK project paths in settings.gradle.kts.
SDK="$ROOT/vendor/light-sdk"
# shellcheck source=/dev/null
source "$ROOT/scripts/android-env.sh"

AVD_NAME="${AVD_NAME:-LightPhoneIII}"
SERIAL="${ANDROID_SERIAL:-emulator-${EMULATOR_PORT:-5554}}"
while [[ $# -gt 0 ]]; do
  case "$1" in
    --serial)
      [[ $# -ge 2 && -n "$2" ]] || { echo "--serial requires emulator-PORT" >&2; exit 2; }
      SERIAL="$2"; shift 2 ;;
    --help|-h)
      echo "Usage: AVD_NAME=LightPhoneIII $0 [--serial emulator-5554]"
      echo "Installs the bundled SDK's LightOS emulator as a privileged app on the selected AVD."
      exit 0 ;;
    *) echo "Unknown argument: $1" >&2; exit 2 ;;
  esac
done
if [[ ! "$SERIAL" =~ ^emulator-([0-9]+)$ ]]; then
  echo "Setup requires an Android emulator serial, such as emulator-5554; got: $SERIAL" >&2
  exit 2
fi
PORT=$((10#${BASH_REMATCH[1]}))
if (( PORT < 5554 || PORT > 5682 || PORT % 2 != 0 )); then
  echo "Emulator port must be an even number from 5554 through 5682." >&2
  exit 2
fi
SERIAL="emulator-$PORT"
export ANDROID_SERIAL="$SERIAL"
ADB=(adb -s "$SERIAL")
KEYS_DIR="$SDK/sdk/emulator/keys"
KEYSTORE="$KEYS_DIR/platform.jks"
WORK_DIR=""
cleanup() {
  if [[ -n "$WORK_DIR" ]]; then rm -rf "$WORK_DIR"; fi
}
trap cleanup EXIT
trap 'exit 129' HUP
trap 'exit 130' INT
trap 'exit 143' TERM

assert_selected_emulator() {
  local running_avd qemu
  running_avd="$("${ADB[@]}" emu avd name | tr -d '\r' | head -1)"
  qemu="$("${ADB[@]}" shell getprop ro.kernel.qemu | tr -d '\r')"
  if [[ "$running_avd" != "$AVD_NAME" || "$qemu" != 1 ]]; then
    echo "Refusing device changes: $SERIAL must be emulator AVD '$AVD_NAME' (reported '$running_avd', qemu='$qemu')." >&2
    exit 1
  fi
}

wait_for_boot() {
  local attempt boot
  for (( attempt=0; attempt<120; attempt++ )); do
    if "${ADB[@]}" get-state >/dev/null 2>&1; then
      boot="$("${ADB[@]}" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r' || true)"
      if [[ "$boot" == 1 ]]; then
        assert_selected_emulator
        return
      fi
    fi
    sleep 2
  done
  echo "Timed out waiting for $SERIAL to boot." >&2
  exit 1
}

restart_adb_as_root() {
  assert_selected_emulator
  "${ADB[@]}" root
  wait_for_boot
  if [[ "$("${ADB[@]}" shell id -u | tr -d '\r')" != 0 ]]; then
    echo "The selected emulator does not permit adb root. Use an API 34 AOSP test-keys image." >&2
    exit 1
  fi
}

if [[ ! -d "$SDK" ]]; then
  echo "Light SDK not found at $SDK" >&2
  exit 1
fi

# start-emulator handles both existing and absent targets with a bounded wait
# and checks the AVD name. No device mutation below uses an implicit adb target.
AVD_NAME="$AVD_NAME" "$ROOT/scripts/start-emulator.sh" --serial "$SERIAL"
assert_selected_emulator
BUILD_TAGS="$("${ADB[@]}" shell getprop ro.build.tags | tr -d '\r')"
if [[ "$BUILD_TAGS" != *test-keys* ]]; then
  echo "The selected AVD is not a test-keys image and cannot use the AOSP platform test key." >&2
  exit 1
fi

if [[ ! -f "$KEYSTORE" ]]; then
  echo "Generating AOSP platform test keystore..."
  WORK_DIR="$(mktemp -d "${TMPDIR:-/tmp}/lightphone-platform-key.XXXXXX")"
  mkdir -p "$KEYS_DIR"
  curl --fail --silent --show-error --location --proto '=https' --tlsv1.2 \
    --connect-timeout 15 --max-time 120 -o "$WORK_DIR/platform.x509.pem" \
    https://raw.githubusercontent.com/wfairclough/android_aosp_keys/refs/heads/master/platform.x509.pem
  curl --fail --silent --show-error --location --proto '=https' --tlsv1.2 \
    --connect-timeout 15 --max-time 120 -o "$WORK_DIR/platform.pk8" \
    https://raw.githubusercontent.com/wfairclough/android_aosp_keys/refs/heads/master/platform.pk8
  openssl pkcs8 -inform DER -nocrypt -in "$WORK_DIR/platform.pk8" -out "$WORK_DIR/platform.pem"
  openssl pkcs12 -export \
    -in "$WORK_DIR/platform.x509.pem" \
    -inkey "$WORK_DIR/platform.pem" \
    -name platform \
    -out "$WORK_DIR/platform.p12" \
    -passout pass:android
  keytool -importkeystore \
    -srckeystore "$WORK_DIR/platform.p12" \
    -srcstoretype PKCS12 \
    -srcstorepass android \
    -destkeystore "$WORK_DIR/platform.jks" \
    -deststoretype JKS \
    -deststorepass android \
    -noprompt
  mv "$WORK_DIR/platform.jks" "$KEYSTORE"
  echo "Keystore written to $KEYSTORE"
fi

echo "Building LightOS emulator APK..."
(cd "$ROOT" && ./gradlew :sdk:emulator:assembleDebug -PincludeLightEmulator=true --no-daemon)

APK="$SDK/sdk/emulator/build/outputs/apk/debug/emulator-debug.apk"
if [[ ! -f "$APK" ]]; then
  echo "Emulator APK not found at $APK" >&2
  exit 1
fi

echo "Preparing system partition on $SERIAL ($AVD_NAME)..."
restart_adb_as_root
if ! "${ADB[@]}" remount; then
  assert_selected_emulator
  "${ADB[@]}" disable-verity
  "${ADB[@]}" reboot
  wait_for_boot
  restart_adb_as_root
  "${ADB[@]}" remount
fi

echo "Installing LightOS emulator as priv-app..."
assert_selected_emulator
"${ADB[@]}" shell mkdir -p /system/priv-app/LightOSEmulator
"${ADB[@]}" push "$APK" /system/priv-app/LightOSEmulator/LightOSEmulator.apk
"${ADB[@]}" reboot
wait_for_boot

echo "Verifying installation..."
PACKAGE_PATH="$("${ADB[@]}" shell pm path com.thelightphone.sdk.emulator)"
echo "$PACKAGE_PATH"
if [[ "$PACKAGE_PATH" != *"/system/priv-app/LightOSEmulator/"* ]]; then
  echo "LightOS emulator was not registered from the privileged app directory." >&2
  exit 1
fi
PACKAGE_UID="$("${ADB[@]}" shell pm list packages -U com.thelightphone.sdk.emulator | tr -d '\r')"
echo "$PACKAGE_UID"
if [[ "$PACKAGE_UID" != *"com.thelightphone.sdk.emulator uid:1000"* ]]; then
  echo "LightOS emulator is not running with system UID 1000." >&2
  exit 1
fi
"${ADB[@]}" shell getprop ro.build.description

echo "Setting LightOS emulator as launcher and disabling animations..."
assert_selected_emulator
"${ADB[@]}" shell cmd package set-home-activity com.thelightphone.sdk.emulator/.MainActivity
"${ADB[@]}" shell settings put global window_animation_scale 0
"${ADB[@]}" shell settings put global transition_animation_scale 0
"${ADB[@]}" shell settings put global animator_duration_scale 0

echo "LightOS emulator setup complete on $SERIAL ($AVD_NAME)."
