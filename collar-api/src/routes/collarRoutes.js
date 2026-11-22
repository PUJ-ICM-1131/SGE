import { Router } from 'express';

/** Rutas delgadas: leen la petición, llaman al servicio y responden. La lógica vive en CollarService. */
export function collarRoutes(service) {
  const router = Router();

  router.get('/', async (req, res) => {
    const items = await service.list(parseListFilter(req.query));
    res.json({ items, total: items.length });
  });

  router.post('/', async (req, res) => {
    const collar = await service.create(req.body);
    res.status(201).location(`${req.baseUrl}/${collar.id}`).json(collar);
  });

  router.get('/:id', async (req, res) => {
    res.json(await service.get(req.params.id));
  });

  router.patch('/:id', async (req, res) => {
    res.json(await service.update(req.params.id, req.body));
  });

  router.delete('/:id', async (req, res) => {
    await service.remove(req.params.id);
    res.status(204).end();
  });

  router.get('/:id/position', async (req, res) => {
    res.json(await service.positionOf(req.params.id));
  });

  return router;
}

export function positionRoutes(service) {
  const router = Router();

  // GET /v1/positions?farmId=...&animalIds=a,b,c — solo collares activos.
  router.get('/', async (req, res) => {
    const { farmId, animalIds } = parseListFilter(req.query);
    res.json(await service.positions({ farmId, animalIds }));
  });

  return router;
}

/** ?animalIds=a,b,c o ?animalIds=a&animalIds=b → ['a','b','c']. */
function parseListFilter(query) {
  const raw = [query.animalIds ?? []].flat().flatMap((v) => String(v).split(','));
  const animalIds = raw.map((v) => v.trim()).filter(Boolean);
  return {
    farmId: typeof query.farmId === 'string' && query.farmId ? query.farmId : undefined,
    status: typeof query.status === 'string' && query.status ? query.status.toUpperCase() : undefined,
    animalIds: animalIds.length ? animalIds : undefined,
  };
}
