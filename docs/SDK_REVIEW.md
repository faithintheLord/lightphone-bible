# Light SDK readiness review — October 16, 2026

## Versions reviewed

- Previous application SDK: 0.0.9, commit `ef859bff63a123a44339be058dd33474ae5cfb90`, with a local context-access patch.
- Current official release selected for this tool: [v0.2.0](https://github.com/lightphone/light-sdk/releases/tag/v0.2.0), commit `0577efca9bde782fbfd37309adcf78aa4d5b29df`.
- Latest upstream `main` fetched and reviewed: `69216b3cd2e6a45abab63268c4d08800a5aa33da` (October 15, 2026).

The review covered client, UI, shared, server, emulator, and trust libraries; all examples; Gradle plugin and lint rules; builder/extractor; signing tools; tests; and repository documentation/build configuration. Vector XML resources were structurally inspected, and binary/media/signature fixtures inventoried. The [file-by-file coverage manifest](review-evidence/sdk-review-coverage.json) accounts for all 461 tracked files: 328 complete text reads (32,013 lines), 106 vector XML inspections, and 27 binary/cryptographic inventories. This is a compatibility/readiness review, not a claim of exhaustive SDK security certification. Post-release additions in main concern optional audio, NFC, haptics, UI styling, and stronger server-package validation; the Bible tool does not require those optional APIs.

## App changes required

- Replace patched Android context exposure with official `SealedLightContext.readAsset`.
- Use the current `LightJobResult` constructors and handle SDK 0.2.0's job-input string representation.
- Update dependency catalog and Android minimum/target to SDK defaults (compile 36, minimum/target 34, JDK 17).
- Resolve keyboard through the SDK's pinned public JitPack coordinate; remove locally patched keyboard composite build.
- Commit production server `com.lightos`; keep emulator settings temporary and local.
- Place source in `tool/`, with metadata and an independently extractable build script. Light's builder discards root Gradle files and other developer modules.
- Keep release artifacts unsigned for Light's signing pipeline; development-key APKs are only for testing.

## Publishing requirements

The [official SDK submission instructions](https://github.com/lightphone/light-sdk#submitting-your-tool) now describe an open Dashboard submission flow. A public GitHub repository, immutable source ref, globally unique package, increasing versionCode, and at least one screenshot are required. Light builds/signs the submitted source and performs approval review. A local APK cannot establish Light approval.

## Data checks

The current BSB display dataset has all 1,189 expected chapter files across 66 books (upstream tree `a4a2c0558c26e0281ba4e3f0e5479398459e2183`). The app's former book-level URLs returned 404; it now reads the published per-chapter `base/display/{BOOK}/{BOOK}{chapter}.json` format. Display data is identified as CC0 by the source's attribution document.

Bundled dictionaries were checked against Open Scriptures commit `0acd2f251c2d35ff8db2dece4e0593979d3ac223`: 5,523 Greek entries and 8,674 Hebrew entries, unchanged as dictionary objects. Source headers, transformations, and hashes are preserved alongside the assets.

## Notes for reviewers

The tool metadata declares internet and network-state access. Standard SDK manifests contribute additional permissions for shared SDK functionality; inspect the merged release manifest, rather than assuming the metadata list is the entire APK list. The app does not invoke camera, location, microphone, or push features.
