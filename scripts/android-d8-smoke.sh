#!/usr/bin/env bash
# Convert the Android client JAR (+ StAX/Woodstox runtime deps) with D8.
# This is not an emulator test; it verifies DEX generation on API 26 bytecode.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

ANDROID_MOD="chemistry-opencmis-android/chemistry-opencmis-android-client"
R8_VERSION="${R8_VERSION:-8.7.18}"
MIN_API="${ANDROID_D8_MIN_API:-26}"
OUT_DIR="${TMPDIR:-/tmp}/opencmis-android-d8-$$"

cleanup() { rm -rf "$OUT_DIR"; }
trap cleanup EXIT

mkdir -p "$OUT_DIR"

echo "==> Package Android client"
mvn -pl "$ANDROID_MOD" -am package -DskipTests -q

FINAL_NAME=$(mvn -pl "$ANDROID_MOD" -q -DforceStdout help:evaluate -Dexpression=project.build.finalName)
JAR="$ANDROID_MOD/target/${FINAL_NAME}.jar"
if [[ ! -f "$JAR" ]]; then
  echo "FAIL: expected Android JAR not found: $JAR"
  ls -la "$ANDROID_MOD/target"/*.jar 2>/dev/null || true
  exit 1
fi
echo "Using JAR: $JAR"

echo "==> Resolve R8/D8 ($R8_VERSION) from Google Maven"
R8_JAR="$HOME/.m2/repository/com/android/tools/r8/${R8_VERSION}/r8-${R8_VERSION}.jar"
if [[ ! -f "$R8_JAR" ]]; then
  # R8 is published to Google Maven, not Maven Central.
  mvn -q org.apache.maven.plugins:maven-dependency-plugin:3.8.1:get \
    -DremoteRepositories="https://dl.google.com/dl/android/maven2/" \
    -Dartifact="com.android.tools:r8:${R8_VERSION}"
fi
if [[ ! -f "$R8_JAR" ]]; then
  mkdir -p "$(dirname "$R8_JAR")"
  curl -fsSL \
    "https://dl.google.com/dl/android/maven2/com/android/tools/r8/${R8_VERSION}/r8-${R8_VERSION}.jar" \
    -o "$R8_JAR"
fi
test -f "$R8_JAR"

# Collect Woodstox / StAX jars from the local reactor/maven repo via dependency:copy
mvn -pl "$ANDROID_MOD" -q org.apache.maven.plugins:maven-dependency-plugin:3.8.1:copy-dependencies \
  -DincludeArtifactIds=woodstox-core,stax-api,stax2-api \
  -DoutputDirectory="$OUT_DIR/libs"

echo "==> D8 convert (min-api ${MIN_API})"
mkdir -p "$OUT_DIR/dex"
java -cp "$R8_JAR" com.android.tools.r8.D8 \
  --release \
  --min-api "$MIN_API" \
  --output "$OUT_DIR/dex" \
  "$JAR" \
  "$OUT_DIR"/libs/*.jar

if ls "$OUT_DIR/dex"/*.dex >/dev/null 2>&1; then
  echo "OK: D8 produced DEX under $OUT_DIR/dex (min-api ${MIN_API}) from $JAR"
  ls -la "$OUT_DIR/dex"
else
  echo "FAIL: no classes*.dex produced in $OUT_DIR/dex"
  ls -la "$OUT_DIR/dex" || true
  exit 1
fi
