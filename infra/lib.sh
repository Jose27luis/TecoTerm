#!/usr/bin/env bash
set -euo pipefail

raiz="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
imagen_android="tecoterm-android"
opciones_docker=()

construir_imagen_android() {
    docker build --quiet --tag "$imagen_android" "$raiz/infra/android" > /dev/null
}

ejecutar() {
    docker run --rm \
        --volume "$raiz":/workspace \
        --volume tecoterm-gradle:/root/.gradle \
        --workdir /workspace \
        "${opciones_docker[@]}" \
        "$imagen_android" \
        "$@"
}
