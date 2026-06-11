package org.dietai.project.ui.dietitian

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth

@Composable
fun DiyetisyenHomeScreen(
    cikisYap: () -> Unit,
    onDanisanSecildi: (String) -> Unit, // Navigation callback
    viewModel: DietitianViewModel = viewModel { DietitianViewModel() }
) {
    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()
    val auth = Firebase.auth

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Danışan Listesi 📋",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Detaylar ve diyet yazmak için isme tıkla 👇",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(20.dp))

            if (uiState.isLoading) {
                CircularProgressIndicator()
            } else if (uiState.error != null) {
                Text(uiState.error!!, color = Color.Red)
            } else {
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(uiState.clientList) { danisan ->
                        DanisanKarti(danisan = danisan, onClick = { onDanisanSecildi(danisan.uid) })
                    }
                }
            }

            Button(
                onClick = { 
                    scope.launch { 
                        auth.signOut()
                        cikisYap() 
                    } 
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.padding(top = 10.dp).fillMaxWidth()
            ) {
                Icon(Icons.Default.ExitToApp, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Çıkış Yap")
            }
        }
    }
}

@Composable
fun DanisanKarti(danisan: Danisan, onClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = danisan.adSoyad.uppercase(), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.weight(1f))
                Text(text = "👉", style = MaterialTheme.typography.headlineSmall)
            }

            Text(text = "E-posta: ${danisan.email}", style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "VKI: ${"%.2f".format(danisan.vki)}", fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.width(10.dp))

                val renk = when(danisan.durum) {
                    "Normal" -> Color.Green
                    "Analiz Yok" -> Color.Gray
                    else -> Color.Red
                }
                Surface(color = renk, shape = MaterialTheme.shapes.small) {
                    Text(text = " ${danisan.durum} ", color = Color.White, modifier = Modifier.padding(4.dp), style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

private fun String.format(value: Double): String {
    return "${(value * 100).toInt() / 100.0}"
}
