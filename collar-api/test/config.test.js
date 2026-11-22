import { test } from 'node:test';
import assert from 'node:assert/strict';
import { DEV_API_KEY, loadConfig } from '../src/config.js';

const STRONG_KEY = 'a'.repeat(64);

test('en desarrollo usa la clave de desarrollo y escucha en todas las interfaces', () => {
  const config = loadConfig({});
  assert.equal(config.apiKey, DEV_API_KEY);
  assert.equal(config.usingDevKey, true);
  assert.equal(config.host, '0.0.0.0');
  assert.equal(config.port, 8080);
});

test('en producción escucha solo en loopback (lo que espera Tailscale Funnel)', () => {
  const config = loadConfig({ NODE_ENV: 'production', COLLAR_API_KEY: STRONG_KEY });
  assert.equal(config.host, '127.0.0.1');
  assert.equal(config.trustProxy, 'loopback');
});

test('en producción no arranca sin clave, con la de desarrollo o con una corta', () => {
  assert.throws(() => loadConfig({ NODE_ENV: 'production' }), /COLLAR_API_KEY/);
  assert.throws(() => loadConfig({ NODE_ENV: 'production', COLLAR_API_KEY: DEV_API_KEY }), /COLLAR_API_KEY/);
  assert.throws(() => loadConfig({ NODE_ENV: 'production', COLLAR_API_KEY: 'corta' }), /COLLAR_API_KEY/);
});

test('rechaza puerto y límite inválidos', () => {
  assert.throws(() => loadConfig({ PORT: 'abc' }), /PORT/);
  assert.throws(() => loadConfig({ RATE_LIMIT_PER_MINUTE: '0' }), /RATE_LIMIT/);
});
