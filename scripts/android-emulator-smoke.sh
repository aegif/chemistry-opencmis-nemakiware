#!/usr/bin/env bash
# Emulator-side smoke for the Android client DEX (API 26).
# Expects an already-running emulator (e.g. android-emulator-runner).
# Builds / reuses D8 output, pushes classes.dex, verifies SDK level + file on device.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

echo "==> Ensure D8 DEX exists"
./scripts/android-d8-smoke.sh

# android-d8-smoke cleans its TMPDIR on exit; rebuild DEX into a stable path.
ANDROID_MOD="chemistry-opencmis-android/chemistry-opencmis-android-client"
FINAL_NAME=$(mvn -pl "$ANDROID_MOD" -q -DforceStdout help:evaluate -Dexpression=project.build.finalName)
JAR="$ANDROID_MOD/target/${FINAL_NAME}.jar"
test -f "$JAR"

R8_VERSION="${R8_VERSION:-8.7.18}"
R8_JAR="$HOME/.m2/repository/com/android/tools/r8/${R8_VERSION}/r8-${R8_VERSION}.jar"
test -f "$R8_JAR"

OUT_DIR="${TMPDIR:-/tmp}/opencmis-android-emu-smoke"
rm -rf "$OUT_DIR"
mkdir -p "$OUT_DIR/libs" "$OUT_DIR/dex"

mvn -pl "$ANDROID_MOD" -q org.apache.maven.plugins:maven-dependency-plugin:3.8.1:copy-dependencies \
  -DincludeArtifactIds=woodstox-core,stax-api,stax2-api \
  -DoutputDirectory="$OUT_DIR/libs"

java -cp "$R8_JAR" com.android.tools.r8.D8 \
  --release \
  --min-api "${ANDROID_D8_MIN_API:-26}" \
  --output "$OUT_DIR/dex" \
  "$JAR" \
  "$OUT_DIR"/libs/*.jar

DEX=$(ls "$OUT_DIR/dex"/*.dex | head -1)
test -f "$DEX"
echo "DEX: $DEX"

echo "==> Wait for device"
adb wait-for-device
# Wait until boot completed
for i in $(seq 1 60); do
  BOOT=$(adb shell getprop sys.boot_completed 2>/dev/null | tr -d '\r' || true)
  if [[ "$BOOT" == "1" ]]; then
    break
  fi
  sleep 2
done

SDK=$(adb shell getprop ro.build.version.sdk | tr -d '\r')
echo "Device SDK: $SDK"
if [[ "$SDK" != "26" && "$SDK" != "${ANDROID_EMU_EXPECT_SDK:-26}" ]]; then
  echo "WARN: expected API ${ANDROID_EMU_EXPECT_SDK:-26}, got $SDK (continuing if device is usable)"
fi
# Require at least API 26 for our min-api DEX
if [[ "$SDK" -lt 26 ]]; then
  echo "FAIL: device API $SDK < 26"
  exit 1
fi

REMOTE_DIR=/data/local/tmp/opencmis-smoke
adb shell "rm -rf $REMOTE_DIR && mkdir -p $REMOTE_DIR"
adb push "$DEX" "$REMOTE_DIR/classes.dex"
adb shell "ls -la $REMOTE_DIR/classes.dex"
SIZE=$(adb shell "wc -c < $REMOTE_DIR/classes.dex" | tr -d '\r' | tr -d ' ')
if [[ -z "$SIZE" || "$SIZE" -lt 1000 ]]; then
  echo "FAIL: remote classes.dex missing or too small ($SIZE)"
  exit 1
fi

echo "OK: emulator API $SDK accepted OpenCMIS classes.dex ($SIZE bytes) at $REMOTE_DIR"
