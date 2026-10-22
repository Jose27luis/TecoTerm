#!/bin/sh
set -eu

: "${TECOTERM_SSHD_PASSWORD:?Falta TECOTERM_SSHD_PASSWORD}"

ssh-keygen -A > /dev/null

if ! id teco > /dev/null 2>&1; then
    adduser -D -s /bin/bash teco
fi
echo "teco:${TECOTERM_SSHD_PASSWORD}" | chpasswd > /dev/null
install -d -m 700 -o teco -g teco /home/teco/.ssh

exec /usr/sbin/sshd.pam -D -e
