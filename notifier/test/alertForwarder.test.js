import { test } from 'node:test';
import assert from 'node:assert/strict';
import { AlertForwarder } from '../src/alertForwarder.js';
import { fakeFirebase } from './fakeFirestore.js';

function setup() {
  const fb = fakeFirebase({
    'farms/f1/members/dueño': { role: 'OWNER', isActive: true },
    'farms/f1/members/pedro': { role: 'WORKER', isActive: true },
    'farms/f1/members/antiguo': { role: 'WORKER', isActive: false },
    'farms/f1/alerts/a1': { animalName: 'Lola', status: 'OPEN', forwardedAt: 123, forwardedBy: 'dueño' },
    'users/dueño': { fcmTokens: ['tok-dueño'] },
    'users/pedro': { fcmTokens: ['tok-pedro'] },
    'users/antiguo': { fcmTokens: ['tok-antiguo'] },
  });
  const forwarder = new AlertForwarder({ db: fb.db, messaging: fb.messaging, log: () => {} });
  return { fb, forwarder, alert: fb.listenedDoc('farms/f1/alerts/a1') };
}

test('reenvía la alerta solo a los trabajadores activos', async () => {
  const { fb, forwarder, alert } = setup();
  await forwarder.forward(alert);

  assert.equal(fb.sent.length, 1);
  assert.deepEqual(fb.sent[0].tokens, ['tok-pedro']);
  assert.equal(fb.sent[0].data.title, 'Aviso del propietario: Lola salió de los potreros');
  assert.equal(fb.sent[0].data.alertId, 'a1');
});

test('no reenvía dos veces la misma alerta (p. ej. tras reiniciar el servicio)', async () => {
  const { fb, forwarder, alert } = setup();
  await forwarder.forward(alert);
  await forwarder.forward(alert);
  assert.equal(fb.sent.length, 1);
});
