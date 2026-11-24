# Release test checklist

Record actual results in SUBMISSION.md. An unchecked item is not a passed test.

## Current candidate — 1.0.2 / SDK 0.2.10

- Local tests, lint, debug build, and extracted-source ARM64 release: pending.
- GitHub CI for the final candidate commit: pending.
- Device/emulator runtime and upgrade checks: not performed for this candidate.
- Existing `com.lightphone.bible` package exception from Light: pending. Local compilation does not establish server acceptance of the reserved ID.

## Historical results — 1.0.0 / SDK 0.2.0, October 17, 2026

These results do not validate the current SDK upgrade.

- All 38 unit tests passed in each debug/release variant. Lint has zero errors and one SDK-generated launcher-icon warning. Local extraction/optimized release and Linux CI passed; see [build evidence](review-evidence/release-build.json).
- Complete downloads and SQLite integrity were checked on the isolated Android 14 ARM64 emulator: both translations, every expected chapter, all BSB mappings, and both dictionaries. [Counts and migration evidence](review-evidence/device-data-check.json).
- Offline checks passed for cold launch, reading, comparison, selected-verse search navigation, Strong's definitions, and concordance navigation. Legacy text migration retained a bookmark and note.
- The optimized release passed the 45-second launch/process/crash smoke test. Saved data survived the debug-to-release upgrade; note editing and deletion, highlight removal, and Saved refresh passed.
- Plan completion, refreshed totals, resuming the first incomplete day, chapter links, and a single back action after multiple day changes passed in the optimized release.
- Physical Light Phone III, LightOS service integration, light-theme device appearance, physical keys, and a Light-signed server artifact remain untested. Use the complete acceptance checklist below on the phone before requesting final approval. The emulator checks do not mark every scenario below as passed.

## Automated

- Unit tests: Scripture parsing, bounded references, resumable download behavior, job input decoding, reader text/word annotation integrity, morphology decoder.
- Android lint and debug APK assembly.
- Source extraction using the official SDK builder.
- Optimized unsigned ARM64 release built from the extracted source against the unmodified pinned SDK.
- Root `sdkVersion`, vendored SDK version, and local dependency catalog agree on SDK 0.2.10.
- Release manifest: `com.lightphone.bible`, version 1.0.2/code 3, production server `com.lightos`, SDK 0.2.10, system-managed orientation, non-debuggable.
- Bundled Strong's entries and provenance checksums; plan day counts and complete chapter coverage.

## Device/emulator

1. Clean install: initial download status is visible; disconnect/reconnect the network and retry. Text becomes readable before all word-study downloads finish.
2. Navigate Genesis and John; previous/next works across chapter/book boundaries; reopening restores reading position.
3. Select a verse, bookmark it, change/remove its highlight, add/edit/delete a note, and reopen it from Saved.
   - Repeat note editing with the external Light keyboard and embedded fallback: submit, cancel, reopen, and verify saved text after restarting the app.
4. Check the home bottom bar: Settings on the left, Books in the center, Search on the right. Confirm SEARCH and SETTINGS are absent from the scrollable action list and both icons still work.
   - Test with the external Light keyboard available and with the SDK's embedded fallback. Open Search from Home and Reader: the keyboard and "Search Bible" title appear directly. Use on-screen Back and system/physical Back separately; each returns to the originating screen before a query is submitted.
   - Submit empty or whitespace-only text: no search runs and the editor remains available. The embedded keyboard stays open; after the external keyboard closes, tap the editor text to reopen it. Search both translations, including punctuation. Confirm results open the correct translation and verse, and Back from the verse restores the results without reopening the keyboard.
   - Tap the query to edit it, change or clear the text, then cancel using the editor's on-screen Back button or the external keyboard's cancel action. The previous query and results remain. Submitting blank text leaves previous results available on cancel. Check physical Back separately in both keyboard modes, including return from the external keyboard.
   - Repeat editing/submitting to verify the keyboard continues to accept input. Check a query with no matches and searching before a translation is downloaded; the message remains visible and the query can still be edited.
5. Set each reading size. Enable comparison and change primary translation; confirm text/labels update on returning to Reader.
6. Open a BSB mapped word, read its Strong's definition, and follow a concordance result back to the correct verse.
7. Open each reading plan, advance a day, mark progress, leave, and resume. Confirm there is no growing stack of previous plan days.
8. Relaunch without network: reading, search, saved items, plans, and downloaded word study work.
9. Verify all screens in LightOS light and dark themes, keyboard entry/back navigation, physical back/home behavior, and phone sleep/resume.
10. Install the optimized release on a Light Phone III after local/emulator checks. Confirm process recreation, download recovery, and no crash logs.
11. Install Light's signed 1.0.2 as an update over the earlier signed Bible version; confirm bookmarks, notes, highlights, reading position, downloads, and plan progress survive. Do not substitute an uninstall/reinstall for this check.

Use actual screenshots of the validated app for the listing. Light requires at least one image. Never substitute a mockup for test evidence.
