# Light Phone Bible

An offline-first Bible reader for [Light Phone III](https://github.com/lightphone/light-sdk), built with the Light SDK. Read the **Berean Standard Bible (BSB)** and **World English Bible (WEB)**, with bookmarks, highlights, notes, search, reading plans, and BSB original-language word study via Strong's numbers.

Target status: **Light-approved Community Tool** when Light's submission pipeline opens.

## Features

- Book / chapter / verse reading with resume position and font size
- BSB + WEB translations (extensible catalog)
- Side-by-side translation compare
- Bookmarks, highlights (4 colors), and notes (all local)
- Full-text search (offline FTS)
- Reading plans (Bible in a Year, NT in 90 Days, Psalms in 30 Days)
- Verse of the Day
- **Word study (BSB):** tap a word for Strong's Hebrew/Greek definitions (openscriptures + BSB interlinear)

## Requirements

- Android Studio / JDK 17
- Git submodules: `vendor/light-sdk` and `vendor/light-keyboard` (keyboard UI is built from source; GitHub Packages credentials are optional)

## Setup

```bash
git clone --recurse-submodules https://github.com/<your-org>/lightphone-bible.git
cd lightphone-bible

# If submodule missing:
git submodule update --init --recursive
./scripts/patch-light-sdk-vendor.sh
./scripts/patch-keyboard-vendor.sh

# Copy local.properties.example → local.properties and set sdk.dir
cp local.properties.example local.properties

# Optional: regenerate Strong's JSON from openscriptures
./scripts/prepare-strongs.sh
```

## Build & run

```bash
source scripts/android-env.sh
./gradlew :bible:check
./gradlew :bible:installDebug
adb shell am start -n com.lightphone.bible/com.thelightphone.sdk.LightActivity
```

## Emulator testing (LightOS)

This repo is configured for the LightOS emulator workflow (`serverPackage = "com.thelightphone.sdk.emulator"` in `bible/lighttool.toml`).

1. **SDK + AVD:** `source scripts/android-env.sh`, then `./scripts/create-avd.sh` (creates `LightPhoneIII`, API 34, 1080×1240).
2. **LightOS emulator (one-time):** `./scripts/setup-lightos-emulator.sh` — builds `:sdk:emulator`, installs as priv-app (`uid=1000`).
3. **Install Bible:** `./gradlew :bible:installDebug` with the emulator running.
4. **Smoke test:** `./scripts/smoke-test-emulator.sh`

Switch `serverPackage` back to `com.lightos` before sideloading to real LP3 hardware.

See [vendor/light-sdk/docs/system_app/README.md](vendor/light-sdk/docs/system_app/README.md) for details.

## First launch

On first open the app downloads BSB and WEB text (~15–20 MB) and BSB interlinear data. Wi-Fi is recommended. Strong's dictionaries (~1.5 MB) are bundled in the APK.

## Project structure

```
lightphone-bible/
├── bible/              # Application module (your tool)
├── vendor/light-sdk/   # Git submodule — official Light SDK
├── scripts/            # vendor patches, AVD, emulator setup, smoke tests
└── docs/               # Submission templates
```

## Submission

See [SUBMISSION.md](SUBMISSION.md), [PRIVACY.md](PRIVACY.md), and [THIRD_PARTY_LICENSES.md](THIRD_PARTY_LICENSES.md).

To reserve tool id `com.lightphone.bible`, file an issue using [docs/COMMUNITY_TOOL_ISSUE.md](docs/COMMUNITY_TOOL_ISSUE.md).

## License

MIT — see [LICENSE](LICENSE). Bible text and Strong's data are separately licensed; see [THIRD_PARTY_LICENSES.md](THIRD_PARTY_LICENSES.md).
