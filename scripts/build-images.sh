#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
for module in api-gateway auth-service user-service product-service payment-service; do
 docker build -f "$module/Dockerfile" -t "proyecto03/$module:3.1" .
done
