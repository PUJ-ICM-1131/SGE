import { createServer } from 'node:http';
import { applicationDefault, initializeApp } from 'firebase-admin/app';
import { getFirestore } from 'firebase-admin/firestore';
import { getMessaging } from 'firebase-admin/messaging';
import { AlertForwarder } from './alertForwarder.js';
import { ChatNotifier } from './chatNotifier.js';
import { createPositionsClient } from './collarClient.js';
import { loadConfig } from './config.js';
import { PaddockWatcher } from './paddockWatcher.js';
import { TransactionNotifier } from './transactionNotifier.js';

let config;
try {
  config = loadConfig();
} catch (err) {
  console.error(`Configuración inválida: ${err.message}`);
  process.exit(1);
}

// applicationDefault() lee el JSON indicado en GOOGLE_APPLICATION_CREDENTIALS.
initializeApp({ credential: applicationDefault() });
const db = getFirestore();
const messaging = getMessaging();
const log = (msg) => console.log(msg);

const notifier = new TransactionNotifier({ db, messaging, config, log });
notifier.start();

// Chat propietario ↔ trabajador (#74).
const chat = new ChatNotifier({ db, messaging, config, log });
chat.start();

// Alertas de vacas fuera de los potreros (#73).
const forwarder = new AlertForwarder({ db, messaging, log });
forwarder.start();
let watcher = null;
if (config.collarApiKey) {
  watcher = new PaddockWatcher({
    db,
    messaging,
    config,
    fetchPositions: createPositionsClient({ url: config.collarApiUrl, apiKey: config.collarApiKey }),
    log,
  });
  watcher.start();
} else {
  log('COLLAR_API_KEY no está definida: las alertas de potreros quedan apagadas');
}

// Estado para systemctl/curl dentro del LXC (solo loopback, no se publica).
const health = createServer((req, res) => {
  if (req.url !== '/health') {
    res.writeHead(404).end();
    return;
  }
  const status = notifier.stats.listening ? 200 : 503;
  res.writeHead(status, { 'Content-Type': 'application/json' });
  res.end(JSON.stringify({
    status: status === 200 ? 'ok' : 'starting',
    ...notifier.stats,
    paddockAlerts: watcher?.stats ?? 'apagadas (falta COLLAR_API_KEY)',
    alertForwards: forwarder.stats,
    chat: chat.stats,
  }));
});
health.listen(config.healthPort, config.healthHost, () => {
  console.log(`Estado en http://${config.healthHost}:${config.healthPort}/health` +
    (config.notifyAuthor ? ' (NOTIFY_AUTHOR activo: también avisa al autor)' : ''));
});

for (const signal of ['SIGINT', 'SIGTERM']) {
  process.on(signal, () => {
    notifier.stop();
    chat.stop();
    forwarder.stop();
    watcher?.stop();
    health.close(() => process.exit(0));
  });
}
