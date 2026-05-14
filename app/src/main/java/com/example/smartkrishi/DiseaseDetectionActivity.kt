package com.example.smartkrishi

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage

class DiseaseDetectionActivity : ComponentActivity() {

    private val viewModel: DiseaseViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SmartKrishiTheme {
                DiseaseDetectionScreen(viewModel = viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiseaseDetectionScreen(viewModel: DiseaseViewModel) {
    val ctx          = LocalContext.current
    val state        by viewModel.state.collectAsState()
    val selectedUri  by viewModel.selectedImage.collectAsState()

    // URI for camera capture
    var cameraUri by remember { mutableStateOf<Uri?>(null) }

    // ── Camera URI creator ────────────────────────────────────
    fun createCameraUri(): Uri {
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "disease_${System.currentTimeMillis()}.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        }
        return ctx.contentResolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values
        )!!
    }

    // ── Gallery launcher ──────────────────────────────────────
    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> uri?.let { viewModel.onImageSelected(it) } }

    // ── Camera launcher ───────────────────────────────────────
    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) cameraUri?.let { viewModel.onImageSelected(it) }
    }

    // ── Camera permission launcher ────────────────────────────
    val cameraPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val uri = createCameraUri()
            cameraUri = uri
            cameraLauncher.launch(uri)
        } else {
            Toast.makeText(ctx, "Camera permission is required", Toast.LENGTH_SHORT).show()
        }
    }

    fun launchCamera() {
        if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED) {
            val uri = createCameraUri()
            cameraUri = uri
            cameraLauncher.launch(uri)
        } else {
            cameraPermLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Disease Detection 🔬", fontWeight = FontWeight.ExtraBold) },
                navigationIcon = {
                    IconButton({ (ctx as DiseaseDetectionActivity).finish() }) {
                        Icon(Icons.Default.ArrowBack, null, tint = Color.White)
                    }
                },
                actions = {
                    if (state !is DiseaseUiState.Idle) {
                        IconButton(onClick = { viewModel.reset() }) {
                            Icon(Icons.Default.Refresh, null, tint = Color.White)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = GreenPrimary, titleContentColor = Color.White)
            )
        }
    ) { pad ->
        Column(
            Modifier.fillMaxSize().background(PageBg).padding(pad)
                .verticalScroll(rememberScrollState()).padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // ── Info card ─────────────────────────────────────
            Card(
                Modifier.fillMaxWidth(), RoundedCornerShape(14.dp),
                CardDefaults.cardColors(containerColor = BlueBg),
                CardDefaults.cardElevation(0.dp)
            ) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("🔬", fontSize = 20.sp)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "Take a clear photo of the affected leaf or plant. The AI will identify the disease and suggest treatment.",
                        fontSize = 12.sp, color = Color(0xFF1565C0), lineHeight = 16.sp
                    )
                }
            }

            // ── Image input section ───────────────────────────
            Card(
                Modifier.fillMaxWidth(), RoundedCornerShape(18.dp),
                CardDefaults.cardColors(containerColor = CardWhite),
                CardDefaults.cardElevation(2.dp)
            ) {
                Column(
                    Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(4.dp, 18.dp)
                            .background(GreenPrimary, RoundedCornerShape(2.dp)))
                        Spacer(Modifier.width(8.dp))
                        Text("Select Crop Image", fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp, color = GreenPrimary)
                    }

                    // Image preview or placeholder
                    Box(
                        Modifier.fillMaxWidth().height(200.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(GreenSurface)
                            .border(
                                if (selectedUri != null) 2.dp else 1.dp,
                                if (selectedUri != null) GreenPrimary else InputBorder,
                                RoundedCornerShape(14.dp)
                            )
                            .clickable { galleryLauncher.launch("image/*") },
                        contentAlignment = Alignment.Center
                    ) {
                        if (selectedUri != null) {
                            AsyncImage(
                                model = selectedUri,
                                contentDescription = "Selected crop image",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(14.dp))
                            )
                            // Overlay badge
                            Box(
                                Modifier.align(Alignment.TopEnd)
                                    .padding(8.dp)
                                    .background(GreenPrimary, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text("✓ Image selected", fontSize = 10.sp,
                                    color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("🌿", fontSize = 40.sp)
                                Spacer(Modifier.height(8.dp))
                                Text("Tap to select from gallery",
                                    fontSize = 13.sp, color = TextSecondary,
                                    fontWeight = FontWeight.Medium)
                                Text("or use buttons below",
                                    fontSize = 11.sp, color = TextHint)
                            }
                        }
                    }

                    // Camera and gallery buttons
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Camera button
                        OutlinedButton(
                            onClick = { launchCamera() },
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                1.5.dp, GreenPrimary),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = GreenPrimary)
                        ) {
                            Icon(Icons.Default.CameraAlt, null,
                                modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Camera", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }

                        // Gallery button
                        OutlinedButton(
                            onClick = { galleryLauncher.launch("image/*") },
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                1.5.dp, Color(0xFF1565C0)),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color(0xFF1565C0))
                        ) {
                            Icon(Icons.Default.Image, null,
                                modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Gallery", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // ── Detect button ─────────────────────────────────
            val isLoading = state is DiseaseUiState.Loading

            // Pulse animation on button when image is ready
            val scale by animateFloatAsState(
                targetValue = if (selectedUri != null && !isLoading) 1f else 0.97f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                label = "btn_scale"
            )

            Button(
                onClick = { viewModel.detect(ctx) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .graphicsLayer { scaleX = scale; scaleY = scale },
                shape = RoundedCornerShape(16.dp),
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selectedUri != null) GreenPrimary
                                     else Color(0xFF9E9E9E)
                )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        Modifier.size(22.dp), color = Color.White, strokeWidth = 2.5.dp)
                    Spacer(Modifier.width(10.dp))
                    Text("Analyzing image...", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                } else {
                    Text("🔍  Detect Disease", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                }
            }

            // ── Loading overlay card ──────────────────────────
            AnimatedVisibility(
                visible = isLoading,
                enter = fadeIn() + expandVertically(),
                exit  = fadeOut() + shrinkVertically()
            ) {
                Card(
                    Modifier.fillMaxWidth(), RoundedCornerShape(16.dp),
                    CardDefaults.cardColors(containerColor = GreenSurface),
                    CardDefaults.cardElevation(0.dp),
                    androidx.compose.foundation.BorderStroke(1.dp, InputBorder)
                ) {
                    Row(
                        Modifier.padding(16.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            Modifier.size(28.dp), color = GreenPrimary, strokeWidth = 3.dp)
                        Spacer(Modifier.width(14.dp))
                        Column {
                            Text("AI is analyzing your image...",
                                fontSize = 14.sp, fontWeight = FontWeight.Bold, color = GreenPrimary)
                            Text("Scanning for crop diseases",
                                fontSize = 12.sp, color = TextSecondary)
                        }
                    }
                }
            }

            // ── Result section ────────────────────────────────
            AnimatedVisibility(
                visible = state is DiseaseUiState.Success,
                enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
                exit  = fadeOut()
            ) {
                if (state is DiseaseUiState.Success) {
                    DiseaseResultCard(result = (state as DiseaseUiState.Success).result)
                }
            }

            // ── Error section ─────────────────────────────────
            AnimatedVisibility(
                visible = state is DiseaseUiState.Error,
                enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
                exit  = fadeOut()
            ) {
                if (state is DiseaseUiState.Error) {
                    val err = state as DiseaseUiState.Error
                    DiseaseErrorCard(
                        error = err,
                        onRetry = { viewModel.detect(ctx) },
                        onReset = { viewModel.reset() }
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

// ── Disease result card ───────────────────────────────────────
@Composable
fun DiseaseResultCard(result: DiseaseResponse) {
    val cropName    = result.crop?.replaceFirstChar { it.uppercase() } ?: "Unknown"
    val diseaseName = result.disease?.replaceFirstChar { it.uppercase() } ?: "Unknown"
    val solution    = result.solution ?: "Consult a local agronomist for treatment advice."
    val confidence  = result.confidence

    val isHealthy = diseaseName.contains("healthy", ignoreCase = true) ||
                    diseaseName.contains("no disease", ignoreCase = true)

    val headerBg    = if (isHealthy) GreenSurface else Color(0xFFFFEBEE)
    val headerBorder = if (isHealthy) Color(0xFFA5D6A7) else Color(0xFFEF9A9A)
    val headerColor = if (isHealthy) GreenPrimary else Color(0xFFC62828)

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {

        Text("Detection Result",
            fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = TextPrimary)

        // ── Main result card ──────────────────────────────────
        Card(
            Modifier.fillMaxWidth(), RoundedCornerShape(18.dp),
            CardDefaults.cardColors(containerColor = headerBg),
            CardDefaults.cardElevation(3.dp),
            androidx.compose.foundation.BorderStroke(1.5.dp, headerBorder)
        ) {
            Column(Modifier.padding(18.dp)) {

                // Status badge
                Box(
                    Modifier.background(
                        headerColor.copy(alpha = 0.12f),
                        RoundedCornerShape(8.dp)
                    ).padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        if (isHealthy) "✅ Healthy Plant" else "⚠️ Disease Detected",
                        fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = headerColor
                    )
                }

                Spacer(Modifier.height(14.dp))

                // Crop row
                ResultRow(
                    icon  = "🌱",
                    label = "Crop",
                    value = cropName,
                    valueColor = TextPrimary
                )

                HorizontalDivider(
                    Modifier.padding(vertical = 10.dp),
                    color = headerBorder, thickness = 0.5.dp
                )

                // Disease row
                ResultRow(
                    icon  = if (isHealthy) "✅" else "🦠",
                    label = "Condition",
                    value = diseaseName,
                    valueColor = headerColor
                )

                // Confidence if available
                confidence?.let { conf ->
                    HorizontalDivider(
                        Modifier.padding(vertical = 10.dp),
                        color = headerBorder, thickness = 0.5.dp
                    )
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Confidence", fontSize = 12.sp, color = TextSecondary)
                        Column(horizontalAlignment = Alignment.End) {
                            Text("${"%.1f".format(conf)}%",
                                fontSize = 16.sp, fontWeight = FontWeight.ExtraBold,
                                color = headerColor)
                            LinearProgressIndicator(
                                progress = { (conf / 100f).coerceIn(0f, 1f) },
                                modifier = Modifier.width(80.dp).height(5.dp),
                                color = headerColor,
                                trackColor = headerBorder,
                                strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                            )
                        }
                    }
                }
            }
        }

        // ── Solution card ─────────────────────────────────────
        if (!isHealthy) {
            Card(
                Modifier.fillMaxWidth(), RoundedCornerShape(16.dp),
                CardDefaults.cardColors(containerColor = CardWhite),
                CardDefaults.cardElevation(2.dp),
                androidx.compose.foundation.BorderStroke(0.5.dp, InputBorder)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(36.dp).background(GreenSurface, CircleShape),
                            contentAlignment = Alignment.Center) {
                            Text("💊", fontSize = 18.sp)
                        }
                        Spacer(Modifier.width(10.dp))
                        Text("Recommended Solution",
                            fontWeight = FontWeight.ExtraBold, fontSize = 14.sp,
                            color = GreenPrimary)
                    }
                    Spacer(Modifier.height(12.dp))
                    HorizontalDivider(color = DividerColor, thickness = 0.5.dp)
                    Spacer(Modifier.height(12.dp))
                    Text(solution, fontSize = 13.sp, color = TextPrimary,
                        lineHeight = 20.sp)
                }
            }

            // ── Expert tip card ───────────────────────────────
            Card(
                Modifier.fillMaxWidth(), RoundedCornerShape(14.dp),
                CardDefaults.cardColors(containerColor = AmberBg),
                CardDefaults.cardElevation(0.dp),
                androidx.compose.foundation.BorderStroke(1.dp, AmberBorder)
            ) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
                    Text("💡", fontSize = 16.sp)
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text("Expert Tip", fontWeight = FontWeight.Bold,
                            fontSize = 12.sp, color = Color(0xFFE65100))
                        Text(
                            "Always isolate affected plants immediately. " +
                            "Apply treatment early in the morning or late evening for best results. " +
                            "Consult your local Krishi Vigyan Kendra for verified advice.",
                            fontSize = 11.sp, color = Color(0xFF5D4037), lineHeight = 16.sp
                        )
                    }
                }
            }
        } else {
            // Healthy plant tips
            Card(
                Modifier.fillMaxWidth(), RoundedCornerShape(14.dp),
                CardDefaults.cardColors(containerColor = GreenSurface),
                CardDefaults.cardElevation(0.dp),
                androidx.compose.foundation.BorderStroke(1.dp, InputBorder)
            ) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
                    Text("🌟", fontSize = 16.sp)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Your plant looks healthy! Keep monitoring regularly and " +
                        "maintain proper irrigation and nutrition to prevent diseases.",
                        fontSize = 12.sp, color = GreenPrimary, lineHeight = 17.sp
                    )
                }
            }
        }
    }
}

// ── Reusable result row ───────────────────────────────────────
@Composable
fun ResultRow(icon: String, label: String, value: String, valueColor: Color) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(icon, fontSize = 16.sp)
            Spacer(Modifier.width(8.dp))
            Text(label, fontSize = 13.sp, color = TextSecondary)
        }
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = valueColor)
    }
}

// ── Error card ────────────────────────────────────────────────
@Composable
fun DiseaseErrorCard(
    error: DiseaseUiState.Error,
    onRetry: () -> Unit,
    onReset: () -> Unit
) {
    val (icon, bg, border) = when (error.type) {
        DiseaseErrorType.NOT_RECOGNIZED -> Triple("🔍", Color(0xFFFFF8E1), AmberBorder)
        DiseaseErrorType.NO_INTERNET    -> Triple("📡", Color(0xFFEDE7F6), Color(0xFFCE93D8))
        DiseaseErrorType.TIMEOUT        -> Triple("⏱️", Color(0xFFFFF8E1), AmberBorder)
        DiseaseErrorType.SERVER         -> Triple("🔧", RedBg, Color(0xFFEF9A9A))
        DiseaseErrorType.NO_IMAGE       -> Triple("📸", GreenSurface, InputBorder)
        else                            -> Triple("⚠️", RedBg, Color(0xFFEF9A9A))
    }

    Card(
        Modifier.fillMaxWidth(), RoundedCornerShape(16.dp),
        CardDefaults.cardColors(containerColor = bg),
        CardDefaults.cardElevation(0.dp),
        androidx.compose.foundation.BorderStroke(1.dp, border)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(icon, fontSize = 24.sp)
                Spacer(Modifier.width(10.dp))
                Text(error.message, fontSize = 13.sp, color = TextPrimary,
                    lineHeight = 18.sp, modifier = Modifier.weight(1f))
            }

            if (error.type != DiseaseErrorType.NO_IMAGE) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onRetry,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, GreenPrimary),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = GreenPrimary)
                    ) { Text("Retry", fontSize = 13.sp, fontWeight = FontWeight.Bold) }

                    TextButton(
                        onClick = onReset,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Try new image", fontSize = 13.sp, color = TextSecondary)
                    }
                }
            }
        }
    }
}
