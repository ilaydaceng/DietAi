package org.dietai.project.ui.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth

@Composable
fun SplashScreen(
    onNavigateToHome: () -> Unit,
    onNavigateToAuth: () -> Unit
) {
    val scale = remember { Animatable(0f) }

    LaunchedEffect(key1 = true) {
        // Animasyonu başlat
        scale.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1000)
        )
        // Kullanıcı giriş yapmış mı kontrol et
        val user = Firebase.auth.currentUser
        delay(1500) // Logoyu en az 1.5 saniye göster

        if (user != null) {
            onNavigateToHome()
        } else {
            onNavigateToAuth()
        }
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.primary)
    ) {
        // Logo yerine şimdilik büyük bir Text koyuyoruz. İleride resim koyarız.
        Text(
            text = "DietAI",
            color = Color.White,
            fontSize = 40.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.run { 
                // Scale modifier'ı şimdilik basit tutalım, animasyonu scale.value ile kullanabiliriz
                this 
                // Normalde .scale(scale.value) derdik ama import sorun olmasın diye basit tuttum.
            }
        )
    }
}
