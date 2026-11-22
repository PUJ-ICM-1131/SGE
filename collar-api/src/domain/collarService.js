import { randomUUID } from 'node:crypto';
import { conflict, notFound } from './errors.js';
import { COLLAR_STATUS, parseCreateCollar, parsePatchCollar } from './collarValidation.js';
import { simulatePosition } from './simulation.js';

/**
 * Reglas de negocio de los collares. No sabe nada de HTTP ni de cómo se guardan:
 * recibe el repositorio y el reloj por constructor (fácil de probar).
 */
export class CollarService {
  /** @param {{repository: import('../repository/inMemoryCollarRepository.js').InMemoryCollarRepository, now?: () => number}} deps */
  constructor({ repository, now = Date.now }) {
    this.repository = repository;
    this.now = now;
  }

  list(filter) {
    return this.repository.list(filter);
  }

  async get(id) {
    const collar = await this.repository.get(id);
    if (!collar) throw notFound(`No existe el collar ${id}`);
    return collar;
  }

  async create(body) {
    const data = parseCreateCollar(body);
    await this.#assertUnique(data);
    const timestamp = new Date(this.now()).toISOString();
    return this.repository.create({
      id: randomUUID(),
      ...data,
      createdAt: timestamp,
      updatedAt: timestamp,
    });
  }

  async update(id, body) {
    const changes = parsePatchCollar(body);
    const current = await this.get(id);
    await this.#assertUnique({ ...current, ...changes }, id);
    return this.repository.update(id, { ...changes, updatedAt: new Date(this.now()).toISOString() });
  }

  async remove(id) {
    await this.get(id);
    await this.repository.delete(id);
  }

  /** Posición simulada de un collar. Un collar inactivo no reporta (409). */
  async positionOf(id) {
    const collar = await this.get(id);
    if (collar.status !== COLLAR_STATUS.ACTIVE) throw conflict(`El collar ${collar.serial} está inactivo`);
    return toReading(collar, this.now());
  }

  /** Posiciones de los collares activos que cumplen el filtro (lo que consulta la app cada pocos segundos). */
  async positions(filter) {
    const generatedAt = this.now();
    const collars = await this.repository.list({ ...filter, status: COLLAR_STATUS.ACTIVE });
    return { generatedAt, positions: collars.map((c) => toReading(c, generatedAt)) };
  }

  /** Un serial es único; un animal solo puede llevar un collar. */
  async #assertUnique({ serial, animalId }, ignoreId) {
    const sameSerial = await this.repository.findBySerial(serial);
    if (sameSerial && sameSerial.id !== ignoreId) throw conflict(`Ya existe un collar con serial ${serial}`);
    if (animalId) {
      const sameAnimal = await this.repository.findByAnimalId(animalId);
      if (sameAnimal && sameAnimal.id !== ignoreId) {
        throw conflict(`El animal ${animalId} ya tiene el collar ${sameAnimal.serial}`);
      }
    }
  }
}

function toReading(collar, nowMs) {
  return {
    collarId: collar.id,
    serial: collar.serial,
    animalId: collar.animalId,
    ...simulatePosition(collar, nowMs),
  };
}
