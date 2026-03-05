#!/usr/bin/env bash
set -euo pipefail

# Generate local.properties for Android SDK path
echo "sdk.dir=$ANDROID_HOME" > android/local.properties
