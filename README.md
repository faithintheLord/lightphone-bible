# Light Phone Bible

An offline Bible reader for Light Phone III, built with the official [Light SDK](https://github.com/lightphone/light-sdk). Read the Berean Standard Bible (BSB) and World English Bible (WEB), compare translations, save verses and notes, follow reading plans, and explore BSB Strong's word mappings.

This is an independent community project. Light's approval and distribution are separate from local builds; see [submission status](SUBMISSION.md).

## Features

- Book/chapter browsing, selected-verse navigation, reading position, and adjustable text size
- BSB and WEB, with optional comparison while reading
- Local bookmarks, highlights, and editable notes
- Offline text search of the selected translation
- Bible in a Year, New Testament in 90 Days, and Psalms in 30 Days
- Verse of the Day and BSB Hebrew/Greek word study
- Resumable downloads that retain already downloaded chapters

No accounts, advertising, analytics, or push notifications. Scripture and word mappings require an initial HTTPS download. Strong's dictionaries and reading plans are bundled. Completed content works offline. Read the [privacy policy](PRIVACY.md) for network and storage details.

## Build

Requirements: JDK 17, Android SDK platform 36 and build tools 36.0.0, Python 3.11+, and Git. The tool supports Android 14 / Light Phone III. Internet access to Google Maven, Maven Central, the Gradle Plugin Portal, and JitPack is required for the first build. No GitHub Packages credentials are needed.

```bash
git clone --recurse-submodules https://github.com/faithintheLord/lightphone-bible.git
cd lightphone-bible
cp local.properties.example local.properties
# Set sdk.dir in local.properties to your Android SDK installation.
source scripts/android-env.sh
./gradlew :tool:check :tool:assembleDebug
```

The SDK submodule is pinned to official release **v0.2.0**, commit `0577efca9bde782fbfd37309adcf78aa4d5b29df`. SDK sources are unmodified. Asset access uses `SealedLightContext.readAsset`; no framework-context or keyboard patches are required.

## Test and install

```bash
./gradlew :tool:installDebug
adb shell am start -n com.lightphone.bible/com.thelightphone.sdk.LightActivity
./scripts/smoke-test-emulator.sh
```

Local debug APKs use the SDK's public development key. Local release builds are unsigned and intended for validation. Light builds and signs the published public commit.

The committed `tool/lighttool.toml` targets `com.lightos`, as required on a real phone. Basic reading can be tested on a standard Android emulator. For LightOS integration testing, use [Light's emulator setup](vendor/light-sdk/docs/system_app/README.md); `scripts/setup-lightos-emulator.sh` builds the emulator via `-PincludeLightEmulator=true`. Use `scripts/launch-bible.sh --lightos-emulator` for a temporary emulator-targeted build; the script restores production metadata afterward.

## Submission build

```bash
./scripts/verify-submission.sh
```

This extracts only `tool/` files using Light's official builder code, overlays them on a clean copy of the pinned SDK, and builds an unsigned, optimized ARM64 release. It checks that the tool does not depend on this repository's custom root build wiring. Results are written to `build/submission-check/`. Light's server build and review are still required.

See [SUBMISSION.md](SUBMISSION.md), [listing copy](docs/STORE_LISTING.md), [test checklist](docs/TEST_CHECKLIST.md), and [SDK review](docs/SDK_REVIEW.md).

## Data and licenses

App code is [MIT licensed](LICENSE). Scripture and dictionaries have separate terms documented in [THIRD_PARTY_LICENSES.md](THIRD_PARTY_LICENSES.md). The dictionary generation script pins and verifies its upstream source; run `scripts/prepare-strongs.sh` to reproduce the bundled JSON and notices.

Report bugs through [GitHub issues](https://github.com/faithintheLord/lightphone-bible/issues). Do not include private notes or personal information in public reports.
