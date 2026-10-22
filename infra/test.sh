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

contrasena="$(head -c 24 /dev/urandom | od -An -tx1 | tr -d ' \n')"

docker network create "$red" > /dev/null
docker run --detach --name "$servidor" --network "$red" --env TECOTERM_SSHD_PASSWORD="$contrasena" tecoterm-sshd > /dev/null

for _ in $(seq 1 30); do
    if docker exec "$servidor" nc -z 127.0.0.1 22 2> /dev/null; then
        break
    fi
    sleep 1
done
docker exec "$servidor" nc -z 127.0.0.1 22

clave_host() {
    docker exec "$servidor" cat "/etc/ssh/ssh_host_$1_key.pub"
}

opciones_docker=(
    --network "$red"
    --env TECOTERM_SSHD_HOST="$servidor"
    --env TECOTERM_SSHD_PORT=22
    --env TECOTERM_SSHD_USER=teco
    --env TECOTERM_SSHD_PASSWORD="$contrasena"
    --env TECOTERM_SSHD_HOST_KEY_ED25519="$(clave_host ed25519)"
    --env TECOTERM_SSHD_HOST_KEY_RSA="$(clave_host rsa)"
    --env TECOTERM_SSHD_HOST_KEY_ECDSA="$(clave_host ecdsa)"
)
ejecutar ./gradlew --no-daemon test "$@"
