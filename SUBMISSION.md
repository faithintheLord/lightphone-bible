# Light Tool Library submission

Repository: https://github.com/faithintheLord/lightphone-bible

This is a submission candidate, not a Light-approved release. Light builds, signs, and reviews the public source commit. The local unsigned APK is a validation artifact.

## Submission fields

| Field | Value |
| --- | --- |
| Tool name | Bible |
| Package | `com.lightphone.bible` |
| Source directory | `tool/` |
| Version | `1.0.2` (`versionCode = 3`) |
| Candidate branch | `codex/sdk-0.2.10-submission` (use its final immutable commit when submitting) |
| SDK | `v0.2.10`, commit `7f95f38b2b5c59f67ea11e921ab6195243907c6d` |
| Production server | `com.lightos` |
| Listing and changelog | [STORE_LISTING.md](docs/STORE_LISTING.md) |
| Privacy | [PRIVACY.md](PRIVACY.md) |
| Content notices | [THIRD_PARTY_LICENSES.md](THIRD_PARTY_LICENSES.md) |
| Listing images | [Historical 1.0.0 screenshots](docs/screenshots/README.md); refresh after candidate device testing |

Root `gradle.properties` sets `sdkVersion=0.2.10`, which [selects Light's server builder image](https://github.com/lightphone/light-sdk/blob/v0.2.10/docs/repo/README.md#required-files). The vendored SDK and local dependency catalog match that version. Version code 3 advances beyond the earlier 1.0.1 candidate's code 2; confirm it also exceeds the latest version listed in the Dashboard before submitting.

## Existing package and server acceptance

The app owner chose to retain `com.lightphone.bible` for the existing Dashboard listing and installed-app identity. SDK 0.2.10 [reserves the `lightphone` and `thelightphone` namespaces](https://github.com/lightphone/light-sdk/blob/v0.2.10/docs/tool_metadata/README.md#id--your-tools-unique-identifier): its Gradle plugin warns, and its documentation says the build server rejects them. Light must confirm an exception for this existing package before this candidate can be described as meeting all submission requirements. Renaming the ID would create a separate tool.

The earlier 1.0.1 submission of `1e34baa010f6a2e4c35cc11ee9fc8cef83ccd867` reported “The APK could not be published. Please retry.” The Dashboard message does not identify the cause. The reserved-name rule is a possible cause, not a confirmed diagnosis.

## Current candidate validation — November 23, 2026

Record results for 1.0.2 / SDK 0.2.10 here and in [the release test checklist](docs/TEST_CHECKLIST.md). Historical results below do not validate this candidate.

- Debug/release unit tests, Android lint, and debug assembly: pending.
- SDK metadata, dependency, and restricted-API validation: pending; reserved-ID warning expected.
- Official source extraction and optimized unsigned ARM64 release: pending.
- GitHub Linux CI for the final candidate commit: pending.
- Device/emulator runtime, saved-data upgrade, and external/embedded keyboard checks: not performed for this candidate.
- Light's server build, signed APK testing, package exception, and approval: pending.

## Historical validation — 1.0.0 / SDK 0.2.0

The following results were recorded for the October 17, 2026 candidate `v1.0.0-rc.1`, not the current SDK upgrade:

- Debug build: passed.
- Debug and release unit tests: 38 passed in each variant, zero failures/errors.
- Android lint: zero errors; one SDK-generated manifest warning (no Android launcher icon).
- SDK plugin metadata, dependency, and restricted-API validation: passed.
- Source extraction: passed using Light's official builder and a clean copy of the pinned SDK.
- Optimized ARM64 release: final extracted-source build passed. The build used clean source commit `73bfc77b3e4cda8043e4f9ee5f1e09189d95c137`; subsequent candidate changes concern scripts, documentation, and screenshots only. [Artifact hashes and provenance](docs/review-evidence/release-build.json).
- GitHub Linux CI: tests, lint, debug build, source extraction, and optimized release build [passed](https://github.com/faithintheLord/lightphone-bible/actions/runs/39004412986). Its tool source tree matches the final local release.
- Emulator: launch, Scripture navigation, bookmarks, notes, restart persistence, and legacy-text migration preserving saved data passed. Both translations and all word mappings completed. Offline reading, comparison, search, definitions, and concordance navigation passed.
- Optimized emulator runtime: 45-second cold-launch/process/crash smoke test passed. Saved-data upgrade, note editing/deletion, highlight removal, and reading-plan progress/resume/navigation passed. Actual release screenshots are included.
- Physical Light Phone III / LightOS checks: not performed; no physical phone is connected.

The isolated Android 14 ARM64 emulator used a 1080 × 1240 display. A database integrity check returned `ok`: BSB has 31,086 readable verses; WEB has 31,098; each has 66 books and 1,189 chapters. There are 821,929 BSB word mappings, 5,523 Greek dictionary entries, and 8,674 Hebrew entries. [Device data evidence](docs/review-evidence/device-data-check.json). This does not replace testing LightOS integration and a Light-signed build on the physical phone.

The historical build emitted R8 Kotlin metadata compatibility warnings with Kotlin 2.3.20 and AGP 8.12.3. SDK 0.2.10 retains those toolchain versions; record this candidate's actual build warnings and runtime results separately. The historical Android launcher-icon warning came from the SDK-generated tool manifest.

Live source checks found both translations complete (66 books / 1,189 chapters each), all expected BSB word-mapping chapter files, and exact dictionary correspondence to the pinned source. Reading plans cover all intended chapters without gaps, duplicates, or empty days. Scripture normalization matched all 62,189 provider verse records against its simplified output; five WEB records contain only editorial footnotes and are omitted without renumbering readable verses. These source checks are separate from testing downloads on a phone.

## Reviewer notes

An initial connection downloads BSB and WEB Scripture followed by BSB word mappings. This can take several minutes and many chapter requests. Downloaded chapters survive interrupted downloads, and completed translations become readable while word mappings continue. Word-study dictionaries and reading plans are bundled. Notes, bookmarks, highlights, and progress stay in local app storage; see the privacy policy for third-party download requests and system-backup behavior.

The metadata requests internet and network-state access. Inspect the candidate's merged manifest for SDK/transitive permissions. Bible does not invoke camera, microphone, location, push, or alerts features. WorkManager manages resumable background work. SDK 0.2.10 uses the external Light keyboard when available and falls back to the embedded keyboard; exercise both search and note editing paths.

## Submit through Light

Follow [Light's current submission instructions](https://github.com/lightphone/light-sdk#submitting-your-tool):

1. Confirm with Light that the existing `com.lightphone.bible` listing may continue using its reserved package ID.
2. Open the existing Bible entry under Manage Custom Tools in the [Light Dashboard](https://dashboard.thelightphone.com/). Check that no earlier build is still active and that version code 3 exceeds the latest listed version.
3. Select Submit Build and enter the final public immutable Git commit from `codex/sdk-0.2.10-submission`, plus the 1.0.2 release notes from the listing document.
4. Inspect the server build result. A successful build produces Light's signed APK and a pending-approval version. Test that artifact on Light Phone III, including saved-data upgrade and keyboard flows.
5. Refresh screenshots from the tested candidate and resolve review feedback. Light requires at least one screenshot for approval.

Preparing this SDK upgrade does not submit it to Light or establish approval.
