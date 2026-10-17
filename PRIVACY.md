# Privacy — Light Phone Bible

Updated October 16, 2026.

Light Phone Bible has no accounts, advertising, analytics, or developer-operated servers. Bookmarks, highlights, notes, reading history, plan progress, downloaded Scripture, and settings are stored in the app's private storage on your device.

## Network requests

The app downloads BSB and WEB Scripture from [Free Use Bible API](https://bible.helloao.org), and BSB word mappings from [BSB Publishing on GitHub](https://github.com/BSB-publishing/bsb-data-output), served by `raw.githubusercontent.com`. These services receive your IP address and the requested translation/book/chapter paths as part of ordinary HTTPS requests. Their service policies apply. The app does not send your notes, bookmarks, highlights, reading history, searches, or other personal content to them.

Downloads run on first launch and when you request or retry them. Completed chapters remain available offline, and incomplete downloads can resume. Strong's dictionaries and reading plans are bundled with the app. After the content you want is downloaded, reading, search, saved items, plans, and word study work offline.

## Permissions and storage

The tool declares internet and network-state access. The standard Light SDK and its dependencies can add platform permissions in the merged APK (including camera and vibration support); this Bible tool does not invoke camera, microphone, location, or push-notification features. It does not request those runtime permissions.

Personal content remains until you delete it in the app or clear/uninstall the app. Device-level backup or transfer behavior is controlled by Android/LightOS and your device settings. There is no app-operated cloud backup or sync.

## Contact

Report privacy questions through [the project issue tracker](https://github.com/faithintheLord/lightphone-bible/issues). Avoid including private notes or other personal information in a public issue.
