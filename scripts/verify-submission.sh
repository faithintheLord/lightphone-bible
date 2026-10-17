#!/usr/bin/env bash
# Build exactly the source subset Light's builder accepts, against the pinned SDK.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
source "$ROOT/scripts/android-env.sh"
SDK="$ROOT/vendor/light-sdk"
SOURCE_REF="$(git -C "$ROOT" rev-parse HEAD)"
SOURCE_DIRTY="$(git -C "$ROOT" status --porcelain -- tool)"
OUT="${1:-$ROOT/build/submission-check}"
mkdir -p "$OUT"
OUT="$(cd "$OUT" && pwd)"
WORKSPACE="$(mktemp -d "$OUT/sdk.XXXXXX")"
trap 'rm -rf "$WORKSPACE"' EXIT
SDK_REF="$(git -C "$SDK" rev-parse HEAD)"
git -C "$SDK" archive "$SDK_REF" | tar -x -C "$WORKSPACE"
PYTHONPATH="$SDK/builder" python3 -m lightbuilder prepare \
  --dev-repo "$ROOT" --tool-path tool \
  --workspace-tool "$WORKSPACE/tool" --output-dir "$OUT"
(cd "$WORKSPACE" && ./gradlew :tool:assembleRelease \
  -DlightSdk.toolOnly=true -DlightSdk.unsigned=true \
  -DlightSdk.abiFilters=arm64-v8a --console=plain) 2>&1 | tee "$OUT/build.log"
cp "$WORKSPACE/tool/build/outputs/apk/release/tool-release-unsigned.apk" "$OUT/lightphone-bible-unsigned.apk"
apkanalyzer manifest print "$OUT/lightphone-bible-unsigned.apk" > "$OUT/AndroidManifest.xml"
python3 - "$OUT" "$SDK_REF" "$ROOT" "$SOURCE_REF" "$SOURCE_DIRTY" <<'PY'
import hashlib, json, sys, tomllib, zipfile
import xml.etree.ElementTree as ET
from pathlib import Path
out=Path(sys.argv[1]); apk=out/'lightphone-bible-unsigned.apk'
with zipfile.ZipFile(apk) as archive:
    abis={name.split('/')[1] for name in archive.namelist() if name.startswith('lib/') and name.endswith('.so')}
assert abis <= {'arm64-v8a'}, abis
a='{http://schemas.android.com/apk/res/android}'
manifest=ET.parse(out/'AndroidManifest.xml').getroot()
app=manifest.find('application')
tool=tomllib.loads((Path(sys.argv[3])/'tool/lighttool.toml').read_text())['tool']
assert manifest.attrib['package']==tool['id']
assert manifest.get(a+'versionCode')==str(tool['versionCode'])
assert manifest.get(a+'versionName')==tool['versionName']
assert app.get(a+'debuggable', 'false')=='false'
metadata={n.get(a+'name'):n.get(a+'value') for n in manifest.iter('meta-data')}
assert metadata['com.thelightphone.sdk.LIGHT_SERVER_PACKAGE']=='com.lightos'
assert metadata['com.thelightphone.sdk.SDK_VERSION']=='0.2.0'
(out/'artifact.json').write_text(json.dumps({
    'sdkCommit':sys.argv[2], 'sourceCommit':sys.argv[4], 'sourceHasUncommittedChanges':bool(sys.argv[5]),
    'extractedSourceSha256':hashlib.sha256((out/'extracted-source.zip').read_bytes()).hexdigest(),
    'file':apk.name,
    'sha256':hashlib.sha256(apk.read_bytes()).hexdigest(),
    'sizeBytes':apk.stat().st_size,
    'status':'Local unsigned validation artifact; Light builds and signs the submitted public commit.'
}, indent=2)+'\n')
PY
printf 'Submission build verified: %s\n' "$OUT"
