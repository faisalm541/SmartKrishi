package com.example.smartkrishi

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

// ── Updated MarketItem with extra fields ──────────────────────
data class MarketItem(
    val emoji: String,
    val name: String,
    val price: String,
    val market: String  = "",
    val minPrice: Int   = 0,
    val maxPrice: Int   = 0,
    val trend: String,
    val rawPrice: Int
)

// ── Static fallback data ──────────────────────────────────────
val staticMarketData = listOf(
    MarketItem("🌾", "Wheat",     "₹2,200/quintal", "Mandi", 2000, 2400, "↑", 2200),
    MarketItem("🌿", "Rice",      "₹1,950/quintal", "Mandi", 1800, 2100, "→", 1950),
    MarketItem("🌸", "Cotton",    "₹6,500/quintal", "Mandi", 6000, 7000, "↑", 6500),
    MarketItem("🌽", "Maize",     "₹1,850/quintal", "Mandi", 1700, 2000, "↓", 1850),
    MarketItem("🌾", "Barley",    "₹1,650/quintal", "Mandi", 1500, 1800, "→", 1650),
    MarketItem("🫘", "Soybean",   "₹4,200/quintal", "Mandi", 3900, 4500, "↑", 4200),
    MarketItem("🥜", "Groundnut", "₹5,800/quintal", "Mandi", 5500, 6100, "↑", 5800),
    MarketItem("🌼", "Mustard",   "₹5,100/quintal", "Mandi", 4800, 5400, "→", 5100),
    MarketItem("🧅", "Onion",     "₹1,200/quintal", "Mandi", 900,  1500, "↓", 1200),
    MarketItem("🍅", "Tomato",    "₹800/quintal",   "Mandi", 600,  1100, "↓", 800),
)

// ── Screen state ──────────────────────────────────────────────
sealed class MarketState {
    object Loading                          : MarketState()
    data class Success(
        val items: List<MarketItem>,
        val isLive: Boolean,
        val lastUpdated: String
    )                                       : MarketState()
    data class Error(val message: String)   : MarketState()
}

class MarketActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { SmartKrishiTheme { MarketScreen() } }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarketScreen() {
    val ctx   = LocalContext.current
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    var state by remember { mutableStateOf<MarketState>(MarketState.Loading) }
    var selectedFilter by remember { mutableStateOf("All") }

    val filters = listOf("All", "Grains", "Oilseeds", "Vegetables")

    // ── Fetch from API ────────────────────────────────────────
    fun fetchPrices() {
        scope.launch {
            state = MarketState.Loading
            try {
                val response = MarketApi.service.getMarketPrices(limit = 100)
                val parsed   = parseMarketRecords(response.records)
                val time     = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())

                if (parsed.isEmpty()) {
                    // API returned no matching records → use fallback
                    state = MarketState.Success(staticMarketData, false, "Static data")
                } else {
                    state = MarketState.Success(parsed, true, "Updated $time")
                }
            } catch (e: Exception) {
                // Any failure → silent fallback to static data
                state = MarketState.Success(staticMarketData, false, "Offline data")
            }
        }
    }

    // Fetch on first load
    LaunchedEffect(Unit) { fetchPrices() }

    // ── Filter logic ──────────────────────────────────────────
    fun filterByCategory(items: List<MarketItem>): List<MarketItem> {
        val categoryMap = mapOf(
            "Grains"     to listOf("Wheat", "Rice", "Maize", "Barley", "Jowar"),
            "Oilseeds"   to listOf("Soybean", "Groundnut", "Mustard", "Sunflower"),
            "Vegetables" to listOf("Onion", "Tomato", "Potato", "Garlic")
        )
        return if (selectedFilter == "All") items
        else items.filter { item ->
            categoryMap[selectedFilter]?.any {
                it.equals(item.name, ignoreCase = true)
            } == true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Market Prices 💰", fontWeight = FontWeight.ExtraBold) },
                navigationIcon = {
                    IconButton({ (ctx as MarketActivity).finish() }) {
                        Icon(Icons.Default.ArrowBack, null, tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { fetchPrices() }) {
                        Icon(Icons.Default.Refresh, null, tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = GreenPrimary, titleContentColor = Color.White)
            )
        }
    ) { pad ->
        Column(
            Modifier.fillMaxSize().background(PageBg)
                .padding(pad).padding(horizontal = 18.dp)
        ) {
            Spacer(Modifier.height(14.dp))

            when (val s = state) {

                // ── Loading ───────────────────────────────────
                is MarketState.Loading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(44.dp),
                                color = GreenPrimary, strokeWidth = 4.dp)
                            Spacer(Modifier.height(14.dp))
                            Text("Fetching live mandi prices...",
                                fontSize = 14.sp, fontWeight = FontWeight.Bold, color = GreenPrimary)
                            Text("Connecting to data.gov.in",
                                fontSize = 12.sp, color = TextSecondary)
                        }
                    }
                }

                // ── Success ───────────────────────────────────
                is MarketState.Success -> {
                    val allItems  = s.items
                    val best      = allItems.maxByOrNull { it.rawPrice }
                    val displayed = filterByCategory(
                        allItems.filter { it.name.contains(query, ignoreCase = true) }
                    )

                    // Live / offline badge
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier.size(8.dp)
                                    .background(
                                        if (s.isLive) Color(0xFF2E7D32) else Color(0xFFBF6F00),
                                        RoundedCornerShape(4.dp)
                                    )
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                if (s.isLive) "Live data" else "Offline data",
                                fontSize = 11.sp,
                                color = if (s.isLive) GreenPrimary else Color(0xFFBF6F00),
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(s.lastUpdated, fontSize = 10.sp, color = TextSecondary)
                    }

                    Spacer(Modifier.height(10.dp))

                    // Best to sell banner
                    best?.let {
                        Card(
                            Modifier.fillMaxWidth(), RoundedCornerShape(16.dp),
                            CardDefaults.cardColors(AmberBg), CardDefaults.cardElevation(2.dp),
                            androidx.compose.foundation.BorderStroke(1.dp, AmberBorder)
                        ) {
                            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text("🏆", fontSize = 26.sp)
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)) {
                                    Text("BEST TO SELL TODAY", fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold, color = AmberText,
                                        letterSpacing = 0.5.sp)
                                    Text("${it.emoji}  ${it.name}  —  ${it.price}",
                                        fontWeight = FontWeight.ExtraBold, fontSize = 14.sp,
                                        color = GreenPrimary)
                                    if (it.market.isNotBlank()) {
                                        Text("${it.market}", fontSize = 11.sp, color = TextSecondary)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // Search bar
                    Card(
                        Modifier.fillMaxWidth(), RoundedCornerShape(14.dp),
                        CardDefaults.cardColors(CardWhite), CardDefaults.cardElevation(1.dp)
                    ) {
                        Row(
                            Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Search, null,
                                tint = TextSecondary, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(10.dp))
                            BasicTextField(
                                value = query, onValueChange = { query = it },
                                singleLine = true,
                                textStyle = TextStyle(fontSize = 14.sp, color = TextPrimary),
                                decorationBox = { inner ->
                                    Box {
                                        if (query.isEmpty()) Text("Search crops...",
                                            fontSize = 13.sp, color = TextHint)
                                        inner()
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    // Category filter chips — Fix #7: removed redundant Box wrapper
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(filters) { f ->
                            val sel = selectedFilter == f
                            FilterChip(
                                selected = sel,
                                onClick  = { selectedFilter = f },
                                label    = { Text(f, fontSize = 12.sp,
                                    fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal) },
                                colors   = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = GreenPrimary,
                                    selectedLabelColor     = Color.White,
                                    containerColor         = CardWhite
                                )
                            )
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    if (displayed.isEmpty()) {
                        Box(Modifier.fillMaxWidth().padding(top = 32.dp),
                            contentAlignment = Alignment.Center) {
                            Text("No crops found for \"$query\"",
                                fontSize = 14.sp, color = TextSecondary)
                        }
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(displayed) { item ->
                                MarketItemCard(item)
                            }
                            item { Spacer(Modifier.height(20.dp)) }
                        }
                    }
                }

                // ── Error (should not reach — fallback handles it) ─
                is MarketState.Error -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("📡", fontSize = 48.sp)
                            Spacer(Modifier.height(12.dp))
                            Text(s.message, fontSize = 14.sp, color = TextSecondary)
                            Spacer(Modifier.height(16.dp))
                            Button(onClick = { fetchPrices() },
                                colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)) {
                                Text("Retry")
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Market item card ──────────────────────────────────────────
@Composable
fun MarketItemCard(item: MarketItem) {
    val trendColor = when (item.trend) { "↑" -> TrendUp; "↓" -> TrendDown; else -> TrendFlat }
    val trendBg    = when (item.trend) { "↑" -> GreenSurface; "↓" -> RedBg; else -> Color(0xFFF5F5F5) }

    Card(
        Modifier.fillMaxWidth(), RoundedCornerShape(14.dp),
        CardDefaults.cardColors(CardWhite), CardDefaults.cardElevation(1.dp)
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {

            // Emoji box
            Box(
                Modifier.size(46.dp).background(GreenSurface, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) { Text(item.emoji, fontSize = 24.sp) }

            Spacer(Modifier.width(12.dp))

            // Name + price + market
            Column(Modifier.weight(1f)) {
                Text(item.name, fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp, color = TextPrimary)
                Text(item.price, fontSize = 12.sp, color = TextSecondary)
                if (item.market.isNotBlank() && item.market != "Mandi") {
                    Text(item.market, fontSize = 10.sp, color = TextHint)
                }
                // Min/max range if available and different
                if (item.minPrice > 0 && item.maxPrice > item.minPrice) {
                    Text(
                        "₹${"%,d".format(item.minPrice)}–₹${"%,d".format(item.maxPrice)}",
                        fontSize = 10.sp, color = TextHint
                    )
                }
            }

            // Trend box
            Box(
                Modifier.size(38.dp).background(trendBg, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(item.trend, fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold, color = trendColor)
            }
        }
    }
}
