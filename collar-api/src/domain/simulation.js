/**
 * Simulación determinista del movimiento de una vaca con collar GPS.
 *
 * La posición es una función pura de (id del collar, anclaje, radio, instante):
 *  - no hace falta guardar el recorrido, solo la configuración del collar;
 *  - dos celulares que consultan la misma vaca ven exactamente el mismo punto.
 *
 * Modelo: la vaca recorre una curva de Lissajous suave alrededor del anclaje (el centro
 * del potrero) sin pasar de radiusMeters. El avance sobre la curva es
 * p(t) = t − (T/2π)·sin(2πt/T + φ), cuya derivada 1 − cos(·) va de 0 a 2: la vaca se
 * detiene a pastar y luego camina.
 */

export const METERS_PER_DEGREE = 111_320;

/** Hash FNV-1a de 32 bits: convierte el id en una semilla estable. */
function hashString(value) {
  let hash = 0x811c9dc5;
  for (let i = 0; i < value.length; i++) {
    hash ^= value.charCodeAt(i);
    hash = Math.imul(hash, 0x01000193);
  }
  return hash >>> 0;
}

/** Generador pseudoaleatorio mulberry32: mismos números para la misma semilla. */
function seededRandom(seed) {
  let state = seed;
  return () => {
    state = (state + 0x6d2b79f5) >>> 0;
    let t = state;
    t = Math.imul(t ^ (t >>> 15), t | 1);
    t ^= t + Math.imul(t ^ (t >>> 7), t | 61);
    return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
  };
}

/** Parámetros de movimiento propios de cada collar (no dependen del radio). */
function movementFor(collarId) {
  const rnd = seededRandom(hashString(collarId));
  const between = (min, max) => min + (max - min) * rnd();
  return {
    a1: between(0.012, 0.025),
    a2: between(0.025, 0.045),
    b1: between(0.012, 0.025),
    b2: between(0.025, 0.045),
    phases: [rnd(), rnd(), rnd(), rnd(), rnd()].map((x) => x * 2 * Math.PI),
    grazePeriod: between(60, 140), // segundos entre pausas para pastar
    batteryOffset: between(0, 60),
  };
}

/** Desplazamiento en metros (este, norte) respecto al anclaje en el instante t (segundos). */
function offsetMeters(movement, amplitude, t) {
  const { a1, a2, b1, b2, phases, grazePeriod } = movement;
  const w = (2 * Math.PI) / grazePeriod;
  const p = t - Math.sin(w * t + phases[4]) / w;
  const east = amplitude * (0.65 * Math.sin(a1 * p + phases[0]) + 0.35 * Math.sin(a2 * p + phases[1]));
  const north = amplitude * (0.65 * Math.cos(b1 * p + phases[2]) + 0.35 * Math.sin(b2 * p + phases[3]));
  return { east, north };
}

const round = (value, decimals) => Number(value.toFixed(decimals));

/**
 * Lectura simulada del collar en el instante [nowMs].
 * @param {{id: string, anchor: {lat: number, lng: number}, radiusMeters: number}} collar
 * @param {number} nowMs epoch en milisegundos
 */
export function simulatePosition(collar, nowMs) {
  const movement = movementFor(collar.id);
  // Este y norte llegan cada uno hasta la amplitud: la distancia máxima es amplitud·√2.
  const amplitude = collar.radiusMeters / Math.SQRT2;
  const t = nowMs / 1000;
  const current = offsetMeters(movement, amplitude, t);
  const previous = offsetMeters(movement, amplitude, t - 1);

  const dEast = current.east - previous.east;
  const dNorth = current.north - previous.north;
  const { lat: anchorLat, lng: anchorLng } = collar.anchor;

  return {
    lat: round(anchorLat + current.north / METERS_PER_DEGREE, 7),
    lng: round(anchorLng + current.east / (METERS_PER_DEGREE * Math.cos((anchorLat * Math.PI) / 180)), 7),
    speedMps: round(Math.hypot(dEast, dNorth), 2), // metros recorridos en el último segundo
    headingDeg: round((((Math.atan2(dEast, dNorth) * 180) / Math.PI) + 360) % 360, 1),
    batteryPercent: Math.round(100 - ((t / 600 + movement.batteryOffset) % 60)),
    timestamp: nowMs,
  };
}
