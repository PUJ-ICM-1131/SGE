import { test } from 'node:test';
import assert from 'node:assert/strict';
import { buildDataMessage, formatCop, isDeadTokenError, isRecent, recipientUids } from '../src/transactionMessage.js';

test('formatea pesos colombianos igual que la app', () => {
  assert.equal(formatCop(0), '$0');
  assert.equal(formatCop(5_000_000), '$50.000');
  assert.equal(formatCop(125_000_000), '$1.250.000');
  assert.equal(formatCop(-3_000_000), '-$30.000');
});

test('solo avisa transacciones recientes', () => {
  const now = 1_000_000_000;
  assert.equal(isRecent({ createdAt: now - 5 * 60_000 }, now, 30), true);
  assert.equal(isRecent({ createdAt: now - 31 * 60_000 }, now, 30), false);
  assert.equal(isRecent({}, now, 30), false);
});

test('no avisa a quien registró, salvo en modo demo', () => {
  assert.deepEqual(recipientUids(['dueño', 'trabajador'], 'trabajador', false), ['dueño']);
  assert.deepEqual(recipientUids(['dueño', 'trabajador', 'dueño'], 'trabajador', true), ['dueño', 'trabajador']);
  assert.deepEqual(recipientUids(['dueño'], 'dueño', false), []);
});

test('el mensaje es de solo datos, con strings y prioridad alta', () => {
  const message = buildDataMessage({
    farmId: 'f1',
    transactionId: 't1',
    farmName: 'La Esperanza',
    transaction: { type: 'EXPENSE', amountCents: 3_000_000, categoryName: 'Alimentación', createdByName: 'Juan' },
  });
  assert.equal(message.notification, undefined); // la app arma la notificación
  assert.equal(message.android.priority, 'high');
  assert.equal(message.data.type, 'transaction_created');
  assert.equal(message.data.transactionId, 't1');
  assert.equal(message.data.title, 'Nuevo egreso: -$30.000');
  assert.equal(message.data.body, 'Juan registró -$30.000 en Alimentación · La Esperanza');
  for (const value of Object.values(message.data)) assert.equal(typeof value, 'string');
});

test('solo borra tokens que FCM da por muertos', () => {
  assert.equal(isDeadTokenError('messaging/registration-token-not-registered'), true);
  assert.equal(isDeadTokenError('messaging/invalid-registration-token'), true);
  assert.equal(isDeadTokenError('messaging/invalid-argument'), false);
  assert.equal(isDeadTokenError('messaging/internal-error'), false);
});
