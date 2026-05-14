package com.example.smartkrishi

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SplashActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { SmartKrishiTheme { SplashScreen() } }
        // Fix #15: Use coroutine instead of deprecated Handler
        lifecycleScope.launch {
            delay(2800)
            startActivity(Intent(this@SplashActivity, LoginActivity::class.java))
            finish()
        }
    }
}

@Composable
fun SplashScreen() {
    val inf = rememberInfiniteTransition(label = "")
    val pulse by inf.animateFloat(
        0.93f, 1.07f,
        infiniteRepeatable(tween(900, easing = EaseInOut), RepeatMode.Reverse), ""
    )
    val d1 by inf.animateFloat(0.25f, 1f, infiniteRepeatable(tween(500), RepeatMode.Reverse), "")
    val d2 by inf.animateFloat(0.25f, 1f, infiniteRepeatable(tween(500, delayMillis = 170), RepeatMode.Reverse), "")
    val d3 by inf.animateFloat(0.25f, 1f, infiniteRepeatable(tween(500, delayMillis = 340), RepeatMode.Reverse), "")

    Box(
        Modifier.fillMaxSize().background(GreenPrimary),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {

            Box(
                Modifier.size(120.dp).scale(pulse).clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.13f)),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    Modifier.size(88.dp).clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🌱", fontSize = 44.sp)
                }
            }

            Spacer(Modifier.height(28.dp))

            Text("Smart Krishi", fontSize = 38.sp, fontWeight = FontWeight.ExtraBold,
                color = Color.White, letterSpacing = 0.5.sp)

            Spacer(Modifier.height(6.dp))

            Text("AI Crop Advisor for Farmers", fontSize = 15.sp,
                color = Color.White.copy(alpha = 0.78f), textAlign = TextAlign.Center)

            Spacer(Modifier.height(14.dp))

            // Fix #4: Show version number instead of SIH 2025 badge
            Surface(shape = RoundedCornerShape(24.dp), color = Color.White.copy(alpha = 0.14f)) {
                Text(
                    "🌾  स्मार्ट कृषि  ·  v1.0",
                    Modifier.padding(horizontal = 22.dp, vertical = 9.dp),
                    color = Color.White.copy(alpha = 0.92f), fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(Modifier.height(64.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf(d1, d2, d3).forEach { a ->
                    Box(Modifier.size(9.dp).clip(CircleShape).background(Color.White.copy(alpha = a)))
                }
            }

            Spacer(Modifier.height(16.dp))
            Text("Loading...", fontSize = 11.sp, color = Color.White.copy(alpha = 0.45f))
        }
    }
}
