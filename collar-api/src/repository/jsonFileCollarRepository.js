import { mkdir, readFile, rename, writeFile } from 'node:fs/promises';
import { dirname } from 'node:path';
import { InMemoryCollarRepository } from './inMemoryCollarRepository.js';

/**
 * Guarda los collares en un archivo JSON para que sobrevivan a un reinicio.
 * Suficiente para un proveedor simulado; para producción se cambiaría por una base de datos
 * implementando los mismos métodos.
 *
 * Escritura atómica: se escribe un .tmp y se renombra, así un corte no deja el archivo a medias.
 * Las escrituras se encadenan para que dos peticiones simultáneas no se pisen.
 */
export class JsonFileCollarRepository extends InMemoryCollarRepository {
  #pendingWrite = Promise.resolve();

  constructor(filePath, initial) {
    super(initial);
    this.filePath = filePath;
  }

  static async open(filePath) {
    let initial = [];
    try {
      initial = JSON.parse(await readFile(filePath, 'utf8')).collars ?? [];
    } catch (err) {
      if (err.code !== 'ENOENT') throw new Error(`No se pudo leer ${filePath}: ${err.message}`);
    }
    return new JsonFileCollarRepository(filePath, initial);
  }

  async persist() {
    const snapshot = JSON.stringify({ collars: [...this.collars.values()] }, null, 2);
    const write = async () => {
      await mkdir(dirname(this.filePath), { recursive: true });
      const tmp = `${this.filePath}.tmp`;
      await writeFile(tmp, snapshot, 'utf8');
      await rename(tmp, this.filePath);
    };
    this.#pendingWrite = this.#pendingWrite.then(write, write);
    return this.#pendingWrite;
  }
}
