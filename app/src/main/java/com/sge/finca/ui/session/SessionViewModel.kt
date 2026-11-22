package com.sge.finca.ui.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sge.finca.domain.model.FarmRole
import com.sge.finca.domain.model.Permission
import com.sge.finca.domain.model.Session
import com.sge.finca.domain.repository.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * Sesión y rol del usuario para la UI (issue #28).
 *
 * AppNavigation crea una sola instancia (a nivel de Activity) y la usa para ocultar opciones y
 * proteger rutas. Una pantalla también puede pedirla con `hiltViewModel()`.
 */
@HiltViewModel
class SessionViewModel @Inject constructor(
    sessionRepository: SessionRepository,
) : ViewModel() {

    val session: StateFlow<Session> = sessionRepository.session

    /** Rol actual; null sin sesión, sin finca o con la membresía inactiva. */
    val currentUserRole: StateFlow<FarmRole?> = session
        .map { it.role }
        .stateIn(viewModelScope, SharingStarted.Eagerly, session.value.role)

    fun can(permission: Permission): Boolean = session.value.can(permission)
}
