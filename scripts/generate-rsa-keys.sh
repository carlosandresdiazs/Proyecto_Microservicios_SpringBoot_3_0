#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p secrets/rsa
if [[ -f secrets/rsa/private.pem || -f secrets/rsa/public.pem ]]; then
  if [[ -f secrets/rsa/private.pem && -f secrets/rsa/public.pem ]]; then
    echo 'Las claves RSA existentes se conservan. No se sobrescriben automáticamente.'
    exit 0
  fi
  echo 'ERROR: hay una sola clave RSA; revisa secrets/rsa antes de continuar.' >&2
  exit 1
fi
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:3072 -out secrets/rsa/private.pem
openssl rsa -pubout -in secrets/rsa/private.pem -out secrets/rsa/public.pem
chmod 600 secrets/rsa/private.pem
chmod 644 secrets/rsa/public.pem
printf 'Claves RSA generadas; nunca subir private.pem a Git.\n'
