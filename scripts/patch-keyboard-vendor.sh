#!/usr/bin/env bash
# Align vendored light-keyboard with lightphone-bible / light-sdk toolchain.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
KB="$ROOT/vendor/light-keyboard"

if [[ ! -d "$KB" ]]; then
  echo "Missing vendor/light-keyboard. Run: git submodule update --init vendor/light-keyboard" >&2
  exit 1
fi

# Match com.thelightphone.lp3keyboard:ui:0.0.11 consumed by light-sdk.
KEYBOARD_REF="${KEYBOARD_REF:-48aa73d}"
if [[ -d "$KB/.git" ]]; then
  git -C "$KB" checkout --quiet "$KEYBOARD_REF"
fi

SETTINGS="$KB/settings.gradle.kts"
if grep -q 'include(":app")' "$SETTINGS"; then
  sed -i '' '/include(":app")/d' "$SETTINGS"
fi

TOML="$KB/gradle/libs.versions.toml"
sed -i '' 's/^agp = ".*"/agp = "8.12.3"/' "$TOML"
sed -i '' 's/^kotlin = ".*"/kotlin = "2.3.20"/' "$TOML"

sed -i '' 's|distributionUrl=.*|distributionUrl=https\\://services.gradle.org/distributions/gradle-9.0.0-bin.zip|' \
  "$KB/gradle/wrapper/gradle-wrapper.properties"

UI="$KB/ui/build.gradle.kts"
sed -i '' 's/compileSdk = [0-9]*/compileSdk = 36/' "$UI"
sed -i '' 's/JavaVersion.VERSION_11/JavaVersion.VERSION_17/g' "$UI"
if grep -q 'kotlinOptions' "$UI"; then
  perl -0pi -e 's/    kotlinOptions \{\n        jvmTarget = "11"\n    \}\n\}/    }\n}\n\nkotlin {\n    compilerOptions {\n        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)\n    }\n}/s' "$UI"
elif ! grep -q 'kotlin \{' "$UI"; then
  perl -0pi -e 's/    compileOptions \{\n        sourceCompatibility = JavaVersion\.VERSION_17\n        targetCompatibility = JavaVersion\.VERSION_17\n    \}\n\}/    compileOptions {\n        sourceCompatibility = JavaVersion.VERSION_17\n        targetCompatibility = JavaVersion.VERSION_17\n    }\n}\n\nkotlin {\n    compilerOptions {\n        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)\n    }\n}/s' "$UI"
fi

ROOT_PROPS="$ROOT/local.properties"
KB_PROPS="$KB/local.properties"
if [[ -f "$ROOT_PROPS" ]] && grep -q '^sdk.dir=' "$ROOT_PROPS"; then
  SDK_DIR="$(grep '^sdk.dir=' "$ROOT_PROPS" | cut -d= -f2-)"
  printf 'sdk.dir=%s\n' "$SDK_DIR" > "$KB_PROPS"
fi

STYLE="$KB/ui/src/main/java/com/thelightphone/lp3Keyboard/ui/Style.kt"
if [[ -f "$STYLE" ]] && grep -q 'R.font.akkurat' "$STYLE"; then
  cat > "$STYLE" <<'EOF'
package com.thelightphone.lp3Keyboard.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily

// Akkurat font files are license-restricted and not shipped in this repo.
val akkuratFamily = FontFamily.Default

@Immutable
data class Lp3KeyboardColors(
    val background: Color,
    val foreground: Color,
)

val DarkKeyboardColors = Lp3KeyboardColors(
    background = Color.Black,
    foreground = Color.White,
)

val LightKeyboardColors = Lp3KeyboardColors(
    background = Color.White,
    foreground = Color.Black,
)

val LocalKeyboardColors = staticCompositionLocalOf { DarkKeyboardColors }

@Composable
fun Lp3KeyboardTheme(
    colors: Lp3KeyboardColors = DarkKeyboardColors,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalKeyboardColors provides colors) {
        content()
    }
}
EOF
fi

echo "Patched vendor/light-keyboard for composite build."
