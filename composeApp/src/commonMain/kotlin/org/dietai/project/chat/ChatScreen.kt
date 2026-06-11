package org.dietai.project.chat

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.util.encodeBase64
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import org.dietai.project.network.*
import com.preat.peekaboo.image.picker.SelectionMode
import com.preat.peekaboo.image.picker.rememberImagePickerLauncher

import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun ChatScreen(viewModel: ChatViewModel = viewModel { ChatViewModel() }) {
    val scope = rememberCoroutineScope()
    val userContext by viewModel.userContext.collectAsState()
    var inputText by remember { mutableStateOf("") }
    val messages = remember { mutableStateListOf<Pair<String, Boolean>>() }
    var isLoading by remember { mutableStateOf(false) }
    var selectedImageBytes by remember { mutableStateOf<ByteArray?>(null) }

    val imagePicker = rememberImagePickerLauncher(
        selectionMode = SelectionMode.Single,
        scope = scope,
        onResult = { byteArrays ->
            byteArrays.firstOrNull()?.let {
                selectedImageBytes = it
            }
        }
    )

    val client = remember {
        HttpClient {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                    encodeDefaults = true
                })
            }
            install(HttpTimeout) {
                requestTimeoutMillis = 60000
                connectTimeoutMillis = 60000
                socketTimeoutMillis = 60000
            }
        }
    }

    val apiKey = org.dietai.project.Config.GEMINI_API_KEY

    Column(modifier = Modifier.fillMaxSize().padding(16.dp).safeDrawingPadding()) {
        Text("AI Diyetisyen Sohbeti", style = MaterialTheme.typography.headlineMedium)

        // Hızlı Sorular (Quick Prompts)
        if (messages.isEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            Text("💡 Sana Nasıl Yardımcı Olabilirim?", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val prompts = listOf("Bugün ne yemeliyim?", "Metabolizmamı nasıl hızlandırırım?", "Tatlı krizimi nasıl önlerim?", "Su içmeyi unuttum!")
                prompts.forEach { prompt ->
                    SuggestionChip(
                        onClick = { inputText = prompt },
                        label = { Text(prompt) }
                    )
                }
            }
        }

        LazyColumn(modifier = Modifier.weight(1f).padding(vertical = 8.dp)) {
            items(messages) { msg ->
                val sender = if (msg.second) "Siz" else "Diyetisyen"
                Card(
                    modifier = Modifier.padding(vertical = 4.dp).fillMaxWidth(0.85f),
                    colors = CardDefaults.cardColors(
                        containerColor = if (msg.second) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text(text = sender, style = MaterialTheme.typography.labelSmall)
                        Text(text = msg.first)
                    }
                }
            }
        }

        if (isLoading) LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp))

        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            IconButton(onClick = { imagePicker.launch() }) {
                Icon(
                    Icons.Default.CameraAlt, 
                    contentDescription = "Fotoğraf", 
                    tint = if (selectedImageBytes != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
            }
            TextField(
                value = inputText,
                onValueChange = { inputText = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Sorunuzu yazın...") },
                enabled = !isLoading
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = {
                    if (inputText.isNotBlank() || selectedImageBytes != null) {
                        val userMsg = inputText.ifBlank { "Lütfen bu yemeğin içeriğini ve kalorisini detaylıca analiz et." }
                        messages.add((if (selectedImageBytes != null) "📸 [Fotoğraf] " else "") + userMsg to true)
                        
                        val imageToSend = selectedImageBytes
                        selectedImageBytes = null
                        inputText = ""
                        isLoading = true
                        
                        scope.launch {
                            try {
                                val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey"

                                val basePrompt = "Sen 'DietAI' uygulamasının uzman, profesyonel ve güler yüzlü diyetisyenisin. Kullanıcılara beslenme, diyet, sağlıklı yaşam ve kilo kontrolü hakkında bilimsel, kısa ve net cevaplar ver. Tıbbi tanı koyma, gerektiğinde doktora yönlendir.\nÖNEMLİ GÖREV 1 (YEMEK): Eğer kullanıcı sana bir şey yediğini veya içtiğini söylerse (fotoğraf veya yazı ile), cevabının EN SONUNA mutlaka şu formatta bir JSON kodu ekle: {\"action\":\"log_meal\", \"meal\":\"Yediği Şey\", \"calories\":300, \"protein\": 15, \"carbs\": 40, \"fat\": 10, \"type\":\"Ara Öğün\"}. (Type değerleri: Kahvaltı, Öğle Yemeği, Akşam Yemeği, Ara Öğün). Bunu sadece yeni bir şey yediğini belirttiğinde yap."
                                val userDetails = if (userContext.adSoyad.isNotEmpty() && userContext.boy > 0) {
                                    "\n\nŞu an konuştuğun danışanın bilgileri:\n- Ad: ${userContext.adSoyad}\n- Boy: ${userContext.boy} cm\n- Kilo: ${userContext.kilo} kg\n- Vücut Kitle İndeksi (VKİ): ${userContext.vki}\nLütfen bu bilgilere göre tamamen kişiselleştirilmiş cevaplar ver."
                                } else ""
                                val systemPrompt = basePrompt + userDetails
                                val partsList = mutableListOf<Part>()
                                partsList.add(Part(text = userMsg))
                                
                                if (imageToSend != null) {
                                    val base64Image = imageToSend.encodeBase64()
                                    partsList.add(Part(inlineData = InlineData(mimeType = "image/jpeg", data = base64Image)))
                                }

                                val response = client.post(url) {
                                    contentType(ContentType.Application.Json)
                                    setBody(
                                        GeminiRequest(
                                            systemInstruction = SystemInstruction(parts = listOf(Part(text = systemPrompt))),
                                            contents = listOf(Content(parts = partsList))
                                        )
                                    )
                                }

                                val responseBody = response.bodyAsText()
                                if (response.status == HttpStatusCode.OK) {
                                    val json = Json.parseToJsonElement(responseBody)
                                    val text = json.jsonObject["candidates"]?.jsonArray?.getOrNull(0)
                                        ?.jsonObject?.get("content")?.jsonObject?.get("parts")
                                        ?.jsonArray?.getOrNull(0)?.jsonObject?.get("text")?.jsonPrimitive?.content
                                        
                                    if (text != null) {
                                        var displayText = text
                                        val startIndex = text.indexOf("{\"action\"")
                                        if (startIndex != -1) {
                                            val endIndex = text.indexOf("}", startIndex)
                                            if (endIndex != -1) {
                                                val jsonString = text.substring(startIndex, endIndex + 1)
                                                try {
                                                    val jsonElement = Json { ignoreUnknownKeys = true }.parseToJsonElement(jsonString).jsonObject
                                                    if (jsonElement["action"]?.jsonPrimitive?.content == "log_meal") {
                                                        val mealDesc = jsonElement["meal"]?.jsonPrimitive?.content ?: "Bilinmeyen"
                                                        val calories = jsonElement["calories"]?.jsonPrimitive?.intOrNull ?: 0
                                                        val protein = jsonElement["protein"]?.jsonPrimitive?.intOrNull ?: 0
                                                        val carbs = jsonElement["carbs"]?.jsonPrimitive?.intOrNull ?: 0
                                                        val fat = jsonElement["fat"]?.jsonPrimitive?.intOrNull ?: 0
                                                        val type = jsonElement["type"]?.jsonPrimitive?.content ?: "Ara Öğün"
                                                        viewModel.logMeal(mealDesc, type, calories, protein, carbs, fat)
                                                        
                                                        displayText = text.replace(jsonString, "").replace("`", "").trim()
                                                        displayText += "\n\n✅ [Otomatik Kayıt]: $mealDesc ($calories kcal | P: ${protein}g, K: ${carbs}g, Y: ${fat}g) günlüğünüze eklendi!"
                                                    }
                                                } catch (e: Exception) {
                                                    println("JSON parse hatası: ${e.message}")
                                                }
                                            }
                                        }
                                        messages.add(displayText to false)
                                    } else {
                                        messages.add("Cevap üretilemedi." to false)
                                    }
                                } else {
                                    val errorMsg = if (response.status.value == 503) {
                                        "Sistem şu an çok yoğun (Google Gemini Sunucuları). Lütfen birkaç saniye bekleyip tekrar deneyin."
                                    } else {
                                        "Bir hata oluştu (${response.status.value}). Lütfen API anahtarınızı kontrol edin veya tekrar deneyin."
                                    }
                                    messages.add(errorMsg to false)
                                    println("API Error Response: $responseBody")
                                }
                            } catch (e: Exception) {
                                messages.add("Bağlantı Hatası: ${e.message}" to false)
                            } finally { isLoading = false }
                        }
                    }
                },
                enabled = !isLoading && (inputText.isNotBlank() || selectedImageBytes != null)
            ) { Text("Gönder") }
        }
    }
}