import { test } from 'node:test';
import assert from 'node:assert/strict';
import { mkdtemp, readFile, rm } from 'node:fs/promises';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { JsonFileCollarRepository } from '../src/repository/jsonFileCollarRepository.js';

const sample = (id, serial) => ({
  id,
  serial,
  farmId: 'finca-1',
  animalId: null,
  animalName: null,
  anchor: { lat: 4.6, lng: -74 },
  radiusMeters: 40,
  status: 'ACTIVE',
  createdAt: `2026-11-22T00:00:0${id}Z`,
  updatedAt: `2026-11-22T00:00:0${id}Z`,
});

test('los collares sobreviven a un reinicio del servidor', async () => {
  const dir = await mkdtemp(join(tmpdir(), 'collars-'));
  const file = join(dir, 'sub', 'collars.json');
  try {
    const repo = await JsonFileCollarRepository.open(file); // el archivo aún no existe
    await Promise.all([repo.create(sample('1', 'A-1')), repo.create(sample('2', 'A-2'))]);
    await repo.update('1', { radiusMeters: 90 });
    await repo.delete('2');

    const reopened = await JsonFileCollarRepository.open(file);
    const collars = await reopened.list();
    assert.equal(collars.length, 1);
    assert.equal(collars[0].radiusMeters, 90);
    assert.ok(JSON.parse(await readFile(file, 'utf8')).collars);
  } finally {
    await rm(dir, { recursive: true, force: true });
  }
});
