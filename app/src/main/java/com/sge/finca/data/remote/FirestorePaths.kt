package com.sge.finca.data.remote

/**
 * Estructura de Firestore (documentada en docs/5-modelo-datos/README.md):
 *
 * users/{uid}                                  perfil + farmId
 * inviteCodes/{code}                           farmId y nombre de la finca (#29, lectura pública)
 * farms/{farmId}                               nombre, dueño, código de invitación
 * farms/{farmId}/members/{uid}                 rol (OWNER/WORKER)
 * farms/{farmId}/transactions/{id}
 * farms/{farmId}/categories/{id}
 * farms/{farmId}/animalRecords/{id}
 * farms/{farmId}/contacts/{id}
 * farms/{farmId}/reminders/{id}
 * farms/{farmId}/inspectionRoutes/{id}
 * farms/{farmId}/inspectionRoutes/{id}/points/{pointId}
 * farms/{farmId}/paddocks/{id}                 potreros (#71); el notifier de alertas (#73) los lee
 * farms/{farmId}/alerts/{id}                   vacas fuera de los potreros (#73); las crea el notifier
 * farms/{farmId}/chats/{workerUid}             chat propietario ↔ trabajador (#74)
 * farms/{farmId}/chats/{workerUid}/messages/{id}
 */
object FirestorePaths {
    const val USERS = "users"
    const val FARMS = "farms"
    const val MEMBERS = "members"
    const val TRANSACTIONS = "transactions"
    const val CATEGORIES = "categories"
    const val ANIMAL_RECORDS = "animalRecords"
    const val CONTACTS = "contacts"
    const val REMINDERS = "reminders"
    const val INSPECTION_ROUTES = "inspectionRoutes"
    const val POINTS = "points"
    const val INVITE_CODES = "inviteCodes"
    const val PADDOCKS = "paddocks"
    const val ALERTS = "alerts"
    const val CHATS = "chats"
    const val MESSAGES = "messages"

    fun user(uid: String) = "$USERS/${requireId(uid)}"

    /** Índice público código → finca (#29): permite encontrar la finca antes de ser miembro. */
    fun inviteCode(code: String) = "$INVITE_CODES/${requireId(code)}"
    fun farm(farmId: String) = "$FARMS/${requireId(farmId)}"
    fun member(farmId: String, uid: String) = "${farm(farmId)}/$MEMBERS/${requireId(uid)}"
    fun transaction(farmId: String, id: String) = "${farm(farmId)}/$TRANSACTIONS/${requireId(id)}"
    fun category(farmId: String, id: String) = "${farm(farmId)}/$CATEGORIES/${requireId(id)}"
    fun animalRecord(farmId: String, id: String) = "${farm(farmId)}/$ANIMAL_RECORDS/${requireId(id)}"
    fun contact(farmId: String, id: String) = "${farm(farmId)}/$CONTACTS/${requireId(id)}"
    fun reminder(farmId: String, id: String) = "${farm(farmId)}/$REMINDERS/${requireId(id)}"
    fun inspectionRoute(farmId: String, id: String) =
        "${farm(farmId)}/$INSPECTION_ROUTES/${requireId(id)}"
    fun paddock(farmId: String, id: String) = "${farm(farmId)}/$PADDOCKS/${requireId(id)}"
    fun alert(farmId: String, id: String) = "${farm(farmId)}/$ALERTS/${requireId(id)}"
    fun chat(farmId: String, workerUid: String) = "${farm(farmId)}/$CHATS/${requireId(workerUid)}"
    fun chatMessages(farmId: String, workerUid: String) = "${chat(farmId, workerUid)}/$MESSAGES"
    /** Subcolección de una finca, p. ej. farms/{farmId}/transactions. */
    fun farmCollection(farmId: String, collection: String) = "${farm(farmId)}/${requireId(collection)}"

    /** Documento de una subcolección de la finca, p. ej. farms/{farmId}/transactions/{id}. */
    fun farmDocument(farmId: String, collection: String, id: String) =
        "${farmCollection(farmId, collection)}/${requireId(id)}"

    fun routePoint(farmId: String, routeId: String, pointId: String) =
        "${inspectionRoute(farmId, routeId)}/$POINTS/${requireId(pointId)}"

    /** Un id vacío o con "/" rompería la ruta (Firestore lanza excepción o escribe en otro lado). */
    private fun requireId(id: String): String {
        require(id.isNotBlank() && !id.contains('/')) { "Id de Firestore inválido: '$id'" }
        return id
    }
}
