#!/usr/bin/env bash
# Run ANTLR4 query compatibility gates (golden AST + semantic snapshots).
# Also cross-checks the Strict AST corpus against TestParserStrict
# expected strings in the current tree.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

JAVA_HOME="${JAVA_HOME:-}"
if [[ -z "${JAVA_HOME}" && -d "${HOME}/.gradle/jdks/eclipse_adoptium-21-aarch64-os_x.2/jdk-21.0.10+7/Contents/Home" ]]; then
  export JAVA_HOME="${HOME}/.gradle/jdks/eclipse_adoptium-21-aarch64-os_x.2/jdk-21.0.10+7/Contents/Home"
  export PATH="${JAVA_HOME}/bin:${PATH}"
fi

echo "==> Golden AST corpus (support)"
mvn -pl chemistry-opencmis-server/chemistry-opencmis-server-support -am \
  -Dtest=QueryAstCorpusTest,TestParserStrict,TestParserTextSearch \
  -Dsurefire.failIfNoSpecifiedTests=false test

echo "==> Semantic snapshots (inmemory)"
mvn -pl chemistry-opencmis-server/chemistry-opencmis-server-inmemory -am \
  -Dtest=QuerySemanticSnapshotTest,EvalQueryTest \
  -Dsurefire.failIfNoSpecifiedTests=false test

echo "==> Cross-check corpus vs current TestParserStrict expectations"
python3 - <<'PY'
import re, sys
from pathlib import Path

corp = Path("chemistry-opencmis-server/chemistry-opencmis-server-support/src/test/resources/query-compat/strict-ast.corpus")
src_path = Path("chemistry-opencmis-server/chemistry-opencmis-server-support/src/test/java/org/apache/chemistry/opencmis/server/support/query/TestParserStrict.java")
rows = []
for line in corp.read_text().splitlines():
    if not line.startswith("ok\t"):
        continue
    _, rule, inp, exp = line.split("\t", 3)
    rows.append((rule, inp, exp))

src = src_path.read_text()
expects = []
pat = re.compile(
    r'testParser\(\s*"([^"]+)"\s*,\s*"((?:\\.|[^"\\])*)"\s*,\s*"((?:\\.|[^"\\])*)"\s*\)',
    re.S,
)
for m in pat.finditer(src):
    expects.append((m.group(1), m.group(2), m.group(3)))
for m in re.finditer(
    r'testParser\(\s*\n\s*"query"\s*,\s*\n\s*"([^"]+)"\s*\+\s*\n\s*"([^"]+)"\s*,\s*\n\s*"([^"]+)"\s*\)',
    src,
):
    expects.append(("query", m.group(1) + m.group(2), m.group(3)))

by_input = {(r, i): e for r, i, e in rows}
mismatched = []
tracked = 0
for rule, inp, exp in expects:
    if (rule, inp) not in by_input:
        continue
    tracked += 1
    if by_input[(rule, inp)] != exp:
        mismatched.append((rule, inp, exp, by_input[(rule, inp)]))

print(f"TestParserStrict expected triples: {len(expects)}")
print(f"corpus ok rows: {len(rows)}")
print(f"overlapping (same rule+input): {tracked}")
if mismatched:
    print("MISMATCHES:")
    for m in mismatched:
        print(" ", m)
    sys.exit(1)
print("OK: golden corpus matches TestParserStrict expected ASTs")
PY

echo "==> Compatibility gates passed"
