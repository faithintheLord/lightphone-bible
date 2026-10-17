# Third-party content and licenses

## Scripture

**Berean Standard Bible (BSB)** — public domain. [Berean Bible](https://berean.bible/) dedicated the translation to the public domain on April 30, 2023. The Holy Bible, Berean Standard Bible, is produced in cooperation with Bible Hub, Discovery Bible, OpenBible.com, and the Berean Bible Translation Committee.

**World English Bible (WEB)** — public domain. “World English Bible” is a trademark of eBible.org; the name should not be used for altered translation text. [Publisher's notice](https://ebible.org/engwebp/copyright.htm). This app displays the supplied verse text; word-study annotations do not replace it with interlinear wording.

Both translations are downloaded through [Free Use Bible API](https://bible.helloao.org/docs/). The API's software license does not replace each translation's content terms.

## Strong's dictionaries

Bundled data comes from [Open Scriptures Strong's](https://github.com/openscriptures/strongs/tree/0acd2f251c2d35ff8db2dece4e0593979d3ac223).

- Greek JSON: copyright 2009 Open Scriptures, CC-BY-SA, derived from XML prepared by Ulrik Petersen from Strong's 1890 Greek dictionary.
- Hebrew JSON: copyright 2010 Open Scriptures, CC-BY-SA, derived from XML; source work by David Instone-Brewer, with editing, corrections, and Unicode transliterations by David Troidl, based on Strong's 1894 Hebrew dictionary.
- The upstream headers specify CC-BY-SA without a version; this project preserves that statement rather than assigning an unsupported version.

The JavaScript wrappers were removed and JSON formatting compacted. Dictionary entries are unchanged. The dictionary data retains its upstream CC-BY-SA terms, separate from the app's MIT code license. Full upstream notices are bundled in [NOTICE.txt](tool/src/main/assets/strongs/NOTICE.txt); source commit, checksums, and transformation are recorded in [provenance.json](tool/src/main/assets/strongs/provenance.json). Reproduce them with `scripts/prepare-strongs.sh`.

## BSB word mappings

The app downloads only the `base/display/` data from [BSB Publishing](https://github.com/BSB-publishing/bsb-data-output), labeled **CC0 / public domain** by its [attribution document](https://github.com/BSB-publishing/bsb-data-output/blob/main/ATTRIBUTION.md). It does not download the separately licensed `index-cc-by` morphology dataset.

## Software

The [Light SDK](https://github.com/lightphone/light-sdk) is MIT licensed; its license is preserved in the pinned submodule. AndroidX, Kotlin, Ktor, and SDK transitive dependencies retain their respective upstream licenses and packaged notices. App source is covered by [LICENSE](LICENSE); that license does not supersede third-party content terms.
