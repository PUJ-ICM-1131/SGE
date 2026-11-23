package com.sge.finca.platform.location

/** Resultado de buscar la posición del usuario para centrar un mapa (#59). */
sealed interface CenterOnMeResult {
    /** La ubicación del celular está apagada. */
    data object LocationOff : CenterOnMeResult

    /** La ubicación está encendida, pero no llegó una posición a tiempo. */
    data object NoSignal : CenterOnMeResult

    data class Located(val location: LocationState) : CenterOnMeResult
}
