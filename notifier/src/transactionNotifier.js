import { claimOnce, sendToUsers } from './push.js';
import { buildDataMessage, isRecent, recipientUids } from './transactionMessage.js';

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
    if (!(await claimOnce(this.db, `${farmId}_${doc.id}`, { farmId, transactionId: doc.id }))) {
      this.stats.skipped++;
      return;
    }

    const [farmSnap, membersSnap] = await Promise.all([farmRef.get(), farmRef.collection('members').get()]);
    const uids = recipientUids(
      membersSnap.docs.map((m) => m.id),
      transaction.createdBy,
      this.config.notifyAuthor,
    );
    const message = buildDataMessage({
      farmId,
      transactionId: doc.id,
      transaction,
      farmName: farmSnap.get('name'),
    });
    const result = await sendToUsers({ db: this.db, messaging: this.messaging, uids, message, log: this.log });
    if (result.targets === 0) {
      this.log(`${farmId}/${doc.id}: sin destinatarios con token, no se envía`);
      return;
    }

    this.stats.sent += result.sent;
    this.stats.failed += result.failed;
    this.log(`${farmId}/${doc.id}: enviado a ${result.sent}/${result.targets} dispositivos`);
  }
}
