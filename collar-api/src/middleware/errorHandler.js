import { HttpError } from '../domain/errors.js';

export function notFoundHandler(req, _res, next) {
  next(new HttpError(404, `No existe ${req.method} ${req.path}`));
}

/** Todas las respuestas de error tienen la forma {"error": "...", "details"?: [...]}. */
export function errorHandler(log) {
  // eslint-disable-next-line no-unused-vars
  return (err, _req, res, _next) => {
    let status = err instanceof HttpError ? err.status : 500;
    let message = err.message;

    // Errores de express.json()
    if (err.type === 'entity.parse.failed') [status, message] = [400, 'El cuerpo no es JSON válido'];
    if (err.type === 'entity.too.large') [status, message] = [413, 'El cuerpo de la petición es demasiado grande'];

    if (status === 500) {
      log(`ERROR ${err.stack ?? err}`);
      message = 'Error interno';
    }
    res.status(status).json({ error: message, ...(err.details ? { details: err.details } : {}) });
  };
}
