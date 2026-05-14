package com.example.smartkrishi

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import kotlinx.coroutines.launch

class LoginActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (FirebaseAuthHelper.isLoggedIn) {
            startActivity(
                Intent(this, HomeActivity::class.java)
                    .putExtra("USER_MOBILE", FirebaseAuthHelper.getUserEmail())
            )
            finish()
            return
        }

        setContent { SmartKrishiTheme { LoginRegisterScreen() } }
    }
}

@Composable
fun LoginRegisterScreen() {
    var tab by remember { mutableStateOf(0) }

    Column(Modifier.fillMaxSize().background(PageBg)) {

        // ── Top green header ─────────────────────────────────
        Box(
            Modifier.fillMaxWidth().background(GreenPrimary)
                .padding(24.dp, 40.dp, 24.dp, 28.dp)
        ) {
            Column {
                // App logo row
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(40.dp)
                            .background(Color.White.copy(.18f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) { Text("🌱", fontSize = 20.sp) }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text("Smart Krishi", fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold, color = Color.White)
                        Text("AI Crop Advisor", fontSize = 11.sp,
                            color = Color.White.copy(.65f))
                    }
                }
                Spacer(Modifier.height(20.dp))
                Text(
                    if (tab == 0) "Welcome back 👋" else "Create account",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
                Text(
                    if (tab == 0) "Sign in to continue"
                    else "Start your smart farming journey",
                    fontSize = 14.sp,
                    color = Color.White.copy(.72f)
                )
            }
        }

        // Curved white section
        Box(Modifier.fillMaxWidth().background(GreenPrimary)) {
            Box(Modifier.fillMaxWidth().height(28.dp).background(
                PageBg, RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
            ))
        }

        // ── Tab switcher ─────────────────────────────────────
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp)
                .background(CardWhite, RoundedCornerShape(14.dp)).padding(4.dp)
        ) {
            listOf("Sign In", "Sign Up").forEachIndexed { i, lbl ->
                Box(
                    Modifier.weight(1f)
                        .background(
                            if (tab == i) GreenPrimary else Color.Transparent,
                            RoundedCornerShape(12.dp)
                        )
                        .clickable { tab = i }
                        .padding(vertical = 13.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(lbl,
                        color = if (tab == i) Color.White else TextSecondary,
                        fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
        }

        Spacer(Modifier.height(4.dp))
        if (tab == 0) SignInForm { tab = 1 } else SignUpForm { tab = 0 }
    }
}

// ── Sign In ───────────────────────────────────────────────────
@Composable
fun SignInForm(onSwitch: () -> Unit) {
    var email    by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var loading  by remember { mutableStateOf(false) }
    val ctx      = LocalContext.current
    val scope    = rememberCoroutineScope()

    val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestIdToken(ctx.getString(R.string.default_web_client_id))
        .requestEmail()
        .build()
    val googleClient = remember { GoogleSignIn.getClient(ctx, gso) }

    val googleLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            scope.launch {
                loading = true
                val res = FirebaseAuthHelper.loginWithGoogle(account.idToken!!)
                loading = false
                if (res.isSuccess) {
                    val user = res.getOrNull()!!
                    Toast.makeText(ctx,
                        "Welcome, ${user.displayName}! 🌾",
                        Toast.LENGTH_SHORT).show()
                    ctx.startActivity(
                        Intent(ctx, HomeActivity::class.java)
                            .putExtra("USER_MOBILE", user.email ?: "")
                    )
                    (ctx as LoginActivity).finish()
                } else {
                    Toast.makeText(ctx,
                        "Google sign-in failed. Try again.",
                        Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: ApiException) {
            Toast.makeText(ctx,
                "Google error: ${e.statusCode}", Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {

        // Google button — shown FIRST (modern standard)
        GoogleButton(loading) {
            googleLauncher.launch(googleClient.signInIntent)
        }

        // OR divider
        OrDivider()

        // Email field
        ModernField("Email address", email, { email = it },
            Icons.Default.Email, KeyboardType.Email)

        // Password field
        ModernField("Password", password, { password = it },
            Icons.Default.Lock, isPassword = true)

        // Forgot password
        Text(
            "Forgot password?",
            color = GreenPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.align(Alignment.End)
                .clickable {
                    if (email.isBlank()) {
                        Toast.makeText(ctx,
                            "Enter your email first", Toast.LENGTH_SHORT).show()
                    } else {
                        scope.launch {
                            FirebaseAuthHelper.sendPasswordReset(email.trim())
                            Toast.makeText(ctx,
                                "Reset link sent to $email 📧",
                                Toast.LENGTH_LONG).show()
                        }
                    }
                }
        )

        // Sign In button
        Button(
            onClick = {
                when {
                    email.isBlank()    ->
                        Toast.makeText(ctx, "Enter email", Toast.LENGTH_SHORT).show()
                    password.isBlank() ->
                        Toast.makeText(ctx, "Enter password", Toast.LENGTH_SHORT).show()
                    else -> scope.launch {
                        loading = true
                        val res = FirebaseAuthHelper.loginWithEmail(
                            email.trim(), password.trim()
                        )
                        loading = false
                        if (res.isSuccess) {
                            ctx.startActivity(
                                Intent(ctx, HomeActivity::class.java)
                                    .putExtra("USER_MOBILE", email.trim())
                            )
                            (ctx as LoginActivity).finish()
                        } else {
                            val msg = res.exceptionOrNull()?.message ?: ""
                            Toast.makeText(ctx,
                                when {
                                    msg.contains("password", true) ->
                                        "Incorrect password. Try again."
                                    msg.contains("no user", true) ||
                                            msg.contains("user not found", true) ->
                                        "No account found. Sign up first."
                                    msg.contains("network", true) ->
                                        "No internet connection."
                                    msg.contains("blocked", true) ->
                                        "Too many attempts. Try later."
                                    else -> "Sign in failed. Try again."
                                },
                                Toast.LENGTH_LONG).show()
                        }
                    }
                }
            },
            Modifier.fillMaxWidth().height(54.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
            enabled = !loading
        ) {
            if (loading) CircularProgressIndicator(
                Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp
            ) else Text("Sign In", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
        }

        // Switch to sign up
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Don't have an account? ", fontSize = 14.sp, color = TextSecondary)
            Text("Sign Up", fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold, color = GreenPrimary,
                modifier = Modifier.clickable { onSwitch() })
        }

        Spacer(Modifier.height(16.dp))
    }
}

// ── Sign Up ───────────────────────────────────────────────────
@Composable
fun SignUpForm(onSwitch: () -> Unit) {
    var name     by remember { mutableStateOf("") }
    var email    by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirm  by remember { mutableStateOf("") }
    var loading  by remember { mutableStateOf(false) }
    var agreeTerms by remember { mutableStateOf(false) }
    val ctx   = LocalContext.current
    val scope = rememberCoroutineScope()
    val db    = remember { DatabaseHelper(ctx) }

    val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestIdToken(ctx.getString(R.string.default_web_client_id))
        .requestEmail()
        .build()
    val googleClient = remember { GoogleSignIn.getClient(ctx, gso) }

    val googleLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            scope.launch {
                loading = true
                val res = FirebaseAuthHelper.loginWithGoogle(account.idToken!!)
                loading = false
                if (res.isSuccess) {
                    val user = res.getOrNull()!!
                    db.registerUser(
                        user.displayName ?: "Farmer",
                        user.email ?: "", "", ""
                    )
                    Toast.makeText(ctx,
                        "Account created! Welcome 🌾",
                        Toast.LENGTH_SHORT).show()
                    ctx.startActivity(
                        Intent(ctx, HomeActivity::class.java)
                            .putExtra("USER_MOBILE", user.email ?: "")
                    )
                    (ctx as LoginActivity).finish()
                } else {
                    Toast.makeText(ctx,
                        "Google sign-up failed. Try again.",
                        Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: ApiException) {
            Toast.makeText(ctx,
                "Google error: ${e.statusCode}", Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {

        // Google sign up first
        GoogleButton(loading, label = "Sign up with Google") {
            googleLauncher.launch(googleClient.signInIntent)
        }

        OrDivider()

        // Name field
        ModernField("Full name", name, { name = it }, Icons.Default.Person)

        // Email field
        ModernField("Email address", email, { email = it },
            Icons.Default.Email, KeyboardType.Email)

        // Password field with strength indicator
        ModernField("Password (min 6 chars)", password, { password = it },
            Icons.Default.Lock, isPassword = true)

        // Password strength bar
        if (password.isNotEmpty()) {
            PasswordStrengthBar(password)
        }

        // Confirm password
        ModernField("Confirm password", confirm, { confirm = it },
            Icons.Default.Lock, isPassword = true,
            isError = confirm.isNotEmpty() && confirm != password,
            errorMsg = "Passwords do not match"
        )

        // Terms checkbox
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Checkbox(
                checked = agreeTerms,
                onCheckedChange = { agreeTerms = it },
                colors = CheckboxDefaults.colors(checkedColor = GreenPrimary)
            )
            Spacer(Modifier.width(4.dp))
            Text("I agree to the ", fontSize = 13.sp, color = TextSecondary)
            Text("Terms & Privacy Policy", fontSize = 13.sp,
                color = GreenPrimary, fontWeight = FontWeight.SemiBold)
        }

        // Create account button
        Button(
            onClick = {
                when {
                    name.isBlank()         ->
                        Toast.makeText(ctx, "Enter your name", Toast.LENGTH_SHORT).show()
                    email.isBlank()        ->
                        Toast.makeText(ctx, "Enter email", Toast.LENGTH_SHORT).show()
                    !email.contains("@")   ->
                        Toast.makeText(ctx, "Enter a valid email", Toast.LENGTH_SHORT).show()
                    password.length < 6    ->
                        Toast.makeText(ctx, "Password must be 6+ characters",
                            Toast.LENGTH_SHORT).show()
                    password != confirm    ->
                        Toast.makeText(ctx, "Passwords do not match",
                            Toast.LENGTH_SHORT).show()
                    !agreeTerms            ->
                        Toast.makeText(ctx, "Please agree to Terms & Privacy Policy",
                            Toast.LENGTH_SHORT).show()
                    else -> scope.launch {
                        loading = true
                        val res = FirebaseAuthHelper.registerWithEmail(
                            email.trim(), password.trim()
                        )
                        loading = false
                        if (res.isSuccess) {
                            db.registerUser(
                                name.trim(), email.trim(), "", password.trim()
                            )
                            Toast.makeText(ctx,
                                "Welcome, ${name.trim()}! 🌱",
                                Toast.LENGTH_SHORT).show()
                            ctx.startActivity(
                                Intent(ctx, HomeActivity::class.java)
                                    .putExtra("USER_MOBILE", email.trim())
                            )
                            (ctx as LoginActivity).finish()
                        } else {
                            val msg = res.exceptionOrNull()?.message ?: ""
                            Toast.makeText(ctx,
                                when {
                                    msg.contains("already", true) ->
                                        "Email already registered. Sign in instead."
                                    msg.contains("network", true) ->
                                        "No internet connection."
                                    msg.contains("invalid", true) ->
                                        "Invalid email address."
                                    else -> "Sign up failed. Try again."
                                },
                                Toast.LENGTH_LONG).show()
                        }
                    }
                }
            },
            Modifier.fillMaxWidth().height(54.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
            enabled = !loading
        ) {
            if (loading) CircularProgressIndicator(
                Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp
            ) else Text("Create Account", fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold)
        }

        // Switch to sign in
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Already have an account? ", fontSize = 14.sp, color = TextSecondary)
            Text("Sign In", fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold, color = GreenPrimary,
                modifier = Modifier.clickable { onSwitch() })
        }

        Spacer(Modifier.height(16.dp))
    }
}

// ── Reusable composables ──────────────────────────────────────

@Composable
fun GoogleButton(loading: Boolean, label: String = "Continue with Google",
                 onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        Modifier.fillMaxWidth().height(54.dp),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, InputBorder),
        enabled = !loading,
        colors = ButtonDefaults.outlinedButtonColors(containerColor = CardWhite)
    ) {
        Box(
            Modifier.size(24.dp)
                .background(Color.White, RoundedCornerShape(4.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text("G", fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold, color = Color(0xFFDB4437))
        }
        Spacer(Modifier.width(12.dp))
        Text(label, fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold, color = TextPrimary)
    }
}

@Composable
fun OrDivider() {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        HorizontalDivider(Modifier.weight(1f), color = InputBorder)
        Text("  or  ", fontSize = 12.sp, color = TextSecondary)
        HorizontalDivider(Modifier.weight(1f), color = InputBorder)
    }
}

@Composable
fun ModernField(
    label: String, value: String, onValue: (String) -> Unit,
    icon: ImageVector, keyboard: KeyboardType = KeyboardType.Text,
    isPassword: Boolean = false,
    isError: Boolean = false,
    errorMsg: String = ""
) {
    var show by remember { mutableStateOf(false) }
    Column {
        OutlinedTextField(
            value = value, onValueChange = onValue,
            label = { Text(label, fontSize = 13.sp) },
            leadingIcon = {
                Icon(icon, null, tint = if (isError) Color.Red else GreenPrimary,
                    modifier = Modifier.size(20.dp))
            },
            trailingIcon = if (isPassword) {{
                IconButton({ show = !show }) {
                    Icon(
                        if (show) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        null, tint = TextSecondary, modifier = Modifier.size(18.dp)
                    )
                }
            }} else null,
            visualTransformation = if (isPassword && !show)
                PasswordVisualTransformation() else VisualTransformation.None,
            keyboardOptions = KeyboardOptions(keyboardType = keyboard),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            singleLine = true,
            isError = isError,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor      = GreenPrimary,
                unfocusedBorderColor    = InputBorder,
                errorBorderColor        = Color.Red,
                focusedLabelColor       = GreenPrimary,
                unfocusedLabelColor     = TextSecondary,
                focusedContainerColor   = InputBg,
                unfocusedContainerColor = InputBg
            )
        )
        if (isError && errorMsg.isNotBlank()) {
            Text(errorMsg, color = Color.Red, fontSize = 11.sp,
                modifier = Modifier.padding(start = 16.dp, top = 2.dp))
        }
    }
}

@Composable
fun PasswordStrengthBar(password: String) {
    val strength = when {
        password.length >= 10 &&
                password.any { it.isUpperCase() } &&
                password.any { it.isDigit() } &&
                password.any { "!@#\$%^&*".contains(it) } -> 4
        password.length >= 8 &&
                password.any { it.isUpperCase() } &&
                password.any { it.isDigit() } -> 3
        password.length >= 6 &&
                (password.any { it.isUpperCase() } || password.any { it.isDigit() }) -> 2
        else -> 1
    }
    val (color, label) = when (strength) {
        4    -> Color(0xFF2E7D32) to "Strong 💪"
        3    -> Color(0xFF558B2F) to "Good"
        2    -> Color(0xFFF57F17) to "Fair"
        else -> Color(0xFFC62828) to "Weak"
    }
    Column {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            repeat(4) { i ->
                Box(
                    Modifier.weight(1f).height(4.dp)
                        .background(
                            if (i < strength) color else InputBorder,
                            RoundedCornerShape(2.dp)
                        )
                )
            }
        }
        Text("Password strength: $label", fontSize = 11.sp,
            color = color, modifier = Modifier.padding(top = 3.dp))
    }
}