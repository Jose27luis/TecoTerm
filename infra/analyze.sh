#!/usr/bin/env bash
set -euo pipefail

source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"

construir_imagen_android

ejecutar bash -c '
    set -euo pipefail
    ktlint "**/*.kt" "**/*.kts" "!**/build/**"
    ./gradlew --no-daemon --quiet :detekt-rules:jar
    fuentes="$(find . -path "*/build" -prune -o -type d -name src -print | paste -sd, -)"
    java -jar /opt/detekt-cli.jar \
        --input "$fuentes" \
        --excludes "**/build/**" \
        --config config/detekt/detekt.yml \
        --build-upon-default-config \
        --plugins detekt-rules/build/libs/detekt-rules.jar
    ./gradlew --no-daemon --quiet lint
'
