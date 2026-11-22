import { test, before, after } from 'node:test';
import assert from 'node:assert/strict';
import { createApp } from '../src/app.js';
import { InMemoryCollarRepository } from '../src/repository/inMemoryCollarRepository.js';

let server;
let baseUrl;

before(async () => {
  const app = createApp({
    repository: new InMemoryCollarRepository(),
    apiKey: 'k',
    rateLimitPerMinute: 3,
  });
  server = app.listen(0, '127.0.0.1');
  await new Promise((resolve) => server.once('listening', resolve));
  baseUrl = `http://127.0.0.1:${server.address().port}`;
});

after(() => new Promise((resolve) => server.close(resolve)));

// Simula clientes distintos llegando a través de Funnel (proxy en 127.0.0.1 con X-Forwarded-For).
const getAs = (clientIp, key = 'k') =>
  fetch(`${baseUrl}/v1/collars`, { headers: { 'X-Api-Key': key, 'X-Forwarded-For': clientIp } });

test('limita peticiones por IP real del cliente y avisa cuándo reintentar', async () => {
  for (let i = 0; i < 3; i++) assert.equal((await getAs('203.0.113.1')).status, 200);
  const blocked = await getAs('203.0.113.1');
  assert.equal(blocked.status, 429);
  assert.ok(Number(blocked.headers.get('retry-after')) > 0);

  // Otro cliente no se ve afectado
  assert.equal((await getAs('203.0.113.2')).status, 200);
});

test('el límite también frena intentos de adivinar la clave', async () => {
  for (let i = 0; i < 3; i++) assert.equal((await getAs('203.0.113.9', 'mala')).status, 401);
  assert.equal((await getAs('203.0.113.9', 'mala')).status, 429);
});

test('agrega cabeceras de seguridad y oculta Express', async () => {
  const res = await fetch(`${baseUrl}/health`);
  assert.equal(res.headers.get('x-content-type-options'), 'nosniff');
  assert.equal(res.headers.get('cache-control'), 'no-store');
  assert.equal(res.headers.get('x-powered-by'), null);
});
