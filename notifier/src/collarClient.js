/**
 * Cliente mínimo de collar-api (#52) para las alertas de potreros (#73). Corre en el mismo LXC,
 * así que habla con la API por loopback, sin pasar por Tailscale Funnel.
 */
export function createPositionsClient({ url, apiKey, fetchImpl = globalThis.fetch, timeoutMs = 5_000 }) {
  /** Posiciones actuales de los collares activos de [farmId]. */
  return async function fetchPositions(farmId) {
    const response = await fetchImpl(`${url}/v1/positions?farmId=${encodeURIComponent(farmId)}`, {
      headers: { 'X-Api-Key': apiKey },
      signal: AbortSignal.timeout(timeoutMs),
    });
    if (!response.ok) throw new Error(`collar-api respondió ${response.status} para la finca ${farmId}`);
    const body = await response.json();
    return Array.isArray(body.positions) ? body.positions : [];
  };
}
