export const DEV_API_KEY = 'sge-dev-key';
const MIN_PRODUCTION_KEY_LENGTH = 32;

/**
 * Lee y valida la configuración desde variables de entorno.
 * En producción (NODE_ENV=production) se niega a arrancar con una clave débil o la de desarrollo,
 * porque con Tailscale Funnel la API queda abierta a internet.
 */
export function loadConfig(env = process.env) {
  const production = env.NODE_ENV === 'production';
  const apiKey = env.COLLAR_API_KEY ?? (production ? undefined : DEV_API_KEY);

  if (production) {
    if (!apiKey || apiKey === DEV_API_KEY || apiKey.length < MIN_PRODUCTION_KEY_LENGTH) {
      throw new Error(
        `En producción COLLAR_API_KEY debe tener al menos ${MIN_PRODUCTION_KEY_LENGTH} caracteres ` +
          'y no puede ser la clave de desarrollo. Genera una con: openssl rand -hex 32',
      );
    }
  }

  const port = Number(env.PORT ?? 8080);
  if (!Number.isInteger(port) || port < 1 || port > 65535) throw new Error(`PORT inválido: ${env.PORT}`);

  const rateLimitPerMinute = Number(env.RATE_LIMIT_PER_MINUTE ?? 120);
  if (!Number.isInteger(rateLimitPerMinute) || rateLimitPerMinute < 1) {
    throw new Error(`RATE_LIMIT_PER_MINUTE inválido: ${env.RATE_LIMIT_PER_MINUTE}`);
  }

  return {
    production,
    apiKey,
    usingDevKey: apiKey === DEV_API_KEY,
    port,
    // Detrás de Funnel debe ser 127.0.0.1: Tailscale solo hace proxy a loopback y así
    // la API no queda expuesta en la red local. En desarrollo 0.0.0.0 permite probar desde un celular.
    host: env.HOST ?? (production ? '127.0.0.1' : '0.0.0.0'),
    dataFile: env.DATA_FILE ?? 'data/collars.json',
    // Funnel reenvía desde 127.0.0.1 con X-Forwarded-For: confiar solo en loopback para leer la IP real.
    trustProxy: env.TRUST_PROXY ?? 'loopback',
    rateLimitPerMinute,
  };
}
