#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
command -v kubectl >/dev/null || { echo "Falta kubectl"; exit 1; }
kubectl cluster-info >/dev/null || { echo "Cluster Kubernetes no disponible"; exit 1; }
CURRENT="$(kubectl config current-context)"
if [[ "$CURRENT" != "docker-desktop" && "${ALLOW_OTHER_K8S_CONTEXT:-}" != "yes" ]]; then
  echo "Contexto activo: $CURRENT. Este instalador está destinado a docker-desktop."
  echo "Para clúster externo, configure su registro de imágenes y ALLOW_OTHER_K8S_CONTEXT=yes."
  exit 1
fi
[[ -f secrets/rsa/private.pem && -f secrets/rsa/public.pem ]] || { echo "Ejecuta primero: bash scripts/generate-rsa-keys.sh"; exit 1; }
if command -v docker >/dev/null && [[ "$CURRENT" == "docker-desktop" ]]; then
  for module in api-gateway auth-service user-service product-service payment-service; do
    docker image inspect "proyecto03/$module:3.1" >/dev/null 2>&1 || {
      echo "Falta la imagen local proyecto03/$module:3.1. Ejecuta primero bash scripts/build-images.sh" >&2
      exit 1
    }
  done
fi
kubectl apply -f k8s/00-namespace.yaml
# Reutilización de Secrets, útil en despliegues con PVC ya inicializados
if ! kubectl -n proyecto-03 get secret app-secrets >/dev/null 2>&1; then
  echo "Creando Secrets para laboratorio; no se guardan en Git ni en archivos permanentes."
  read -r -s -p 'Contraseña de auth_db y users_db: ' AUTH_PASS; echo
  read -r -s -p 'Contraseña de products_db: ' PRODUCT_PASS; echo
  read -r -s -p 'Contraseña de payments_db: ' PAYMENT_PASS; echo
  read -r -s -p 'Secreto HMAC de webhook (> 32 caracteres): ' WEBHOOK_PASS; echo
  read -r -s -p 'Contraseña de administrador Keycloak: ' KC_PASS; echo
  [[ ${#AUTH_PASS} -ge 12 && ${#PRODUCT_PASS} -ge 12 && ${#PAYMENT_PASS} -ge 12 && ${#WEBHOOK_PASS} -ge 32 && ${#KC_PASS} -ge 12 ]] || { echo 'Contraseñas demasiado cortas'; exit 1; }
  TMP="$(mktemp)"; chmod 600 "$TMP"
  trap 'rm -f "$TMP"' EXIT
  printf 'AUTH_DB_PASSWORD=%s\nUSERS_DB_PASSWORD=%s\nPRODUCT_DB_PASSWORD=%s\nPAYMENTS_DB_PASSWORD=%s\nWEBHOOK_SECRET=%s\nKEYCLOAK_ADMIN_PASSWORD=%s\n' "$AUTH_PASS" "$AUTH_PASS" "$PRODUCT_PASS" "$PAYMENT_PASS" "$WEBHOOK_PASS" "$KC_PASS" > "$TMP"
  kubectl -n proyecto-03 create secret generic app-secrets --from-env-file="$TMP"
  rm -f "$TMP"; trap - EXIT
  unset AUTH_PASS PRODUCT_PASS PAYMENT_PASS WEBHOOK_PASS KC_PASS
else
  echo 'Secret app-secrets existente: se reutiliza para no invalidar contraseñas de PostgreSQL.'
fi
if ! kubectl -n proyecto-03 get secret rsa-keys >/dev/null 2>&1; then
  kubectl -n proyecto-03 create secret generic rsa-keys --from-file=secrets/rsa/private.pem --from-file=secrets/rsa/public.pem
fi
# El cliente confidencial de Keycloak recibe un secreto aleatorio por instalación.
# Nunca incluir el secreto en Git ni en ConfigMap (que no es confidencial).
if ! kubectl -n proyecto-03 get secret keycloak-realm >/dev/null 2>&1; then
  command -v openssl >/dev/null || { echo 'Falta openssl para generar el secreto OAuth'; exit 1; }
  TEMP_REALM="$(mktemp)"; chmod 600 "$TEMP_REALM"
  trap 'rm -f "$TEMP_REALM"' EXIT
  CLIENT_SECRET="$(openssl rand -hex 32)"
  sed "s/GENERATE_AT_DEPLOYMENT/$CLIENT_SECRET/g" keycloak/proyecto03-realm.json > "$TEMP_REALM"
  kubectl -n proyecto-03 create secret generic keycloak-realm --from-file=proyecto03-realm.json="$TEMP_REALM"
  rm -f "$TEMP_REALM"; trap - EXIT
  unset CLIENT_SECRET
  echo 'Realm Keycloak generado en Kubernetes Secret; consultar credenciales desde el administrador Keycloak si se requiere.'
else
  echo 'Se conserva el Secret keycloak-realm existente.'
fi
kubectl apply -f k8s/01-configmap.yaml
kubectl apply -f k8s/02-databases.yaml
kubectl apply -f k8s/04-keycloak.yaml
kubectl apply -f k8s/03-applications.yaml
kubectl -n proyecto-03 get deployments,pods,svc,pvc
echo 'Abrir terminales: kubectl -n proyecto-03 port-forward svc/api-gateway 8080:8080'
echo '                  kubectl -n proyecto-03 port-forward svc/keycloak 8085:8080'
