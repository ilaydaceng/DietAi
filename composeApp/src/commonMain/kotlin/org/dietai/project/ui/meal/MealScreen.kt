package org.dietai.project.ui.meal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import org.dietai.project.components.charts.PieChart

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MealScreen(
    viewModel: MealViewModel = viewModel { MealViewModel() }
) {
    val uiState by viewModel.uiState.collectAsState()
    var showDialog by remember { mutableStateOf(false) }

    val totalCalories = uiState.meals.sumOf { it.calories }
    val dailyGoal = 2000

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showDialog = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Öğün Ekle") }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Yemek Günlüğü 🍽️",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )

                val cal = java.util.Calendar.getInstance()
                val gun = cal.get(java.util.Calendar.DAY_OF_MONTH)
                val ay = cal.get(java.util.Calendar.MONTH) + 1
                val yil = cal.get(java.util.Calendar.YEAR)
                val gunAdi = when (cal.get(java.util.Calendar.DAY_OF_WEEK)) {
                    java.util.Calendar.MONDAY -> "Pazartesi"
                    java.util.Calendar.TUESDAY -> "Salı"
                    java.util.Calendar.WEDNESDAY -> "Çarşamba"
                    java.util.Calendar.THURSDAY -> "Perşembe"
                    java.util.Calendar.FRIDAY -> "Cuma"
                    java.util.Calendar.SATURDAY -> "Cumartesi"
                    java.util.Calendar.SUNDAY -> "Pazar"
                    else -> ""
                }

                Text(
                    "$gun ${monthName(ay)} $yil, $gunAdi",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Kalori Özet Kartı
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    "Günlük Kalori",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                )
                                Row(verticalAlignment = Alignment.Bottom) {
                                    Text(
                                        "$totalCalories",
                                        fontSize = 40.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        " / $dailyGoal kcal",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f),
                                        modifier = Modifier.padding(bottom = 6.dp)
                                    )
                                }
                            }
                            Icon(
                                Icons.Default.LocalFireDepartment,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = if (totalCalories > dailyGoal) Color(0xFFE53935) else MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        val progress = (totalCalories.toFloat() / dailyGoal).coerceIn(0f, 1f)
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxWidth().height(8.dp),
                            color = if (totalCalories > dailyGoal) Color(0xFFE53935) else MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            if (totalCalories >= dailyGoal) "Günlük hedefe ulaştınız!"
                            else "Kalan: ${dailyGoal - totalCalories} kcal",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                        
                        // Pasta Grafik (Makrolar)
                        val totalProtein = uiState.meals.sumOf { it.protein }.toFloat()
                        val totalCarbs = uiState.meals.sumOf { it.carbs }.toFloat()
                        val totalFat = uiState.meals.sumOf { it.fat }.toFloat()
                        
                        if (totalProtein > 0 || totalCarbs > 0 || totalFat > 0) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Makro Dağılımı", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelLarge)
                            
                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.weight(1f)) {
                                    PieChart(
                                        data = mapOf(
                                            "Protein" to totalProtein,
                                            "Karbonhidrat" to totalCarbs,
                                            "Yağ" to totalFat
                                        ),
                                        colors = listOf(Color(0xFF4CAF50), Color(0xFFFF9800), Color(0xFF2196F3))
                                    )
                                }
                                Column(modifier = Modifier.padding(start = 16.dp)) {
                                    NutrientLegend("Protein", "${totalProtein.toInt()}g", Color(0xFF4CAF50))
                                    NutrientLegend("Karb.", "${totalCarbs.toInt()}g", Color(0xFFFF9800))
                                    NutrientLegend("Yağ", "${totalFat.toInt()}g", Color(0xFF2196F3))
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Öğün tiplerine göre özet
                val mealTypes = listOf("Kahvaltı", "Öğle", "Akşam", "Ara Öğün")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    mealTypes.forEach { type ->
                        val typeCalories = uiState.meals.filter { it.type == type }.sumOf { it.calories }
                        val emoji = when (type) {
                            "Kahvaltı" -> "🌅"
                            "Öğle" -> "☀️"
                            "Akşam" -> "🌙"
                            else -> "🍎"
                        }
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(emoji, fontSize = 18.sp)
                                Text(
                                    type,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                                Text(
                                    "$typeCalories",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    "kcal",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
            }

            if (uiState.isLoading) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) { CircularProgressIndicator() }
                }
            } else if (uiState.meals.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🍽️", fontSize = 56.sp)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Bugün henüz bir şey yemediniz", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("AI Diyetisyeninize ne yediğinizi söyleyin,\nveya yemeğinizin fotoğrafını atın.\nMakrolarınızı otomatik hesaplasın! ✨", style = MaterialTheme.typography.bodyMedium, color = Color.Gray, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        }
                    }
                }
            } else {
                val grouped = uiState.meals.groupBy { it.type }
                val order = listOf("Kahvaltı", "Öğle", "Akşam", "Ara Öğün")
                order.forEach { type ->
                    val mealsOfType = grouped[type] ?: return@forEach
                    item {
                        val emoji = when (type) {
                            "Kahvaltı" -> "🌅"
                            "Öğle" -> "☀️"
                            "Akşam" -> "🌙"
                            else -> "🍎"
                        }
                        Text(
                            "$emoji $type",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    items(mealsOfType) { meal ->
                        MealCard(meal, onDelete = { viewModel.deleteMeal(meal.id) })
                    }
                    item { Spacer(modifier = Modifier.height(8.dp)) }
                }
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }

    if (showDialog) {
        AddMealDialog(
            onDismiss = { showDialog = false },
            onConfirm = { desc, type, calories, protein, carbs, fat ->
                viewModel.addMeal(desc, type, calories, protein, carbs, fat)
                showDialog = false
            }
        )
    }
}

@Composable
fun MealCard(meal: MealEntry, onDelete: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    meal.description,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (meal.protein > 0) NutrientChip("P: ${meal.protein}g", Color(0xFF4CAF50))
                    if (meal.carbs > 0) NutrientChip("K: ${meal.carbs}g", Color(0xFFFF9800))
                    if (meal.fat > 0) NutrientChip("Y: ${meal.fat}g", Color(0xFF2196F3))
                }
                if (meal.timestamp.isNotEmpty()) {
                    Text(
                        "🕒 ${meal.timestamp}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "${meal.calories}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Text("kcal", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Sil",
                        modifier = Modifier.size(18.dp),
                        tint = Color.Gray
                    )
                }
            }
        }
    }
}

@Composable
fun NutrientChip(text: String, color: Color) {
    Box(
        modifier = Modifier
            .background(color.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun NutrientLegend(label: String, value: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
        Box(modifier = Modifier.size(12.dp).background(color, RoundedCornerShape(2.dp)))
        Spacer(modifier = Modifier.width(8.dp))
        Text(label, style = MaterialTheme.typography.labelMedium)
        Spacer(modifier = Modifier.width(8.dp))
        Text(value, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
fun AddMealDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, Int, Int, Int, Int) -> Unit
) {
    var text by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("Kahvaltı") }
    var calories by remember { mutableStateOf("") }
    var protein by remember { mutableStateOf("") }
    var carbs by remember { mutableStateOf("") }
    var fat by remember { mutableStateOf("") }

    val mealTypes = listOf("Kahvaltı", "Öğle", "Akşam", "Ara Öğün")
    val mealEmojis = listOf("🌅", "☀️", "🌙", "🍎")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Öğün Ekle", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Öğün Tipi",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    mealTypes.forEachIndexed { i, t ->
                        FilterChip(
                            selected = type == t,
                            onClick = { type = t },
                            label = { Text("${mealEmojis[i]} $t", fontSize = 11.sp) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Ne yedin?") },
                    placeholder = { Text("örn: Tavuk göğsü, pilav") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = calories,
                    onValueChange = { calories = it.filter { c -> c.isDigit() } },
                    label = { Text("Kalori (kcal)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    trailingIcon = {
                        Text("kcal", modifier = Modifier.padding(end = 8.dp), color = Color.Gray)
                    }
                )

                Text(
                    "Besin Değerleri (isteğe bağlı)",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.Gray
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = protein,
                        onValueChange = { protein = it.filter { c -> c.isDigit() } },
                        label = { Text("Protein") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        suffix = { Text("g") }
                    )
                    OutlinedTextField(
                        value = carbs,
                        onValueChange = { carbs = it.filter { c -> c.isDigit() } },
                        label = { Text("Karb.") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        suffix = { Text("g") }
                    )
                    OutlinedTextField(
                        value = fat,
                        onValueChange = { fat = it.filter { c -> c.isDigit() } },
                        label = { Text("Yağ") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        suffix = { Text("g") }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (text.isNotBlank()) {
                        onConfirm(
                            text, type,
                            calories.toIntOrNull() ?: 0,
                            protein.toIntOrNull() ?: 0,
                            carbs.toIntOrNull() ?: 0,
                            fat.toIntOrNull() ?: 0
                        )
                    }
                }
            ) { Text("Ekle") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("İptal") }
        }
    )
}

fun monthName(month: Int): String = when (month) {
    1 -> "Ocak"; 2 -> "Şubat"; 3 -> "Mart"; 4 -> "Nisan"
    5 -> "Mayıs"; 6 -> "Haziran"; 7 -> "Temmuz"; 8 -> "Ağustos"
    9 -> "Eylül"; 10 -> "Ekim"; 11 -> "Kasım"; 12 -> "Aralık"
    else -> ""
}