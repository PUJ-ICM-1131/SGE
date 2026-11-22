import { badRequest } from './errors.js';

export const COLLAR_STATUS = Object.freeze({ ACTIVE: 'ACTIVE', INACTIVE: 'INACTIVE' });
export const RADIUS_LIMITS = Object.freeze({ min: 10, max: 500, default: 40 });

const EDITABLE_FIELDS = ['serial', 'farmId', 'animalId', 'animalName', 'anchor', 'radiusMeters', 'status'];

const isPlainObject = (v) => typeof v === 'object' && v !== null && !Array.isArray(v);
const isText = (v, max) => typeof v === 'string' && v.trim().length > 0 && v.length <= max;
const isLat = (v) => typeof v === 'number' && Number.isFinite(v) && v >= -90 && v <= 90;
const isLng = (v) => typeof v === 'number' && Number.isFinite(v) && v >= -180 && v <= 180;

/** Reglas por campo. Cada una devuelve un mensaje de error o null. */
const RULES = {
  serial: (v) => (isText(v, 40) && /^[A-Za-z0-9-]+$/.test(v) ? null : 'serial: 1–40 letras, números o guiones'),
  farmId: (v) => (isText(v, 128) ? null : 'farmId es obligatorio (máx. 128 caracteres)'),
  animalId: (v) => (v === null || isText(v, 128) ? null : 'animalId debe ser texto (máx. 128) o null'),
  animalName: (v) => (v === null || (typeof v === 'string' && v.length <= 80) ? null : 'animalName: máx. 80 caracteres'),
  anchor: (v) =>
    isPlainObject(v) && isLat(v.lat) && isLng(v.lng) ? null : 'anchor debe ser {lat, lng} con coordenadas válidas',
  radiusMeters: (v) =>
    typeof v === 'number' && v >= RADIUS_LIMITS.min && v <= RADIUS_LIMITS.max
      ? null
      : `radiusMeters debe estar entre ${RADIUS_LIMITS.min} y ${RADIUS_LIMITS.max}`,
  status: (v) => (Object.values(COLLAR_STATUS).includes(v) ? null : 'status debe ser ACTIVE o INACTIVE'),
};

function checkFields(body, fields) {
  const errors = fields
    .filter((field) => field in body)
    .map((field) => RULES[field](body[field]))
    .filter(Boolean);
  if (errors.length > 0) throw badRequest('Datos inválidos', errors);
}

function rejectUnknownFields(body) {
  const unknown = Object.keys(body).filter((k) => !EDITABLE_FIELDS.includes(k));
  if (unknown.length > 0) throw badRequest(`Campos no permitidos: ${unknown.join(', ')}`);
}

const normalizeAnchor = ({ lat, lng }) => ({ lat, lng });

/** Valida el cuerpo de POST /v1/collars y devuelve los datos con valores por defecto. */
export function parseCreateCollar(body) {
  if (!isPlainObject(body)) throw badRequest('El cuerpo debe ser un objeto JSON');
  rejectUnknownFields(body);
  const missing = ['serial', 'farmId', 'anchor'].filter((f) => body[f] === undefined);
  if (missing.length > 0) throw badRequest(`Faltan campos obligatorios: ${missing.join(', ')}`);
  checkFields(body, EDITABLE_FIELDS);

  return {
    serial: body.serial.trim().toUpperCase(),
    farmId: body.farmId.trim(),
    animalId: body.animalId?.trim() || null,
    animalName: body.animalName?.trim() || null,
    anchor: normalizeAnchor(body.anchor),
    radiusMeters: body.radiusMeters ?? RADIUS_LIMITS.default,
    status: body.status ?? COLLAR_STATUS.ACTIVE,
  };
}

/** Valida el cuerpo de PATCH /v1/collars/:id; solo devuelve los campos enviados. */
export function parsePatchCollar(body) {
  if (!isPlainObject(body)) throw badRequest('El cuerpo debe ser un objeto JSON');
  rejectUnknownFields(body);
  if (Object.keys(body).length === 0) throw badRequest('No hay campos para actualizar');
  checkFields(body, EDITABLE_FIELDS);

  const changes = { ...body };
  if ('serial' in changes) changes.serial = changes.serial.trim().toUpperCase();
  if ('farmId' in changes) changes.farmId = changes.farmId.trim();
  if ('animalId' in changes) changes.animalId = changes.animalId?.trim() || null;
  if ('animalName' in changes) changes.animalName = changes.animalName?.trim() || null;
  if ('anchor' in changes) changes.anchor = normalizeAnchor(changes.anchor);
  return changes;
}
