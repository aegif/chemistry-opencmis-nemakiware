#!/usr/bin/env bash
# Verify the OSGi client bundle MANIFEST after package.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

OSGI_MOD="chemistry-opencmis-osgi/chemistry-opencmis-osgi-client"
FINAL_NAME=$(mvn -pl "$OSGI_MOD" -q -DforceStdout help:evaluate -Dexpression=project.build.finalName)
JAR="$OSGI_MOD/target/${FINAL_NAME}.jar"
if [[ ! -f "$JAR" ]]; then
  echo "==> Packaging OSGi client"
  mvn -pl "$OSGI_MOD" -am package -DskipTests -q
fi
test -f "$JAR"

MANIFEST=$(mktemp)
trap 'rm -f "$MANIFEST"' EXIT
unzip -p "$JAR" META-INF/MANIFEST.MF > "$MANIFEST"

echo "==> Checking $JAR MANIFEST"
grep -E 'Bundle-SymbolicName|Import-Package|Bundle-ClassPath|Embed' "$MANIFEST" | head -40 || true

# Flatten Import-Package (may be multi-line) for matching
FLAT=$(tr '\n' ' ' < "$MANIFEST" | tr -d '\r')

echo "$FLAT" | grep -q 'org\.apache\.hc' || {
  echo "FAIL: MANIFEST missing org.apache.hc Import-Package entry"
  exit 1
}
echo "$FLAT" | grep -q 'okhttp3' || {
  echo "FAIL: MANIFEST missing okhttp3 Import-Package entry"
  exit 1
}
echo "$FLAT" | grep -q 'Bundle-SymbolicName' || {
  echo "FAIL: MANIFEST missing Bundle-SymbolicName"
  exit 1
}

# HC5 should be embedded as a direct dependency of the OSGi module.
if ! jar tf "$JAR" | grep -E 'httpclient5|lib/httpclient5' >/dev/null; then
  echo "FAIL: expected embedded httpclient5 jar under Bundle-ClassPath/lib"
  jar tf "$JAR" | head -50
  exit 1
fi

echo "OK: OSGi bundle MANIFEST checks passed for $JAR"
