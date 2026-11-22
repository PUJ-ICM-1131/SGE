#!/usr/bin/env bash
# Instala o actualiza SGE Notifier en el mismo LXC de Proxmox que collar-api.
# Uso (como root, desde la carpeta notifier del repo):  bash deploy/install.sh
# Es idempotente: volver a ejecutarlo actualiza el código sin tocar la configuración.
set -euo pipefail

APP_DIR=/opt/sge-notifier
ETC_DIR=/etc/sge-notifier
ENV_FILE="$ETC_DIR/env"
CREDENTIAL="$ETC_DIR/service-account.json"
SERVICE=sge-notifier
REPO_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

if [[ $EUID -ne 0 ]]; then
  echo "Ejecuta como root: sudo bash deploy/install.sh" >&2
  exit 1
fi

node_major() { node -v 2>/dev/null | sed -E 's/^v([0-9]+).*/\1/' || echo 0; }

# 1. Node 20+ (si collar-api ya está instalado, ya lo tienes)
if [[ "$(node_major)" -lt 20 ]]; then
  echo "==> Instalando Node.js y npm"
  apt-get update -qq
  apt-get install -y -qq nodejs npm curl
fi
if [[ "$(node_major)" -lt 20 ]]; then
  echo "Se necesita Node 20+, y el sistema tiene $(node -v). Usa Debian 13 o instala Node desde nodejs.org." >&2
  exit 1
fi
command -v curl >/dev/null || apt-get install -y -qq curl

# 2. La cuenta de servicio debe estar antes de arrancar
mkdir -p "$ETC_DIR"
if [[ ! -f "$CREDENTIAL" ]]; then
  cat >&2 <<EOF
Falta la cuenta de servicio de Firebase en $CREDENTIAL

  1. Firebase Console → ⚙ Configuración del proyecto → Cuentas de servicio
     → "Generar nueva clave privada" (descarga un .json).
  2. Cópiala al contenedor, por ejemplo desde tu PC:
       scp icm-sge-firebase-adminsdk-xxxx.json root@sge-collar-api:$CREDENTIAL
  3. Vuelve a ejecutar: bash deploy/install.sh

Ese archivo da acceso de administrador a todo Firebase: nunca lo subas a GitHub.
EOF
  exit 1
fi
chown root:root "$CREDENTIAL"
chmod 600 "$CREDENTIAL"

# 3. Código: solo lo necesario para ejecutar
echo "==> Copiando la aplicación a $APP_DIR"
mkdir -p "$APP_DIR"
rm -rf "$APP_DIR/src"
cp -r "$REPO_DIR/src" "$REPO_DIR/package.json" "$REPO_DIR/package-lock.json" "$APP_DIR/"
(cd "$APP_DIR" && npm ci --omit=dev --no-fund --no-audit --loglevel=error)

# 4. Configuración (se crea una sola vez; edítala a mano después)
if [[ ! -f "$ENV_FILE" ]]; then
  echo "==> Creando $ENV_FILE"
  cat >"$ENV_FILE" <<EOF
# true = también avisa a quien registró la transacción (demo con un solo celular)
NOTIFY_AUTHOR=false
# Transacciones más viejas que esto (minutos) no se avisan
MAX_AGE_MINUTES=30
# /health solo en 127.0.0.1 (collar-api usa el 8080)
HEALTH_PORT=8081
EOF
  chmod 644 "$ENV_FILE"
fi

# 5. Servicio systemd
echo "==> Instalando el servicio $SERVICE"
install -m 644 "$REPO_DIR/deploy/$SERVICE.service" "/etc/systemd/system/$SERVICE.service"
systemctl daemon-reload
systemctl enable "$SERVICE" >/dev/null
systemctl restart "$SERVICE"

# 6. Verificación: /health responde 200 cuando el listener de Firestore está activo
PORT=$(grep -E '^HEALTH_PORT=' "$ENV_FILE" | cut -d= -f2)
PORT=${PORT:-8081}
for _ in $(seq 1 20); do
  if curl -fsS "http://127.0.0.1:$PORT/health" >/dev/null 2>&1; then
    echo "==> Servicio escuchando transacciones nuevas"
    curl -fsS "http://127.0.0.1:$PORT/health"; echo
    exit 0
  fi
  sleep 1
done

cat >&2 <<EOF
El servicio arrancó pero aún no escucha Firestore. Revisa el log:
  journalctl -u $SERVICE -n 30

Si dice FAILED_PRECONDITION, falta el índice: el mensaje trae un enlace para crearlo
(o ver "Índice de Firestore" en el README). Tras crearlo, espera 1–2 min: el servicio reintenta solo.
EOF
exit 1
