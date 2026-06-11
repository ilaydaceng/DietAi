package org.dietai.project.ui.dietitian

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import org.dietai.project.ui.profile.StatItem // Reusing StatItem
import org.dietai.project.components.charts.LineChart
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientDetailScreen(
    userId: String,
    onNavigateBack: () -> Unit,
    onNavigateToChat: (String, String) -> Unit,
    viewModel: DietitianViewModel = viewModel { DietitianViewModel() }
) {
    val uiState by viewModel.uiState.collectAsState()
    var dietText by remember { mutableStateOf("") }
    
    // Yüklenince verileri çek
    LaunchedEffect(userId) {
        viewModel.loadClientDetails(userId)
    }
    
    // Veri gelince text field'ı doldur
    LaunchedEffect(uiState.currentDietPlan) {
        if (dietText.isEmpty()) {
            dietText = uiState.currentDietPlan
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.selectedClient?.adSoyad ?: "Danışan Detayı") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Geri")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val dietitianId = Firebase.auth.currentUser?.uid
                        val clientName = uiState.selectedClient?.adSoyad ?: "Danışan"
                        if (dietitianId != null) {
                            val chatId = "${dietitianId}_$userId"
                            onNavigateToChat(chatId, clientName)
                        }
                    }) {
                        Icon(Icons.Default.Chat, contentDescription = "Mesaj Gönder")
                    }
                }
            )
        }
    ) { padding ->
        if (uiState.isLoadingDetails) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                // Danışan Bilgileri Kartı (Üstte sabit)
                Card(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Vücut Analizi", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            StatItem("Boy", "${uiState.clientHeight} cm")
                            StatItem("Kilo", "${uiState.clientWeight} kg")
                            StatItem("VKI", "${uiState.selectedClient?.vki?.let { ((it * 100.0).toInt() / 100.0).toString() } ?: "-"} ")
                            StatItem("Durum", uiState.selectedClient?.durum ?: "-")
                        }
                    }
                }

                // TAB YAPISI
                var selectedTabIndex by remember { mutableStateOf(0) }
                val tabs = listOf("Diyet", "Kilo", "Yemek", "Egzersiz")

                TabRow(selectedTabIndex = selectedTabIndex) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = { selectedTabIndex = index },
                            text = { Text(title) }
                        )
                    }
                }

                when (selectedTabIndex) {
                    0 -> { // DİYET YAZ
                        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                            Text("Diyet Listesi Düzenle 🥗", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Button(
                                onClick = { viewModel.generateDietPlanWithAI(userId) },
                                modifier = Modifier.fillMaxWidth().height(45.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                            ) {
                                Text("🪄 Yapay Zeka ile Taslak Oluştur")
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            OutlinedTextField(
                                value = dietText,
                                onValueChange = { dietText = it },
                                modifier = Modifier.fillMaxWidth().weight(1f),
                                placeholder = { Text("Sabah:\n...\nÖğle:\n...\nAkşam:\n...") }
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            if (uiState.saveMessage != null && uiState.saveMessage!!.contains("Diyet")) {
                                Text(uiState.saveMessage!!, color = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.height(8.dp))
                            }

                            Button(
                                onClick = { viewModel.saveDietPlan(userId, dietText) },
                                modifier = Modifier.fillMaxWidth().height(50.dp)
                            ) {
                                Text("Listeyi Kaydet ve Gönder")
                            }
                        }
                    }
                    1 -> { // KİLO GEÇMİŞİ
                        if (uiState.clientWeightHistory.isEmpty()) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Kilo kaydı yok.") }
                        } else {
                            Column(modifier = Modifier.fillMaxSize()) {
                                val chartData = uiState.clientWeightHistory.map { it.weight }.reversed()
                                LineChart(data = chartData)
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(" Geçmiş Kayıtlar", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 16.dp))

                                androidx.compose.foundation.lazy.LazyColumn(modifier = Modifier.padding(16.dp).weight(1f)) {
                                    items(uiState.clientWeightHistory.size) { index -> 
                                        val entry = uiState.clientWeightHistory[index]
                                        Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                            Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                Text(entry.date)
                                                Text("${entry.weight} kg", fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    2 -> { // YEMEKLER
                        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                            Text("Günlük Yemek Kayıtları", style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(8.dp))
                            if (uiState.clientMeals.isEmpty()) {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Kayıt yok.") }
                            } else {
                                androidx.compose.foundation.lazy.LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(uiState.clientMeals.size) { i ->
                                        val meal = uiState.clientMeals[i]
                                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                                            Row(modifier = Modifier.padding(12.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                Column {
                                                    Text(meal.description, fontWeight = FontWeight.Bold)
                                                    Text(meal.timestamp, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                                }
                                                Text("${meal.calories} kcal", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    3 -> { // EGZERSİZLER (FAZ 7)
                        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                            // Hedef Atama Kutusu
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("Danışana Hedef Ata 🎯", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Text("Şu anki hedef: ${if(uiState.clientExerciseTarget.isEmpty()) "Yok" else uiState.clientExerciseTarget}", style = MaterialTheme.typography.bodySmall)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    
                                    var targetInput by remember { mutableStateOf("") }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        OutlinedTextField(
                                            value = targetInput,
                                            onValueChange = { targetInput = it },
                                            placeholder = { Text("Örn: Bugün 500 Kalori Yak") },
                                            modifier = Modifier.weight(1f).height(50.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Button(onClick = { viewModel.setExerciseTarget(userId, targetInput); targetInput = "" }) {
                                            Text("Ata")
                                        }
                                    }
                                    if (uiState.saveMessage != null && uiState.saveMessage!!.contains("Hedef")) {
                                        Text(uiState.saveMessage!!, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Egzersiz Geçmişi", style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            if (uiState.clientExercises.isEmpty()) {
                                Box(Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) { Text("Henüz egzersiz kaydı yok.") }
                            } else {
                                androidx.compose.foundation.lazy.LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(uiState.clientExercises.size) { i ->
                                        val ex = uiState.clientExercises[i]
                                        Card(colors = CardDefaults.cardColors(containerColor = if(ex.isCompleted) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.errorContainer.copy(alpha=0.3f))) {
                                            Row(modifier = Modifier.padding(12.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                                Column {
                                                    Text("${ex.type} (${ex.duration} dk)", fontWeight = FontWeight.Bold)
                                                    Text(ex.timestamp, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                                }
                                                Column(horizontalAlignment = Alignment.End) {
                                                    Text("🔥 ${ex.burnedCalories} kcal", color = Color(0xFFE53935), fontWeight = FontWeight.Bold)
                                                    Text(if(ex.isCompleted) "✅ Yapıldı" else "⏳ Bekliyor", style = MaterialTheme.typography.labelSmall, color = if(ex.isCompleted) Color(0xFF4CAF50) else Color.Gray)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
