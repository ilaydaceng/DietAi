package org.dietai.project.ui.clock

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.datetime.Clock

@Composable
fun ClockScreen() {
    var ticks by remember { mutableStateOf(Clock.System.now().toEpochMilliseconds()) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            ticks = Clock.System.now().toEpochMilliseconds()
        }
    }

    // Manuel Saat Hesaplama
    val totalSeconds = ticks / 1000
    val hour = ((totalSeconds / 3600 + 3) % 24).toInt() // Türkiye saati (+3)
    val minute = ((totalSeconds / 60) % 60).toInt()
    
    // Örnek Günlük Rutin Verisi
    val dailyRoutine = listOf(
        RoutineItem("08:00", "Kahvaltı Zamanı", "Güne sağlıklı başla", Icons.Default.Restaurant, true),
        RoutineItem("10:00", "Su Hatırlatıcısı", "En az 2 bardak su iç", Icons.Default.LocalDrink, true),
        RoutineItem("13:00", "Öğle Yemeği", "Protein ağırlıklı beslen", Icons.Default.Restaurant, false),
        RoutineItem("16:00", "Ara Öğün & Su", "Metabolizmayı hızlandır", Icons.Default.LocalDrink, false),
        RoutineItem("18:30", "Egzersiz Vakti", "45 Dk Kardiyo / Yürüyüş", Icons.Default.DirectionsRun, false),
        RoutineItem("20:00", "Akşam Yemeği", "Hafif yiyecekler tercih et", Icons.Default.Restaurant, false)
    )

    Column(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
    ) {
        // Üst Kısım: Dijital Saat
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primary)
                .padding(vertical = 32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "GÜNLÜK PLANLAYICI",
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                    style = MaterialTheme.typography.labelLarge,
                    letterSpacing = 2.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}",
                    fontSize = 72.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }

        // Alt Kısım: Timeline (Zaman Çizelgesi)
        Column(modifier = Modifier.padding(16.dp).fillMaxSize()) {
            Text(
                "Bugünün Rutini",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(dailyRoutine) { item ->
                    RoutineCard(item, currentHour = hour)
                }
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
    }
}

@Composable
fun RoutineCard(item: RoutineItem, currentHour: Int) {
    val itemHour = item.time.split(":")[0].toInt()
    val isPast = itemHour < currentHour
    val isCurrent = itemHour == currentHour

    val cardColor = when {
        isCurrent -> MaterialTheme.colorScheme.primaryContainer
        isPast -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = cardColor),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // İkon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                        RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    item.icon,
                    contentDescription = null,
                    tint = if (isCurrent) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))

            // Metinler
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    item.time,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isCurrent) MaterialTheme.colorScheme.primary else Color.Gray,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    item.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal
                )
                Text(
                    item.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }

            // Durum İkonu (Tik)
            if (isPast || item.isCompleted) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = "Tamamlandı",
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

data class RoutineItem(
    val time: String,
    val title: String,
    val subtitle: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val isCompleted: Boolean
)