import { FieldValue } from 'firebase-admin/firestore';
import { buildDataMessage, isDeadTokenError, isRecent, recipientUids } from './transactionMessage.js';

/** Código gRPC de Firestore cuando el documento ya existe (create() sobre uno existente). */
const ALREADY_EXISTS = 6;
const RESTART_DELAY_MS = 30_000;

/**
 * Escucha las transacciones nuevas de todas las fincas y avisa por FCM a los demás miembros.
 *
 * - Idempotente: antes de enviar crea notificationLog/{finca}_{transacción}. Si ya existe
 *   (reinicio del servicio, evento repetido), no vuelve a avisar.
 * - Limpia los tokens que FCM reporta como inválidos (app desinstalada, sesión cerrada).
 * - Si el listener se cae (red, permisos), se reinicia solo a los 30 s.
 */
export class TransactionNotifier {
  constructor({ db, messaging, config, log = console.log, now = Date.now }) {
    this.db = db;
    this.messaging = messaging;
    this.config = config;
    this.log = log;
    this.now = now;
    this.stats = { listening: false, startedAt: null, lastEventAt: null, sent: 0, failed: 0, skipped: 0 };
  }

  start() {
    // Solo transacciones creadas desde poco antes de arrancar: no se releen todas las fincas
    // en cada reinicio (cuesta lecturas) ni se avisa de movimientos viejos.
    const since = this.now() - this.config.maxAgeMinutes * 60_000;
    this.stats.startedAt = new Date().toISOString();

    this.unsubscribe = this.db
      .collectionGroup('transactions')
      .where('createdAt', '>=', since)
      .onSnapshot(
        (snapshot) => {
          this.stats.listening = true;
          for (const change of snapshot.docChanges()) {
            if (change.type !== 'added') continue;
            this.handle(change.doc).catch((err) => {
              this.stats.failed++;
              this.log(`ERROR procesando ${change.doc.ref.path}: ${err.stack ?? err}`);
            });
          }
        },
        (err) => {
          this.stats.listening = false;
          // FAILED_PRECONDITION trae en el mensaje el enlace para crear el índice que falta.
          this.log(`ERROR del listener de Firestore (reintento en 30 s): ${err.message}`);
          setTimeout(() => this.start(), RESTART_DELAY_MS).unref();
        },
      );
    this.log(`Escuchando transacciones nuevas (desde ${new Date(since).toISOString()})`);
  }

  stop() {
    this.unsubscribe?.();
    this.stats.listening = false;
  }

  async handle(doc) {
    this.stats.lastEventAt = new Date().toISOString();
    const transaction = doc.data();
    const farmRef = doc.ref.parent.parent;
    if (!farmRef) return;
    const farmId = farmRef.id;

    if (!isRecent(transaction, this.now(), this.config.maxAgeMinutes)) {
      this.stats.skipped++;
      return;
    }
    if (!(await this.claim(farmId, doc.id))) {
      this.stats.skipped++;
      return;
    }

    const [farmSnap, membersSnap] = await Promise.all([farmRef.get(), farmRef.collection('members').get()]);
    const uids = recipientUids(
      membersSnap.docs.map((m) => m.id),
      transaction.createdBy,
      this.config.notifyAuthor,
    );
    const targets = await this.tokensFor(uids);
    if (targets.length === 0) {
      this.log(`${farmId}/${doc.id}: sin destinatarios con token, no se envía`);
      return;
    }

    const message = buildDataMessage({
      farmId,
      transactionId: doc.id,
      transaction,
      farmName: farmSnap.get('name'),
    });
    const result = await this.messaging.sendEachForMulticast({
      ...message,
      tokens: targets.map((t) => t.token),
    });

    this.stats.sent += result.successCount;
    this.stats.failed += result.failureCount;
    await this.removeDeadTokens(targets, result.responses);
    this.log(`${farmId}/${doc.id}: enviado a ${result.successCount}/${targets.length} dispositivos`);
  }

  /** Marca la transacción como avisada. Devuelve false si otro proceso o evento ya lo hizo. */
  async claim(farmId, transactionId) {
    try {
      await this.db.collection('notificationLog').doc(`${farmId}_${transactionId}`).create({
        farmId,
        transactionId,
        notifiedAt: FieldValue.serverTimestamp(),
      });
      return true;
    } catch (err) {
      if (err.code === ALREADY_EXISTS) return false;
      throw err;
    }
  }

  /** Tokens FCM de cada usuario (users/{uid}.fcmTokens), uno por celular donde inició sesión. */
  async tokensFor(uids) {
    if (uids.length === 0) return [];
    const snaps = await this.db.getAll(...uids.map((uid) => this.db.collection('users').doc(uid)));
    return snaps.flatMap((snap) =>
      (snap.get('fcmTokens') ?? [])
        .filter((token) => typeof token === 'string' && token.length > 0)
        .map((token) => ({ uid: snap.id, token })),
    );
  }

  async removeDeadTokens(targets, responses) {
    const dead = responses
      .map((response, i) => ({ response, target: targets[i] }))
      .filter(({ response }) => !response.success && isDeadTokenError(response.error?.code));
    await Promise.all(
      dead.map(({ target }) =>
        this.db
          .collection('users')
          .doc(target.uid)
          .update({ fcmTokens: FieldValue.arrayRemove(target.token) }),
      ),
    );
    if (dead.length > 0) this.log(`Se eliminaron ${dead.length} tokens inválidos`);
  }
}
