package com.example.smartkrishi

import android.Manifest
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

class HomeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val mobile = intent.getStringExtra("USER_MOBILE") ?: ""
        setContent { SmartKrishiTheme { HomeScreen(mobile) } }
    }
}

data class FeatureCard(
    val emoji: String,
    val title: String,
    val desc: String,
    val bg: Color
)

@Composable
fun HomeScreen(mobile: String) {
    val ctx     = LocalContext.current
    val db      = remember { DatabaseHelper(ctx) }
    val name    = remember {
        FirebaseAuthHelper.getUserDisplayName()
            .ifBlank { db.getUserName(mobile) }
            .ifBlank { "Farmer" }
    }
    val scope   = rememberCoroutineScope()
    val initial = name.firstOrNull()?.uppercaseChar()?.toString() ?: "F"

    var weather           by remember { mutableStateOf(WeatherUiState()) }
    var soilData          by remember { mutableStateOf<AgroSoilData?>(null) }
    var soilLoading       by remember { mutableStateOf(false) }
    var soilError         by remember { mutableStateOf<String?>(null) }
    var riskAlerts        by remember { mutableStateOf<List<RiskAlert>>(emptyList()) }
    var aiTips            by remember { mutableStateOf<List<String>>(emptyList()) }
    var bestCrop          by remember { mutableStateOf<Triple<String, String, String>?>(null) }
    var permissionGranted by remember { mutableStateOf(hasLocationPermission(ctx)) }
    var showAlerts        by remember { mutableStateOf(false) }
    var showTips          by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        permissionGranted =
            permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
    }

    fun fetchAll() {
        scope.launch {
            // Fix #11: Pre-check internet before any API call
            if (!isInternetAvailable(ctx)) {
                weather = WeatherUiState(
                    condition    = "No internet connection",
                    emoji        = "📡",
                    locationName = "Connect to WiFi or mobile data",
                    advisory     = "Please connect to the internet for live weather and soil data.",
                    isLoading    = false,
                    error        = "offline"
                )
                return@launch
            }
            weather = WeatherUiState(isLoading = true, locationName = "Detecting location...")
            try {
                val loc = getCurrentLocation(ctx)
                val weatherDeferred = async { WeatherApi.service.getWeather(loc.lat, loc.lon) }
                val soilDeferred    = async {
                    soilLoading = true
                    getOrFetchSoilData(ctx, loc.lat, loc.lon)
                }
                val cityName = try {
                    val geo = WeatherApi.geocoding.getCityName(loc.lat, loc.lon)
                    val a   = geo.address
                    listOfNotNull(
                        a?.city ?: a?.town ?: a?.village ?: a?.county, a?.state
                    ).joinToString(", ").ifBlank { "Your Location" }
                } catch (e: Exception) { "Lat: %.2f, Lon: %.2f".format(loc.lat, loc.lon) }

                val data = weatherDeferred.await()
                val c    = data.current
                LiveFarmData.updateFromWeather(c, loc.lat, loc.lon)
                val sm = c.soil_moisture_0_to_1cm
                val st = c.soil_temperature_0cm

                weather = WeatherUiState(
                    temperature     = "${c.temperature_2m.toInt()}°C",
                    feelsLike       = "${c.apparent_temperature.toInt()}°C",
                    humidity        = "${c.relative_humidity_2m}%",
                    wind            = "${c.wind_speed_10m} m/s",
                    rain            = "${c.rain} mm",
                    uvIndex         = "%.1f".format(c.uv_index),
                    soilTemp        = "${st.toInt()}°C",
                    soilMoisture    = "%.2f".format(sm),
                    condition       = getWeatherCondition(c.weather_code),
                    emoji           = getWeatherEmoji(c.weather_code),
                    locationName    = cityName,
                    advisory        = getFarmingAdvisory(c.weather_code, c.temperature_2m, sm, c.rain),
                    forecast        = parseForecast(data.daily),
                    isLoading       = false,
                    rawTemp         = c.temperature_2m,
                    rawHumidity     = c.relative_humidity_2m,
                    rawSoilMoisture = sm,
                    rawSoilTemp     = st,
                    rawRain         = c.rain,
                    rawWeatherCode  = c.weather_code,
                    rawUvIndex      = c.uv_index
                )

                riskAlerts = getRiskAlerts(c.weather_code, c.temperature_2m, sm, c.rain, c.uv_index, sm)
                aiTips     = getAITips(c.weather_code, c.temperature_2m, c.relative_humidity_2m, sm, c.rain, c.uv_index, st)
                bestCrop   = getBestCropToday(c.temperature_2m, sm, st, c.rain, c.weather_code)

                val soilResult = soilDeferred.await()
                soilLoading = false
                if (soilResult.isSuccess) {
                    val agro = soilResult.getOrNull()!!
                    soilData  = agro
                    soilError = null
                    LiveFarmData.updateFromAgro(
                        agro.rawMoisture,
                        agro.surfaceTemp.removeSuffix("°C").toDoubleOrNull() ?: st
                    )
                } else {
                    soilError = soilResult.exceptionOrNull()?.message
                }
            } catch (e: SecurityException) {
                weather = WeatherUiState(condition = "Location permission denied", emoji = "📍",
                    locationName = "Grant location permission",
                    advisory = "Allow location to see live weather.",
                    isLoading = false, error = e.message)
                soilLoading = false
            } catch (e: Exception) {
                weather = WeatherUiState(condition = "Could not fetch weather", emoji = "📡",
                    locationName = "Check internet connection",
                    advisory = "Connect to internet for live data.",
                    isLoading = false, error = e.message)
                soilLoading = false
            }
        }
    }

    LaunchedEffect(permissionGranted) {
        if (permissionGranted) fetchAll()
        else permissionLauncher.launch(arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ))
    }

    // ── 5 feature cards — Disease Detection added ─────────────
    val features = listOf(
        FeatureCard("🌱", "Crop Advisor",       "Get AI recommendations",  GreenChip),
        FeatureCard("🔬", "Disease Detection",  "Scan crop for disease",   Color(0xFFEDE7F6)),
        FeatureCard("💰", "Market Prices",      "Today's crop rates",      OrangeBg),
        FeatureCard("📋", "My History",         "Past saved results",      BlueBg),
        FeatureCard("👤", "My Profile",         "Farm details & logout",   PurpleBg)
    )

    Column(Modifier.fillMaxSize().background(PageBg).verticalScroll(rememberScrollState())) {

        // ── Green Header ─────────────────────────────────────
        Box(Modifier.fillMaxWidth().background(GreenPrimary)
            .padding(20.dp, 36.dp, 20.dp, 22.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(48.dp).clip(CircleShape).background(Color.White.copy(.18f)),
                    contentAlignment = Alignment.Center) {
                    Text(initial, fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold, color = Color.White)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    // Fix #5: Truncate long names so greeting doesn't overflow
                    Text(
                        "Namaste, ${name.split(" ").first()}! 🌾",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        if (weather.isLoading) "Detecting location..."
                        else weather.locationName,
                        fontSize = 11.sp, color = Color.White.copy(.68f), maxLines = 1
                    )
                }
                Box(Modifier.clickable { showAlerts = !showAlerts }) {
                    Icon(Icons.Default.Notifications, null,
                        tint = Color.White, modifier = Modifier.size(24.dp))
                    if (riskAlerts.isNotEmpty()) {
                        Box(
                            Modifier.size(16.dp).clip(CircleShape)
                                .background(Color(0xFFFF5722)).align(Alignment.TopEnd),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("${riskAlerts.size}", fontSize = 8.sp,
                                color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Spacer(Modifier.width(8.dp))
                IconButton(onClick = {
                    FirebaseAuthHelper.logout()
                    ctx.startActivity(Intent(ctx, LoginActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
                }) {
                    Icon(Icons.Default.ExitToApp, null,
                        tint = Color.White, modifier = Modifier.size(22.dp))
                }
            }
        }

        Box(Modifier.fillMaxWidth().background(GreenPrimary)) {
            Box(Modifier.fillMaxWidth().height(28.dp).background(
                PageBg, RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)))
        }

        Column(Modifier.padding(horizontal = 18.dp)) {

            // ── Risk Alerts ───────────────────────────────────
            val highAlerts = riskAlerts.filter { it.level == AlertLevel.HIGH }
            if (highAlerts.isNotEmpty() || showAlerts) {
                (if (showAlerts) riskAlerts else highAlerts).forEach { alert ->
                    val alertBg = when (alert.level) {
                        AlertLevel.HIGH   -> Color(0xFFFFEBEE)
                        AlertLevel.MEDIUM -> Color(0xFFFFF8E1)
                        AlertLevel.LOW    -> GreenSurface
                    }
                    val alertBorder = when (alert.level) {
                        AlertLevel.HIGH   -> Color(0xFFEF9A9A)
                        AlertLevel.MEDIUM -> AmberBorder
                        AlertLevel.LOW    -> InputBorder
                    }
                    Card(Modifier.fillMaxWidth().padding(bottom = 8.dp), RoundedCornerShape(14.dp),
                        CardDefaults.cardColors(containerColor = alertBg),
                        CardDefaults.cardElevation(0.dp),
                        androidx.compose.foundation.BorderStroke(1.dp, alertBorder)) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
                            Text(alert.icon, fontSize = 20.sp)
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(alert.title, fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp, color = TextPrimary)
                                Text(alert.message, fontSize = 11.sp,
                                    color = TextSecondary, lineHeight = 15.sp)
                                Text("→ ${alert.action}", fontSize = 11.sp,
                                    color = GreenPrimary, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            }

            // ── Weather Card ─────────────────────────────────
            Card(Modifier.fillMaxWidth(), RoundedCornerShape(18.dp),
                CardDefaults.cardColors(containerColor = GreenSurface),
                CardDefaults.cardElevation(2.dp),
                androidx.compose.foundation.BorderStroke(0.5.dp, InputBorder)) {
                when {
                    !permissionGranted -> {
                        Column(Modifier.padding(16.dp).fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("📍", fontSize = 32.sp)
                            Spacer(Modifier.height(8.dp))
                            Text("Location permission needed",
                                fontWeight = FontWeight.Bold, fontSize = 14.sp,
                                color = GreenPrimary, textAlign = TextAlign.Center)
                            Spacer(Modifier.height(10.dp))
                            Button(onClick = {
                                permissionLauncher.launch(arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION))
                            }, colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                                shape = RoundedCornerShape(10.dp)) {
                                Text("Allow Location", fontSize = 13.sp)
                            }
                        }
                    }
                    weather.isLoading -> {
                        Row(Modifier.padding(18.dp).fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(Modifier.size(28.dp),
                                color = GreenPrimary, strokeWidth = 3.dp)
                            Spacer(Modifier.width(14.dp))
                            Column {
                                Text("Fetching live weather...", fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold, color = GreenPrimary)
                                Text("Getting GPS, weather & soil data",
                                    fontSize = 11.sp, color = TextSecondary)
                            }
                        }
                    }
                    weather.error != null && weather.condition.contains("Could not", true) -> {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(weather.emoji, fontSize = 28.sp)
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(weather.condition, fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp, color = Color(0xFFC62828))
                                Text(weather.locationName, fontSize = 12.sp, color = TextSecondary)
                            }
                            IconButton(onClick = { fetchAll() }) {
                                Icon(Icons.Default.Refresh, null, tint = GreenPrimary)
                            }
                        }
                    }
                    else -> {
                        Column(Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(weather.emoji, fontSize = 40.sp)
                                Spacer(Modifier.width(14.dp))
                                Column(Modifier.weight(1f)) {
                                    Text("${weather.temperature}  ·  ${weather.condition}",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 16.sp, color = GreenPrimary)
                                    Text("Feels like ${weather.feelsLike}",
                                        fontSize = 12.sp, color = TextSecondary)
                                    Text("💧 ${weather.humidity}  🌬️ ${weather.wind}  ☀️ UV ${weather.uvIndex}",
                                        fontSize = 11.sp, color = TextSecondary)
                                }
                                IconButton(onClick = { fetchAll() }, modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Default.Refresh, null, tint = GreenPrimary,
                                        modifier = Modifier.size(18.dp))
                                }
                            }
                            Spacer(Modifier.height(12.dp))
                            HorizontalDivider(color = InputBorder, thickness = 0.5.dp)
                            Spacer(Modifier.height(12.dp))
                            Row(Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically) {
                                Text("Soil Data", fontWeight = FontWeight.ExtraBold,
                                    fontSize = 12.sp, color = GreenPrimary)
                                Box(Modifier.background(
                                    if (soilData != null) GreenSurface else AmberBg,
                                    RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)) {
                                    Text(
                                        if (soilData != null) "✅ AgroMonitoring"
                                        else if (soilLoading) "⏳ Loading..."
                                        else "📡 Open-Meteo est.",
                                        fontSize = 9.sp, fontWeight = FontWeight.Bold,
                                        color = if (soilData != null) GreenPrimary else Color(0xFFBF6F00))
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            when {
                                soilLoading -> {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        CircularProgressIndicator(Modifier.size(16.dp),
                                            color = GreenPrimary, strokeWidth = 2.dp)
                                        Spacer(Modifier.width(8.dp))
                                        Text("Fetching real soil data...",
                                            fontSize = 11.sp, color = TextSecondary)
                                    }
                                }
                                soilData != null -> {
                                    Row(Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        SoilChip("🌡️", "Surface", soilData!!.surfaceTemp, Modifier.weight(1f))
                                        SoilChip("🌡️", "10cm", soilData!!.depthTemp, Modifier.weight(1f))
                                        SoilChip("💧", "Moisture", soilData!!.moisturePercent, Modifier.weight(1f))
                                    }
                                }
                                else -> {
                                    Row(Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        SoilChip("🌡️", "Soil Temp", weather.soilTemp, Modifier.weight(1f))
                                        SoilChip("💧", "Moisture", weather.soilMoisture, Modifier.weight(1f))
                                        SoilChip("🌧️", "Rain", weather.rain, Modifier.weight(1f))
                                    }
                                }
                            }
                            if (weather.advisory.isNotBlank()) {
                                Spacer(Modifier.height(12.dp))
                                HorizontalDivider(color = InputBorder, thickness = 0.5.dp)
                                Spacer(Modifier.height(8.dp))
                                Text(weather.advisory, fontSize = 12.sp,
                                    color = Color(0xFF2E7D32), lineHeight = 17.sp,
                                    fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            }

            // ── 7-Day Forecast ────────────────────────────────
            if (weather.forecast.isNotEmpty()) {
                Spacer(Modifier.height(14.dp))
                Text("7-Day Forecast", fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.sp, color = TextPrimary)
                Spacer(Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(weather.forecast) { day ->
                        Card(Modifier.width(80.dp), RoundedCornerShape(14.dp),
                            CardDefaults.cardColors(containerColor = CardWhite),
                            CardDefaults.cardElevation(1.dp),
                            androidx.compose.foundation.BorderStroke(0.5.dp, InputBorder)) {
                            Column(Modifier.padding(8.dp).fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(day.dayName, fontSize = 9.sp,
                                    color = TextSecondary, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(4.dp))
                                Text(day.emoji, fontSize = 20.sp)
                                Spacer(Modifier.height(4.dp))
                                Text("${day.maxTemp}°", fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold, color = TextPrimary)
                                Text("${day.minTemp}°", fontSize = 10.sp, color = TextSecondary)
                                if (day.rainMm > 0) {
                                    Text("${day.rainMm.toInt()}mm",
                                        fontSize = 9.sp, color = Color(0xFF1565C0))
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // ── Best Crop Today ───────────────────────────────
            bestCrop?.let { (emoji, cropName, reason) ->
                Card(Modifier.fillMaxWidth(), RoundedCornerShape(16.dp),
                    CardDefaults.cardColors(containerColor = GreenSurface),
                    CardDefaults.cardElevation(2.dp),
                    androidx.compose.foundation.BorderStroke(1.dp, InputBorder)) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("🏆", fontSize = 22.sp)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Best Crop Today", fontSize = 10.sp,
                                color = TextSecondary, fontWeight = FontWeight.Bold)
                            Text("$emoji  $cropName", fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp, color = GreenPrimary)
                            Text(reason, fontSize = 11.sp, color = TextSecondary)
                        }
                        TextButton(onClick = {
                            ctx.startActivity(Intent(ctx, CropInputActivity::class.java)
                                .putExtra("USER_MOBILE", mobile))
                        }) {
                            Text("Try →", fontSize = 12.sp, color = GreenPrimary,
                                fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // ── AI Tips ───────────────────────────────────────
            if (aiTips.isNotEmpty()) {
                Card(Modifier.fillMaxWidth(),
                    RoundedCornerShape(0.dp, 14.dp, 14.dp, 14.dp),
                    CardDefaults.cardColors(containerColor = AmberBg),
                    CardDefaults.cardElevation(0.dp),
                    androidx.compose.foundation.BorderStroke(1.dp, AmberBorder)) {
                    Column(Modifier.padding(12.dp, 10.dp)) {
                        Row(Modifier.fillMaxWidth().clickable { showTips = !showTips },
                            verticalAlignment = Alignment.CenterVertically) {
                            Text("💡", fontSize = 16.sp)
                            Spacer(Modifier.width(8.dp))
                            Text("AI Farming Tips", fontWeight = FontWeight.Bold,
                                fontSize = 12.sp, color = Color(0xFFE65100),
                                modifier = Modifier.weight(1f))
                            Text(if (showTips) "▲" else "▼",
                                fontSize = 11.sp, color = Color(0xFFE65100))
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(aiTips.first(), fontSize = 11.sp,
                            color = Color(0xFF5D4037), lineHeight = 16.sp)
                        if (showTips) {
                            aiTips.drop(1).forEach { tip ->
                                Spacer(Modifier.height(6.dp))
                                HorizontalDivider(color = AmberBorder, thickness = 0.5.dp)
                                Spacer(Modifier.height(6.dp))
                                Text(tip, fontSize = 11.sp,
                                    color = Color(0xFF5D4037), lineHeight = 16.sp)
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── Feature Grid — 5 cards (3 rows: 2+2+1) ───────
            Text("What do you need?", fontWeight = FontWeight.ExtraBold,
                fontSize = 16.sp, color = TextPrimary)
            Spacer(Modifier.height(10.dp))

            // Fix #3: Replace LazyVerticalGrid (nested scroll conflict) with plain Column of Rows
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Row 1
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    features.take(2).forEachIndexed { i, f ->
                        FeatureCardItem(Modifier.weight(1f), f) {
                            val intent = when (i) {
                                0 -> Intent(ctx, CropInputActivity::class.java)
                                1 -> Intent(ctx, DiseaseDetectionActivity::class.java)
                                else -> return@FeatureCardItem
                            }.putExtra("USER_MOBILE", mobile)
                            ctx.startActivity(intent)
                        }
                    }
                }
                // Row 2
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    features.drop(2).take(2).forEachIndexed { i, f ->
                        FeatureCardItem(Modifier.weight(1f), f) {
                            val intent = when (i + 2) {
                                2 -> Intent(ctx, MarketActivity::class.java)
                                3 -> Intent(ctx, HistoryActivity::class.java)
                                else -> return@FeatureCardItem
                            }.putExtra("USER_MOBILE", mobile)
                            ctx.startActivity(intent)
                        }
                    }
                }
                // Row 3 — Profile card half width, spacer fills the other half
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    FeatureCardItem(Modifier.weight(1f), features[4]) {
                        ctx.startActivity(Intent(ctx, ProfileActivity::class.java)
                            .putExtra("USER_MOBILE", mobile))
                    }
                    Spacer(Modifier.weight(1f))
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

// Fix #3: Reusable feature card for the home grid
@Composable
fun FeatureCardItem(modifier: Modifier, f: FeatureCard, onClick: () -> Unit) {
    Card(
        modifier.height(126.dp).clickable { onClick() },
        RoundedCornerShape(18.dp),
        CardDefaults.cardColors(containerColor = f.bg),
        CardDefaults.cardElevation(2.dp)
    ) {
        Column(Modifier.fillMaxSize().padding(14.dp),
            verticalArrangement = Arrangement.Center) {
            Text(f.emoji, fontSize = 30.sp)
            Spacer(Modifier.height(8.dp))
            Text(f.title, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, color = TextPrimary)
            Text(f.desc, fontSize = 10.sp, color = TextSecondary, lineHeight = 13.sp)
        }
    }
}

@Composable
fun SoilChip(emoji: String, label: String, value: String, modifier: Modifier) {
    Card(modifier, RoundedCornerShape(10.dp),
        CardDefaults.cardColors(containerColor = CardWhite),
        CardDefaults.cardElevation(0.dp),
        androidx.compose.foundation.BorderStroke(0.5.dp, InputBorder)) {
        Column(Modifier.padding(8.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally) {
            Text(emoji, fontSize = 16.sp)
            Text(value, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = GreenPrimary)
            Text(label, fontSize = 9.sp, color = TextSecondary, textAlign = TextAlign.Center)
        }
    }
}
