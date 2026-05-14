package com.example.smartkrishi

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class HistoryActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val mobile = intent.getStringExtra("USER_MOBILE") ?: ""
        setContent { SmartKrishiTheme { HistoryScreen(mobile) } }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(mobile: String) {
    val ctx = LocalContext.current
    val db  = remember { DatabaseHelper(ctx) }

    // Fix #2: Use mutableStateOf so list can refresh
    var list by remember { mutableStateOf(db.getRecommendations(mobile)) }

    // Fix #2: Reload on every composition entry
    LaunchedEffect(Unit) {
        list = db.getRecommendations(mobile)
    }

    val emojiMap = mapOf("Wheat" to "🌾", "Rice" to "🌿", "Cotton" to "🌸",
        "Maize" to "🌽", "Soybean" to "🫘", "Groundnut" to "🥜", "Barley" to "🌾")
    val bgMap = mapOf("Wheat" to GreenSurface, "Rice" to GreenChip, "Cotton" to OrangeBg,
        "Maize" to Color(0xFFFFF8E1), "Soybean" to BlueBg, "Groundnut" to Color(0xFFFFF3E0), "Barley" to GreenSurface)

    Scaffold(topBar = {
        TopAppBar(
            title = { Text("My History 📋", fontWeight = FontWeight.ExtraBold) },
            navigationIcon = {
                IconButton({ (ctx as HistoryActivity).finish() }) {
                    Icon(Icons.Default.ArrowBack, null, tint = Color.White)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = GreenPrimary, titleContentColor = Color.White)
        )
    }) { pad ->
        Box(Modifier.fillMaxSize().background(PageBg).padding(pad)) {
            if (list.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("🌱", fontSize = 72.sp)
                    Spacer(Modifier.height(16.dp))
                    Text("No history yet", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = TextSecondary)
                    Spacer(Modifier.height(6.dp))
                    Text("Save a crop recommendation\nto see it here",
                        fontSize = 14.sp, color = TextHint, textAlign = TextAlign.Center, lineHeight = 20.sp)
                }
            } else {
                LazyColumn(Modifier.fillMaxSize().padding(horizontal = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(vertical = 14.dp)) {
                    item {
                        Text("${list.size} result${if (list.size > 1) "s" else ""} saved",
                            fontSize = 12.sp, color = TextSecondary,
                            modifier = Modifier.padding(bottom = 2.dp))
                    }
                    // Fix #9: Swipe-to-delete support
                    items(list, key = { it.id }) { rec ->
                        val dismissState = rememberSwipeToDismissBoxState(
                            confirmValueChange = { value ->
                                if (value == SwipeToDismissBoxValue.EndToStart) {
                                    db.deleteRecommendation(rec.id)
                                    list = db.getRecommendations(mobile)
                                    true
                                } else false
                            }
                        )
                        SwipeToDismissBox(
                            state = dismissState,
                            backgroundContent = {
                                Box(
                                    Modifier.fillMaxSize()
                                        .background(Color(0xFFFFEBEE), RoundedCornerShape(16.dp))
                                        .padding(end = 20.dp),
                                    contentAlignment = Alignment.CenterEnd
                                ) {
                                    Text("🗑️ Delete", fontSize = 13.sp,
                                        color = Color(0xFFC62828), fontWeight = FontWeight.Bold)
                                }
                            }
                        ) {
                            val emoji = emojiMap[rec.cropName] ?: "🌿"
                            val bg    = bgMap[rec.cropName] ?: GreenSurface
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(CardWhite),
                                elevation = CardDefaults.cardElevation(2.dp)
                            ) {
                                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Box(Modifier.size(52.dp).background(bg, RoundedCornerShape(14.dp)),
                                        contentAlignment = Alignment.Center) {
                                        Text(emoji, fontSize = 26.sp)
                                    }
                                    Spacer(Modifier.width(14.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(rec.cropName, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = TextPrimary)
                                        Text("pH: ${rec.soilPh}  ·  ${rec.soilType}", fontSize = 11.sp, color = TextSecondary)
                                        Spacer(Modifier.height(3.dp))
                                        Text(rec.result, fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold, color = GreenPrimary)
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(rec.date, fontSize = 10.sp, color = TextHint)
                                        Spacer(Modifier.height(4.dp))
                                        Box(Modifier.background(GreenSurface, RoundedCornerShape(6.dp))
                                            .padding(horizontal = 8.dp, vertical = 3.dp)) {
                                            Text("Saved", fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold, color = GreenPrimary)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    item { Spacer(Modifier.height(16.dp)) }
                }
            }
        }
    }
}
