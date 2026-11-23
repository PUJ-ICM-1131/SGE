/**
 * Lógica pura de las alertas de potreros (#73), sin Firebase ni red, para probarla sola.
 * Los potreros tienen el formato que guarda la app (#71): points = [{lat, lng}, ...] en orden,
 * sin repetir el primer punto.
 */

export const ALERT_OPENED = 'animal_outside';
export const ALERT_FORWARDED = 'animal_outside_forwarded';

/** Lecturas seguidas fuera de los potreros antes de avisar: evita falsas alarmas por ruido del GPS. */
export const CONSECUTIVE_READINGS = 2;

/** ¿El punto está dentro del polígono? Cruce de rayos, igual que PaddockGeometry.contains en la app. */
export function contains(points, { lat, lng }) {
  if (!Array.isArray(points) || points.length < 3) return false;
  let inside = false;
  for (let i = 0, j = points.length - 1; i < points.length; j = i++) {
    const a = points[i];
    const b = points[j];
    if ((a.lat > lat) !== (b.lat > lat)) {
      const crossLng = ((b.lng - a.lng) * (lat - a.lat)) / (b.lat - a.lat) + a.lng;
      if (lng < crossLng) inside = !inside;
    }
  }
  return inside;
}

export function isInsideAny(paddocks, position) {
  return paddocks.some((paddock) => contains(paddock.points, position));
}

/**
 * Aplica una lectura de posiciones de una finca.
 * - Una vaca está "fuera" cuando no está dentro de NINGÚN potrero de la finca.
 * - [outsideCounts]: animalId → lecturas seguidas fuera hasta ahora.
 * Devuelve las cuentas nuevas, las vacas que llevan [required] lecturas o más fuera ([wentOut],
 * con su posición) y las que están dentro ([inside]), para resolver sus alertas.
 */
export function evaluatePositions({ positions, paddocks, outsideCounts, required = CONSECUTIVE_READINGS }) {
  const counts = new Map();
  const wentOut = [];
  const inside = new Set();
  for (const position of positions) {
    if (!position.animalId) continue;
    if (isInsideAny(paddocks, position)) {
      inside.add(position.animalId);
      continue;
    }
    const count = (outsideCounts.get(position.animalId) ?? 0) + 1;
    counts.set(position.animalId, count);
    if (count >= required) wentOut.push(position);
  }
  return { counts, wentOut, inside };
}

/** Miembros activos con [role] ("OWNER" | "WORKER"). Sin isActive se toman como activos (#28). */
export function membersWithRole(members, role) {
  return members.filter((m) => m.role === role && m.isActive !== false).map((m) => m.id);
}

/**
 * Mensaje FCM de solo datos, como el de transacciones: la app arma la notificación con su canal
 * "Alertas del ganado" y al tocarla abre el detalle de la alerta.
 */
export function buildAlertMessage({ type, farmId, alertId, animalName, farmName }) {
  const who = animalName || 'Una vaca';
  const forwarded = type === ALERT_FORWARDED;
  const title = forwarded
    ? `Aviso del propietario: ${who} salió de los potreros`
    : `${who} salió de los potreros`;
  const body = (forwarded
    ? 'Ve a revisarla. Toca para ver dónde está'
    : 'Toca para ver dónde está y avisar a los trabajadores') + (farmName ? ` · ${farmName}` : '');

  return {
    data: { type, farmId: String(farmId), alertId: String(alertId), title, body },
    android: {
      priority: 'high',
      // Pasadas unas horas la vaca ya pudo volver: el aviso deja de servir.
      ttl: 6 * 60 * 60 * 1000,
    },
  };
}
