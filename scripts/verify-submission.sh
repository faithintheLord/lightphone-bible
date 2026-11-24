#!/usr/bin/env bash
# Build exactly the source subset Light's builder accepts, against the pinned SDK.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
source "$ROOT/scripts/android-env.sh"
SDK="$ROOT/vendor/light-sdk"
SOURCE_REF="$(git -C "$ROOT" rev-parse HEAD)"
SOURCE_DIRTY="$(git -C "$ROOT" status --porcelain -- tool gradle.properties vendor/light-sdk)"
OUT="${1:-$ROOT/build/submission-check}"
mkdir -p "$OUT"
OUT="$(cd "$OUT" && pwd)"
WORKSPACE="$(mktemp -d "$OUT/sdk.XXXXXX")"
trap 'rm -rf "$WORKSPACE"' EXIT
SDK_REF="$(git -C "$SDK" rev-parse HEAD)"
git -C "$SDK" archive "$SDK_REF" | tar -x -C "$WORKSPACE"
# Light selects its builder image from the submitted root sdkVersion. Check
# that this local extracted-source build uses the same SDK selection.
SDK_VERSION="$(python3 - "$ROOT" "$WORKSPACE" <<'PY'
import re, sys
from pathlib import Path

root, sdk = map(Path, sys.argv[1:])
for relative in ('gradle.properties', 'tool/lighttool.toml', 'tool/build.gradle.kts'):
    assert (root / relative).is_file(), f'Missing required submission file: {relative}'
assert (root / 'tool/src/main').is_dir(), 'Missing required submission directory: tool/src/main'

def sdk_version(path):
    versions = re.findall(r'^sdkVersion\s*=\s*(\S+)\s*$', path.read_text(), re.MULTILINE)
    assert len(versions) == 1, f'Expected one sdkVersion in {path}'
    return versions[0]

selected = sdk_version(root / 'gradle.properties')
actual = sdk_version(sdk / 'gradle.properties')
assert selected == actual, f'Submitted sdkVersion {selected} differs from pinned SDK {actual}'
print(actual)
PY
)"
PYTHONPATH="$SDK/builder" python3 -m lightbuilder prepare \
  --dev-repo "$ROOT" --tool-path tool \
  --workspace-tool "$WORKSPACE/tool" --output-dir "$OUT"
python3 - "$ROOT/tool" "$WORKSPACE/tool" "$OUT" <<'PY'
import json, sys, zipfile
from pathlib import Path

source, staged, out = map(Path, sys.argv[1:])
report = json.loads((out / 'extraction.json').read_text())
reported = {p if p in ('build.gradle.kts', 'lighttool.toml') else f'src/main/{p}' for p in report['files']}
staged_files = {'build.gradle.kts', 'lighttool.toml'} | {
    p.relative_to(staged).as_posix() for p in (staged / 'src/main').rglob('*') if p.is_file()
}
with zipfile.ZipFile(out / 'extracted-source.zip') as archive:
    assert set(archive.namelist()) == reported == staged_files, 'Extracted source inventory differs from staged build inputs'
    for name in archive.namelist():
        assert archive.read(name) == (source / name).read_bytes() == (staged / name).read_bytes(), f'Extracted source differs: {name}'
PY
(cd "$WORKSPACE" && ./gradlew :tool:assembleRelease \
  -DlightSdk.toolOnly=true -DlightSdk.unsigned=true \
  -DlightSdk.abiFilters=arm64-v8a --console=plain) 2>&1 | tee "$OUT/build.log"
cp "$WORKSPACE/tool/build/outputs/apk/release/tool-release-unsigned.apk" "$OUT/lightphone-bible-unsigned.apk"
apkanalyzer manifest print "$OUT/lightphone-bible-unsigned.apk" > "$OUT/AndroidManifest.xml"
python3 - "$OUT" "$SDK_REF" "$ROOT" "$SOURCE_REF" "$SOURCE_DIRTY" "$SDK_VERSION" <<'PY'
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
assert metadata['com.thelightphone.sdk.SDK_VERSION']==sys.argv[6]
(out/'artifact.json').write_text(json.dumps({
    'sdkVersion':sys.argv[6], 'sdkCommit':sys.argv[2],
    'sourceCommit':sys.argv[4], 'sourceHasUncommittedChanges':bool(sys.argv[5]),
    'extractedSourceSha256':hashlib.sha256((out/'extracted-source.zip').read_bytes()).hexdigest(),
    'file':apk.name,
    'sha256':hashlib.sha256(apk.read_bytes()).hexdigest(),
    'sizeBytes':apk.stat().st_size,
    'status':'Local unsigned validation artifact; Light builds and signs the submitted public commit.'
}, indent=2)+'\n')
PY
printf 'Submission build verified: %s\n' "$OUT"
