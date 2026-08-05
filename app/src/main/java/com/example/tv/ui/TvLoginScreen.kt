package com.example.tv.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import com.example.tv.theme.TvBg
import com.example.tv.theme.TvBorder
import com.example.tv.theme.TvPanel
import com.example.tv.theme.TvRed
import com.example.tv.theme.TvTextGray
import com.example.tv.theme.TvTextWhite
import com.example.tv.ui.components.TvOutlineButton
import com.example.tv.ui.components.TvPrimaryButton
import com.example.tv.ui.components.TvTextField
import kotlinx.coroutines.delay

private const val ADMIN_URL = "https://app.flixplayer.pro"

/**
 * شاشة تسجيل الدخول — نفس بنية #loginScreen في تطبيق سطح المكتب حرفياً: صندوق دخول عادي، فاصل
 * "OR"، وصندوق "ربط عبر الهاتف" (QR)، مع صندوق إنشاء حساب يظهر بدلاً منهما عند الطلب.
 */
@Composable
fun TvLoginScreen(
    authError: String?,
    isSyncing: Boolean,
    onLogin: (identifier: String, password: String) -> Unit,
    onRegister: (email: String, username: String, password: String, phone: String) -> Unit,
    onPairingApproved: (session: org.json.JSONObject) -> Unit
) {
    var showRegister by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TvBg),
        contentAlignment = Alignment.Center
    ) {
        if (!showRegister) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(32.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TvLoginBox(
                    authError = authError,
                    isSyncing = isSyncing,
                    onLogin = onLogin,
                    onShowRegister = { showRegister = true }
                )
                Text("OR", color = TvTextGray, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                TvPhonePairBox(onPairingApproved = onPairingApproved)
            }
        } else {
            TvRegisterBox(
                authError = authError,
                isSyncing = isSyncing,
                onRegister = onRegister,
                onShowLogin = { showRegister = false }
            )
        }
    }
}

@Composable
private fun TvBrand() {
    Row {
        Text("FXTV", color = TvRed, fontWeight = FontWeight.Black, fontSize = 26.sp)
        Text(" PLAYER", color = TvTextWhite, fontWeight = FontWeight.Black, fontSize = 26.sp)
    }
}

@Composable
private fun TvLoginBox(
    authError: String?,
    isSyncing: Boolean,
    onLogin: (String, String) -> Unit,
    onShowRegister: () -> Unit
) {
    var identifier by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .width(380.dp)
            .background(TvPanel, RoundedCornerShape(16.dp))
            .padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TvBrand()
        Spacer(Modifier.height(6.dp))
        Text("Sign in to continue watching", color = TvTextGray, fontSize = 13.sp)
        Spacer(Modifier.height(24.dp))

        TvTextField(
            value = identifier,
            onValueChange = { identifier = it },
            placeholder = "Username or email",
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        TvTextField(
            value = password,
            onValueChange = { password = it },
            placeholder = "Password",
            isPassword = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(16.dp))
        TvPrimaryButton(
            text = if (isSyncing) "Signing in..." else "Sign In",
            enabled = !isSyncing,
            modifier = Modifier.fillMaxWidth(),
            onClick = { onLogin(identifier, password) }
        )
        if (!authError.isNullOrBlank()) {
            Spacer(Modifier.height(10.dp))
            Text(authError, color = Color(0xFFFF5C5C), fontSize = 13.sp, textAlign = TextAlign.Center)
        }
        Spacer(Modifier.height(16.dp))
        Row {
            Text("No account? ", color = TvTextGray, fontSize = 12.sp)
            Text(
                "Create one",
                color = TvRed,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                modifier = Modifier.padding(0.dp)
            )
        }
        Spacer(Modifier.height(4.dp))
        TvOutlineButton(text = "Create Account", onClick = onShowRegister)
    }
}

@Composable
private fun TvPhonePairBox(onPairingApproved: (org.json.JSONObject) -> Unit) {
    var code by remember { mutableStateOf<String?>(null) }
    var statusText by remember { mutableStateOf("Generating code...") }
    var pollTick by remember { mutableStateOf(0) }

    // يولّد كوداً جديداً عند الدخول للشاشة، ثم يستطلع الموافقة كل 3 ثوانٍ — نفس منطق سطح المكتب
    LaunchedEffect(Unit) {
        while (true) {
            val result = com.example.data.AdminPanelClient.createTvPairing(ADMIN_URL)
            if (result.success && result.code != null) {
                code = result.code
                statusText = "Waiting for approval on your phone..."
                var approved = false
                while (!approved) {
                    delay(3000)
                    val check = com.example.data.AdminPanelClient.checkTvPairing(ADMIN_URL, result.code)
                    when (check.status) {
                        "approved" -> {
                            approved = true
                            check.session?.let { onPairingApproved(it) }
                        }
                        "expired" -> {
                            statusText = "Code expired. Generating a new one..."
                            approved = true // يخرج من الحلقة الداخلية ليولّد كوداً جديداً من الخارجية
                        }
                        else -> { /* pending: يستمر بالاستطلاع */ }
                    }
                }
            } else {
                statusText = "Could not generate a code. Retrying..."
                delay(4000)
            }
        }
    }

    Column(
        modifier = Modifier
            .width(340.dp)
            .background(TvPanel, RoundedCornerShape(16.dp))
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TvBrand()
        Spacer(Modifier.height(6.dp))
        Text("Sign in with your phone", color = TvTextGray, fontSize = 13.sp)
        Spacer(Modifier.height(14.dp))
        Text(
            "Open FXTV Player on your phone → Account → Link TV, then scan or enter this code:",
            color = TvTextGray,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            lineHeight = 17.sp
        )
        Spacer(Modifier.height(14.dp))

        if (code != null) {
            val pairUrl = "$ADMIN_URL/pair.php?code=$code&device=tv"
            AsyncImage(
                model = "https://api.qrserver.com/v1/create-qr-code/?size=180x180&data=${java.net.URLEncoder.encode(pairUrl, "UTF-8")}",
                contentDescription = "Pairing QR code",
                modifier = Modifier
                    .size(180.dp)
                    .background(Color.White, RoundedCornerShape(10.dp))
                    .padding(8.dp)
            )
            Spacer(Modifier.height(10.dp))
            Text(
                code ?: "",
                color = TvTextWhite,
                fontWeight = FontWeight.Black,
                fontSize = 24.sp,
                letterSpacing = 6.sp
            )
        } else {
            Box(modifier = Modifier.size(180.dp), contentAlignment = Alignment.Center) {
                Text("···", color = TvTextGray, fontSize = 24.sp)
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(statusText, color = TvTextGray, fontSize = 12.sp, textAlign = TextAlign.Center)
    }
}

@Composable
private fun TvRegisterBox(
    authError: String?,
    isSyncing: Boolean,
    onRegister: (email: String, username: String, password: String, phone: String) -> Unit,
    onShowLogin: () -> Unit
) {
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .width(380.dp)
            .background(TvPanel, RoundedCornerShape(16.dp))
            .padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TvBrand()
        Spacer(Modifier.height(6.dp))
        Text("Create a new account", color = TvTextGray, fontSize = 13.sp)
        Spacer(Modifier.height(20.dp))

        TvTextField(username, { username = it }, "Username", Modifier.fillMaxWidth())
        Spacer(Modifier.height(10.dp))
        TvTextField(email, { email = it }, "Email (optional)", Modifier.fillMaxWidth(), keyboardType = KeyboardType.Email)
        Spacer(Modifier.height(10.dp))
        TvTextField(phone, { phone = it }, "Phone number (optional)", Modifier.fillMaxWidth(), keyboardType = KeyboardType.Phone)
        Spacer(Modifier.height(10.dp))
        TvTextField(password, { password = it }, "Password", Modifier.fillMaxWidth(), isPassword = true)
        Spacer(Modifier.height(10.dp))
        TvTextField(confirmPassword, { confirmPassword = it }, "Confirm password", Modifier.fillMaxWidth(), isPassword = true)
        Spacer(Modifier.height(16.dp))

        TvPrimaryButton(
            text = if (isSyncing) "Creating..." else "Create Account",
            enabled = !isSyncing,
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                localError = when {
                    username.isBlank() || password.isBlank() -> "Username and password are required"
                    password != confirmPassword -> "Passwords do not match"
                    else -> null
                }
                if (localError == null) onRegister(email, username, password, phone)
            }
        )
        val shownError = localError ?: authError
        if (!shownError.isNullOrBlank()) {
            Spacer(Modifier.height(10.dp))
            Text(shownError, color = Color(0xFFFF5C5C), fontSize = 13.sp, textAlign = TextAlign.Center)
        }
        Spacer(Modifier.height(16.dp))
        TvOutlineButton(text = "Already have an account? Sign in", onClick = onShowLogin)
    }
}
