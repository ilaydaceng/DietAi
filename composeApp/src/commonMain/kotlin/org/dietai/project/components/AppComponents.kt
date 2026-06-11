package org.dietai.project.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

// --- 1. PROFİL BAŞLIĞI BİLEŞENİ ---
@Composable
fun ProfileHeader(adSoyad: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text("Merhaba, 👋", style = MaterialTheme.typography.titleMedium, color = Color.Gray)
            Text(
                text = adSoyad.uppercase(),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }
        // Profil İkonu
        Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.primaryContainer) {
            Text("👤", modifier = Modifier.padding(10.dp), style = MaterialTheme.typography.titleLarge)
        }
    }
}

// --- 2. DİYET LİSTESİ KARTI (CHECKLIST HALİ) ---
@Composable
fun DietListCard(
    diyetSatirlari: List<String>,     // Artık tek bir yazı değil, satır listesi alıyor
    yapilanlar: List<Boolean>,        // Hangi satıra tik atılmış? (True/False listesi)
    onDurumDegisti: (Int, Boolean) -> Unit // Tıklanınca ne olsun?
) {
    if (diyetSatirlari.isNotEmpty()) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
            modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "👨‍⚕️ Günlük Hedeflerin:",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2E7D32),
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(10.dp))

                // Her satır için bir Checkbox ve Yazı oluşturuyoruz
                diyetSatirlari.forEachIndexed { index, satir ->
                    if (satir.isNotBlank()) { // Boş satırları atla
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Checkbox(
                                checked = if (index < yapilanlar.size) yapilanlar[index] else false,
                                onCheckedChange = { yeniDurum ->
                                    onDurumDegisti(index, yeniDurum)
                                },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = Color(0xFF2E7D32),
                                    uncheckedColor = Color.Gray
                                )
                            )
                            Text(
                                text = satir,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (index < yapilanlar.size && yapilanlar[index]) Color.Gray else Color.Black,
                                textDecoration = if (index < yapilanlar.size && yapilanlar[index]) androidx.compose.ui.text.style.TextDecoration.LineThrough else null
                            )
                        }
                    }
                }
            }
        }
    }
}

// --- 3. SU TAKİP KARTI (GÜNCELLENDİ: Veriyi dışarıdan alıyor) ---
@Composable
fun WaterTrackingCard(
    icilenSu: Int,             // Sayıyı dışarıdan al
    onSuEkle: () -> Unit,      // Ekleme emri gelince ne yapsın?
    onSuAzalt: () -> Unit      // Azaltma emri gelince ne yapsın?
) {
    val hedefSu = 10

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD)),
        modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Günlük Su Hedefi 💧", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFF1565C0))

            Spacer(modifier = Modifier.height(15.dp))

            val progress by animateFloatAsState(targetValue = icilenSu.toFloat() / hedefSu.toFloat())

            LinearProgressIndicator(
                progress = { if (progress > 1f) 1f else progress },
                modifier = Modifier.fillMaxWidth().height(10.dp),
                color = Color(0xFF2196F3),
                trackColor = Color.White,
            )

            Spacer(modifier = Modifier.height(10.dp))
            Text("$icilenSu / $hedefSu Bardak", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)

            Spacer(modifier = Modifier.height(10.dp))

            Row {
                Button(onClick = onSuAzalt, colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Blue)) { Text("-") }
                Spacer(modifier = Modifier.width(20.dp))
                Button(onClick = onSuEkle, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3))) { Text("+ Bardak Ekle") }
            }
        }
    }
}