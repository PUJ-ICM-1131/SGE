import { test, beforeEach, afterEach } from 'node:test';
import assert from 'node:assert/strict';
import { createApp } from '../src/app.js';
import { InMemoryCollarRepository } from '../src/repository/inMemoryCollarRepository.js';

const API_KEY = 'test-key';
const NOW = 1_795_000_000_000;
let server;
let baseUrl;

beforeEach(async () => {
  const app = createApp({ repository: new InMemoryCollarRepository(), apiKey: API_KEY, now: () => NOW });
  server = app.listen(0, '127.0.0.1');
  await new Promise((resolve) => server.once('listening', resolve));
  baseUrl = `http://127.0.0.1:${server.address().port}`;
});

afterEach(() => new Promise((resolve) => server.close(resolve)));

async function call(method, path, body, { key = API_KEY } = {}) {
  const res = await fetch(`${baseUrl}${path}`, {
    method,
    headers: {
      ...(body !== undefined ? { 'Content-Type': 'application/json' } : {}),
      ...(key ? { 'X-Api-Key': key } : {}),
    },
    body: body === undefined ? undefined : typeof body === 'string' ? body : JSON.stringify(body),
  });
  const text = await res.text();
  return { status: res.status, headers: res.headers, body: text ? JSON.parse(text) : null };
}

const newCollar = (overrides = {}) => ({
  serial: 'sge-0001',
  farmId: 'finca-1',
  animalId: 'animal-1',
  animalName: 'Lola',
  anchor: { lat: 4.628, lng: -74.065 },
  ...overrides,
});

test('GET /health no pide API key', async () => {
  const res = await call('GET', '/health', undefined, { key: null });
  assert.equal(res.status, 200);
  assert.deepEqual(res.body, { status: 'ok' });
});

test('todas las rutas /v1 piden API key', async () => {
  assert.equal((await call('GET', '/v1/collars', undefined, { key: null })).status, 401);
  assert.equal((await call('GET', '/v1/positions', undefined, { key: 'otra' })).status, 401);
});

test('CRUD completo de un collar', async () => {
  // Crear
  const created = await call('POST', '/v1/collars', newCollar());
  assert.equal(created.status, 201);
  const { id } = created.body;
  assert.match(id, /^[0-9a-f-]{36}$/);
  assert.equal(created.headers.get('location'), `/v1/collars/${id}`);
  assert.equal(created.body.serial, 'SGE-0001'); // se normaliza a mayúsculas
  assert.equal(created.body.radiusMeters, 40); // valor por defecto
  assert.equal(created.body.status, 'ACTIVE');

  // Leer
  const read = await call('GET', `/v1/collars/${id}`);
  assert.equal(read.status, 200);
  assert.deepEqual(read.body, created.body);

  // Listar
  const list = await call('GET', '/v1/collars?farmId=finca-1');
  assert.equal(list.body.total, 1);

  // Actualizar parcialmente
  const patched = await call('PATCH', `/v1/collars/${id}`, { radiusMeters: 80, animalName: 'Lolita' });
  assert.equal(patched.status, 200);
  assert.equal(patched.body.radiusMeters, 80);
  assert.equal(patched.body.animalName, 'Lolita');
  assert.equal(patched.body.serial, 'SGE-0001');

  // Eliminar
  assert.equal((await call('DELETE', `/v1/collars/${id}`)).status, 204);
  assert.equal((await call('GET', `/v1/collars/${id}`)).status, 404);
  assert.equal((await call('DELETE', `/v1/collars/${id}`)).status, 404);
});

test('valida los datos y explica qué está mal', async () => {
  const missing = await call('POST', '/v1/collars', { serial: 'A1' });
  assert.equal(missing.status, 400);
  assert.match(missing.body.error, /farmId, anchor/);

  const bad = await call('POST', '/v1/collars', newCollar({ anchor: { lat: 200, lng: 0 }, radiusMeters: 5 }));
  assert.equal(bad.status, 400);
  assert.equal(bad.body.details.length, 2);

  const unknown = await call('POST', '/v1/collars', newCollar({ hacker: true }));
  assert.equal(unknown.status, 400);
  assert.match(unknown.body.error, /hacker/);

  assert.equal((await call('POST', '/v1/collars', '{no es json')).status, 400);
});

test('serial único y un solo collar por animal', async () => {
  await call('POST', '/v1/collars', newCollar());
  const sameSerial = await call('POST', '/v1/collars', newCollar({ serial: 'SGE-0001', animalId: 'otro' }));
  assert.equal(sameSerial.status, 409);
  const sameAnimal = await call('POST', '/v1/collars', newCollar({ serial: 'SGE-0002' }));
  assert.equal(sameAnimal.status, 409);
  assert.match(sameAnimal.body.error, /animal-1/);
});

test('posición de un collar y de toda la finca', async () => {
  const a = (await call('POST', '/v1/collars', newCollar())).body;
  const b = (await call('POST', '/v1/collars', newCollar({ serial: 'SGE-0002', animalId: 'animal-2' }))).body;
  await call('POST', '/v1/collars', newCollar({ serial: 'SGE-0003', animalId: 'animal-3', farmId: 'otra-finca' }));

  const one = await call('GET', `/v1/collars/${a.id}/position`);
  assert.equal(one.status, 200);
  assert.equal(one.body.animalId, 'animal-1');
  assert.equal(one.body.timestamp, NOW);

  const farm = await call('GET', '/v1/positions?farmId=finca-1');
  assert.equal(farm.body.generatedAt, NOW);
  assert.deepEqual(farm.body.positions.map((p) => p.animalId).sort(), ['animal-1', 'animal-2']);

  const byAnimals = await call('GET', '/v1/positions?animalIds=animal-2,animal-3');
  assert.deepEqual(byAnimals.body.positions.map((p) => p.animalId).sort(), ['animal-2', 'animal-3']);

  // Un collar inactivo deja de reportar
  await call('PATCH', `/v1/collars/${b.id}`, { status: 'INACTIVE' });
  assert.equal((await call('GET', `/v1/collars/${b.id}/position`)).status, 409);
  const afterDeactivate = await call('GET', '/v1/positions?farmId=finca-1');
  assert.deepEqual(afterDeactivate.body.positions.map((p) => p.animalId), ['animal-1']);
});

test('rutas desconocidas responden 404 en JSON', async () => {
  const res = await call('GET', '/v1/nada');
  assert.equal(res.status, 404);
  assert.ok(res.body.error);
});
