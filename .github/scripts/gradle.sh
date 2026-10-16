#!/usr/bin/env bash
# Runs Gradle with the given arguments. When it fails, the Kotlin compiler errors, the failed tests
# and the "What went wrong" section are published as GitHub annotations, so they can be read in the
# pull request without opening the full log.
set -o pipefail
log="${RUNNER_TEMP:-/tmp}/gradle-$(date +%s%N).log"
./gradlew "$@" --console=plain 2>&1 | tee "$log"
status=${PIPESTATUS[0]}
[ "$status" -eq 0 ] && exit 0

encode() { sed -e 's/%/%25/g' | sed ':a;N;$!ba;s/\n/%0A/g'; }

kotlin=$(grep -E '^e: ' "$log" | sed -E 's#^e: file://[^ ]*/((app|core|feature|build-logic)/[^ ]+) #\1 #' | head -50 | encode)
[ -n "$kotlin" ] && echo "::error title=Errores de Kotlin::$kotlin"

python3 - <<'PY'
import glob, xml.etree.ElementTree as ET
lines = []
for path in glob.glob('**/build/test-results/**/*.xml', recursive=True):
    try:
        root = ET.parse(path).getroot()
    except ET.ParseError:
        continue
    for case in root.iter('testcase'):
        for failure in list(case.findall('failure')) + list(case.findall('error')):
            text = ((failure.get('message') or '') + '\n' + (failure.text or '')).strip().splitlines()
            lines.append(f"{case.get('classname')}.{case.get('name')}: {' | '.join(text[:4])[:400]}")
if lines:
    body = '\n'.join(lines[:60]).replace('%', '%25').replace('\r', '').replace('\n', '%0A')
    print(f'::error title=Tests fallidos ({len(lines)})::' + body)
PY

wrong=$(awk '/What went wrong:/{flag=1} flag{print} /^\* Try:/{flag=0}' "$log" | head -60 | encode)
[ -n "$wrong" ] && echo "::error title=Gradle::$wrong"
exit "$status"
