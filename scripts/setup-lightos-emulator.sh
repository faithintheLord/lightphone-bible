#!/usr/bin/env bash
# One-time LightOS emulator setup per vendor/light-sdk/docs/system_app/README.md
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
SDK="${LIGHT_SDK_ROOT:-$ROOT/vendor/light-sdk}"
# shellcheck source=/dev/null
source "$ROOT/scripts/android-env.sh"

AVD_NAME="${AVD_NAME:-LightPhoneIII}"
KEYS_DIR="$SDK/sdk/emulator/keys"
KEYSTORE="$KEYS_DIR/platform.jks"

if [[ ! -d "$SDK" ]]; then
  echo "Light SDK not found at $SDK" >&2
  exit 1
fi

if [[ ! -f "$KEYSTORE" ]]; then
  echo "Generating AOSP platform test keystore..."
  mkdir -p "$KEYS_DIR"
  curl -fsSL -o /tmp/platform.x509.pem \
    https://raw.githubusercontent.com/wfairclough/android_aosp_keys/refs/heads/master/platform.x509.pem
  curl -fsSL -o /tmp/platform.pk8 \
    https://raw.githubusercontent.com/wfairclough/android_aosp_keys/refs/heads/master/platform.pk8
  openssl pkcs8 -inform DER -nocrypt -in /tmp/platform.pk8 -out /tmp/platform.pem
  openssl pkcs12 -export \
    -in /tmp/platform.x509.pem \
    -inkey /tmp/platform.pem \
    -name platform \
    -out /tmp/platform.p12 \
    -passout pass:android
  keytool -importkeystore \
    -srckeystore /tmp/platform.p12 \
    -srcstoretype PKCS12 \
    -srcstorepass android \
    -destkeystore "$KEYSTORE" \
    -deststoretype JKS \
    -deststorepass android \
    -noprompt
  rm -f /tmp/platform.pk8 /tmp/platform.x509.pem /tmp/platform.pem /tmp/platform.p12
  echo "Keystore written to $KEYSTORE"
fi

export JAVA_HOME="${JAVA_HOME:-/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home}"
echo "Building LightOS emulator APK..."
(cd "$ROOT" && ./scripts/patch-keyboard-vendor.sh && ./gradlew :sdk:emulator:assembleDebug --no-daemon)

APK="$ROOT/vendor/light-sdk/sdk/emulator/build/outputs/apk/debug/emulator-debug.apk"
if [[ ! -f "$APK" ]]; then
  echo "Emulator APK not found at $APK" >&2
  exit 1
fi

if ! adb get-state >/dev/null 2>&1; then
  echo "Starting emulator $AVD_NAME (writable-system)..."
  emulator -avd "$AVD_NAME" -writable-system -no-snapshot-load -no-audio -no-boot-anim &
  echo "Waiting for device..."
  adb wait-for-device
  boot_completed=""
  for _ in $(seq 1 120); do
    boot_completed="$(adb shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')"
    if [[ "$boot_completed" == "1" ]]; then
      break
    fi
    sleep 2
  done
fi

echo "Preparing system partition..."
adb root
if ! adb remount 2>/dev/null; then
  adb disable-verity
  adb reboot
  adb wait-for-device
  boot_completed=""
  for _ in $(seq 1 120); do
    boot_completed="$(adb shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')"
    if [[ "$boot_completed" == "1" ]]; then
      break
    fi
    sleep 2
  done
  adb root
  adb remount
fi

echo "Installing LightOS emulator as priv-app..."
adb shell mkdir -p /system/priv-app/LightOSEmulator
adb push "$APK" /system/priv-app/LightOSEmulator/LightOSEmulator.apk
adb reboot
adb wait-for-device
boot_completed=""
for _ in $(seq 1 120); do
  boot_completed="$(adb shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')"
  if [[ "$boot_completed" == "1" ]]; then
    break
  fi
  sleep 2
done

echo "Verifying installation..."
adb shell pm path com.thelightphone.sdk.emulator
adb shell dumpsys package com.thelightphone.sdk.emulator | grep "uid=" || true
adb shell getprop ro.build.description

echo "Setting LightOS emulator as launcher and disabling animations..."
adb shell cmd package set-home-activity com.thelightphone.sdk.emulator/.MainActivity || true
adb shell settings put global window_animation_scale 0
adb shell settings put global transition_animation_scale 0
adb shell settings put global animator_duration_scale 0

echo "LightOS emulator setup complete."
