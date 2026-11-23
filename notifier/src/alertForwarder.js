import { ALERT_FORWARDED, buildAlertMessage, membersWithRole } from './geofence.js';
import { claimOnce, sendToUsers } from './push.js';

const RESTART_DELAY_MS = 30_000;

/**
 * Reenvío de alertas (#73): cuando el propietario toca "Reenviar a trabajadores", la app escribe
 * forwardedAt/forwardedBy en la alerta y aquí se avisa a los trabajadores activos de la finca.
 *
 * - Escucha collectionGroup "alerts" sin filtros (no necesita índices; son pocas).
 * - Idempotente con notificationLog/alertForward_{finca}_{alerta}: al reiniciar no reenvía las
 *   que ya avisó.
 */
export class AlertForwarder {
  constructor({ db, messaging, log = console.log }) {
    this.db = db;
    this.messaging = messaging;
    this.log = log;
    this.stats = { listening: false, forwarded: 0, sent: 0, failed: 0 };
  }

  start() {
    this.unsubscribe = this.db.collectionGroup('alerts').onSnapshot(
      (snapshot) => {
        this.stats.listening = true;
        for (const change of snapshot.docChanges()) {
          if (change.type === 'removed' || !change.doc.get('forwardedAt')) continue;
          this.forward(change.doc).catch((err) => {
            this.stats.failed++;
            this.log(`ERROR reenviando ${change.doc.ref.path}: ${err.stack ?? err}`);
          });
        }
      },
      (err) => {
        this.stats.listening = false;
        this.log(`ERROR del listener de alertas (reintento en 30 s): ${err.message}`);
        setTimeout(() => this.start(), RESTART_DELAY_MS).unref();
      },
    );
  }

  stop() {
    this.unsubscribe?.();
    this.stats.listening = false;
  }

  async forward(doc) {
    const farmId = doc.ref.parent.parent?.id;
    if (!farmId) return;
    if (!(await claimOnce(this.db, `alertForward_${farmId}_${doc.id}`, { farmId, alertId: doc.id }))) return;

    const membersSnap = await this.db.collection('farms').doc(farmId).collection('members').get();
    const workers = membersWithRole(
      membersSnap.docs.map((m) => ({ id: m.id, role: m.get('role'), isActive: m.get('isActive') })),
      'WORKER',
    );
    const message = buildAlertMessage({
      type: ALERT_FORWARDED, farmId, alertId: doc.id, animalName: doc.get('animalName'),
    });
    const result = await sendToUsers({ db: this.db, messaging: this.messaging, uids: workers, message, log: this.log });
    this.stats.forwarded++;
    this.stats.sent += result.sent;
    this.stats.failed += result.failed;
    this.log(`${farmId}/${doc.id}: alerta reenviada a ${result.sent}/${result.targets} dispositivos de trabajadores`);
  }
}
