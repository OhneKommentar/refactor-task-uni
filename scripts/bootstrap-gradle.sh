#!/usr/bin/env bash
set -euo pipefail

dir="$(cd "$(dirname "$0")/.." && pwd)"
BOOTSTRAP_DIR="$dir/.gradle/bootstrap"
GRADLE_VERSION="8.9"
GRADLE_DIST="gradle-${GRADLE_VERSION}-bin.zip"
GRADLE_URL="https://services.gradle.org/distributions/${GRADLE_DIST}"

mkdir -p "$BOOTSTRAP_DIR"
ZIP_PATH="$BOOTSTRAP_DIR/$GRADLE_DIST"
if [ ! -f "$ZIP_PATH" ]; then
  echo "Downloading Gradle ${GRADLE_VERSION}..."
  curl -fSL "$GRADLE_URL" -o "$ZIP_PATH"
fi

UNZIP_DIR="$BOOTSTRAP_DIR/gradle-${GRADLE_VERSION}"
if [ ! -d "$UNZIP_DIR" ]; then
  echo "Unpacking Gradle..."
  unzip -q "$ZIP_PATH" -d "$BOOTSTRAP_DIR"
fi

cd "$dir/backend"
"$UNZIP_DIR/bin/gradle" wrapper --gradle-version "$GRADLE_VERSION"

echo "Gradle wrapper generated. You can now run ./gradlew tasks."
