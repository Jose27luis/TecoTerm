#!/usr/bin/env bash
set -euo pipefail

source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"

if [[ ! -f "$raiz/secrets/keystore.properties" ]]; then
    echo "Falta secrets/keystore.properties con storeFile, storePassword, keyAlias y keyPassword." >&2
    exit 1
fi

construir_imagen_android
ejecutar ./gradlew --no-daemon :app:assembleRelease

echo "$raiz/app/build/outputs/apk/release/app-release.apk"
