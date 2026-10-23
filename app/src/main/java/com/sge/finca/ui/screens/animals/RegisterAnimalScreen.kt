package com.sge.finca.ui.screens.animals

import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sge.finca.ui.components.AnimalAvatar
import com.sge.finca.ui.components.CameraCapture
import com.sge.finca.ui.components.CameraPermissionGate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterAnimalScreen(
    viewModel: RegisterAnimalViewModel,
    onNavigateBack: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit) { viewModel.fetchLocation() }

    // El ViewModel avisa con saveSuccess; la pantalla ejecuta la lambda de navegación.
    LaunchedEffect(state.saveSuccess) {
        if (state.saveSuccess) onNavigateBack()
    }

    // Si el dispositivo no tiene cámara, solo se ofrece la galería.
    val hasCamera = remember {
        context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)
    }

    // Photo Picker: no requiere permisos de almacenamiento.
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) viewModel.onGalleryImagePicked(uri)
    }

    CameraPermissionGate(onGranted = viewModel::onOpenCamera) { requestCamera ->
        if (state.isCameraOpen) {
            CameraCapture(
                createPhotoFile = viewModel::createPhotoFile,
                onPhotoCaptured = viewModel::onPhotoCaptured,
                onError = viewModel::onCameraError,
                onClose = viewModel::onCloseCamera,
            )
        } else {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("Registrar Animal") },
                        navigationIcon = {
                            IconButton(onClick = onNavigateBack) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                            }
                        },
                    )
                },
            ) { innerPadding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                ) {
                    // Foto de perfil circular del animal (Coil)
                    AnimalAvatar(
                        photoUri = state.photoUri,
                        size = 180.dp,
                        contentDescription = "Foto del animal",
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        if (hasCamera) {
                            OutlinedButton(
                                onClick = requestCamera,
                                modifier = Modifier.weight(1f),
                            ) {
                                Text("Tomar foto")
                            }
                        }
                        OutlinedButton(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                                )
                            },
                            modifier = Modifier.weight(1f),
                        ) {
                            Text("Galería")
                        }
                    }
                    if (state.photoUri != null) {
                        TextButton(onClick = viewModel::onPhotoRemoved) {
                            Text("Quitar foto")
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    CowVariantPicker(
                        selected = state.variant,
                        onSelected = viewModel::onVariantSelected,
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = state.name,
                        onValueChange = viewModel::onNameChanged,
                        label = { Text("Nombre del animal") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = state.locationText,
                        onValueChange = {},
                        label = { Text("Ubicación GPS") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = false,
                        singleLine = true,
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = state.notes,
                        onValueChange = viewModel::onNotesChanged,
                        label = { Text("Notas (opcional)") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                    )

                    state.errorMessage?.let { message ->
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = message,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = viewModel::onSave,
                        enabled = !state.isSaving,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                    ) {
                        Text(if (state.isSaving) "Guardando..." else "Guardar")
                    }
                }
            }
        }
    }
}
