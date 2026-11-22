/**
 * Lógica pura (sin Firebase): decide si una transacción se notifica, a quién y con qué texto.
 * Separada para probarla sin credenciales ni red.
 */

/** 5_000_000 centavos → "$50.000" (pesos colombianos, sin decimales), igual que la app. */
export function formatCop(cents) {
  const pesos = Math.trunc(Math.abs(cents) / 100);
  const grouped = String(pesos).replace(/\B(?=(\d{3})+(?!\d))/g, '.');
  return `${cents < 0 ? '-' : ''}$${grouped}`;
}

/**
 * ¿Vale la pena avisar? Se ignoran las transacciones muy viejas: por ejemplo, las que llegan
 * en la carga inicial al arrancar el servicio o tras días sin conexión.
 */
export function isRecent(transaction, nowMs, maxAgeMinutes) {
  const createdAt = Number(transaction.createdAt);
  if (!Number.isFinite(createdAt)) return false;
  return nowMs - createdAt <= maxAgeMinutes * 60_000;
}

/**
 * Quiénes reciben el aviso: los miembros de la finca menos quien la registró
 * (a menos que [notifyAuthor] esté activo, útil para la demo con un solo celular).
 */
export function recipientUids(memberUids, authorUid, notifyAuthor) {
  return [...new Set(memberUids)].filter((uid) => notifyAuthor || uid !== authorUid);
}

/**
 * Mensaje FCM de solo datos (sin bloque "notification"): así la app arma la notificación
 * con su canal "Movimientos" y al tocarla abre el detalle, también en segundo plano.
 * FCM exige que todos los valores de "data" sean strings.
 */
export function buildDataMessage({ farmId, transactionId, transaction, farmName }) {
  const isIncome = transaction.type === 'INCOME';
  const amount = `${isIncome ? '+' : '-'}${formatCop(Number(transaction.amountCents) || 0)}`;
  const who = transaction.createdByName || 'Alguien';
  const title = `${isIncome ? 'Nuevo ingreso' : 'Nuevo egreso'}: ${amount}`;
  const body = `${who} registró ${amount} en ${transaction.categoryName || 'sin categoría'}` +
    (farmName ? ` · ${farmName}` : '');

  return {
    data: {
      type: 'transaction_created',
      farmId: String(farmId),
      transactionId: String(transactionId),
      title,
      body,
    },
    android: {
      // Alta prioridad: Android entrega el mensaje aunque el celular esté en reposo.
      priority: 'high',
      // Si el celular está apagado más de un día, el aviso ya no sirve.
      ttl: 24 * 60 * 60 * 1000,
    },
  };
}

/** Errores de FCM que significan "este token ya no sirve, bórralo". */
export function isDeadTokenError(errorCode) {
  return (
    errorCode === 'messaging/registration-token-not-registered' ||
    errorCode === 'messaging/invalid-registration-token'
  );
  // Ojo: 'messaging/invalid-argument' NO va aquí; también lo produce un mensaje mal armado
  // y borraría tokens buenos.
}
