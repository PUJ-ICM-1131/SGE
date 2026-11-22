import { HttpError } from '../domain/errors.js';

/**
 * Límite de peticiones por IP en ventana fija de 1 minuto (en memoria).
 * Suficiente para una sola instancia; con varias habría que moverlo a Redis.
 * La app consulta cada 3 s (20 por minuto por celular), así que 120/min deja margen.
 */
export function rateLimit({ limit, windowMs = 60_000, now = Date.now }) {
  const hits = new Map(); // ip -> { count, resetAt }

  const sweep = setInterval(() => {
    const t = now();
    for (const [ip, entry] of hits) if (entry.resetAt <= t) hits.delete(ip);
  }, windowMs);
  sweep.unref(); // no impide que el proceso termine

  return (req, res, next) => {
    const t = now();
    let entry = hits.get(req.ip);
    if (!entry || entry.resetAt <= t) {
      entry = { count: 0, resetAt: t + windowMs };
      hits.set(req.ip, entry);
    }
    entry.count++;

    const remaining = Math.max(0, limit - entry.count);
    res.set('RateLimit-Limit', String(limit));
    res.set('RateLimit-Remaining', String(remaining));

    if (entry.count > limit) {
      res.set('Retry-After', String(Math.ceil((entry.resetAt - t) / 1000)));
      return next(new HttpError(429, 'Demasiadas peticiones, intenta en un momento'));
    }
    next();
  };
}
