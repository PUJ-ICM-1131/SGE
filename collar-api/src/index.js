import { createApp } from './app.js';
import { loadConfig } from './config.js';
import { JsonFileCollarRepository } from './repository/jsonFileCollarRepository.js';

let config;
try {
  config = loadConfig();
} catch (err) {
  console.error(`Configuración inválida: ${err.message}`);
  process.exit(1);
}

if (config.usingDevKey) {
  console.warn(`COLLAR_API_KEY no definida: usando la clave de desarrollo. No expongas así el servicio.`);
}

const repository = await JsonFileCollarRepository.open(config.dataFile);
const app = createApp({
  repository,
  apiKey: config.apiKey,
  trustProxy: config.trustProxy,
  rateLimitPerMinute: config.rateLimitPerMinute,
  log: (msg) => console.log(msg),
});

const server = app.listen(config.port, config.host, () => {
  console.log(
    `Proveedor de collares GPS en http://${config.host}:${config.port} ` +
      `(${config.production ? 'producción' : 'desarrollo'}, datos: ${config.dataFile})`,
  );
});

for (const signal of ['SIGINT', 'SIGTERM']) {
  process.on(signal, () => server.close(() => process.exit(0)));
}
