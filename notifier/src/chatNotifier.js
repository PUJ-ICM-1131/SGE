import { membersWithRole } from './geofence.js';
import { claimOnce, sendToUsers } from './push.js';
import { isRecent } from './transactionMessage.js';

const RESTART_DELAY_MS = 30_000;
const PREVIEW_LENGTH = 200;

export const CHAT_MESSAGE = 'chat_message';

/** Quién recibe el aviso de un mensaje nuevo: el otro lado de la conversación (#74). */
export function chatRecipients({ members, workerUid, senderUid }) {
  if (senderUid === workerUid) return membersWithRole(members, 'OWNER');
  return membersWithRole(members, 'WORKER').filter((uid) => uid === workerUid);
}

/** "Mensaje de Pedro", o del propietario / de un trabajador si no tiene nombre. */
export function chatTitle(sender) {
  if (sender?.displayName) return `Mensaje de ${sender.displayName}`;
  return sender?.role === 'OWNER' ? 'Mensaje del propietario' : 'Mensaje de un trabajador';
}

/** Mensaje FCM de solo datos: la app lo muestra en su canal "Mensajes" y abre la conversación. */
export function buildChatMessage({ farmId, workerUid, title, text }) {
  const body = text.length > PREVIEW_LENGTH ? `${text.slice(0, PREVIEW_LENGTH)}…` : text;
  return {
    data: { type: CHAT_MESSAGE, farmId: String(farmId), workerUid: String(workerUid), title, body },
    android: { priority: 'high', ttl: 24 * 60 * 60 * 1000 },
  };
}

/**
 * Chat propietario ↔ trabajador (#74): avisa al otro participante cuando llega un mensaje.
 *
 * - Escucha los documentos de conversación (farms/{finca}/chats/{trabajador}), que la app
 *   actualiza con cada mensaje. Son pocos y sin filtros: no necesita índices.
 * - Al arrancar llegan todas las conversaciones: solo se avisa el último mensaje si es reciente.
 * - Idempotente con notificationLog/chat_{finca}_{trabajador}_{hora del mensaje}.
 */
export class ChatNotifier {
  constructor({ db, messaging, config, log = console.log, now = Date.now }) {
    this.db = db;
    this.messaging = messaging;
    this.config = config;
    this.log = log;
    this.now = now;
    this.stats = { listening: false, sent: 0, failed: 0 };
  }

  start() {
    this.unsubscribe = this.db.collectionGroup('chats').onSnapshot(
      (snapshot) => {
        this.stats.listening = true;
        for (const change of snapshot.docChanges()) {
          if (change.type === 'removed') continue;
          this.handle(change.doc).catch((err) => {
            this.stats.failed++;
            this.log(`ERROR avisando ${change.doc.ref.path}: ${err.stack ?? err}`);
          });
        }
      },
      (err) => {
        this.stats.listening = false;
        this.log(`ERROR del listener del chat (reintento en 30 s): ${err.message}`);
        setTimeout(() => this.start(), RESTART_DELAY_MS).unref();
      },
    );
  }

  stop() {
    this.unsubscribe?.();
    this.stats.listening = false;
  }

  async handle(doc) {
    const farmId = doc.ref.parent.parent?.id;
    const { lastMessage, lastMessageAt, lastSenderUid } = doc.data() ?? {};
    if (!farmId || !lastMessage || !lastSenderUid) return;
    if (!isRecent({ createdAt: lastMessageAt }, this.now(), this.config.maxAgeMinutes)) return;
    const claimId = `chat_${farmId}_${doc.id}_${lastMessageAt}`;
    if (!(await claimOnce(this.db, claimId, { farmId, workerUid: doc.id }))) return;

    const membersSnap = await this.db.collection('farms').doc(farmId).collection('members').get();
    const members = membersSnap.docs.map((m) => ({
      id: m.id, role: m.get('role'), isActive: m.get('isActive'), displayName: m.get('displayName'),
    }));
    const uids = chatRecipients({ members, workerUid: doc.id, senderUid: lastSenderUid });
    const message = buildChatMessage({
      farmId,
      workerUid: doc.id,
      title: chatTitle(members.find((m) => m.id === lastSenderUid)),
      text: lastMessage,
    });
    const result = await sendToUsers({ db: this.db, messaging: this.messaging, uids, message, log: this.log });
    this.stats.sent += result.sent;
    this.stats.failed += result.failed;
    this.log(`${farmId}/chats/${doc.id}: mensaje avisado a ${result.sent}/${result.targets} dispositivos`);
  }
}
