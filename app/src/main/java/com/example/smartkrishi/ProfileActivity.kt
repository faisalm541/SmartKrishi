package com.example.smartkrishi

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ProfileActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val mobile = intent.getStringExtra("USER_MOBILE") ?: ""
        setContent { SmartKrishiTheme { ProfileScreen(mobile) } }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(mobile: String) {
    val ctx     = LocalContext.current
    val db      = remember { DatabaseHelper(ctx) }

    // Prefer Firebase name, fall back to SQLite, then default
    val name = remember {
        FirebaseAuthHelper.getUserDisplayName()
            .ifBlank { db.getUserName(mobile) }
            .ifBlank { "Farmer" }
    }

    val village = remember { db.getUserVillage(mobile) }
    val count   = remember { db.getRecommendationCount(mobile) }
    val initial = name.firstOrNull()?.uppercaseChar()?.toString() ?: "F"

    // Get Firebase email or fall back to mobile passed in
    val displayEmail = remember {
        FirebaseAuthHelper.getUserEmail().ifBlank { mobile }
    }

    // Fix #10: Record last login time on profile open
    val lastLogin = remember {
        SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date())
    }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text("My Profile 👤", fontWeight = FontWeight.ExtraBold) },
            navigationIcon = {
                IconButton({ (ctx as ProfileActivity).finish() }) {
                    Icon(Icons.Default.ArrowBack, null, tint = Color.White)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = GreenPrimary,
                titleContentColor = Color.White
            )
        )
    }) { pad ->
        Column(
            Modifier
                .fillMaxSize()
                .background(PageBg)
                .padding(pad)
                .verticalScroll(rememberScrollState())
        ) {

            // ── Green Header with Avatar ──────────────────────
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(GreenPrimary)
                    .padding(20.dp, 10.dp, 20.dp, 44.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {

                    // Avatar circle
                    Box(
                        Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(.18f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            Modifier
                                .size(68.dp)
                                .clip(CircleShape)
                                .background(Color.White),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                initial,
                                fontSize = 30.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = GreenPrimary
                            )
                        }
                    }

                    Spacer(Modifier.height(10.dp))
                    Text(
                        name,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                    Text(
                        "Smart Krishi Farmer",
                        fontSize = 12.sp,
                        color = Color.White.copy(.7f)
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        displayEmail,
                        fontSize = 11.sp,
                        color = Color.White.copy(.6f)
                    )
                }
            }

            // Curved transition
            Box(Modifier.fillMaxWidth().background(GreenPrimary)) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(28.dp)
                        .background(
                            PageBg,
                            RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
                        )
                )
            }

            Column(
                Modifier.padding(horizontal = 18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {

                // ── Stat Cards ───────────────────────────────
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    listOf(
                        Triple("$count", "Saved",   GreenSurface),
                        Triple("Active", "Status",  OrangeBg),
                        Triple("SIH '25","Project", BlueBg)
                    ).forEach { (v, l, bg) ->
                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = bg),
                            elevation = CardDefaults.cardElevation(1.dp)
                        ) {
                            Column(
                                Modifier.padding(10.dp).fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    v,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = GreenPrimary
                                )
                                Text(l, fontSize = 9.sp, color = TextSecondary)
                            }
                        }
                    }
                }

                // ── Farm Details Card ────────────────────────
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = CardWhite),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(
                        Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        Text(
                            "Farm Details",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp,
                            color = TextPrimary
                        )
                        Spacer(Modifier.height(12.dp))

                        listOf(
                            "👨‍🌾  Name"        to name,
                            "📧  Email"        to displayEmail,
                            "🏘️  Village"      to village.ifBlank { "Not set" },
                            "🕐  Last Login"   to lastLogin,
                            "📱  App Version"  to "v1.0"
                        ).forEachIndexed { i, (lbl, value) ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(lbl, fontSize = 13.sp, color = TextSecondary)
                                Text(
                                    value,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            }
                            if (i < 4) HorizontalDivider(
                                color = DividerColor,
                                thickness = 0.5.dp
                            )
                        }
                    }
                }

                // ── Auth Provider Info Card ──────────────────
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = BlueBg),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Row(
                        Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🔐", fontSize = 20.sp)
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                "Signed in with Firebase",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = Color(0xFF1565C0)
                            )
                            Text(
                                if (FirebaseAuthHelper.getUserEmail().isNotBlank())
                                    "Email / Google authentication"
                                else "Local authentication",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                    }
                }

                // ── SIH Badge Card ───────────────────────────
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = GreenSurface),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Row(
                        Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🌾", fontSize = 20.sp)
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                "Smart India Hackathon 2025",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = GreenPrimary
                            )
                            Text(
                                "Problem 30 · AI-Based Crop Recommendation",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                    }
                }

                Spacer(Modifier.height(4.dp))

                // ── Logout Button ────────────────────────────
                Button(
                    onClick = {
                        FirebaseAuthHelper.logout()
                        ctx.startActivity(
                            Intent(ctx, LoginActivity::class.java)
                                .addFlags(
                                    Intent.FLAG_ACTIVITY_NEW_TASK or
                                            Intent.FLAG_ACTIVITY_CLEAR_TASK
                                )
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = RedBtn)
                ) {
                    Text(
                        "Logout",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                Spacer(Modifier.height(20.dp))
            }
        }
    }
}