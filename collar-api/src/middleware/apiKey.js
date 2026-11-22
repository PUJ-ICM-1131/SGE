import { timingSafeEqual } from 'node:crypto';
import { unauthorized } from '../domain/errors.js';

/** Exige el header X-Api-Key. La comparación en tiempo constante evita adivinar la clave por tiempos. */
export function requireApiKey(apiKey) {
  const expected = Buffer.from(apiKey);
  return (req, _res, next) => {
    const given = Buffer.from(req.get('X-Api-Key') ?? '');
    if (given.length !== expected.length || !timingSafeEqual(given, expected)) {
      return next(unauthorized('API key inválida o ausente'));
    }
    next();
  };
}
