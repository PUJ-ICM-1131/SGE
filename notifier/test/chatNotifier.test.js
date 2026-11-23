import { test } from 'node:test';
import assert from 'node:assert/strict';
import { ChatNotifier, chatTitle } from '../src/chatNotifier.js';
import { fakeFirebase } from './fakeFirestore.js';

const NOW = 1_795_000_000_000;

function setup(thread) {
  const fb = fakeFirebase({
    'farms/f1/members/dueño': { role: 'OWNER', isActive: true, displayName: 'Sam' },
    'farms/f1/members/pedro': { role: 'WORKER', isActive: true, displayName: 'Pedro' },
    'farms/f1/members/juan': { role: 'WORKER', isActive: true, displayName: 'Juan' },
    'farms/f1/chats/pedro': thread,
    'users/dueño': { fcmTokens: ['tok-dueño'] },
    'users/pedro': { fcmTokens: ['tok-pedro'] },
    'users/juan': { fcmTokens: ['tok-juan'] },
  });
  const notifier = new ChatNotifier({
    db: fb.db, messaging: fb.messaging, config: { maxAgeMinutes: 30 }, log: () => {}, now: () => NOW,
  });
  return { fb, notifier, doc: fb.listenedDoc('farms/f1/chats/pedro') };
}

test('un mensaje del trabajador le llega al propietario', async () => {
  const { fb, notifier, doc } = setup({ lastMessage: 'Ya revisé el potrero', lastMessageAt: NOW, lastSenderUid: 'pedro' });
  await notifier.handle(doc);
  assert.equal(fb.sent.length, 1);
  assert.deepEqual(fb.sent[0].tokens, ['tok-dueño']);
  assert.equal(fb.sent[0].data.title, 'Mensaje de Pedro');
  assert.equal(fb.sent[0].data.workerUid, 'pedro');
});

test('un mensaje del propietario le llega solo a ese trabajador', async () => {
  const { fb, notifier, doc } = setup({ lastMessage: 'Revisa a Lola', lastMessageAt: NOW, lastSenderUid: 'dueño' });
  await notifier.handle(doc);
  assert.deepEqual(fb.sent[0].tokens, ['tok-pedro']);
  assert.equal(fb.sent[0].data.title, 'Mensaje de Sam');
});

test('el mismo mensaje no se avisa dos veces', async () => {
  const { fb, notifier, doc } = setup({ lastMessage: 'Hola', lastMessageAt: NOW, lastSenderUid: 'pedro' });
  await notifier.handle(doc);
  await notifier.handle(doc);
  assert.equal(fb.sent.length, 1);
});

test('no avisa mensajes viejos al arrancar el servicio', async () => {
  const { fb, notifier, doc } = setup({ lastMessage: 'Hola', lastMessageAt: NOW - 3_600_000, lastSenderUid: 'pedro' });
  await notifier.handle(doc);
  assert.equal(fb.sent.length, 0);
});

test('título sin nombre', () => {
  assert.equal(chatTitle({ role: 'OWNER' }), 'Mensaje del propietario');
  assert.equal(chatTitle(undefined), 'Mensaje de un trabajador');
});
