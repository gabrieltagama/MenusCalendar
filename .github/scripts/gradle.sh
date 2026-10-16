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

grep -E '^e: ' "$log" | head -40 | while read -r line; do
  file=$(echo "$line" | sed -nE 's#^e: file://[^ ]*/(app|core|feature|build-logic)/([^:]+):([0-9]+):([0-9]+) .*#\1/\2#p')
  row=$(echo "$line" | sed -nE 's#^e: file://[^:]+:([0-9]+):([0-9]+) .*#\1#p')
  message=$(echo "$line" | sed -E 's#^e: file://[^ ]+ ##')
  if [ -n "$file" ]; then echo "::error file=$file,line=$row,title=Kotlin::$message"; else echo "::error title=Kotlin::$(echo "$line" | encode)"; fi
done

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
            text = (failure.get('message') or failure.text or '').strip().splitlines()
            lines.append(f"{case.get('classname')}.{case.get('name')}: {' | '.join(text[:6])[:600]}")
for line in lines[:30]:
    print('::error title=Test fallido::' + line.replace('%', '%25').replace('\n', '%0A'))
PY

wrong=$(awk '/What went wrong:/{flag=1} flag{print} /^\* Try:/{flag=0}' "$log" | head -60 | encode)
[ -n "$wrong" ] && echo "::error title=Gradle::$wrong"
exit "$status"
