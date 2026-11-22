import express from 'express';
import { CollarService } from './domain/collarService.js';
import { requireApiKey } from './middleware/apiKey.js';
import { errorHandler, notFoundHandler } from './middleware/errorHandler.js';
import { rateLimit } from './middleware/rateLimit.js';
import { collarRoutes, positionRoutes } from './routes/collarRoutes.js';

/**
 * Arma la aplicación con sus dependencias inyectadas (repositorio, reloj, clave),
 * para poder probarla sin archivos ni red.
 */
export function createApp({
  repository,
  apiKey,
  now = Date.now,
  log = () => {},
  trustProxy = 'loopback',
  rateLimitPerMinute = 120,
}) {
  if (!apiKey) throw new Error('apiKey es obligatoria');
  const service = new CollarService({ repository, now });
  const app = express();

  app.disable('x-powered-by');
  // Detrás de Tailscale Funnel la petición llega desde 127.0.0.1; la IP real viene en X-Forwarded-For.
  app.set('trust proxy', trustProxy);
  app.use((_req, res, next) => {
    res.set({
      'X-Content-Type-Options': 'nosniff',
      'Cache-Control': 'no-store',
      'Referrer-Policy': 'no-referrer',
    });
    next();
  });
  app.use(express.json({ limit: '64kb' }));
  app.use((req, res, next) => {
    const started = Date.now();
    res.on('finish', () =>
      log(`${req.ip} ${req.method} ${req.originalUrl} ${res.statusCode} ${Date.now() - started}ms`),
    );
    next();
  });

  app.get('/health', (_req, res) => res.json({ status: 'ok' }));

  const v1 = express.Router();
  // El límite va antes de la clave: también frena a quien intente adivinarla.
  v1.use(rateLimit({ limit: rateLimitPerMinute, now }));
  v1.use(requireApiKey(apiKey));
  v1.use('/collars', collarRoutes(service));
  v1.use('/positions', positionRoutes(service));
  app.use('/v1', v1);

  app.use(notFoundHandler);
  app.use(errorHandler(log));
  return app;
}
