#!/bin/sh
set -eu
# Usage: ./scripts/create-certs.sh 192.168.1.45 [computer.local]
IP=${1:-}
DOMAIN=${2:-}
if [ -z "$IP" ]; then echo "Использование: $0 <LAN-IP> [имя.local]" >&2; exit 1; fi
case "$IP" in *[!0-9.]*|'') echo 'Укажите IPv4-адрес локальной сети.' >&2; exit 1;; esac
if [ -n "$DOMAIN" ]; then case "$DOMAIN" in *[!A-Za-z0-9.-]*) echo 'Недопустимое имя домена.' >&2; exit 1;; esac; fi
mkdir -p certs
if [ -e certs/ca.key ] || [ -e certs/ca.crt ] || [ -e certs/server.key ] || [ -e certs/server.crt ]; then echo 'Файлы сертификатов уже существуют. Удалите или сохраните каталог certs вручную перед повторным запуском.' >&2; exit 1; fi
umask 077
openssl genrsa -out certs/ca.key 4096
openssl req -x509 -new -key certs/ca.key -sha256 -days 3650 -out certs/ca.crt -subj '/CN=Mesto local development CA' -addext 'basicConstraints=critical,CA:TRUE' -addext 'keyUsage=critical,keyCertSign,cRLSign'
openssl genrsa -out certs/server.key 2048
openssl req -new -key certs/server.key -out certs/server.csr -subj '/CN=Mesto local server'
SAN="IP:$IP,IP:127.0.0.1,DNS:localhost"
[ -z "$DOMAIN" ] || SAN="$SAN,DNS:$DOMAIN"
printf 'subjectAltName=%s\nbasicConstraints=critical,CA:FALSE\nkeyUsage=critical,digitalSignature,keyEncipherment\nextendedKeyUsage=serverAuth\n' "$SAN" > certs/server.ext
openssl x509 -req -in certs/server.csr -CA certs/ca.crt -CAkey certs/ca.key -CAcreateserial -out certs/server.crt -days 825 -sha256 -extfile certs/server.ext
rm certs/server.csr certs/server.ext certs/ca.srl
echo 'Готово. Установите certs/ca.crt на устройство, приватные ключи никому не передавайте.'
