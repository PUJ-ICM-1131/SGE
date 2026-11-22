import { test } from 'node:test';
import assert from 'node:assert/strict';
import { METERS_PER_DEGREE, simulatePosition } from '../src/domain/simulation.js';

const anchor = { lat: 4.628, lng: -74.065 };
const T0 = 1_795_000_000_000;
const collar = (id, radiusMeters = 40) => ({ id, anchor, radiusMeters });

function distanceMeters(a, b) {
  const dNorth = (a.lat - b.lat) * METERS_PER_DEGREE;
  const dEast = (a.lng - b.lng) * METERS_PER_DEGREE * Math.cos((a.lat * Math.PI) / 180);
  return Math.hypot(dNorth, dEast);
}

test('el mismo collar en el mismo instante da la misma lectura', () => {
  assert.deepEqual(simulatePosition(collar('vaca-1'), T0), simulatePosition(collar('vaca-1'), T0));
});

test('collares distintos siguen trayectorias distintas', () => {
  const a = simulatePosition(collar('vaca-1'), T0);
  const b = simulatePosition(collar('vaca-2'), T0);
  assert.notDeepEqual([a.lat, a.lng], [b.lat, b.lng]);
});

for (const radius of [10, 40, 200]) {
  test(`con radio ${radius} m la vaca nunca sale del potrero en 24 h`, () => {
    for (let s = 0; s < 86_400; s += 37) {
      const r = simulatePosition(collar('lola', radius), T0 + s * 1000);
      const d = distanceMeters(r, anchor);
      assert.ok(d <= radius + 0.5, `se alejó ${d.toFixed(1)} m en t=${s}`);
    }
  });
}

test('con radio 40 m la velocidad es realista y la vaca a veces se detiene', () => {
  let stopped = 0;
  let moving = 0;
  for (let s = 0; s < 3_600; s += 5) {
    const r = simulatePosition(collar('manchas'), T0 + s * 1000);
    assert.ok(r.speedMps >= 0 && r.speedMps < 4, `velocidad irreal ${r.speedMps}`);
    if (r.speedMps < 0.5) stopped++;
    else moving++;
  }
  assert.ok(stopped > 0, 'nunca se detuvo');
  assert.ok(moving > 0, 'nunca se movió');
});

test('rumbo y batería están en rango', () => {
  const r = simulatePosition(collar('x'), T0);
  assert.ok(r.headingDeg >= 0 && r.headingDeg < 360);
  assert.ok(r.batteryPercent >= 40 && r.batteryPercent <= 100);
  assert.equal(r.timestamp, T0);
});
