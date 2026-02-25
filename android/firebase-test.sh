#!/bin/bash

set -euo pipefail

# Defaults
MINIFIED=false
TEST_CLASS=""

while [[ $# -gt 0 ]]; do
    case "$1" in
        --minified) MINIFIED=true; shift ;;
        --class) TEST_CLASS="$2"; shift 2 ;;
        *) echo "Usage: $0 [--minified] [--class com.thecityandthebike.SomeTest]"; exit 1 ;;
    esac
done

ARGS=(-f firebase-test-lab.yml)

if "$MINIFIED"; then
    ARGS+=(-f minified=true)
fi

if [[ -n "$TEST_CLASS" ]]; then
    ARGS+=(-f "test_class=$TEST_CLASS")
fi

echo "Triggering Firebase Test Lab workflow..."
gh workflow run "${ARGS[@]}"
echo "Workflow dispatched. Watch progress: gh run watch"
