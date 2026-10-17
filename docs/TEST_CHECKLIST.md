# Release test checklist

Record actual results in SUBMISSION.md. An unchecked item is not a passed test.

## Automated

- Unit tests: Scripture parsing, bounded references, resumable download behavior, job input decoding, reader text/word annotation integrity, morphology decoder.
- Android lint and debug APK assembly.
- Source extraction using the official SDK builder.
- Optimized unsigned ARM64 release built from the extracted source against the unmodified pinned SDK.
- Release manifest: `com.lightphone.bible`, expected version, production server `com.lightos`, SDK 0.2.0, system-managed orientation, non-debuggable.
- Bundled Strong's entries and provenance checksums; plan day counts and complete chapter coverage.

## Device/emulator

1. Clean install: initial download status is visible; disconnect/reconnect the network and retry. Text becomes readable before all word-study downloads finish.
2. Navigate Genesis and John; previous/next works across chapter/book boundaries; reopening restores reading position.
3. Select a verse, bookmark it, change/remove its highlight, add/edit/delete a note, and reopen it from Saved.
4. Search both translations, including punctuation and an empty query. Confirm results open the correct translation and verse.
5. Set each reading size. Enable comparison and change primary translation; confirm text/labels update on returning to Reader.
6. Open a BSB mapped word, read its Strong's definition, and follow a concordance result back to the correct verse.
7. Open each reading plan, advance a day, mark progress, leave, and resume. Confirm there is no growing stack of previous plan days.
8. Relaunch without network: reading, search, saved items, plans, and downloaded word study work.
9. Verify all screens in LightOS light and dark themes, keyboard entry/back navigation, physical back/home behavior, and phone sleep/resume.
10. Install the optimized release on a Light Phone III after local/emulator checks. Confirm process recreation, download recovery, and no crash logs.

Use actual screenshots of the validated app for the listing. Light requires at least one image. Never substitute a mockup for test evidence.
