import { ALERT_OPENED, buildAlertMessage, evaluatePositions, membersWithRole } from './geofence.js';
import { sendToUsers } from './push.js';

const RESTART_DELAY_MS = 30_000;

/**
 * Alertas de vacas fuera de los potreros (#73).
 *
 * - Escucha los potreros de todas las fincas (collectionGroup "paddocks", sin filtros: no
 *   necesita índices).
 * - Cada [config.geofenceIntervalMs] pide a collar-api las posiciones de cada finca con potreros.
 * - Una vaca fuera de todos los potreros en 2 lecturas seguidas abre una alerta en
 *   farms/{finca}/alerts/{id} (una sola abierta por vaca) y se avisa a los propietarios.
 * - Cuando la vaca vuelve a entrar, la alerta pasa a RESOLVED, sin aviso.
 */
export class PaddockWatcher {
  constructor({ db, messaging, config, fetchPositions, log = console.log, now = Date.now }) {
    this.db = db;
    this.messaging = messaging;
    this.config = config;
    this.fetchPositions = fetchPositions;
    this.log = log;
    this.now = now;
    /** farmId → [{ id, points }] */
    this.paddocksByFarm = new Map();
    /** farmId → (animalId → lecturas seguidas fuera) */
    this.outsideCounts = new Map();
    /** farmId → (animalId → id de su alerta abierta); se carga de Firestore la primera vez. */
    this.openAlerts = new Map();
    this.stats = {
      listening: false, lastCheckAt: null, lastError: null,
      alertsOpened: 0, alertsResolved: 0, sent: 0, failed: 0,
    };
  }

  start() {
    this.unsubscribe = this.db.collectionGroup('paddocks').onSnapshot(
      (snapshot) => {
        this.stats.listening = true;
        this.paddocksByFarm = groupByFarm(snapshot.docs);
      },
      (err) => {
        this.stats.listening = false;
        this.log(`ERROR del listener de potreros (reintento en 30 s): ${err.message}`);
        setTimeout(() => this.start(), RESTART_DELAY_MS).unref();
      },
    );
    if (!this.timer) {
      this.timer = setInterval(() => this.checkAll(), this.config.geofenceIntervalMs);
      this.timer.unref();
    }
    this.log(`Vigilando potreros cada ${this.config.geofenceIntervalMs / 1000} s`);
  }

  stop() {
    this.unsubscribe?.();
    clearInterval(this.timer);
    this.timer = null;
    this.stats.listening = false;
  }

  async checkAll() {
    for (const [farmId, paddocks] of this.paddocksByFarm) {
      try {
        await this.checkFarm(farmId, paddocks);
      } catch (err) {
        this.stats.lastError = `${farmId}: ${err.message}`;
        this.log(`ERROR revisando la finca ${farmId}: ${err.message}`);
      }
    }
    this.stats.lastCheckAt = new Date(this.now()).toISOString();
  }

  async checkFarm(farmId, paddocks) {
    const positions = await this.fetchPositions(farmId);
    const { counts, wentOut, inside } = evaluatePositions({
      positions,
      paddocks,
      outsideCounts: this.outsideCounts.get(farmId) ?? new Map(),
    });
    this.outsideCounts.set(farmId, counts);

    const open = await this.openAlertsFor(farmId);
    for (const position of wentOut) {
      if (open.has(position.animalId)) continue;
      open.set(position.animalId, await this.openAlert(farmId, position));
    }
    for (const [animalId, alertId] of open) {
      if (!inside.has(animalId)) continue;
      await this.alerts(farmId).doc(alertId).update({ status: 'RESOLVED', resolvedAt: this.now() });
      open.delete(animalId);
      this.stats.alertsResolved++;
      this.log(`${farmId}: la vaca ${animalId} volvió a un potrero, alerta ${alertId} resuelta`);
    }
  }

  async openAlertsFor(farmId) {
    if (!this.openAlerts.has(farmId)) {
      const snapshot = await this.alerts(farmId).where('status', '==', 'OPEN').get();
      this.openAlerts.set(farmId, new Map(snapshot.docs.map((doc) => [doc.get('animalId'), doc.id])));
    }
    return this.openAlerts.get(farmId);
  }

  async openAlert(farmId, position) {
    const farmRef = this.db.collection('farms').doc(farmId);
    const [farmSnap, animalSnap, membersSnap] = await Promise.all([
      farmRef.get(),
      farmRef.collection('animalRecords').doc(position.animalId).get(),
      farmRef.collection('members').get(),
    ]);
    const animalName = animalSnap.get('name') || position.serial || 'Una vaca';

    const ref = this.alerts(farmId).doc();
    await ref.set({
      type: 'ANIMAL_OUTSIDE',
      animalId: position.animalId,
      animalName,
      lat: position.lat,
      lng: position.lng,
      status: 'OPEN',
      createdAt: this.now(),
      forwardedAt: null,
      forwardedBy: null,
      resolvedAt: null,
    });
    this.stats.alertsOpened++;

    const owners = membersWithRole(
      membersSnap.docs.map((m) => ({ id: m.id, role: m.get('role'), isActive: m.get('isActive') })),
      'OWNER',
    );
    const message = buildAlertMessage({
      type: ALERT_OPENED, farmId, alertId: ref.id, animalName, farmName: farmSnap.get('name'),
    });
    const result = await sendToUsers({ db: this.db, messaging: this.messaging, uids: owners, message, log: this.log });
    this.stats.sent += result.sent;
    this.stats.failed += result.failed;
    this.log(`${farmId}: ${animalName} salió de los potreros → alerta ${ref.id}, aviso a ${result.sent}/${result.targets}`);
    return ref.id;
  }

  alerts(farmId) {
    return this.db.collection('farms').doc(farmId).collection('alerts');
  }
}

/** Potreros válidos (al menos 3 puntos) agrupados por finca. */
function groupByFarm(docs) {
  const byFarm = new Map();
  for (const doc of docs) {
    const farmId = doc.ref.parent.parent?.id;
    const points = doc.get('points');
    if (!farmId || !Array.isArray(points) || points.length < 3) continue;
    if (!byFarm.has(farmId)) byFarm.set(farmId, []);
    byFarm.get(farmId).push({ id: doc.id, points });
  }
  return byFarm;
}
