# Light-Approved Community Tool — Submission Checklist

Use this checklist when Light's build/sign pipeline opens.

## Repository

- [ ] Public GitHub repository URL: `https://github.com/<org>/lightphone-bible`
- [ ] Git tag for submission (e.g. `v1.0.0`) points to exact commit
- [ ] `vendor/light-sdk` submodule pinned to official Light SDK release (currently `ef859bf`, post-`v0.0.8` main)
- [ ] `./gradlew :bible:check` passes locally and in CI

## Tool metadata (`bible/lighttool.toml`)

- [ ] `id = "com.lightphone.bible"` — **deferred:** file [docs/COMMUNITY_TOOL_ISSUE.md](docs/COMMUNITY_TOOL_ISSUE.md) when ready to publish (do not open during initial dev)
- [ ] `versionCode` strictly greater than any prior published build
- [ ] `versionName` semver display string
- [ ] Only allowlisted permissions: `INTERNET`, `ACCESS_NETWORK_STATE`

## Light ethos

- [ ] UI built with Light SDK components (`LightTheme`, `LightTopBar`, `LightBottomBar`, `LightScrollView`, `LightText`)
- [ ] No ads, accounts, social feeds, push notifications, or analytics
- [ ] Distraction-free reading experience

## Legal / attribution

- [ ] [LICENSE](LICENSE) (app code)
- [ ] [PRIVACY.md](PRIVACY.md)
- [ ] [THIRD_PARTY_LICENSES.md](THIRD_PARTY_LICENSES.md)
- [ ] In-app Settings → About shows translation and Strong's attributions

## Reviewer notes

- First launch downloads BSB + WEB (~15–20 MB) and BSB interlinear data; Wi-Fi recommended
- Strong's dictionaries bundled in APK (~1.5 MB)
- Word study available for BSB only (tap a word in reader)
- Offline fully functional after sync completes

## Version discipline

| Submission | versionCode | versionName | Git tag |
|------------|-------------|-------------|---------|
| Initial    | 1           | 1.0.0       | v1.0.0  |

Increment `versionCode` on every submission to Light's build server.
