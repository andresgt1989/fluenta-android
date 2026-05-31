#!/bin/bash
# Build APK on VPS and deploy to download endpoint.
# Runs automatically after every code change.
# Founder just opens: https://fluenta.alturya.com/download/app?t=<TOKEN>

set -e

export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
export ANDROID_HOME=/opt/android-sdk
export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools

cd /opt/fluenta-android

echo "[$(date '+%H:%M:%S')] Building APK..."
./gradlew assembleDebug --no-daemon --quiet

APK_SRC="app/build/outputs/apk/debug/app-debug.apk"
APK_DEST="/opt/fluenta-apk/app-debug.apk"

mkdir -p /opt/fluenta-apk
cp "$APK_SRC" "$APK_DEST"

SIZE=$(du -h "$APK_DEST" | cut -f1)
echo "[$(date '+%H:%M:%S')] ✅ APK built ($SIZE) → $APK_DEST"
echo "Download: https://fluenta.alturya.com/download/app"
