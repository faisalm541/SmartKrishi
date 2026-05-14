package com.example.smartkrishi

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.*

class ResultActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SmartKrishiTheme {
                ResultScreen(
                    mobile = intent.getStringExtra("USER_MOBILE") ?: "",
                    crop1  = intent.getStringExtra("CROP_1") ?: "",
                    conf1  = intent.getFloatExtra("CONF_1", 0f),
                    crop2  = intent.getStringExtra("CROP_2") ?: "",
                    conf2  = intent.getFloatExtra("CONF_2", 0f),
                    crop3  = intent.getStringExtra("CROP_3") ?: "",
                    conf3  = intent.getFloatExtra("CONF_3", 0f)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultScreen(
    mobile: String,
    crop1: String, conf1: Float,
    crop2: String, conf2: Float,
    crop3: String, conf3: Float
) {
    val ctx  = LocalContext.current
    val db   = remember { DatabaseHelper(ctx) }
    var saved by remember { mutableStateOf(false) }

    // Build recommendation list from passed data
    val recommendations = remember {
        buildList {
            if (crop1.isNotBlank()) add(CropRecommendation(crop1, conf1))
            if (crop2.isNotBlank()) add(CropRecommendation(crop2, conf2))
            if (crop3.isNotBlank()) add(CropRecommendation(crop3, conf3))
        }
    }

    val top = recommendations.firstOrNull()
        ?: return  // safety guard

    val (suitLabel, suitColor) = getSuitabilityLabel(top.confidence)

    Scaffold(topBar = {
        TopAppBar(
            title = { Text("AI Result 🤖", fontWeight = FontWeight.ExtraBold) },
            navigationIcon = {
                IconButton({ (ctx as ResultActivity).finish() }) {
                    Icon(Icons.Default.ArrowBack, null, tint = Color.White)
                }
            },
            actions = {
                IconButton({
                    val txt = buildString {
                        append("🌾 Smart Krishi ML Recommendation\n\n")
                        append("Best Crop: ${getCropEmoji(top.crop)} ${top.crop.replaceFirstChar { it.uppercase() }}\n")
                        append("Confidence: ${"%.1f".format(top.confidence)}%\n")
                        append("Suitability: $suitLabel\n")
                        append("Season: ${getCropSeason(top.crop)}\n\n")
                        if (recommendations.size > 1) {
                            append("Alternatives:\n")
                            recommendations.drop(1).forEach { r ->
                                append("• ${r.crop.replaceFirstChar { it.uppercase() }} — ${"%.1f".format(r.confidence)}%\n")
                            }
                        }
                    }
                    ctx.startActivity(Intent.createChooser(
                        Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, txt)
                        }, "Share Result"
                    ))
                }) { Icon(Icons.Default.Share, null, tint = Color.White) }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = GreenPrimary, titleContentColor = Color.White)
        )
    }) { pad ->
        Column(
            Modifier.fillMaxSize().background(PageBg).padding(pad)
                .verticalScroll(rememberScrollState()).padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {

            // ── ML model badge ────────────────────────────────
            Card(
                Modifier.fillMaxWidth(), RoundedCornerShape(10.dp),
                CardDefaults.cardColors(containerColor = BlueBg),
                CardDefaults.cardElevation(0.dp)
            ) {
                Row(Modifier.padding(10.dp, 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("🤖", fontSize = 14.sp)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Predicted by Machine Learning model trained on 2,200+ crop records",
                        fontSize = 10.sp, color = Color(0xFF1565C0), lineHeight = 14.sp
                    )
                }
            }

            // ── Best crop card ────────────────────────────────
            Card(
                Modifier.fillMaxWidth(), RoundedCornerShape(20.dp),
                CardDefaults.cardColors(GreenSurface),
                CardDefaults.cardElevation(3.dp),
                androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFA5D6A7))
            ) {
                Column(Modifier.padding(18.dp)) {

                    Text("BEST MATCH", fontSize = 9.sp, fontWeight = FontWeight.Bold,
                        color = GreenLight, letterSpacing = 1.sp)
                    Spacer(Modifier.height(10.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(getCropEmoji(top.crop), fontSize = 52.sp)
                        Spacer(Modifier.width(16.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                top.crop.replaceFirstChar { it.uppercase() },
                                fontSize = 26.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = GreenPrimary
                            )
                            Text(getCropSeason(top.crop), fontSize = 12.sp, color = TextSecondary)
                        }
                        // Suitability badge
                        Box(
                            Modifier.background(
                                Color(suitColor).copy(alpha = 0.15f),
                                RoundedCornerShape(10.dp)
                            ).padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                suitLabel,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(suitColor)
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // Confidence bar
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("ML Confidence", fontSize = 12.sp, color = TextSecondary)
                        Text("${"%.1f".format(top.confidence)}%",
                            fontSize = 20.sp, fontWeight = FontWeight.ExtraBold,
                            color = GreenPrimary)
                    }
                    Spacer(Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { (top.confidence / 100f).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().height(10.dp),
                        color = Color(suitColor),
                        trackColor = Color.White,
                        strokeCap = StrokeCap.Round
                    )

                    Spacer(Modifier.height(14.dp))

                    // What confidence means
                    Card(
                        Modifier.fillMaxWidth(), RoundedCornerShape(10.dp),
                        CardDefaults.cardColors(CardWhite)
                    ) {
                        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.Top) {
                            Text("💡", fontSize = 14.sp)
                            Spacer(Modifier.width(6.dp))
                            Column {
                                Text(
                                    when {
                                        top.confidence >= 70f ->
                                            "Strong match! Your soil & climate conditions are highly suitable for ${top.crop.replaceFirstChar { it.uppercase() }}."
                                        top.confidence >= 40f ->
                                            "Good match. Consider ${top.crop.replaceFirstChar { it.uppercase() }} — check local market prices before sowing."
                                        else ->
                                            "Moderate match. Multiple crops scored similarly for your conditions. Review alternatives below."
                                    },
                                    fontSize = 12.sp, color = TextPrimary, lineHeight = 17.sp
                                )
                            }
                        }
                    }
                }
            }

            // ── Alternative crops ─────────────────────────────
            if (recommendations.size > 1) {
                Text("Alternative Crops",
                    fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = TextPrimary)

                recommendations.drop(1).forEach { rec ->
                    val (altLabel, altColor) = getSuitabilityLabel(rec.confidence)
                    Card(
                        Modifier.fillMaxWidth(), RoundedCornerShape(14.dp),
                        CardDefaults.cardColors(CardWhite), CardDefaults.cardElevation(1.dp)
                    ) {
                        Row(
                            Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(getCropEmoji(rec.crop), fontSize = 32.sp)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    rec.crop.replaceFirstChar { it.uppercase() },
                                    fontWeight = FontWeight.Bold, fontSize = 15.sp
                                )
                                Text(getCropSeason(rec.crop), fontSize = 11.sp, color = TextSecondary)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("${"%.1f".format(rec.confidence)}%",
                                    fontSize = 16.sp, fontWeight = FontWeight.ExtraBold,
                                    color = GreenPrimary)
                                Spacer(Modifier.height(2.dp))
                                LinearProgressIndicator(
                                    progress = { (rec.confidence / 100f).coerceIn(0f, 1f) },
                                    modifier = Modifier.width(70.dp).height(5.dp),
                                    color = Color(altColor),
                                    trackColor = GreenSurface,
                                    strokeCap = StrokeCap.Round
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(altLabel, fontSize = 10.sp,
                                    color = Color(altColor), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // ── Suitability legend ────────────────────────────
            Card(
                Modifier.fillMaxWidth(), RoundedCornerShape(12.dp),
                CardDefaults.cardColors(containerColor = Color(0xFFF8F9FA)),
                CardDefaults.cardElevation(0.dp),
                androidx.compose.foundation.BorderStroke(0.5.dp, InputBorder)
            ) {
                Column(Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Confidence Guide", fontSize = 11.sp,
                        fontWeight = FontWeight.Bold, color = TextSecondary)
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        LegendDot("High",   Color(0xFF2E7D32), "≥ 50%")
                        LegendDot("Medium", Color(0xFFF57F17), "20–49%")
                        LegendDot("Low",    Color(0xFFC62828), "< 20%")
                    }
                }
            }

            // ── Action buttons ────────────────────────────────
            Button(
                onClick = {
                    if (!saved) {
                        val date = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date())
                        db.saveRecommendation(
                            mobile,
                            top.crop.replaceFirstChar { it.uppercase() },
                            "ML",
                            "ML API",
                            "Confidence: ${"%.1f".format(top.confidence)}%",
                            date
                        )
                        saved = true
                        Toast.makeText(ctx, "Saved to history! ✅", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(ctx, "Already saved!", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (saved) TextSecondary else GreenPrimary)
            ) {
                Text(if (saved) "✅  Saved to History" else "Save Result",
                    fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
            }

            OutlinedButton(
                onClick = {
                    ctx.startActivity(
                        Intent(ctx, HomeActivity::class.java)
                            .putExtra("USER_MOBILE", mobile)
                            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    )
                },
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = GreenPrimary),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, GreenPrimary)
            ) {
                Text("Back to Home", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
fun LegendDot(label: String, color: Color, range: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp)
            .background(color, RoundedCornerShape(4.dp)))
        Spacer(Modifier.width(4.dp))
        Text("$label ($range)", fontSize = 10.sp, color = TextSecondary)
    }
}
