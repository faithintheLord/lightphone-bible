# Light SDK readiness review

## Current upgrade — November 23, 2026

Bible 1.0.2 (version code 3) targets official [SDK v0.2.10](https://github.com/lightphone/light-sdk/releases/tag/v0.2.10), commit `7f95f38b2b5c59f67ea11e921ab6195243907c6d`, with unmodified SDK sources. The migration reviews the [changes since v0.2.0](https://github.com/lightphone/light-sdk/compare/v0.2.0...v0.2.10) relevant to the app, its build wiring, metadata, and submission flow. This is a compatibility review, not a fresh security certification or a repeat of the historical file-by-file audit below.

- Root `gradle.properties` must declare `sdkVersion=0.2.10`. Light [uses this field to select its builder image](https://github.com/lightphone/light-sdk/blob/v0.2.10/docs/repo/README.md#required-files); updating the submodule alone is insufficient.
- The local version catalog must match the SDK, including Light keyboard `v0.0.21`, Tool Manager `0.0.18`, and the new media3 datasource/database aliases. Compile SDK 36, minimum/target SDK 34, JDK 17, Kotlin 2.3.20, and AGP 8.12.3 remain unchanged.
- The Bible app's used SDK APIs remain source-compatible. `LightEntryPoint.onAlert` has a default implementation, and `buildDatabase` adds an optional configuration callback. Bible does not request the new alerts capability.
- `LightTextInputEditor` now opens the external Light keyboard when available and uses the embedded keyboard otherwise. Its `initialCaps` parameter became `autoCapitalize`; Bible did not supply the removed parameter. Search and note editing need runtime checks in both keyboard modes, especially cancel and rejected blank searches.
- The source banlist and native-library approval policy did not tighten between these SDK tags. Internet and network-state permissions remain allowed. Production metadata must target `com.lightos`; the emulator launch script already supplies the alternate-server opt-in for temporary emulator builds.
- The app owner chose to retain `com.lightphone.bible`. This is in a [reserved namespace](https://github.com/lightphone/light-sdk/blob/v0.2.10/docs/tool_metadata/README.md#id--your-tools-unique-identifier). The plugin only warns locally, while the documented server policy rejects it. An exception from Light is still required for the existing listing; this upgrade does not claim full submission-policy compliance.

Tests, lint, debug assembly, extracted-source release, final-commit CI, and runtime results are recorded in [SUBMISSION.md](../SUBMISSION.md). They are pending for this candidate until recorded there. Light's signed build and physical-device testing remain separate requirements.

## Historical review — October 16, 2026

The following records describe the SDK 0.2.0 migration and do not establish validation of SDK 0.2.10.

### Versions reviewed

- Previous application SDK: 0.0.9, commit `ef859bff63a123a44339be058dd33474ae5cfb90`, with a local context-access patch.
- Official release selected for that migration: [v0.2.0](https://github.com/lightphone/light-sdk/tree/v0.2.0), commit `0577efca9bde782fbfd37309adcf78aa4d5b29df`.
- Upstream `main` fetched and reviewed at that time: `69216b3cd2e6a45abab63268c4d08800a5aa33da` (October 15, 2026).

The review covered client, UI, shared, server, emulator, and trust libraries; all examples; Gradle plugin and lint rules; builder/extractor; signing tools; tests; and repository documentation/build configuration. Vector XML resources were structurally inspected, and binary/media/signature fixtures inventoried. The [file-by-file coverage manifest](review-evidence/sdk-review-coverage.json) accounts for all 461 tracked files: 328 complete text reads (32,013 lines), 106 vector XML inspections, and 27 binary/cryptographic inventories. This is a compatibility/readiness review, not a claim of exhaustive SDK security certification. Post-release additions in main concern optional audio, NFC, haptics, UI styling, and stronger server-package validation; the Bible tool does not require those optional APIs.

### App changes in the 0.2.0 migration

- Replace patched Android context exposure with official `SealedLightContext.readAsset`.
- Use the current `LightJobResult` constructors and handle SDK 0.2.0's job-input string representation.
- Update dependency catalog and Android minimum/target to SDK defaults (compile 36, minimum/target 34, JDK 17).
- Resolve keyboard through the SDK's pinned public JitPack coordinate; remove locally patched keyboard composite build.
- Commit production server `com.lightos`; keep emulator settings temporary and local.
- Place source in `tool/`, with metadata and an independently extractable build script. Light builds the extracted tool against its SDK rather than the app's root Gradle wiring; root `gradle.properties` still supplies the server's SDK selector.
- Keep release artifacts unsigned for Light's signing pipeline; development-key APKs are only for testing.

### Publishing requirements reviewed at that time

The reviewed submission flow required a public GitHub repository, immutable source ref, globally unique package, increasing versionCode, and at least one screenshot. Light builds/signs the submitted source and performs approval review. A local APK cannot establish Light approval. See the current candidate section above for the reserved-ID policy.

### Historical data checks

The checked BSB display dataset had all 1,189 expected chapter files across 66 books (upstream tree `a4a2c0558c26e0281ba4e3f0e5479398459e2183`). The app's former book-level URLs returned 404; the migration switched to the published per-chapter `base/display/{BOOK}/{BOOK}{chapter}.json` format. Display data was identified as CC0 by the source's attribution document.

Bundled dictionaries were checked against Open Scriptures commit `0acd2f251c2d35ff8db2dece4e0593979d3ac223`: 5,523 Greek entries and 8,674 Hebrew entries, unchanged as dictionary objects. Source headers, transformations, and hashes are preserved alongside the assets.

### Notes for reviewers

The tool metadata declares internet and network-state access. Standard SDK manifests contribute additional permissions for shared SDK functionality; inspect the merged release manifest, rather than assuming the metadata list is the entire APK list. The app does not invoke camera, location, microphone, or push features.
