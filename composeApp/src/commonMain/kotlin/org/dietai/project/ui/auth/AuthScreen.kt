package org.dietai.project.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch

@Composable
fun AuthScreen(
    girisBasarili: (String) -> Unit,
    onSifremiUnuttum: () -> Unit,
    viewModel: AuthViewModel = viewModel { AuthViewModel() }
) {
    val uiState by viewModel.uiState.collectAsState()

    // Başarı Durumunu Dinle
    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            val rol = if (uiState.isDietitian) "Diyetisyen" else "Danışan"
            girisBasarili(rol)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = if (uiState.isDietitian) "Diyetisyen Girişi" else "Danışan Girişi",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Rol Seçimi
        Row(modifier = Modifier.fillMaxWidth().height(50.dp)) {
            Button(
                onClick = { viewModel.onEvent(AuthEvent.SelectRole(false)) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (!uiState.isDietitian) MaterialTheme.colorScheme.primary else Color.LightGray
                ),
                modifier = Modifier.weight(1f).padding(end = 4.dp)
            ) { Text("Danışan") }

            Button(
                onClick = { viewModel.onEvent(AuthEvent.SelectRole(true)) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (uiState.isDietitian) MaterialTheme.colorScheme.primary else Color.LightGray
                ),
                modifier = Modifier.weight(1f).padding(start = 4.dp)
            ) { Text("Diyetisyen") }
        }

        Spacer(modifier = Modifier.height(30.dp))

        if (uiState.isRegisterMode) {
            OutlinedTextField(
                value = uiState.name,
                onValueChange = { viewModel.onEvent(AuthEvent.NameChanged(it)) },
                label = { Text("Ad Soyad") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(10.dp))
        }

        OutlinedTextField(
            value = uiState.email,
            onValueChange = { viewModel.onEvent(AuthEvent.EmailChanged(it)) },
            label = { Text("E-Posta") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = uiState.password,
            onValueChange = { viewModel.onEvent(AuthEvent.PasswordChanged(it)) },
            label = { Text("Şifre") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        if (uiState.error != null) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(uiState.error!!, color = MaterialTheme.colorScheme.error)
        }

        if (!uiState.isRegisterMode) {
            TextButton(onClick = onSifremiUnuttum) {
                Text("Şifremi Unuttum?")
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        if (uiState.isLoading) {
            CircularProgressIndicator()
        } else {
            Button(
                onClick = { viewModel.onEvent(AuthEvent.Submit) },
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Text(if (uiState.isRegisterMode) "Kayıt Ol" else "Giriş Yap")
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        TextButton(onClick = { viewModel.onEvent(AuthEvent.ToggleMode) }) {
            Text(if (uiState.isRegisterMode) "Hesabın var mı? Giriş Yap" else "Hesabın yok mu? Kaydol")
        }
    }
}
