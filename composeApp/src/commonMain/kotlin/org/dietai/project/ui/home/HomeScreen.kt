package org.dietai.project.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import kotlinx.coroutines.launch

import org.dietai.project.components.DietListCard
import org.dietai.project.components.WaterTrackingCard

@Composable
fun HomeScreen(
    kullaniciTuru: String,
    cikisYap: () -> Unit,
    onNavigateToChat: (String, String) -> Unit = { _, _ -> },
    viewModel: HomeViewModel = viewModel { HomeViewModel() }
) {
    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()

    var boy by remember { mutableStateOf("") }
    var kilo by remember { mutableStateOf("") }
    var yas by remember { mutableStateOf("") }
    var cinsiyet by remember { mutableStateOf("Kadın") }

    LaunchedEffect(uiState.boy, uiState.kilo) {
        if (boy.isEmpty()) boy = uiState.boy
        if (kilo.isEmpty()) kilo = uiState.kilo
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .background(MaterialTheme.colorScheme.background)
    ) {
        // ─── GRADYAN HEADER ───
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                )
                .padding(horizontal = 24.dp, vertical = 28.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "👋 Hoş geldin,",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                        Text(
                            uiState.name,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "DietAI ile sağlıklı yaşam 🥗",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }

                    // Avatar
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            uiState.name.firstOrNull()?.uppercase() ?: "?",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                // VKI Chip ve Mesajlaşma
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (uiState.vki > 0) {
                        val vkiDurum = when {
                            uiState.vki < 18.5 -> "Zayıf"
                            uiState.vki < 25.0 -> "Normal ✅"
                            uiState.vki < 30.0 -> "Fazla Kilolu"
                            else -> "Obezite"
                        }
                        Box(
                            modifier = Modifier
                                .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(20.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            val vkiFormatli = ((uiState.vki * 10).toInt() / 10.0).toString()
                            Text("$vkiFormatli - $vkiDurum", fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }

                    // Diyetisyen Mesajlaş Butonu
                    if (uiState.diyetisyenId != null) {
                        val chatId = "${uiState.diyetisyenId}_${Firebase.auth.currentUser?.uid}"
                        Button(
                            onClick = { onNavigateToChat(chatId, "Diyetisyenim") },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Diyetisyenle Mesajlaş", fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // ─── İÇERİK ───
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // DİYETİSYEN SEÇİMİ
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        if (uiState.diyetisyenId == null) "👨‍⚕️ Diyetisyenini Seç" else "👨‍⚕️ Diyetisyenim: ${uiState.diyetisyenName}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    var expanded by remember { mutableStateOf(false) }
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = { expanded = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(if (uiState.diyetisyenId == null) "Seçim Yapmak İçin Tıkla" else "Diyetisyenini Değiştir")
                        }
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                            modifier = Modifier.fillMaxWidth(0.9f)
                        ) {
                            uiState.availableDietitians.forEach { diyetisyen ->
                                DropdownMenuItem(
                                    text = { Text(diyetisyen.name) },
                                    onClick = {
                                        viewModel.selectDietitian(diyetisyen.id)
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Hızlı İstatistikler
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                QuickStatCard(
                    modifier = Modifier.weight(1f),
                    emoji = "💧",
                    label = "Su",
                    value = "${uiState.waterIntake}",
                    unit = "bardak",
                    color = Color(0xFF2196F3)
                )
                QuickStatCard(
                    modifier = Modifier.weight(1f),
                    emoji = "⚖️",
                    label = "Kilo",
                    value = if (uiState.kilo.isNotEmpty()) uiState.kilo else "-",
                    unit = "kg",
                    color = Color(0xFF9C27B0)
                )
                QuickStatCard(
                    modifier = Modifier.weight(1f),
                    emoji = "📏",
                    label = "Boy",
                    value = if (uiState.boy.isNotEmpty()) uiState.boy else "-",
                    unit = "cm",
                    color = Color(0xFF4CAF50)
                )
            }

            // Diyet Listesi
            DietListCard(
                diyetSatirlari = uiState.dietLines,
                yapilanlar = uiState.dietChecklist,
                onDurumDegisti = { index, durum -> viewModel.toggleDietItem(index, durum) }
            )

            // Su Takibi
            WaterTrackingCard(
                icilenSu = uiState.waterIntake,
                onSuEkle = { viewModel.updateWater(uiState.waterIntake + 1) },
                onSuAzalt = { viewModel.updateWater(uiState.waterIntake - 1) }
            )

            // Kilo Takibi
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "📉 Kilo Takibi",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Button(
                            onClick = { if (kilo.isNotEmpty()) viewModel.saveWeight(kilo) },
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Text("Güncelle")
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (uiState.weightHistory.isNotEmpty()) {
                        uiState.weightHistory.take(5).forEach { entry ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("📅 ${entry.date}", style = MaterialTheme.typography.bodySmall)
                                Text(
                                    "${entry.weight} kg",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    } else {
                        Text(
                            "Henüz kilo geçmişi yok.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                }
            }

            // Vücut Analiz Formu
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "📏 Vücut Analizi",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = boy,
                            onValueChange = { boy = it },
                            label = { Text("Boy (cm)") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = kilo,
                            onValueChange = { kilo = it },
                            label = { Text("Kilo (kg)") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = yas,
                        onValueChange = { yas = it },
                        label = { Text("Yaş") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = cinsiyet == "Kadın",
                            onClick = { cinsiyet = "Kadın" },
                            label = { Text("👩 Kadın") },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = cinsiyet == "Erkek",
                            onClick = { cinsiyet = "Erkek" },
                            label = { Text("👨 Erkek") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { viewModel.calculateAndSaveVki(boy, kilo, yas, cinsiyet) },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Calculate, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Analiz Et ve Kaydet", fontWeight = FontWeight.Bold)
                    }

                    if (uiState.vki > 0) {
                        Spacer(modifier = Modifier.height(12.dp))
                        val vkiDurum = when {
                            uiState.vki < 18.5 -> "Zayıf ⚠️"
                            uiState.vki < 25.0 -> "Normal ✅"
                            uiState.vki < 30.0 -> "Fazla Kilolu ⚠️"
                            else -> "Obezite 🔴"
                        }
                        val vkiRenk = if (uiState.vki < 25.0) Color(0xFF81C784) else Color(0xFFFF8A65)
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = vkiRenk.copy(alpha = 0.15f)
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("VKİ Sonucu", fontWeight = FontWeight.Medium)
                                val vkiFormatli = ((uiState.vki * 10).toInt() / 10.0).toString()
                                Text("$vkiFormatli - $vkiDurum", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            TextButton(
                onClick = {
                    scope.launch {
                        Firebase.auth.signOut()
                        cikisYap()
                    }
                },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Icon(
                    Icons.Default.ExitToApp,
                    contentDescription = null,
                    tint = Color.Gray,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Çıkış Yap", color = Color.Gray)
            }

            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
fun QuickStatCard(
    modifier: Modifier = Modifier,
    emoji: String,
    label: String,
    value: String,
    unit: String,
    color: Color
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(emoji, fontSize = 22.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = color)
            Text(unit, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            Text(label, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
        }
    }
}