/**
 * Repositorio en memoria. Se usa en las pruebas y es la base del repositorio en archivo.
 * Devuelve copias para que nadie modifique el estado interno por accidente.
 */
export class InMemoryCollarRepository {
  constructor(initial = []) {
    this.collars = new Map(initial.map((c) => [c.id, structuredClone(c)]));
  }

  /** Filtros opcionales: farmId, status y animalIds (lista). Orden: más antiguo primero. */
  async list({ farmId, status, animalIds } = {}) {
    const wanted = animalIds?.length ? new Set(animalIds) : null;
    return [...this.collars.values()]
      .filter((c) => !farmId || c.farmId === farmId)
      .filter((c) => !status || c.status === status)
      .filter((c) => !wanted || wanted.has(c.animalId))
      .sort((a, b) => a.createdAt.localeCompare(b.createdAt))
      .map((c) => structuredClone(c));
  }

  async get(id) {
    const collar = this.collars.get(id);
    return collar ? structuredClone(collar) : null;
  }

  async findBySerial(serial) {
    return this.#findBy((c) => c.serial === serial);
  }

  async findByAnimalId(animalId) {
    return this.#findBy((c) => c.animalId === animalId);
  }

  async create(collar) {
    this.collars.set(collar.id, structuredClone(collar));
    await this.persist();
    return structuredClone(collar);
  }

  async update(id, changes) {
    const updated = { ...this.collars.get(id), ...structuredClone(changes) };
    this.collars.set(id, updated);
    await this.persist();
    return structuredClone(updated);
  }

  async delete(id) {
    this.collars.delete(id);
    await this.persist();
  }

  /** En memoria no hay nada que guardar; el repositorio en archivo lo sobrescribe. */
  async persist() {}

  #findBy(predicate) {
    const found = [...this.collars.values()].find(predicate);
    return found ? structuredClone(found) : null;
  }
}
