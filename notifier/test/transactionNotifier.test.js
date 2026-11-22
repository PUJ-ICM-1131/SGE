import { test } from 'node:test';
import assert from 'node:assert/strict';
import { TransactionNotifier } from '../src/transactionNotifier.js';

/**
 * Firestore y FCM falsos, en memoria: prueban el flujo completo
 * (deduplicación, destinatarios, envío y limpieza de tokens) sin credenciales.
 */
function fakeFirebase({ members, users, farmName = 'La Esperanza', sendResults }) {
  const log = new Set();
  const removed = [];
  const sent = [];

  const docRef = (path) => ({
    id: path.split('/').at(-1),
    async get() {
      return { get: (field) => (field === 'name' ? farmName : undefined) };
    },
    collection(name) {
      return {
        async get() {
          return { docs: members.map((id) => ({ id })) };
        },
        name,
      };
    },
  });

  const db = {
    collection(name) {
      return {
        doc(id) {
          return {
            id,
            async create() {
              if (log.has(id)) throw Object.assign(new Error('existe'), { code: 6 });
              log.add(id);
            },
            async update(change) {
              removed.push({ uid: id, change });
            },
            _uid: id,
            _collection: name,
          };
        },
      };
    },
    async getAll(...refs) {
      return refs.map((ref) => ({ id: ref._uid, get: () => users[ref._uid]?.fcmTokens }));
    },
  };

  const messaging = {
    async sendEachForMulticast(message) {
      sent.push(message);
      const responses = message.tokens.map((token) => sendResults?.[token] ?? { success: true });
      return {
        responses,
        successCount: responses.filter((r) => r.success).length,
        failureCount: responses.filter((r) => !r.success).length,
      };
    },
  };

  const transactionDoc = (id, data) => ({
    id,
    data: () => data,
    ref: { path: `farms/f1/transactions/${id}`, parent: { parent: docRef('farms/f1') } },
  });

  return { db, messaging, sent, removed, transactionDoc };
}

const NOW = 1_795_000_000_000;
const config = { maxAgeMinutes: 30, notifyAuthor: false };

test('avisa a los demás miembros y no al autor', async () => {
  const fb = fakeFirebase({
    members: ['dueño', 'trabajador'],
    users: { dueño: { fcmTokens: ['tok-dueño'] }, trabajador: { fcmTokens: ['tok-trab'] } },
  });
  const notifier = new TransactionNotifier({ ...fb, config, log: () => {}, now: () => NOW });

  await notifier.handle(fb.transactionDoc('t1', {
    type: 'EXPENSE', amountCents: 3_000_000, categoryName: 'Alimentación', createdBy: 'trabajador',
    createdByName: 'Juan', createdAt: NOW - 1000,
  }));

  assert.equal(fb.sent.length, 1);
  assert.deepEqual(fb.sent[0].tokens, ['tok-dueño']);
  assert.equal(fb.sent[0].data.title, 'Nuevo egreso: -$30.000');
});

test('no avisa dos veces la misma transacción', async () => {
  const fb = fakeFirebase({ members: ['dueño'], users: { dueño: { fcmTokens: ['tok'] } } });
  const notifier = new TransactionNotifier({ ...fb, config, log: () => {}, now: () => NOW });
  const doc = fb.transactionDoc('t1', { type: 'INCOME', amountCents: 100, createdBy: 'otro', createdAt: NOW });

  await notifier.handle(doc);
  await notifier.handle(doc); // por ejemplo, tras reiniciar el servicio
  assert.equal(fb.sent.length, 1);
});

test('ignora transacciones viejas', async () => {
  const fb = fakeFirebase({ members: ['dueño'], users: { dueño: { fcmTokens: ['tok'] } } });
  const notifier = new TransactionNotifier({ ...fb, config, log: () => {}, now: () => NOW });
  await notifier.handle(fb.transactionDoc('t1', { type: 'INCOME', amountCents: 100, createdBy: 'otro', createdAt: NOW - 3_600_000 }));
  assert.equal(fb.sent.length, 0);
});

test('borra los tokens que FCM reporta como no registrados', async () => {
  const fb = fakeFirebase({
    members: ['dueño'],
    users: { dueño: { fcmTokens: ['vivo', 'muerto'] } },
    sendResults: { muerto: { success: false, error: { code: 'messaging/registration-token-not-registered' } } },
  });
  const notifier = new TransactionNotifier({ ...fb, config, log: () => {}, now: () => NOW });
  await notifier.handle(fb.transactionDoc('t1', { type: 'INCOME', amountCents: 100, createdBy: 'otro', createdAt: NOW }));

  assert.equal(fb.removed.length, 1);
  assert.equal(fb.removed[0].uid, 'dueño');
  assert.equal(notifier.stats.sent, 1);
  assert.equal(notifier.stats.failed, 1);
});

test('en modo demo también avisa al autor', async () => {
  const fb = fakeFirebase({ members: ['dueño'], users: { dueño: { fcmTokens: ['tok'] } } });
  const notifier = new TransactionNotifier({
    ...fb, config: { ...config, notifyAuthor: true }, log: () => {}, now: () => NOW,
  });
  await notifier.handle(fb.transactionDoc('t1', { type: 'INCOME', amountCents: 100, createdBy: 'dueño', createdAt: NOW }));
  assert.deepEqual(fb.sent[0].tokens, ['tok']);
});
