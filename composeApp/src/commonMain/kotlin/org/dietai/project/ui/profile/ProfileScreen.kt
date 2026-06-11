package org.dietai.project.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun ProfileScreen(
    onSignOut: () -> Unit,
    viewModel: ProfileViewModel = viewModel { ProfileViewModel() }
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.isSignedOut) {
        if (uiState.isSignedOut) {
            onSignOut()
        }
    }

    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    var showEditDialog by remember { mutableStateOf(false) }
    var isMonthly by remember { mutableStateOf(false) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Avatar ve İsim
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.size(60.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = uiState.name,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = uiState.email,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                IconButton(onClick = { showEditDialog = true }) {
                    Icon(Icons.Default.Edit, contentDescription = "Düzenle", tint = MaterialTheme.colorScheme.primary)
                }
            }

            Spacer(modifier = Modifier.height(30.dp))

            // Bilgi Kartı
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatItem("Rol", uiState.role)
                    StatItem("Boy", "${uiState.height} cm")
                    StatItem("Kilo", "${uiState.weight} kg")
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // İstatistikler (Analytics) Kartı - Faz 6
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Kalori Analizi", 
                            style = MaterialTheme.typography.titleMedium, 
                            fontWeight = FontWeight.Bold
                        )
                        // Haftalık / Aylık Toggle
                        Row(
                            modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(Color.LightGray.copy(alpha = 0.3f))
                        ) {
                            Text(
                                "Haftalık",
                                modifier = Modifier
                                    .clickable { isMonthly = false }
                                    .background(if(!isMonthly) MaterialTheme.colorScheme.primary else Color.Transparent)
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                color = if(!isMonthly) MaterialTheme.colorScheme.onPrimary else Color.Gray,
                                fontSize = MaterialTheme.typography.labelSmall.fontSize
                            )
                            Text(
                                "Aylık",
                                modifier = Modifier
                                    .clickable { isMonthly = true }
                                    .background(if(isMonthly) MaterialTheme.colorScheme.primary else Color.Transparent)
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                color = if(isMonthly) MaterialTheme.colorScheme.onPrimary else Color.Gray,
                                fontSize = MaterialTheme.typography.labelSmall.fontSize
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Basit Bar Chart (Sütun Grafiği)
                    Row(
                        modifier = Modifier.fillMaxWidth().height(150.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        val days = if (isMonthly) uiState.monthlyDays else uiState.weeklyDays
                        val intake = if (isMonthly) uiState.monthlyIntake else uiState.weeklyIntake
                        val burned = if (isMonthly) uiState.monthlyBurned else uiState.weeklyBurned
                        val maxCal = maxOf(3000f, (intake + burned).maxOrNull() ?: 3000f)

                        days.forEachIndexed { index, day ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Bottom,
                                modifier = Modifier.fillMaxHeight().weight(1f)
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                                    verticalAlignment = Alignment.Bottom
                                ) {
                                    // Alınan (Yeşil)
                                    val inH = if(maxCal > 0f) (intake.getOrElse(index){0f} / maxCal) else 0f
                                    Box(
                                        modifier = Modifier
                                            .width(if(isMonthly) 20.dp else 12.dp)
                                            .fillMaxHeight(inH)
                                            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                            .background(Color(0xFF4CAF50))
                                    )
                                    // Yakılan (Kırmızı)
                                    val bH = if(maxCal > 0f) (burned.getOrElse(index){0f} / maxCal) else 0f
                                    Box(
                                        modifier = Modifier
                                            .width(if(isMonthly) 20.dp else 12.dp)
                                            .fillMaxHeight(bH)
                                            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                            .background(Color(0xFFE53935))
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(day, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Legend
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(12.dp).background(Color(0xFF4CAF50), CircleShape))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Alınan", style = MaterialTheme.typography.labelSmall)
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(12.dp).background(Color(0xFFE53935), CircleShape))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Yakılan", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Ayarlar Listesi
            ProfileOptionItem(icon = Icons.Default.Lock, title = "Şifre Sıfırla") {
                viewModel.sendPasswordResetEmail()
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            Button(
                onClick = { viewModel.signOut() },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.ExitToApp, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Çıkış Yap")
            }
        }
    }

    if (showEditDialog) {
        EditProfileDialog(
            currentName = uiState.name,
            currentHeight = uiState.height,
            currentWeight = uiState.weight,
            onDismiss = { showEditDialog = false },
            onConfirm = { name, height, weight ->
                viewModel.updateProfile(name, height, weight)
                showEditDialog = false
            }
        )
    }
}

@Composable
fun EditProfileDialog(
    currentName: String,
    currentHeight: String,
    currentWeight: String,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String) -> Unit
) {
    var name by remember { mutableStateOf(currentName) }
    var height by remember { mutableStateOf(currentHeight) }
    var weight by remember { mutableStateOf(currentWeight) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Profili Düzenle") },
        text = {
            Column {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Ad Soyad") })
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = height, onValueChange = { height = it }, label = { Text("Boy (cm)") })
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = weight, onValueChange = { weight = it }, label = { Text("Kilo (kg)") })
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(name, height, weight) }) {
                Text("Kaydet")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("İptal")
            }
        }
    )
}

@Composable
fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        Text(text = label, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
fun ProfileOptionItem(icon: ImageVector, title: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.width(16.dp))
        Text(title, style = MaterialTheme.typography.bodyLarge)
        Spacer(modifier = Modifier.weight(1f))
        Icon(Icons.Default.ArrowForwardIos, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Gray)
    }
    HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
}
