package com.sge.finca.ui.screens.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

@Composable
fun RegisterScreen(
    registerViewModel: RegisterViewModel,
    onNavigateToLogin: () -> Unit,
) {
    val state by registerViewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = if (state.step == 0) "Crear Cuenta" else "Configurar Finca",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.height(32.dp))

        if (state.step == 0) {
            OutlinedTextField(
                value = state.name,
                onValueChange = { registerViewModel.updateName(it) },
                label = { Text("Nombre completo") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = state.email,
                onValueChange = { registerViewModel.updateEmail(it) },
                label = { Text("Correo electrónico") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = state.password,
                onValueChange = { registerViewModel.updatePassword(it) },
                label = { Text("Contraseña") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = state.confirmPassword,
                onValueChange = { registerViewModel.updateConfirmPassword(it) },
                label = { Text("Confirmar contraseña") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
            )

            ErrorMessage(state)
            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { registerViewModel.continueButtonPress() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
            ) {
                Text("Continuar")
            }
        } else {
            OutlinedTextField(
                value = state.farmName,
                onValueChange = { registerViewModel.updateFarmName(it) },
                label = { Text("Nombre de la finca") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )

            ErrorMessage(state)
            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { registerViewModel.createFarmButtonPress() },
                enabled = !state.isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
            ) {
                Text(if (state.isLoading) "Creando cuenta..." else "Crear Finca")
            }
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "— o —",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = state.inviteCode,
                    onValueChange = { registerViewModel.updateInviteCode(it) },
                    label = { Text("Código de invitación") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                )
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedButton(
                    onClick = { registerViewModel.joinFarmButtonPress() },
                    enabled = !state.isLoading,
                    modifier = Modifier.height(56.dp),
                ) {
                    Text("Unirse")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            TextButton(
                onClick = { registerViewModel.backToAccountStep() },
                enabled = !state.isLoading,
            ) {
                Text("Volver")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        TextButton(onClick = onNavigateToLogin) {
            Text("¿Ya tienes cuenta? Inicia sesión")
        }
    }

    // Tras crear la cuenta: ¿activar el inicio rápido con huella?
    if (state.showBiometricOffer) {
        BiometricOfferDialog(
            onAccept = { activity -> registerViewModel.onBiometricOfferAccepted(activity) },
            onDecline = { registerViewModel.onBiometricOfferDeclined() },
        )
    }
}

@Composable
private fun ErrorMessage(state: RegisterState) {
    if (state.showErrorMessage) {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = state.errorMessage,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}