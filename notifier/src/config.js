import { existsSync } from 'node:fs';

/**
 * Configuración desde variables de entorno, validada al arrancar.
 * La credencial de Firebase (cuenta de servicio) llega por GOOGLE_APPLICATION_CREDENTIALS:
 * en el LXC la entrega systemd con LoadCredential, nunca vive en el repositorio.
 */
export function loadConfig(env = process.env) {
  const credentials = env.GOOGLE_APPLICATION_CREDENTIALS;
  if (!credentials || !existsSync(credentials)) {
    throw new Error(
      'Falta la cuenta de servicio de Firebase. Define GOOGLE_APPLICATION_CREDENTIALS con la ruta al JSON ' +
        '(Firebase Console → Configuración del proyecto → Cuentas de servicio → Generar nueva clave privada).',
    );
  }

  const maxAgeMinutes = Number(env.MAX_AGE_MINUTES ?? 30);
  if (!Number.isFinite(maxAgeMinutes) || maxAgeMinutes <= 0) {
    throw new Error(`MAX_AGE_MINUTES inválido: ${env.MAX_AGE_MINUTES}`);
  }

  const healthPort = Number(env.HEALTH_PORT ?? 8081);
  if (!Number.isInteger(healthPort) || healthPort < 1 || healthPort > 65535) {
    throw new Error(`HEALTH_PORT inválido: ${env.HEALTH_PORT}`);
  }

  return {
    credentials,
    // Solo para la demo con un celular: también avisa a quien registró la transacción.
    notifyAuthor: env.NOTIFY_AUTHOR === 'true',
    maxAgeMinutes,
    // /health solo en loopback: es para systemctl/curl dentro del LXC, no se publica con Funnel.
    healthHost: '127.0.0.1',
    healthPort,
  };
}
