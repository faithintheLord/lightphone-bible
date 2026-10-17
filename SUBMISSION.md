# Light Tool Library submission

Repository: https://github.com/faithintheLord/lightphone-bible

This is a submission candidate, not a Light-approved release. Light builds, signs, and reviews the public source commit. The local unsigned APK is a validation artifact.

## Submission fields

| Field | Value |
| --- | --- |
| Tool name | Bible |
| Package | `com.lightphone.bible` |
| Source directory | `tool/` |
| Version | `1.0.0` (`versionCode = 1`) |
| Candidate ref | `v1.0.0-rc.1` |
| SDK | `v0.2.0`, commit `0577efca9bde782fbfd37309adcf78aa4d5b29df` |
| Production server | `com.lightos` |
| Listing and changelog | [STORE_LISTING.md](docs/STORE_LISTING.md) |
| Privacy | [PRIVACY.md](PRIVACY.md) |
| Content notices | [THIRD_PARTY_LICENSES.md](THIRD_PARTY_LICENSES.md) |
| Listing images | [Verified release screenshots](docs/screenshots/README.md) |

Confirm the package is available when registering it in Light's Dashboard; registration makes the package immutable. Version code 1 assumes this package has no previously registered version. Increase it before a later version submission.

## Validation

Current local results are recorded here and in [the release test checklist](docs/TEST_CHECKLIST.md). Do not treat an untested item as passed.

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

The pinned SDK uses Kotlin 2.3.20 with AGP 8.12.3 and emits R8 Kotlin metadata compatibility warnings. Google documents Kotlin 2.3 support in [AGP 8.13.2 / R8 8.13.19](https://developer.android.com/build/releases/agp-8-13-0-release-notes). Both the stable SDK and reviewed upstream main still use the older AGP. Release assembly and the runtime checks above succeed; retain physical-device verification as a release gate. The Android launcher-icon warning is expected for the SDK-generated tool manifest.

Live source checks found both translations complete (66 books / 1,189 chapters each), all expected BSB word-mapping chapter files, and exact dictionary correspondence to the pinned source. Reading plans cover all intended chapters without gaps, duplicates, or empty days. Scripture normalization matched all 62,189 provider verse records against its simplified output; five WEB records contain only editorial footnotes and are omitted without renumbering readable verses. These source checks are separate from testing downloads on a phone.

## Reviewer notes

An initial connection downloads BSB and WEB Scripture followed by BSB word mappings. This can take several minutes and many chapter requests. Downloaded chapters survive interrupted downloads, and completed translations become readable while word mappings continue. Word-study dictionaries and reading plans are bundled. Notes, bookmarks, highlights, and progress stay in local app storage; see the privacy policy for third-party download requests and system-backup behavior.

The metadata requests internet and network-state access. SDK/transitive manifests also include camera, vibration, wake lock, boot completion, foreground-service, and an app-scoped signature permission. Bible does not invoke camera, microphone, location, or push features. WorkManager manages resumable background work.

## Submit through Light

Follow [Light's current submission instructions](https://github.com/lightphone/light-sdk#submitting-your-tool):

1. In the [Light Dashboard](https://dashboard.thelightphone.com/), enable Settings → Account → Developer Account.
2. Open Manage Custom Tools → Submit New Tool and enter the public repository URL. Light reads package metadata from its default branch.
3. Add the prepared listing and actual app screenshots. Light requires at least one screenshot for approval.
4. Open the tool details, select Submit Build, and enter the immutable Git commit or candidate tag plus the release notes from the listing document.
5. Inspect the server build result. A successful build produces Light's signed APK and a pending-approval version. Test that artifact on Light Phone III and resolve any review feedback.

No submission, registration, or message to Light has been sent by this preparation task.
