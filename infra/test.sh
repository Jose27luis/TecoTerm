#!/usr/bin/env bash
set -euo pipefail

source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"

red="tecoterm-test-$$"
servidor="$red-sshd"

limpiar() {
    docker rm --force "$servidor" > /dev/null 2>&1 || true
    docker network rm "$red" > /dev/null 2>&1 || true
}
trap limpiar EXIT

construir_imagen_android
docker build --quiet --tag tecoterm-sshd "$raiz/infra/sshd-test" > /dev/null

docker network create "$red" > /dev/null
docker run --detach --name "$servidor" --network "$red" tecoterm-sshd > /dev/null

for _ in $(seq 1 30); do
    if docker exec "$servidor" nc -z 127.0.0.1 22 2> /dev/null; then
        break
    fi
    sleep 1
done
docker exec "$servidor" nc -z 127.0.0.1 22

opciones_docker=(--network "$red" --env TECOTERM_SSHD_HOST="$servidor" --env TECOTERM_SSHD_PORT=22)
ejecutar ./gradlew --no-daemon test
