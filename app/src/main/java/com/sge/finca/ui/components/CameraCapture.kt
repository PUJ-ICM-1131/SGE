package com.sge.finca.ui.components

import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.io.File

private const val TAG = "CameraCapture"

/**
 * Cámara embebida con CameraX: visor (PreviewView) + botón de captura.
 * La foto se guarda directamente en el archivo que entrega [createPhotoFile]
 * (que vive en el almacenamiento privado de la app).
 */
@Composable
fun CameraCapture(
    createPhotoFile: () -> File,
    onPhotoCaptured: (File) -> Unit,
    onError: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val controller = remember {
        LifecycleCameraController(context).apply {
            setEnabledUseCases(CameraController.IMAGE_CAPTURE)
        }
    }

    val hasFrontAndBackCamera = remember {
        val pm = context.packageManager
        pm.hasSystemFeature(PackageManager.FEATURE_CAMERA) &&
            pm.hasSystemFeature(PackageManager.FEATURE_CAMERA_FRONT)
    }

    // Libera la cámara al salir del visor
    DisposableEffect(controller) {
        onDispose { controller.unbind() }
    }

    BackHandler(onBack = onClose)

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).apply {
                    this.controller = controller
                    controller.bindToLifecycle(lifecycleOwner)
                }
            },
            modifier = Modifier.fillMaxSize(),
        )

        // Guía visual: lo que quede dentro del círculo es lo que se verá en la foto de perfil
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen),
        ) {
            val radius = size.minDimension * 0.42f
            val center = Offset(size.width / 2f, size.height / 2f)
            drawRect(Color.Black.copy(alpha = 0.45f))
            drawCircle(Color.Black, radius = radius, center = center, blendMode = BlendMode.Clear)
            drawCircle(
                color = Color(0xFF4CAF50),
                radius = radius,
                center = center,
                style = Stroke(width = 4.dp.toPx()),
            )
        }

        Text(
            text = "Centra al animal dentro del círculo",
            color = Color.White,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 150.dp),
        )

        IconButton(
            onClick = onClose,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp),
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Cerrar cámara", tint = Color.White)
        }

        // Cambiar entre cámara frontal y trasera (solo si el dispositivo tiene ambas)
        if (hasFrontAndBackCamera) {
            IconButton(
                onClick = {
                    controller.cameraSelector =
                        if (controller.cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) {
                            CameraSelector.DEFAULT_FRONT_CAMERA
                        } else {
                            CameraSelector.DEFAULT_BACK_CAMERA
                        }
                },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp),
            ) {
                Icon(Icons.Default.Cameraswitch, contentDescription = "Cambiar cámara", tint = Color.White)
            }
        }

        FilledIconButton(
            onClick = {
                val file = createPhotoFile()
                val options = ImageCapture.OutputFileOptions.Builder(file).build()
                controller.takePicture(
                    options,
                    ContextCompat.getMainExecutor(context),
                    object : ImageCapture.OnImageSavedCallback {
                        override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                            onPhotoCaptured(file)
                        }

                        override fun onError(exception: ImageCaptureException) {
                            Log.e(TAG, "Image capture failed", exception)
                            file.delete()
                            onError()
                        }
                    },
                )
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(32.dp)
                .size(72.dp),
        ) {
            Icon(
                Icons.Default.PhotoCamera,
                contentDescription = "Tomar foto",
                modifier = Modifier.size(32.dp),
            )
        }
    }
}
