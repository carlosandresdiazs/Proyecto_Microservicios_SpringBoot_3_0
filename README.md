# PROYECTO 03 — Despliegue EXCLUSIVO Kubernetes

Esta distribución **solo ejecuta la solución en Kubernetes**. No incluye Docker Compose como mecanismo alternativo. Docker se usa exclusivamente para construir imágenes OCI que ejecutan los Pods.

## Arquitectura
- API Gateway :8080 (único punto de entrada al cliente)
- auth-service :8081 — JWT RSA legado (firma privada); auth-db
- user-service :8082 — valida JWT RSA (pública); users-db
- product-service :8083 — valida JWT RSA (pública); products-db
- payment-service :8084 — Resource Server OAuth 2.0/OIDC con Keycloak; payments-db; webhooks HMAC sandbox
- Keycloak :8080 dentro del clúster (redirigido a localhost:8085 en el host)
- Namespace `proyecto-03`; PostgreSQL independiente con PVC para cada servicio.

## Requisitos
Docker Desktop con Kubernetes habilitado, Docker Engine, `kubectl`, Git Bash, `openssl`, acceso a imágenes y memoria disponible. Maven no es obligatorio en el host: se usa durante la construcción de las imágenes.

## Inicio rápido — Git Bash en Windows

```bash
kubectl config use-context docker-desktop
kubectl get nodes
bash scripts/generate-rsa-keys.sh
bash scripts/build-images.sh
bash scripts/k8s-setup.sh
```

`k8s-setup.sh` solicita contraseñas de laboratorio sin grabarlas en el repositorio y crea los recursos de forma ordenada. Si se vuelve a ejecutar tras un despliegue anterior, las contraseñas existentes de los PVC de PostgreSQL **deben mantenerse**: cambiar el Secret no actualiza la contraseña interna de una base ya inicializada.

### Comprobar estado

```bash
kubectl -n proyecto-03 get deployments,pods,svc,pvc
kubectl -n proyecto-03 rollout status deployment/api-gateway --timeout=300s
kubectl -n proyecto-03 logs deployment/payment-service --tail=100
```

### Acceso local (abrir DOS terminales Git Bash)

```bash
kubectl -n proyecto-03 port-forward svc/api-gateway 8080:8080
```

```bash
kubectl -n proyecto-03 port-forward svc/keycloak 8085:8080
```

Gateway: `http://localhost:8080/actuator/health` — consola Keycloak: `http://localhost:8085/`.

**OAuth 2.0 en laboratorio:** el issuer publicado por Keycloak es `http://localhost:8085/realms/proyecto03`. El `payment-service` valida esa cadena `iss`, pero obtiene las claves RSA desde la URL **interna** de JWKS `http://keycloak:8080/realms/proyecto03/protocol/openid-connect/certs`. Es válido solo para esta práctica con port-forward y no es una arquitectura de identidad para producción. Para un clúster remoto usar un dominio HTTPS estable accesible desde cliente y Pods, y actualizar **ambos** Keycloak y `OAUTH_ISSUER_URI`.

La aplicación antigua de JWT RSA y los access tokens OAuth 2.0 de Keycloak **no son intercambiables**; pagos exige tokens de Keycloak con `aud=payments-api` y scope adecuado. No se hacen cobros reales.

## Verificación y errores

```bash
kubectl -n proyecto-03 describe pod NOMBRE_POD
kubectl -n proyecto-03 logs deployment/auth-service --tail=80
kubectl -n proyecto-03 get endpoints
kubectl -n proyecto-03 get pvc
```

- `ImagePullBackOff`: construir imágenes en Docker Desktop o publicarlas en registro; las imágenes locales tienen `imagePullPolicy: IfNotPresent`.
- `CrashLoopBackOff`: revisar contraseñas, clave RSA, migraciones Flyway y DNS de PostgreSQL.
- `Pending` con PVC: comprobar StorageClass y almacenamiento del clúster.
- `401`: comprobar firmas, emisor, audiencia, vencimiento y scopes.
- `503`: comprobar `readinessProbe`, selector del Service y endpoints.

## Seguridad y límites

- No ingresar PAN/CVV ni usar tarjetas reales; pasarela de pagos es simulada.
- Keycloak se ejecuta con `start-dev` y almacenamiento local: **solo para laboratorio**, no producción.
- Las credenciales y claves privadas se crean localmente y están excluidas del ZIP. No las envíes a Git.
- Para producción: IdP con HTTPS, almacenamiento persistente soportado, Secret manager, cifrado de etcd, RBAC, NetworkPolicies, backups, observabilidad, políticas de imágenes y auditoría.
- No se han ejecutado compilaciones de Maven, pods ni pruebas de integración en esta sesión. Los YAML/JSON están sujetos a validación estática; el funcionamiento real requiere Docker Desktop y Kubernetes activos.
