import { FieldValue } from 'firebase-admin/firestore';
import { isDeadTokenError } from './transactionMessage.js';

/** Código gRPC de Firestore cuando el documento ya existe (create() sobre uno existente). */
const ALREADY_EXISTS = 6;

/**
 * Marca un aviso como enviado creando notificationLog/{id}. Devuelve false si ya existía
 * (reinicio del servicio, evento repetido): así nunca se avisa dos veces lo mismo.
 */
export async function claimOnce(db, id, data) {
  try {
    await db.collection('notificationLog').doc(id).create({
      ...data,
      notifiedAt: FieldValue.serverTimestamp(),
    });
    return true;
  } catch (err) {
    if (err.code === ALREADY_EXISTS) return false;
    throw err;
  }
}

/**
 * Envía [message] por FCM a los celulares de [uids] (users/{uid}.fcmTokens, uno por celular
 * donde inició sesión) y borra los tokens que FCM reporta como muertos.
 */
export async function sendToUsers({ db, messaging, uids, message, log = console.log }) {
  const targets = await tokensFor(db, uids);
  if (targets.length === 0) return { targets: 0, sent: 0, failed: 0 };

  const result = await messaging.sendEachForMulticast({
    ...message,
    tokens: targets.map((t) => t.token),
  });
  await removeDeadTokens(db, targets, result.responses, log);
  return { targets: targets.length, sent: result.successCount, failed: result.failureCount };
}

async function tokensFor(db, uids) {
  const unique = [...new Set(uids)];
  if (unique.length === 0) return [];
  const snaps = await db.getAll(...unique.map((uid) => db.collection('users').doc(uid)));
  return snaps.flatMap((snap) =>
    (snap.get('fcmTokens') ?? [])
      .filter((token) => typeof token === 'string' && token.length > 0)
      .map((token) => ({ uid: snap.id, token })),
  );
}

async function removeDeadTokens(db, targets, responses, log) {
  const dead = responses
    .map((response, i) => ({ response, target: targets[i] }))
    .filter(({ response }) => !response.success && isDeadTokenError(response.error?.code));
  await Promise.all(
    dead.map(({ target }) =>
      db.collection('users').doc(target.uid).update({ fcmTokens: FieldValue.arrayRemove(target.token) }),
    ),
  );
  if (dead.length > 0) log(`Se eliminaron ${dead.length} tokens inválidos`);
}
