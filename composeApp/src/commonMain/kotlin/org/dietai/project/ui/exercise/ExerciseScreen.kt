package org.dietai.project.ui.exercise

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.Instant

@Composable
fun ExerciseScreen(
    viewModel: ExerciseViewModel = viewModel { ExerciseViewModel() }
) {
    val uiState by viewModel.uiState.collectAsState()
    var showDialog by remember { mutableStateOf(false) }

    // Son 7 günü hesapla
    val timeZone = TimeZone.currentSystemDefault()
    val todayMillis = Clock.System.now().toEpochMilliseconds()
    val last7Days = (6 downTo 0).map { daysAgo ->
        val dateInstant = Instant.fromEpochMilliseconds(todayMillis - (daysAgo * 24L * 60L * 60L * 1000L))
        val date = dateInstant.toLocalDateTime(timeZone).date
        date.toString()
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showDialog = true }, containerColor = MaterialTheme.colorScheme.primary) {
                Icon(Icons.Default.Add, contentDescription = "Egzersiz Ekle", tint = MaterialTheme.colorScheme.onPrimary)
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Başlık
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Egzersiz Takvimi 🏃‍♂️", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text("Spor geçmişini takip et ve tik kazan!", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
            }
            
            // Haftalık Takvim Şeridi
            LazyRow(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                items(last7Days) { dateStr ->
                    val isSelected = uiState.selectedDate == dateStr
                    val dateObj = dateStr.split("-")
                    val dayOfMonth = dateObj.last()
                    
                    // O gün yapılmış spor var mı? (Tick sistemi)
                    val hasCompletedExercise = uiState.allExercises.any { it.timestamp == dateStr && it.isCompleted }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                            .clickable { viewModel.setSelectedDate(dateStr) }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            dayOfMonth,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        if (hasCompletedExercise) {
                            Box(
                                modifier = Modifier.size(20.dp).background(Color(0xFF4CAF50), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                            }
                        } else {
                            Box(modifier = Modifier.size(20.dp).background(Color.Gray.copy(alpha = 0.2f), CircleShape))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Seçili Günün Egzersizleri
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Text(
                    if (uiState.selectedDate == last7Days.last()) "Bugünün Programı" else "Seçili Gün",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Diyetisyen Hedefi
                if (uiState.dailyTarget.isNotBlank()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Star, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text("Diyetisyeninin Hedefi 🎯", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                Text(uiState.dailyTarget, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // AI Koç Önerisi
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { viewModel.getAiExerciseSuggestion() },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🤖 AI Koçtan Tavsiye Al", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onTertiaryContainer)
                        }
                        if (uiState.aiSuggestionLoading) {
                            Spacer(modifier = Modifier.height(8.dp))
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        } else if (uiState.aiSuggestion != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(uiState.aiSuggestion!!, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onTertiaryContainer)
                        } else {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Bugün yediğin yemeklere göre sana en uygun egzersizi önereyim mi? Tıkla!", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))

                if (uiState.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally).padding(32.dp))
                } else if (uiState.displayedExercises.isEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(24.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Bu gün için egzersiz yok. 😴")
                            Text("Hadi bir tane ekle!", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(uiState.displayedExercises) { exercise ->
                            ExerciseCard(
                                exercise = exercise,
                                onStatusChange = { isChecked ->
                                    viewModel.toggleExerciseStatus(exercise.id, isChecked)
                                }
                            )
                        }
                        item { Spacer(modifier = Modifier.height(80.dp)) }
                    }
                }
            }
        }
    }

    if (showDialog) {
        AddExerciseDialog(
            onDismiss = { showDialog = false },
            onConfirm = { type, duration ->
                viewModel.addExercise(type, duration)
                showDialog = false
            }
        )
    }
}

@Composable
fun ExerciseCard(exercise: ExerciseEntry, onStatusChange: (Boolean) -> Unit) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (exercise.isCompleted) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(48.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.DirectionsRun, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(exercise.type, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${exercise.duration} dk", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                    if (exercise.burnedCalories > 0) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("🔥 ${exercise.burnedCalories} kcal", style = MaterialTheme.typography.bodyMedium, color = Color(0xFFE53935))
                    }
                }
            }
            Checkbox(
                checked = exercise.isCompleted,
                onCheckedChange = onStatusChange,
                colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExerciseDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var type by remember { mutableStateOf("Kardiyo") }
    var duration by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }

    val exerciseTypes = listOf("Kardiyo", "Ağırlık", "Yürüyüş", "Yoga", "Yüzme")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Yeni Egzersiz Ekle", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Dropdown Egzersiz Seçimi
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it },
                ) {
                    OutlinedTextField(
                        value = type,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Egzersiz Türü") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        exerciseTypes.forEach { selection ->
                            DropdownMenuItem(
                                text = { Text(selection) },
                                onClick = {
                                    type = selection
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = duration,
                    onValueChange = { duration = it.filter { c -> c.isDigit() } },
                    label = { Text("Süre (Dakika)") },
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = { Text("dk", modifier = Modifier.padding(end = 12.dp), color = Color.Gray) }
                )
                
                // Dinamik kalori tahmini
                val durInt = duration.toIntOrNull() ?: 0
                val estCal = durInt * when (type.lowercase()) {
                    "kardiyo" -> 10; "ağırlık" -> 6; "yürüyüş" -> 5; "yoga" -> 4; "yüzme" -> 8; else -> 5
                }
                if (durInt > 0) {
                    Text("Tahmini Yakım: 🔥 $estCal kcal", color = Color(0xFFE53935), style = MaterialTheme.typography.labelMedium)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (type.isNotBlank() && duration.isNotBlank()) {
                        onConfirm(type, duration)
                    }
                }
            ) { Text("Ekle") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("İptal") }
        }
    )
}
