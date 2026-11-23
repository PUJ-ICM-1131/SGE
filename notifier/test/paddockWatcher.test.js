import { test } from 'node:test';
import assert from 'node:assert/strict';
import { PaddockWatcher } from '../src/paddockWatcher.js';
import { fakeFirebase } from './fakeFirestore.js';

const NOW = 1_795_000_000_000;
const square = [
  { lat: 4.6, lng: -74.1 },
  { lat: 4.6, lng: -74.099 },
  { lat: 4.601, lng: -74.099 },
  { lat: 4.601, lng: -74.1 },
];
const INSIDE = { lat: 4.6005, lng: -74.0995 };
const OUTSIDE = { lat: 4.6005, lng: -74.098 };

function setup({ extraDocs = {} } = {}) {
  const fb = fakeFirebase({
    'farms/f1': { name: 'La Esperanza' },
    'farms/f1/members/dueño': { role: 'OWNER', isActive: true },
    'farms/f1/members/pedro': { role: 'WORKER', isActive: true },
    'farms/f1/animalRecords/lola': { name: 'Lola' },
    'users/dueño': { fcmTokens: ['tok-dueño'] },
    'users/pedro': { fcmTokens: ['tok-pedro'] },
    ...extraDocs,
  });
  let position = { animalId: 'lola', serial: 'SGE-0001', ...OUTSIDE };
  const watcher = new PaddockWatcher({
    db: fb.db,
    messaging: fb.messaging,
    config: { geofenceIntervalMs: 15_000 },
    fetchPositions: async () => [position],
    log: () => {},
    now: () => NOW,
  });
  watcher.paddocksByFarm = new Map([['f1', [{ id: 'p1', points: square }]]]);
  const moveTo = (point) => { position = { ...position, ...point }; };
  const alerts = () => [...fb.docs].filter(([path]) => path.startsWith('farms/f1/alerts/')).map(([, data]) => data);
  return { fb, watcher, moveTo, alerts };
}

test('abre una alerta tras 2 lecturas fuera y avisa solo al propietario', async () => {
  const { fb, watcher, alerts } = setup();

  await watcher.checkAll();
  assert.equal(alerts().length, 0, 'una sola lectura fuera no basta');

  await watcher.checkAll();
  assert.equal(alerts().length, 1);
  assert.equal(alerts()[0].status, 'OPEN');
  assert.equal(alerts()[0].animalName, 'Lola');
  assert.equal(fb.sent.length, 1);
  assert.deepEqual(fb.sent[0].tokens, ['tok-dueño']);
  assert.equal(fb.sent[0].data.title, 'Lola salió de los potreros');
});

test('no repite la alerta mientras la vaca sigue fuera', async () => {
  const { fb, watcher, alerts } = setup();
  for (let i = 0; i < 5; i++) await watcher.checkAll();
  assert.equal(alerts().length, 1);
  assert.equal(fb.sent.length, 1);
});

test('resuelve la alerta cuando la vaca vuelve a entrar, sin avisar', async () => {
  const { fb, watcher, moveTo, alerts } = setup();
  await watcher.checkAll();
  await watcher.checkAll();
  moveTo(INSIDE);
  await watcher.checkAll();

  assert.equal(alerts()[0].status, 'RESOLVED');
  assert.equal(alerts()[0].resolvedAt, NOW);
  assert.equal(fb.sent.length, 1);
});

test('tras reiniciar retoma la alerta abierta en vez de crear otra', async () => {
  const { fb, watcher, alerts } = setup({
    extraDocs: { 'farms/f1/alerts/vieja': { animalId: 'lola', status: 'OPEN' } },
  });
  await watcher.checkAll();
  await watcher.checkAll();
  assert.equal(alerts().length, 1);
  assert.equal(fb.sent.length, 0);
});

test('un error de collar-api no detiene la revisión', async () => {
  const { watcher } = setup();
  watcher.fetchPositions = async () => { throw new Error('collar-api respondió 503'); };
  await watcher.checkAll();
  assert.match(watcher.stats.lastError, /503/);
});
