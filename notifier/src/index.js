import { createServer } from 'node:http';
import { applicationDefault, initializeApp } from 'firebase-admin/app';
import { getFirestore } from 'firebase-admin/firestore';
import { getMessaging } from 'firebase-admin/messaging';
import { loadConfig } from './config.js';
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

const notifier = new TransactionNotifier({
  db: getFirestore(),
  messaging: getMessaging(),
  config,
  log: (msg) => console.log(msg),
});
notifier.start();

// Estado para systemctl/curl dentro del LXC (solo loopback, no se publica).
const health = createServer((req, res) => {
  if (req.url !== '/health') {
    res.writeHead(404).end();
    return;
  }
  const status = notifier.stats.listening ? 200 : 503;
  res.writeHead(status, { 'Content-Type': 'application/json' });
  res.end(JSON.stringify({ status: status === 200 ? 'ok' : 'starting', ...notifier.stats }));
});
health.listen(config.healthPort, config.healthHost, () => {
  console.log(`Estado en http://${config.healthHost}:${config.healthPort}/health` +
    (config.notifyAuthor ? ' (NOTIFY_AUTHOR activo: también avisa al autor)' : ''));
});

for (const signal of ['SIGINT', 'SIGTERM']) {
  process.on(signal, () => {
    notifier.stop();
    health.close(() => process.exit(0));
  });
}
