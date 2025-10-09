#!/usr/bin/env sh
# Minimal gradlew placeholder that delegates to system gradle if present.
if command -v gradle >/dev/null 2>&1; then
  gradle "$@"
else
  echo "Gradle not found in PATH. Please install Gradle or generate a proper Gradle wrapper."
  exit 1
fi
