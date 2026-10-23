package com.sge.finca.ui.screens.animals

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sge.finca.domain.model.Animal
import com.sge.finca.domain.repository.AnimalRepository
import com.sge.finca.platform.media.ImageStorage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class AnimalDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val animalRepository: AnimalRepository,
    private val imageStorage: ImageStorage,
) : ViewModel() {

    private val animalId: String = checkNotNull(savedStateHandle["animalId"])

    private val _uiState = MutableStateFlow(AnimalDetailUiState())
    val uiState: StateFlow<AnimalDetailUiState> = _uiState.asStateFlow()

    /** Animal tal como está guardado (para saber qué foto es la original). */
    private var original: Animal? = null

    init {
        viewModelScope.launch {
            val animal = animalRepository.getAnimal(animalId)
            if (animal == null) {
                _uiState.update { it.copy(isLoading = false, notFound = true) }
                return@launch
            }
            original = animal
            _uiState.update {
                it.copy(
                    isLoading = false,
                    name = animal.name,
                    notes = animal.notes,
                    photoUri = animal.photoUri,
                    variant = CowVariant.fromId(animal.variant),
                    locationText = "%.6f, %.6f".format(animal.latitude, animal.longitude),
                )
            }
        }
    }

    // ---------- Campos ----------

    fun onNameChanged(value: String) = _uiState.update { it.copy(name = value, errorMessage = null) }

    fun onNotesChanged(value: String) = _uiState.update { it.copy(notes = value) }

    fun onVariantSelected(variant: CowVariant) = _uiState.update { it.copy(variant = variant) }

    // ---------- Cámara / galería ----------

    fun onOpenCamera() = _uiState.update { it.copy(isCameraOpen = true) }

    fun onCloseCamera() = _uiState.update { it.copy(isCameraOpen = false) }

    fun createPhotoFile(): File = imageStorage.createPhotoFile()

    fun onPhotoCaptured(file: File) {
        setPhoto(imageStorage.uriFor(file).toString())
        _uiState.update { it.copy(isCameraOpen = false) }
    }

    fun onCameraError() {
        _uiState.update {
            it.copy(isCameraOpen = false, errorMessage = "No se pudo tomar la foto. Intenta de nuevo")
        }
    }

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
        deleteIfNew(_uiState.value.photoUri)
        _uiState.update { it.copy(photoUri = null) }
    }

    private fun setPhoto(newUri: String) {
        deleteIfNew(_uiState.value.photoUri)
        _uiState.update { it.copy(photoUri = newUri, errorMessage = null) }
    }

    /** Borra la foto solo si es una foto nueva (no guardada). La original se borra al guardar. */
    private fun deleteIfNew(uri: String?) {
        if (uri != null && uri != original?.photoUri) {
            viewModelScope.launch { imageStorage.delete(uri) }
        }
    }

    // ---------- Guardar ----------

    fun onSave() {
        val s = _uiState.value
        val base = original ?: return
        if (s.isSaving) return
        if (s.name.isBlank()) {
            _uiState.update { it.copy(errorMessage = "El nombre del animal es obligatorio") }
            return
        }

        _uiState.update { it.copy(isSaving = true, errorMessage = null) }
        viewModelScope.launch {
            try {
                animalRepository.updateAnimal(
                    base.copy(
                        name = s.name.trim(),
                        notes = s.notes.trim(),
                        photoUri = s.photoUri,
                        variant = s.variant.id,
                    ),
                )
                // La foto anterior ya no se usa: se libera el archivo.
                val oldPhoto = base.photoUri
                if (oldPhoto != null && oldPhoto != s.photoUri) imageStorage.delete(oldPhoto)
                _uiState.update { it.copy(isSaving = false, finished = true) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false, errorMessage = "No se pudo guardar los cambios") }
            }
        }
    }

    // ---------- Eliminar ----------

    fun onDeleteClicked() = _uiState.update { it.copy(showDeleteDialog = true) }

    fun onDeleteDismissed() = _uiState.update { it.copy(showDeleteDialog = false) }

    fun onDeleteConfirmed() {
        val s = _uiState.value
        if (s.isSaving) return
        _uiState.update { it.copy(showDeleteDialog = false, isSaving = true) }
        viewModelScope.launch {
            try {
                animalRepository.deleteAnimal(animalId)
                original?.photoUri?.let { imageStorage.delete(it) }
                s.photoUri?.takeIf { it != original?.photoUri }?.let { imageStorage.delete(it) }
                _uiState.update { it.copy(isSaving = false, finished = true) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false, errorMessage = "No se pudo eliminar el animal") }
            }
        }
    }

    /** Si sale sin guardar, no deja fotos nuevas huérfanas. */
    override fun onCleared() {
        val state = _uiState.value
        val current = state.photoUri
        if (!state.finished && current != null && current != original?.photoUri) {
            imageStorage.deleteNow(current)
        }
        super.onCleared()
    }
}
