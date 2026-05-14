package com.example.smartkrishi

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import android.widget.Toast

class CropInputActivity : ComponentActivity() {

    private val viewModel: CropViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val mobile = intent.getStringExtra("USER_MOBILE") ?: ""
        setContent {
            SmartKrishiTheme {
                CropInputScreen(
                    mobile    = mobile,
                    viewModel = viewModel,
                    onResult  = { recs ->
                        val intent = Intent(this, ResultActivity::class.java).apply {
                            putExtra("USER_MOBILE", mobile)
                            putExtra("CROP_1", recs.getOrNull(0)?.crop ?: "")
                            putExtra("CONF_1", recs.getOrNull(0)?.confidence ?: 0f)
                            putExtra("CROP_2", recs.getOrNull(1)?.crop ?: "")
                            putExtra("CONF_2", recs.getOrNull(1)?.confidence ?: 0f)
                            putExtra("CROP_3", recs.getOrNull(2)?.crop ?: "")
                            putExtra("CONF_3", recs.getOrNull(2)?.confidence ?: 0f)
                        }
                        startActivity(intent)
                        viewModel.reset()
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CropInputScreen(
    mobile: String,
    viewModel: CropViewModel,
    onResult: (List<CropRecommendation>) -> Unit
) {
    val ctx   = LocalContext.current
    val scope = rememberCoroutineScope()
    val state by viewModel.state.collectAsState()

    val live = LiveFarmData.isPopulated

    // Fields — auto-fill from LiveFarmData where available
    var nitrogen     by remember { mutableStateOf("") }
    var phosphorus   by remember { mutableStateOf("") }
    var potassium    by remember { mutableStateOf("") }
    var ph           by remember { mutableStateOf("") }
    var temperature  by remember { mutableStateOf(
        if (live) LiveFarmData.temperature.toInt().toString() else "") }
    var humidity     by remember { mutableStateOf(
        if (live) LiveFarmData.humidity.toString() else "") }
    var rainfall     by remember { mutableStateOf(
        if (live) LiveFarmData.rawRain.toString() else "") }

    // Navigate when success
    LaunchedEffect(Unit) {
        viewModel.state.collectLatest { s ->
            if (s is CropPredictState.Success) {
                onResult(s.recommendations)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Crop Advisor 🌿", fontWeight = FontWeight.ExtraBold) },
                navigationIcon = {
                    IconButton({ (ctx as CropInputActivity).finish() }) {
                        Icon(Icons.Default.ArrowBack, null, tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = GreenPrimary, titleContentColor = Color.White)
            )
        }
    ) { pad ->
        Box(Modifier.fillMaxSize()) {

            Column(
                Modifier.fillMaxSize().background(PageBg).padding(pad)
                    .verticalScroll(rememberScrollState()).padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {

                // Auto-fill banner
                if (live) {
                    Card(
                        Modifier.fillMaxWidth(), RoundedCornerShape(12.dp),
                        CardDefaults.cardColors(containerColor = GreenSurface),
                        CardDefaults.cardElevation(0.dp),
                        androidx.compose.foundation.BorderStroke(1.dp, InputBorder)
                    ) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("🌤️", fontSize = 18.sp)
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text("Live weather auto-filled",
                                    fontWeight = FontWeight.Bold, fontSize = 12.sp, color = GreenPrimary)
                                Text("Temperature, humidity & rainfall from your GPS location",
                                    fontSize = 11.sp, color = TextSecondary)
                            }
                        }
                    }
                }

                // ── Nutrient Inputs ───────────────────────────
                Card(
                    Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(CardWhite),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        SectionHeader("🧪", "Soil Nutrients", GreenPrimary)

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Column(Modifier.weight(1f)) {
                                InputBox("Nitrogen (N)", nitrogen, { nitrogen = it }, "e.g. 80")
                            }
                            Column(Modifier.weight(1f)) {
                                InputBox("Phosphorus (P)", phosphorus, { phosphorus = it }, "e.g. 40")
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Column(Modifier.weight(1f)) {
                                InputBox("Potassium (K)", potassium, { potassium = it }, "e.g. 40")
                            }
                            Column(Modifier.weight(1f)) {
                                InputBox("Soil pH (0–14)", ph, { ph = it }, "e.g. 6.5")
                            }
                        }
                    }
                }

                // ── Climate Inputs ────────────────────────────
                Card(
                    Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(CardWhite),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            SectionHeader("🌡️", "Climate Data", Color(0xFF1565C0))
                            if (live) {
                                Spacer(Modifier.width(8.dp))
                                Box(
                                    Modifier.background(BlueBg, RoundedCornerShape(6.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text("Auto from GPS", fontSize = 9.sp,
                                        color = Color(0xFF1565C0), fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Column(Modifier.weight(1f)) {
                                InputBox(
                                    "Temperature °C", temperature,
                                    { temperature = it }, "e.g. 28",
                                    autoFilled = live && temperature.isNotBlank()
                                )
                            }
                            Column(Modifier.weight(1f)) {
                                InputBox(
                                    "Humidity %", humidity,
                                    { humidity = it }, "e.g. 65",
                                    autoFilled = live && humidity.isNotBlank()
                                )
                            }
                        }

                        InputBox(
                            "Rainfall (mm)", rainfall,
                            { rainfall = it }, "e.g. 100",
                            autoFilled = live && rainfall.isNotBlank()
                        )

                        // Refresh from live data
                        if (live) {
                            Text(
                                "↻ Refresh from live data: ${LiveFarmData.temperature.toInt()}°C · " +
                                "${LiveFarmData.humidity}% · ${LiveFarmData.rawRain}mm",
                                fontSize = 10.sp, color = GreenPrimary,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.clickable {
                                    temperature = LiveFarmData.temperature.toInt().toString()
                                    humidity    = LiveFarmData.humidity.toString()
                                    rainfall    = LiveFarmData.rawRain.toString()
                                }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(4.dp))

                // ── Submit Button ─────────────────────────────
                Button(
                    onClick = {
                        val nV  = nitrogen.toFloatOrNull()
                        val pV  = phosphorus.toFloatOrNull()
                        val kV  = potassium.toFloatOrNull()
                        val phV = ph.toFloatOrNull()
                        val tV  = temperature.toFloatOrNull()
                        val hV  = humidity.toFloatOrNull()
                        val rV  = rainfall.toFloatOrNull()

                        when {
                            nV  == null -> Toast.makeText(ctx, "Enter Nitrogen value", Toast.LENGTH_SHORT).show()
                            pV  == null -> Toast.makeText(ctx, "Enter Phosphorus value", Toast.LENGTH_SHORT).show()
                            kV  == null -> Toast.makeText(ctx, "Enter Potassium value", Toast.LENGTH_SHORT).show()
                            phV == null || phV !in 0f..14f ->
                                Toast.makeText(ctx, "Enter valid pH (0–14)", Toast.LENGTH_SHORT).show()
                            tV  == null -> Toast.makeText(ctx, "Enter Temperature", Toast.LENGTH_SHORT).show()
                            hV  == null -> Toast.makeText(ctx, "Enter Humidity", Toast.LENGTH_SHORT).show()
                            rV  == null -> Toast.makeText(ctx, "Enter Rainfall", Toast.LENGTH_SHORT).show()
                            else -> viewModel.predictCrops(nV, pV, kV, tV, hV, phV, rV)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                    enabled = state !is CropPredictState.Loading
                ) {
                    if (state is CropPredictState.Loading) {
                        CircularProgressIndicator(
                            Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                        Spacer(Modifier.width(10.dp))
                        Text("Analyzing crops...", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    } else {
                        Text("Get AI Recommendation  →", fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold)
                    }
                }

                // Error state
                if (state is CropPredictState.Error) {
                    val err = state as CropPredictState.Error
                    Card(
                        Modifier.fillMaxWidth(), RoundedCornerShape(14.dp),
                        CardDefaults.cardColors(containerColor = RedBg),
                        CardDefaults.cardElevation(0.dp),
                        androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF9A9A))
                    ) {
                        Column(Modifier.padding(14.dp)) {
                            val icon = when (err.type) {
                                ErrorType.NO_INTERNET -> "📡"
                                ErrorType.TIMEOUT     -> "⏱️"
                                ErrorType.SERVER      -> "🔧"
                                ErrorType.UNKNOWN     -> "⚠️"
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(icon, fontSize = 20.sp)
                                Spacer(Modifier.width(8.dp))
                                Text(err.message, fontSize = 12.sp,
                                    color = Color(0xFFC62828), lineHeight = 16.sp,
                                    modifier = Modifier.weight(1f))
                            }
                            Spacer(Modifier.height(8.dp))
                            TextButton(
                                onClick = { viewModel.reset() },
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                Text("Dismiss", fontSize = 12.sp, color = Color(0xFFC62828))
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
            }

            // Full-screen loading overlay
            if (state is CropPredictState.Loading) {
                Box(
                    Modifier.fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.35f)),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = CardWhite),
                        elevation = CardDefaults.cardElevation(8.dp)
                    ) {
                        Column(
                            Modifier.padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(52.dp),
                                color = GreenPrimary, strokeWidth = 5.dp
                            )
                            Spacer(Modifier.height(16.dp))
                            Text("Analyzing your field...",
                                fontSize = 16.sp, fontWeight = FontWeight.Bold,
                                color = GreenPrimary)
                            Spacer(Modifier.height(6.dp))
                            Text("ML model is processing",
                                fontSize = 13.sp, color = TextSecondary)
                            Text("your soil & climate data",
                                fontSize = 13.sp, color = TextSecondary)
                        }
                    }
                }
            }
        }
    }
}

// ── Section header helper ─────────────────────────────────────
@Composable
fun SectionHeader(emoji: String, title: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(4.dp, 18.dp)
            .background(color, RoundedCornerShape(2.dp)))
        Spacer(Modifier.width(8.dp))
        Text(title, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = color)
    }
}

// ── Input box with optional AUTO badge ───────────────────────
@Composable
fun InputBox(
    label: String, value: String, onValue: (String) -> Unit,
    hint: String = "", keyboardType: KeyboardType = KeyboardType.Decimal,
    autoFilled: Boolean = false
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label.uppercase(), fontSize = 9.sp, fontWeight = FontWeight.Bold,
                color = if (autoFilled) GreenPrimary else TextSecondary, letterSpacing = 0.5.sp)
            if (autoFilled) {
                Spacer(Modifier.width(4.dp))
                Box(Modifier.background(GreenSurface, RoundedCornerShape(4.dp))
                    .padding(horizontal = 4.dp, vertical = 1.dp)) {
                    Text("AUTO", fontSize = 7.sp, color = GreenPrimary, fontWeight = FontWeight.Bold)
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        Box(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                .background(if (autoFilled) GreenSurface else InputBg)
                .border(1.dp,
                    if (autoFilled) GreenPrimary else InputBorder,
                    RoundedCornerShape(10.dp))
        ) {
            androidx.compose.foundation.text.BasicTextField(
                value = value, onValueChange = onValue,
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(
                    fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary
                ),
                decorationBox = { inner ->
                    Box(Modifier.padding(horizontal = 12.dp, vertical = 11.dp)) {
                        if (value.isEmpty()) Text(hint, fontSize = 13.sp, color = TextHint)
                        inner()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
