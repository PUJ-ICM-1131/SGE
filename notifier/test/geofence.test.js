import { test } from 'node:test';
import assert from 'node:assert/strict';
import {
  ALERT_FORWARDED,
  ALERT_OPENED,
  buildAlertMessage,
  contains,
  evaluatePositions,
  membersWithRole,
} from '../src/geofence.js';

// Cuadrado de ~100 m cerca de Bogotá.
const square = [
  { lat: 4.6, lng: -74.1 },
  { lat: 4.6, lng: -74.099 },
  { lat: 4.601, lng: -74.099 },
  { lat: 4.601, lng: -74.1 },
];
const paddocks = [{ id: 'p1', points: square }];
const inside = { lat: 4.6005, lng: -74.0995 };
const outside = { lat: 4.6005, lng: -74.098 };

test('contains: dentro, fuera y figuras no convexas', () => {
  assert.equal(contains(square, inside), true);
  assert.equal(contains(square, outside), false);
  assert.equal(contains(square.slice(0, 2), inside), false);

  // "L": la esquina superior derecha queda fuera.
  const l = [
    { lat: 4.6, lng: -74.1 }, { lat: 4.6, lng: -74.098 }, { lat: 4.601, lng: -74.098 },
    { lat: 4.601, lng: -74.099 }, { lat: 4.602, lng: -74.099 }, { lat: 4.602, lng: -74.1 },
  ];
  assert.equal(contains(l, { lat: 4.6015, lng: -74.0995 }), true);
  assert.equal(contains(l, { lat: 4.6015, lng: -74.0985 }), false);
});

test('una vaca fuera cuenta como salida solo desde la segunda lectura seguida', () => {
  const position = { animalId: 'lola', ...outside };
  const first = evaluatePositions({ positions: [position], paddocks, outsideCounts: new Map() });
  assert.deepEqual(first.wentOut, []);

  const second = evaluatePositions({ positions: [position], paddocks, outsideCounts: first.counts });
  assert.deepEqual(second.wentOut.map((p) => p.animalId), ['lola']);
});

test('volver a entrar reinicia la cuenta y marca la vaca como dentro', () => {
  const out = { animalId: 'lola', ...outside };
  const back = { animalId: 'lola', ...inside };
  const first = evaluatePositions({ positions: [out], paddocks, outsideCounts: new Map() });
  const second = evaluatePositions({ positions: [back], paddocks, outsideCounts: first.counts });
  assert.equal(second.inside.has('lola'), true);
  assert.equal(second.counts.has('lola'), false);

  const third = evaluatePositions({ positions: [out], paddocks, outsideCounts: second.counts });
  assert.deepEqual(third.wentOut, []);
});

test('ignora collares sin animal asociado', () => {
  const result = evaluatePositions({
    positions: [{ ...outside }],
    paddocks,
    outsideCounts: new Map([[undefined, 5]]),
  });
  assert.deepEqual(result.wentOut, []);
});

test('membersWithRole: solo activos del rol pedido', () => {
  const members = [
    { id: 'dueño', role: 'OWNER' },
    { id: 'pedro', role: 'WORKER', isActive: true },
    { id: 'antiguo', role: 'WORKER', isActive: false },
    { id: 'juan', role: 'WORKER' },
  ];
  assert.deepEqual(membersWithRole(members, 'OWNER'), ['dueño']);
  assert.deepEqual(membersWithRole(members, 'WORKER'), ['pedro', 'juan']);
});

test('mensajes para el dueño y para los trabajadores', () => {
  const opened = buildAlertMessage({ type: ALERT_OPENED, farmId: 'f1', alertId: 'a1', animalName: 'Lola', farmName: 'La Esperanza' });
  assert.equal(opened.data.title, 'Lola salió de los potreros');
  assert.match(opened.data.body, /avisar a los trabajadores · La Esperanza$/);
  assert.equal(opened.data.alertId, 'a1');

  const forwarded = buildAlertMessage({ type: ALERT_FORWARDED, farmId: 'f1', alertId: 'a1', animalName: '' });
  assert.equal(forwarded.data.title, 'Aviso del propietario: Una vaca salió de los potreros');
  assert.equal(forwarded.data.type, ALERT_FORWARDED);
});
