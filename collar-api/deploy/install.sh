#!/usr/bin/env bash
# Instala o actualiza SGE Collar API en un LXC Debian/Ubuntu de Proxmox.
# Uso (como root, desde la carpeta del repo):  bash deploy/install.sh
# Es idempotente: volver a ejecutarlo actualiza el código sin tocar la clave ni los datos.
set -euo pipefail

APP_DIR=/opt/sge-collar-api
ENV_DIR=/etc/sge-collar-api
ENV_FILE="$ENV_DIR/env"
SERVICE=sge-collar-api
REPO_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

if [[ $EUID -ne 0 ]]; then
  echo "Ejecuta como root: sudo bash deploy/install.sh" >&2
  exit 1
fi

node_major() { node -v 2>/dev/null | sed -E 's/^v([0-9]+).*/\1/' || echo 0; }

# 1. Node 20 o superior (Debian 13 lo trae en sus repositorios)
if [[ "$(node_major)" -lt 20 ]]; then
  echo "==> Instalando Node.js y npm"
  apt-get update -qq
  apt-get install -y -qq nodejs npm openssl curl
fi
if [[ "$(node_major)" -lt 20 ]]; then
  echo "Se necesita Node 20+, y el sistema tiene $(node -v). Usa una plantilla Debian 13 o instala Node desde nodejs.org." >&2
  exit 1
fi
command -v openssl >/dev/null || apt-get install -y -qq openssl
command -v curl >/dev/null || apt-get install -y -qq curl

# 2. Código: se copia solo lo necesario para ejecutar
echo "==> Copiando la aplicación a $APP_DIR"
mkdir -p "$APP_DIR"
rm -rf "$APP_DIR/src"
cp -r "$REPO_DIR/src" "$REPO_DIR/package.json" "$REPO_DIR/package-lock.json" "$APP_DIR/"
(cd "$APP_DIR" && npm ci --omit=dev --no-fund --no-audit --loglevel=error)

# 3. Configuración: la clave se genera una sola vez
if [[ ! -f "$ENV_FILE" ]]; then
  echo "==> Generando $ENV_FILE con una clave nueva"
  mkdir -p "$ENV_DIR"
  umask 077
  cat >"$ENV_FILE" <<EOF
COLLAR_API_KEY=$(openssl rand -hex 32)
PORT=8080
RATE_LIMIT_PER_MINUTE=120
EOF
  chmod 600 "$ENV_FILE"
  NEW_KEY=1
fi

# 4. Servicio systemd
echo "==> Instalando el servicio $SERVICE"
install -m 644 "$REPO_DIR/deploy/$SERVICE.service" "/etc/systemd/system/$SERVICE.service"
systemctl daemon-reload
systemctl enable "$SERVICE" >/dev/null
systemctl restart "$SERVICE"

# 5. Verificación
PORT=$(grep -E '^PORT=' "$ENV_FILE" | cut -d= -f2)
PORT=${PORT:-8080}
for _ in $(seq 1 10); do
  if curl -fsS "http://127.0.0.1:$PORT/health" >/dev/null 2>&1; then
    echo "==> Servicio arriba en http://127.0.0.1:$PORT"
    break
  fi
  sleep 1
done
if ! curl -fsS "http://127.0.0.1:$PORT/health" >/dev/null 2>&1; then
  echo "El servicio no respondió. Revisa: journalctl -u $SERVICE -n 50" >&2
  exit 1
fi

if [[ "${NEW_KEY:-0}" == 1 ]]; then
  echo
  echo "API key (cópiala en local.properties de SGE como COLLAR_API_KEY):"
  grep -E '^COLLAR_API_KEY=' "$ENV_FILE" | cut -d= -f2
fi

cat <<EOF

Siguiente paso, publicarla con Tailscale Funnel (una sola vez):
  tailscale funnel --bg $PORT
  tailscale funnel status          # muestra la URL https://<maquina>.<tailnet>.ts.net
EOF
