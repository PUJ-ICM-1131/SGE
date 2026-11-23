/**
 * Firestore y FCM falsos, en memoria, para probar los vigilantes de alertas (#73) sin
 * credenciales. Solo implementa lo que usa el notifier.
 */
export function fakeFirebase(initialDocs = {}) {
  const docs = new Map(Object.entries(initialDocs));
  const sent = [];
  let autoId = 0;

  const snapshotOf = (path) => {
    const data = docs.get(path);
    return {
      id: path.split('/').at(-1),
      exists: data !== undefined,
      get: (field) => data?.[field],
      data: () => data,
    };
  };

  const docRef = (path) => ({
    id: path.split('/').at(-1),
    path,
    async get() {
      return snapshotOf(path);
    },
    async set(data) {
      docs.set(path, { ...data });
    },
    async update(change) {
      docs.set(path, { ...docs.get(path), ...change });
    },
    async create(data) {
      if (docs.has(path)) throw Object.assign(new Error('ya existe'), { code: 6 });
      docs.set(path, { ...data });
    },
    collection: (name) => collectionRef(`${path}/${name}`),
  });

  const childrenOf = (path, matches) =>
    [...docs.keys()]
      .filter((p) => p.startsWith(`${path}/`) && !p.slice(path.length + 1).includes('/'))
      .filter((p) => matches(docs.get(p)))
      .map(snapshotOf);

  const collectionRef = (path) => ({
    doc: (id) => docRef(`${path}/${id ?? `auto-${++autoId}`}`),
    where: (field, op, value) => ({
      async get() {
        return { docs: childrenOf(path, (data) => data[field] === value) };
      },
    }),
    async get() {
      return { docs: childrenOf(path, () => true) };
    },
  });

  const db = {
    collection: (name) => collectionRef(name),
    async getAll(...refs) {
      return Promise.all(refs.map((ref) => ref.get()));
    },
  };

  const messaging = {
    async sendEachForMulticast(message) {
      sent.push(message);
      return {
        responses: message.tokens.map(() => ({ success: true })),
        successCount: message.tokens.length,
        failureCount: 0,
      };
    },
  };

  /** Documento como los que entrega un listener de collectionGroup. */
  const listenedDoc = (path) => {
    const parts = path.split('/');
    return {
      ...snapshotOf(path),
      ref: { path, parent: { parent: { id: parts.at(-3) } } },
    };
  };

  return { db, messaging, docs, sent, listenedDoc };
}
