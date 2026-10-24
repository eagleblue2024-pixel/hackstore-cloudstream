#!/usr/bin/env bash
set -e

GRADLE_HOME="$HOME/.gradle-8.5-bin"
if [ ! -f "$GRADLE_HOME/bin/gradle" ]; then
  echo "==> Setting up Gradle 8.5 for CloudStream plugin build..."
  mkdir -p "$GRADLE_HOME"
  curl -sSL "https://services.gradle.org/distributions/gradle-8.5-bin.zip" -o /tmp/gradle-8.5-bin.zip
  unzip -q -o /tmp/gradle-8.5-bin.zip -d /tmp/gradle-extract
  cp -r /tmp/gradle-extract/gradle-8.5/* "$GRADLE_HOME/"
  chmod +x "$GRADLE_HOME/bin/gradle"
  echo "==> Gradle 8.5 installed successfully."
fi

exec "$GRADLE_HOME/bin/gradle" "$@"
