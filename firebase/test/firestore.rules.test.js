// Pruebas de las Security Rules (issue #28). Corren contra el emulador de Firestore:
//   cd firebase && npm install && npm test
// Requiere Java 11+ (el emulador es un .jar).

const { test, before, after, beforeEach } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const {
  initializeTestEnvironment,
  assertSucceeds,
  assertFails,
} = require('@firebase/rules-unit-testing');
const {
  doc, getDoc, setDoc, updateDoc, deleteDoc, writeBatch, collection, getDocs,
} = require('firebase/firestore');

const FARM = 'finca-1';
const OTHER_FARM = 'finca-2';
const OWNER = 'uid-owner';
const WORKER = 'uid-worker';
const INACTIVE = 'uid-inactive';
const STRANGER = 'uid-stranger';
const INVITE = 'ABC234';
const NEWBIE = 'uid-newbie';

let env;

before(async () => {
  env = await initializeTestEnvironment({
    projectId: 'demo-sge',
    firestore: {
      rules: fs.readFileSync(path.join(__dirname, '..', 'firestore.rules'), 'utf8'),
    },
  });
});

after(async () => {
  await env.cleanup();
});

// Datos de partida, escritos sin reglas.
beforeEach(async () => {
  await env.clearFirestore();
  await env.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await setDoc(doc(db, `farms/${FARM}`), { name: 'La Esperanza', ownerUid: OWNER, inviteCode: INVITE, createdAt: 1 });
    await setDoc(doc(db, `inviteCodes/${INVITE}`), { farmId: FARM, farmName: 'La Esperanza' });
    await setDoc(doc(db, `farms/${FARM}/members/${OWNER}`), { role: 'OWNER', isActive: true, joinedAt: 1 });
    await setDoc(doc(db, `farms/${FARM}/members/${WORKER}`), { role: 'WORKER', isActive: true, joinedAt: 2 });
    await setDoc(doc(db, `farms/${FARM}/members/${INACTIVE}`), { role: 'WORKER', isActive: false, joinedAt: 3 });
    await setDoc(doc(db, `farms/${FARM}/transactions/t1`), tx(WORKER));
    await setDoc(doc(db, `farms/${FARM}/categories/c1`), { name: 'Venta de leche', type: 'INCOME', isDefault: true });
    await setDoc(doc(db, `farms/${OTHER_FARM}`), { name: 'Otra', ownerUid: STRANGER, inviteCode: 'ZZZ999', createdAt: 1 });
    await setDoc(doc(db, `farms/${OTHER_FARM}/members/${STRANGER}`), { role: 'OWNER', isActive: true, joinedAt: 1 });
    await setDoc(doc(db, `farms/${OTHER_FARM}/transactions/x1`), tx(STRANGER));
  });
});

function tx(createdBy, extra = {}) {
  return {
    type: 'EXPENSE', amountCents: 2500000, categoryId: 'c1', categoryName: 'Sanidad',
    dateEpochDay: 20778, createdBy, createdAt: 10, updatedAt: 10, ...extra,
  };
}

const db = (uid) => (uid ? env.authenticatedContext(uid) : env.unauthenticatedContext()).firestore();

// ---------- Sin sesión ----------

test('un usuario sin sesión no lee datos de ninguna finca', async () => {
  await assertFails(getDoc(doc(db(null), `farms/${FARM}`)));
  await assertFails(getDoc(doc(db(null), `farms/${FARM}/transactions/t1`)));
});

// ---------- Aislamiento entre fincas ----------

test('un miembro no accede a datos de otra finca', async () => {
  await assertFails(getDoc(doc(db(OWNER), `farms/${OTHER_FARM}`)));
  await assertFails(getDoc(doc(db(OWNER), `farms/${OTHER_FARM}/transactions/x1`)));
  await assertFails(setDoc(doc(db(WORKER), `farms/${OTHER_FARM}/transactions/x2`), tx(WORKER)));
});

// ---------- Transacciones ----------

test('el trabajador registra transacciones a su nombre', async () => {
  await assertSucceeds(setDoc(doc(db(WORKER), `farms/${FARM}/transactions/t2`), tx(WORKER)));
  await assertFails(setDoc(doc(db(WORKER), `farms/${FARM}/transactions/t3`), tx(OWNER)));
});

test('el trabajador no edita ni elimina transacciones', async () => {
  await assertFails(updateDoc(doc(db(WORKER), `farms/${FARM}/transactions/t1`), { amountCents: 1 }));
  await assertFails(deleteDoc(doc(db(WORKER), `farms/${FARM}/transactions/t1`)));
});

test('el dueño edita y elimina, pero no cambia el creador', async () => {
  await assertSucceeds(updateDoc(doc(db(OWNER), `farms/${FARM}/transactions/t1`), { amountCents: 100, updatedAt: 11 }));
  await assertFails(updateDoc(doc(db(OWNER), `farms/${FARM}/transactions/t1`), { createdBy: OWNER }));
  await assertSucceeds(deleteDoc(doc(db(OWNER), `farms/${FARM}/transactions/t1`)));
});

test('montos no válidos se rechazan', async () => {
  await assertFails(setDoc(doc(db(OWNER), `farms/${FARM}/transactions/t4`), tx(OWNER, { amountCents: 0 })));
  await assertFails(setDoc(doc(db(OWNER), `farms/${FARM}/transactions/t5`), tx(OWNER, { amountCents: 10.5 })));
  await assertFails(setDoc(doc(db(OWNER), `farms/${FARM}/transactions/t6`), tx(OWNER, { type: 'OTRO' })));
});

// ---------- Categorías ----------

test('solo el dueño gestiona categorías; el trabajador las lee', async () => {
  await assertSucceeds(getDoc(doc(db(WORKER), `farms/${FARM}/categories/c1`)));
  await assertFails(setDoc(doc(db(WORKER), `farms/${FARM}/categories/c2`), { name: 'X', type: 'EXPENSE', isDefault: false }));
  await assertSucceeds(setDoc(doc(db(OWNER), `farms/${FARM}/categories/c2`), { name: 'X', type: 'EXPENSE', isDefault: false }));
});

// ---------- Roles ----------

test('el trabajador no puede cambiarse el rol a OWNER ni reactivarse', async () => {
  await assertFails(updateDoc(doc(db(WORKER), `farms/${FARM}/members/${WORKER}`), { role: 'OWNER' }));
  await assertFails(updateDoc(doc(db(INACTIVE), `farms/${FARM}/members/${INACTIVE}`), { isActive: true }));
  // Sí puede editar sus datos personales.
  await assertSucceeds(updateDoc(doc(db(WORKER), `farms/${FARM}/members/${WORKER}`), { displayName: 'Pedro' }));
});

test('el trabajador no puede escribir la membresía de otro', async () => {
  await assertFails(setDoc(doc(db(WORKER), `farms/${FARM}/members/${STRANGER}`), { role: 'WORKER', isActive: true, joinedAt: 5 }));
});

test('el dueño cambia roles y da de baja, pero no se quita el rol a sí mismo', async () => {
  await assertSucceeds(updateDoc(doc(db(OWNER), `farms/${FARM}/members/${WORKER}`), { isActive: false }));
  await assertFails(updateDoc(doc(db(OWNER), `farms/${FARM}/members/${OWNER}`), { role: 'WORKER' }));
});

test('un miembro inactivo pierde el acceso', async () => {
  await assertFails(getDoc(doc(db(INACTIVE), `farms/${FARM}/transactions/t1`)));
  await assertFails(setDoc(doc(db(INACTIVE), `farms/${FARM}/transactions/t7`), tx(INACTIVE)));
});

// ---------- Creación de finca y unión con código ----------

test('crear finca: farm + membresía OWNER + perfil en un batch', async () => {
  const d = db(STRANGER);
  const batch = writeBatch(d);
  batch.set(doc(d, 'farms/nueva'), { name: 'Nueva', ownerUid: STRANGER, inviteCode: 'QWE345', createdAt: 1 });
  batch.set(doc(d, `farms/nueva/members/${STRANGER}`), { role: 'OWNER', isActive: true, joinedAt: 1 });
  batch.set(doc(d, `users/${STRANGER}`), { name: 'S', farmId: 'nueva' }, { merge: true });
  batch.set(doc(d, 'inviteCodes/QWE345'), { farmId: 'nueva', farmName: 'Nueva' });
  await assertSucceeds(batch.commit());
});

test('no se puede declarar dueño de una finca ajena', async () => {
  await assertFails(setDoc(doc(db(STRANGER), `farms/${FARM}/members/${STRANGER}`), { role: 'OWNER', isActive: true, joinedAt: 1 }));
  await assertFails(updateDoc(doc(db(STRANGER), `farms/${FARM}`), { ownerUid: STRANGER }));
});

test('unirse como trabajador exige el código de invitación correcto (#29)', async () => {
  const ref = (uid) => doc(db(uid), `farms/${FARM}/members/${uid}`);
  await assertFails(setDoc(ref('nuevo-1'), { role: 'WORKER', isActive: true, joinedAt: 9, inviteCode: 'MALO11' }));
  await assertSucceeds(setDoc(ref('nuevo-2'), { role: 'WORKER', isActive: true, joinedAt: 9, inviteCode: INVITE }));
});

test('códigos: con sesión se consulta uno conocido, pero no se listan (#29)', async () => {
  await assertSucceeds(getDoc(doc(db(NEWBIE), `inviteCodes/${INVITE}`)));
  await assertFails(getDoc(doc(db(null), `inviteCodes/${INVITE}`)));
  await assertFails(getDocs(collection(db(NEWBIE), 'inviteCodes')));
});

test('códigos: solo el dueño publica el código vigente de su finca (#29)', async () => {
  // Un extraño no puede apuntar un código a una finca ajena.
  await assertFails(setDoc(doc(db(STRANGER), 'inviteCodes/HACK-2345'), { farmId: FARM, farmName: 'x' }));
  // El dueño tampoco publica un código que no es el vigente.
  await assertFails(setDoc(doc(db(OWNER), 'inviteCodes/OTRO-2345'), { farmId: FARM, farmName: 'x' }));
  // Nadie lo modifica ni lo borra salvo el dueño.
  await assertFails(updateDoc(doc(db(OWNER), `inviteCodes/${INVITE}`), { farmName: 'Otra' }));
  await assertFails(deleteDoc(doc(db(WORKER), `inviteCodes/${INVITE}`)));
  // Finca anterior al #29 sin índice: el dueño publica su código vigente.
  await env.withSecurityRulesDisabled((ctx) => deleteDoc(doc(ctx.firestore(), `inviteCodes/${INVITE}`)));
  await assertSucceeds(setDoc(doc(db(OWNER), `inviteCodes/${INVITE}`), { farmId: FARM, farmName: 'La Esperanza' }));
});

test('unirse con código: busca la finca y queda como WORKER (#29)', async () => {
  const d = db(NEWBIE);
  const index = await getDoc(doc(d, `inviteCodes/${INVITE}`));
  const farmId = index.data().farmId;
  const batch = writeBatch(d);
  batch.set(doc(d, `farms/${farmId}/members/${NEWBIE}`), {
    role: 'WORKER', isActive: true, joinedAt: 9, displayName: 'Nuevo', inviteCode: INVITE,
  });
  batch.set(doc(d, `users/${NEWBIE}`), { farmId, role: 'WORKER' }, { merge: true });
  await assertSucceeds(batch.commit());
  // Ya es miembro: ve la finca.
  await assertSucceeds(getDoc(doc(d, `farms/${FARM}`)));
});

test('regenerar: el código anterior deja de servir (#29)', async () => {
  const NEW_CODE = 'NUEV-2345';
  const owner = db(OWNER);
  const batch = writeBatch(owner);
  batch.update(doc(owner, `farms/${FARM}`), { inviteCode: NEW_CODE });
  batch.delete(doc(owner, `inviteCodes/${INVITE}`));
  batch.set(doc(owner, `inviteCodes/${NEW_CODE}`), { farmId: FARM, farmName: 'La Esperanza' });
  await assertSucceeds(batch.commit());

  const d = db(NEWBIE);
  // El código anterior ya no está en el índice.
  assert.equal((await getDoc(doc(d, `inviteCodes/${INVITE}`))).exists(), false);
  await assertFails(setDoc(doc(d, `farms/${FARM}/members/${NEWBIE}`), { role: 'WORKER', isActive: true, joinedAt: 9, inviteCode: INVITE }));
  await assertSucceeds(setDoc(doc(d, `farms/${FARM}/members/${NEWBIE}`), { role: 'WORKER', isActive: true, joinedAt: 9, inviteCode: NEW_CODE }));
});

test('el dueño no puede unirse a su propia finca como trabajador', async () => {
  await assertFails(setDoc(doc(db(OWNER), `farms/${FARM}/members/${OWNER}`), { role: 'WORKER', isActive: true, joinedAt: 9, inviteCode: INVITE }));
});

// ---------- Datos operativos y perfiles ----------

test('el trabajador registra animales, inspecciones, contactos y recordatorios', async () => {
  const d = db(WORKER);
  await assertSucceeds(setDoc(doc(d, `farms/${FARM}/animalRecords/a1`), { name: 'Lola' }));
  await assertSucceeds(setDoc(doc(d, `farms/${FARM}/inspectionRoutes/r1`), { userId: WORKER }));
  await assertSucceeds(setDoc(doc(d, `farms/${FARM}/inspectionRoutes/r1/points/p1`), { order: 0 }));
  await assertSucceeds(setDoc(doc(d, `farms/${FARM}/contacts/k1`), { name: 'Veterinario' }));
  await assertSucceeds(setDoc(doc(d, `farms/${FARM}/reminders/m1`), { title: 'Vacunar', createdBy: WORKER }));
});

test('recordatorios: elimina el dueño o quien lo creó, y nadie se apropia de uno ajeno (#31)', async () => {
  await env.withSecurityRulesDisabled(async (ctx) => {
    const d = ctx.firestore();
    await setDoc(doc(d, `farms/${FARM}/reminders/del-dueño`), { title: 'Pagar luz', createdBy: OWNER });
    await setDoc(doc(d, `farms/${FARM}/reminders/del-trab`), { title: 'Comprar sal', createdBy: WORKER });
    await setDoc(doc(d, `farms/${FARM}/reminders/otro`), { title: 'Revisar cerca', createdBy: OWNER });
  });
  const worker = db(WORKER);
  // No crea a nombre de otro.
  await assertFails(setDoc(doc(worker, `farms/${FARM}/reminders/m2`), { title: 'X', createdBy: OWNER }));
  // Completa o edita uno ajeno, pero no lo borra ni cambia su autor.
  await assertSucceeds(updateDoc(doc(worker, `farms/${FARM}/reminders/del-dueño`), { isCompleted: true }));
  await assertFails(updateDoc(doc(worker, `farms/${FARM}/reminders/del-dueño`), { createdBy: WORKER }));
  await assertFails(deleteDoc(doc(worker, `farms/${FARM}/reminders/del-dueño`)));
  // Borra el suyo; el dueño borra cualquiera.
  await assertSucceeds(deleteDoc(doc(worker, `farms/${FARM}/reminders/del-trab`)));
  await assertSucceeds(deleteDoc(doc(db(OWNER), `farms/${FARM}/reminders/otro`)));
});

test('cada usuario solo ve su propio perfil', async () => {
  await assertSucceeds(setDoc(doc(db(WORKER), `users/${WORKER}`), { name: 'W' }));
  await assertFails(getDoc(doc(db(WORKER), `users/${OWNER}`)));
});
