package com.sge.finca.ui.screens.animals

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sge.finca.domain.model.Animal
import com.sge.finca.domain.repository.AnimalRepository
import com.sge.finca.platform.location.LocationHelper
import com.sge.finca.platform.media.ImageStorage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class RegisterAnimalViewModel @Inject constructor(
    private val locationHelper: LocationHelper,
    private val imageStorage: ImageStorage,
    private val animalRepository: AnimalRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(RegisterAnimalUiState())
    val uiState: StateFlow<RegisterAnimalUiState> = _uiState.asStateFlow()

    fun fetchLocation() {
        viewModelScope.launch {
            val loc = locationHelper.requestCurrentLocation()
            if (loc != null) {
                _uiState.update {
                    it.copy(
                        latitude = loc.latitude,
                        longitude = loc.longitude,
                        locationAccuracy = loc.accuracy,
                        locationText = "%.6f, %.6f (±%.0fm)".format(
                            loc.latitude, loc.longitude, loc.accuracy,
                        ),
                    )
                }
            } else {
                _uiState.update {
                    it.copy(locationText = "Ubicación no disponible")
                }
            }
        }
    }

    fun onNameChanged(value: String) {
        _uiState.update { it.copy(name = value, errorMessage = null) }
    }

    fun onNotesChanged(value: String) {
        _uiState.update { it.copy(notes = value) }
    }

    // ---------- Cámara ----------

    fun onOpenCamera() {
        _uiState.update { it.copy(isCameraOpen = true) }
    }

    fun onCloseCamera() {
        _uiState.update { it.copy(isCameraOpen = false) }
    }

    /** Archivo vacío (en filesDir) donde CameraX guarda la foto. */
    fun createPhotoFile(): File = imageStorage.createPhotoFile()

    /** CameraX terminó de guardar la foto: se muestra en el formulario. */
    fun onPhotoCaptured(file: File) {
        setPhoto(imageStorage.uriFor(file).toString())
        _uiState.update { it.copy(isCameraOpen = false) }
    }

    fun onCameraError() {
        _uiState.update {
            it.copy(isCameraOpen = false, errorMessage = "No se pudo tomar la foto. Intenta de nuevo")
        }
    }

    // ---------- Galería ----------

    /** Photo Picker devolvió un URI temporal: se copia a almacenamiento propio. */
    fun onGalleryImagePicked(uri: Uri) {
        viewModelScope.launch {
            val copied = imageStorage.copyFromGallery(uri)
            if (copied != null) {
                setPhoto(copied.toString())
            } else {
                _uiState.update { it.copy(errorMessage = "No se pudo cargar la imagen") }
            }
        }
    }

    fun onPhotoRemoved() {
        val old = _uiState.value.photoUri
        _uiState.update { it.copy(photoUri = null) }
        if (old != null) viewModelScope.launch { imageStorage.delete(old) }
    }

    /** Cambia la foto del formulario y borra la anterior (si no se había guardado). */
    private fun setPhoto(newUri: String) {
        val old = _uiState.value.photoUri
        _uiState.update { it.copy(photoUri = newUri, errorMessage = null) }
        if (old != null && old != newUri) viewModelScope.launch { imageStorage.delete(old) }
    }

    // ---------- Guardar ----------

    fun onSave() {
        val s = _uiState.value
        if (s.isSaving) return

        val lat = s.latitude
        val lng = s.longitude
        if (s.name.isBlank()) {
            showError("El nombre del animal es obligatorio")
            return
        }
        if (lat == null || lng == null) {
            showError("Aún no hay ubicación GPS. Espera un momento e intenta de nuevo")
            return
        }

        _uiState.update { it.copy(isSaving = true, errorMessage = null) }
        viewModelScope.launch {
            try {
                animalRepository.saveAnimal(
                    Animal(
                        id = UUID.randomUUID().toString(),
                        name = s.name.trim(),
                        notes = s.notes.trim(),
                        photoUri = s.photoUri,
                        latitude = lat,
                        longitude = lng,
                        locationAccuracy = s.locationAccuracy,
                        createdAt = System.currentTimeMillis(),
                    ),
                )
                _uiState.update { it.copy(isSaving = false, saveSuccess = true) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isSaving = false, errorMessage = "No se pudo guardar el animal")
                }
            }
        }
    }

    private fun showError(message: String) {
        _uiState.update { it.copy(errorMessage = message) }
    }

    /** Si el usuario sale sin guardar, no deja una foto huérfana en el almacenamiento. */
    override fun onCleared() {
        val state = _uiState.value
        val orphan = state.photoUri
        if (!state.saveSuccess && orphan != null) {
            imageStorage.deleteNow(orphan)
        }
        super.onCleared()
    }
}