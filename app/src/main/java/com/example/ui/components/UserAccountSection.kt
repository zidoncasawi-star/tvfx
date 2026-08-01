package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.UserAccountEntity
import com.example.ui.theme.DarkCardBg
import com.example.ui.theme.NetflixRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserAccountSection(
    userAccount: UserAccountEntity?,
    authError: String?,
    isSyncing: Boolean = false,
    importProgress: Int = 0,
    playlistsEmpty: Boolean = true,
    onLogin: (identifier: String, pass: String, adminUrl: String) -> Unit,
    onRegister: (email: String, username: String, pass: String, phone: String) -> Unit,
    onLogout: () -> Unit,
    onImportXtream: (username: String, pass: String) -> Unit = { _, _ -> },
    onCheckActivation: () -> Unit = {},
    onUpdateAdminUrl: (String) -> Unit = {},
    onImportAdminXtream: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var authModeTab by remember { mutableIntStateOf(0) } // 0 = Login, 1 = Register
    var adminUrlInput by remember(userAccount) { mutableStateOf(userAccount?.adminServerUrl ?: "") }

    // Login Fields
    var loginIdentifier by remember { mutableStateOf("") }
    var loginPassword by remember { mutableStateOf("") }
    var loginAdminUrl by remember { mutableStateOf("https://app.flixplayer.pro") }
    var showLoginPassword by remember { mutableStateOf(false) }

    // Register Fields
    var regEmail by remember { mutableStateOf("") }
    var regUsername by remember { mutableStateOf("") }
    var regPhoneCode by remember { mutableStateOf("+966") }
    var regPhone by remember { mutableStateOf("") }
    var regPassword by remember { mutableStateOf("") }
    var regConfirmPassword by remember { mutableStateOf("") }
    var showRegPassword by remember { mutableStateOf(false) }
    var localFormError by remember { mutableStateOf<String?>(null) }
    var showCountryPicker by remember { mutableStateOf(false) }

    val countryCodes = listOf(
        "+966" to "السعودية 🇸🇦",
        "+971" to "الإمارات 🇦🇪",
        "+965" to "الكويت 🇰🇼",
        "+974" to "قطر 🇶🇦",
        "+973" to "البحرين 🇧🇭",
        "+968" to "عمان 🇴🇲",
        "+212" to "المغرب 🇲🇦",
        "+20" to "مصر 🇪🇬",
        "+962" to "الأردن 🇯🇴",
        "+961" to "لبنان 🇱🇧",
        "+213" to "الجزائر 🇩🇿",
        "+216" to "تونس 🇹🇳",
        "+218" to "ليبيا 🇱🇾",
        "+249" to "السودان 🇸🇩",
        "+964" to "العراق 🇮🇶",
        "+963" to "سوريا 🇸🇾",
        "+970" to "فلسطين 🇵🇸",
        "+967" to "اليمن 🇾🇪",
        "+222" to "موريتانيا 🇲🇷",
        "+253" to "جيبوتي 🇩🇯",
        "+252" to "الصومال 🇸🇴",
        "+269" to "جزر القمر 🇰🇲"
    )

    Box(modifier = modifier.fillMaxSize()) {
        if (userAccount != null && userAccount.isLoggedIn) {
            // Logged-in User Profile Dashboard
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkCardBg),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, NetflixRed.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // User Avatar Initial Badge
                        Surface(
                            color = NetflixRed,
                            shape = CircleShape,
                            modifier = Modifier.size(80.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = userAccount.fullName.take(1).uppercase(),
                                    color = Color.White,
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = userAccount.fullName,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )

                        Text(
                            text = "@${userAccount.username}",
                            fontSize = 13.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(top = 2.dp)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        if (userAccount.isActivated) {
                            Surface(
                                color = Color(0xFF2E7D32).copy(alpha = 0.2f),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, Color(0xFF4CAF50).copy(alpha = 0.5f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Verified, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "حساب نشط - عضوية VIP ممتازة",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        } else {
                            Surface(
                                color = NetflixRed.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, NetflixRed.copy(alpha = 0.5f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Error, contentDescription = null, tint = NetflixRed, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "حساب غير نشط - بانتظار تفعيل الاشتراك",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                if (!userAccount.isActivated) {
                    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
                    Card(
                        colors = CardDefaults.cardColors(containerColor = DarkCardBg),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.5.dp, NetflixRed),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = NetflixRed,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "تفعيل الاشتراك والخدمات الخارجية",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(10.dp))
                            
                            Text(
                                text = "مرحباً بك! حسابك مسجل بنجاح ولكنه بانتظار التفعيل من قبل لوحة الإدارة بعد إتمام عملية الاشتراك والدفع.",
                                fontSize = 13.sp,
                                color = Color.LightGray,
                                lineHeight = 18.sp
                            )
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            // Activation Code Box
                            Surface(
                                color = NetflixRed.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, NetflixRed.copy(alpha = 0.4f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("كود التفعيل الخاص بك", fontSize = 11.sp, color = Color.Gray)
                                        Text(
                                            text = userAccount.activationCode,
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color.White
                                        )
                                    }
                                    
                                    Button(
                                        onClick = {
                                            clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(userAccount.activationCode))
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = NetflixRed),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("نسخ الكود", fontSize = 12.sp)
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // Check Activation Button
                            Button(
                                onClick = onCheckActivation,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("تحقق من التفعيل 🔄", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // User Info Details
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkCardBg),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "معلومات الحساب والشخصية",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        AccountDetailRow(icon = Icons.Default.Email, label = "البريد الإلكتروني", value = userAccount.email)
                        Divider(color = Color.White.copy(alpha = 0.08f), modifier = Modifier.padding(vertical = 10.dp))
                        AccountDetailRow(icon = Icons.Default.Person, label = "اسم المستخدم", value = userAccount.username)

                        if (userAccount.phoneNumber.isNotBlank()) {
                            Divider(color = Color.White.copy(alpha = 0.08f), modifier = Modifier.padding(vertical = 10.dp))
                            AccountDetailRow(icon = Icons.Default.Phone, label = "رقم الهاتف", value = userAccount.phoneNumber)
                        }
                    }
                }
            }

            // Admin-assigned Xtream subscription card
            if (userAccount.xtreamHost.isNotBlank()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E3A1E)), // Dark green theme for admin approved/ready subscription
                        shape = RoundedCornerShape(24.dp),
                        border = BorderStroke(1.5.dp, Color(0xFF4CAF50)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CloudDownload,
                                    contentDescription = null,
                                    tint = Color(0xFF4CAF50),
                                    modifier = Modifier.size(26.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "اشتراكك المخصص جاهز للاستيراد ⚡",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = "لقد قام مدير النظام بتعيين اشتراك مخصص ومباشر لحسابك. اضغط على استيراد لتفعيل البث الفوري للمحتوى.",
                                fontSize = 13.sp,
                                color = Color.LightGray,
                                lineHeight = 18.sp
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Button(
                                onClick = onImportAdminXtream,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                            ) {
                                Icon(Icons.Default.CloudDownload, contentDescription = null, tint = Color.White)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("استيراد وتفعيل البث الخاص بي 📥", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }
                    }
                }
            }

            // Manual Xtream Codes Import Card
            if (playlistsEmpty) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = DarkCardBg),
                        shape = RoundedCornerShape(24.dp),
                        border = BorderStroke(1.dp, NetflixRed.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CloudDownload,
                                    contentDescription = null,
                                    tint = NetflixRed,
                                    modifier = Modifier.size(26.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "استيراد البث والمحتوى 🔌",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = "اضغط على زر الاستيراد بالأسفل لبدء تحميل القنوات المباشرة، الأفلام والمسلسلات إلى حسابك وتفعيل البث الفوري للمحتوى.",
                                fontSize = 13.sp,
                                color = Color.LightGray,
                                lineHeight = 18.sp
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Button(
                                onClick = { onImportXtream("4357d392ea", "dd828ce13049") },
                                colors = ButtonDefaults.buttonColors(containerColor = NetflixRed),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                            ) {
                                Icon(Icons.Default.CloudDownload, contentDescription = null, tint = Color.White)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("استيراد وتفعيل البث الخاص بي 📥", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }
                    }
                }
            }


            // Options Card
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkCardBg),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "إعدادات الحساب والأمان",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        OutlinedButton(
                            onClick = onLogout,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = NetflixRed),
                            border = BorderStroke(1.dp, NetflixRed),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Logout, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("تسجيل الخروج من الحساب", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    } else {
        // Unauthenticated Auth Screen (Login / Register)
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                // Brand Header
                Surface(
                    color = NetflixRed.copy(alpha = 0.15f),
                    shape = CircleShape,
                    modifier = Modifier.size(68.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = NetflixRed,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "RODIX TV",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black,
                    color = NetflixRed
                )

                Text(
                    text = "بوابة الحساب الشخصي وتجربة البث الرقمية",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                )

                // Login vs Register Segmented Tabs
                TabRow(
                    selectedTabIndex = authModeTab,
                    containerColor = DarkCardBg,
                    contentColor = NetflixRed,
                    indicator = { tabPositions ->
                        TabRowDefaults.Indicator(
                            color = NetflixRed,
                            modifier = Modifier.tabIndicatorOffset(tabPositions[authModeTab])
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                ) {
                    Tab(
                        selected = authModeTab == 0,
                        onClick = {
                            authModeTab = 0
                            localFormError = null
                        },
                        text = { Text("تسجيل الدخول", fontWeight = FontWeight.Bold, fontSize = 14.sp) },
                        icon = { Icon(Icons.Default.Login, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    )
                    Tab(
                        selected = authModeTab == 1,
                        onClick = {
                            authModeTab = 1
                            localFormError = null
                        },
                        text = { Text("إنشاء حساب جديد", fontWeight = FontWeight.Bold, fontSize = 14.sp) },
                        icon = { Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    )
                }
            }

            // Error Display Banner
            val currentError = localFormError ?: authError
            if (currentError != null) {
                item {
                    Surface(
                        color = NetflixRed.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, NetflixRed.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = NetflixRed)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = currentError, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }

            if (authModeTab == 0) {
                // LOGIN FORM
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = DarkCardBg),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text(
                                text = "تسجيل الدخول إلى حسابك",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )

                            OutlinedTextField(
                                value = loginIdentifier,
                                onValueChange = { loginIdentifier = it },
                                label = { Text("البريد الإلكتروني أو اسم المستخدم") },
                                singleLine = true,
                                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = NetflixRed) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = NetflixRed,
                                    focusedLabelColor = NetflixRed,
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                                )
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = loginPassword,
                                onValueChange = { loginPassword = it },
                                label = { Text("كلمة المرور") },
                                singleLine = true,
                                visualTransformation = if (showLoginPassword) VisualTransformation.None else PasswordVisualTransformation(),
                                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = NetflixRed) },
                                trailingIcon = {
                                    IconButton(onClick = { showLoginPassword = !showLoginPassword }) {
                                        Icon(
                                            imageVector = if (showLoginPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = null,
                                            tint = Color.Gray
                                        )
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = NetflixRed,
                                    focusedLabelColor = NetflixRed,
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                                )
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            Button(
                                onClick = {
                                    if (loginIdentifier.isBlank() || loginPassword.isBlank()) {
                                        localFormError = "يرجى ملء كافة الحقول المطلوبة"
                                    } else {
                                        localFormError = null
                                        onLogin(loginIdentifier, loginPassword, "https://app.flixplayer.pro")
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = NetflixRed),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                            ) {
                                Icon(Icons.Default.Login, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("تسجيل الدخول", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Demo Quick Fill
                            TextButton(
                                onClick = {
                                    loginIdentifier = "user@rodixtv.com"
                                    loginPassword = "123456user"
                                    onRegister("user@rodixtv.com", "rodix_user", "123456user", "+9660500000000")
                                },
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            ) {
                                Icon(Icons.Default.AutoFixHigh, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.LightGray)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("دخول سريع بحساب تجريبي تلقائي", fontSize = 12.sp, color = Color.LightGray)
                            }
                        }
                    }
                }
            } else {
                // REGISTER FORM
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = DarkCardBg),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text(
                                text = "إنشاء حساب جديد في RODIX TV",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )

                            OutlinedTextField(
                                value = regUsername,
                                onValueChange = { regUsername = it },
                                label = { Text("اسم المستخدم (Username)") },
                                singleLine = true,
                                leadingIcon = { Icon(Icons.Default.AlternateEmail, contentDescription = null, tint = NetflixRed) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = NetflixRed,
                                    focusedLabelColor = NetflixRed,
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                                )
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedTextField(
                                value = regEmail,
                                onValueChange = { regEmail = it },
                                label = { Text("البريد الإلكتروني") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = NetflixRed) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = NetflixRed,
                                    focusedLabelColor = NetflixRed,
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                                )
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // Phone with Country Code
                            Column {
                                Text(
                                    text = "رقم الواتساب (إجباري لخدمة العملاء)",
                                    fontSize = 12.sp,
                                    color = Color.LightGray,
                                    modifier = Modifier.padding(bottom = 4.dp, start = 4.dp)
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(modifier = Modifier.weight(0.35f)) {
                                        OutlinedTextField(
                                            value = regPhoneCode,
                                            onValueChange = { },
                                            readOnly = true,
                                            singleLine = true,
                                            modifier = Modifier.fillMaxWidth(),
                                            trailingIcon = {
                                                IconButton(onClick = { showCountryPicker = true }) {
                                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color.Gray)
                                                }
                                            },
                                            colors = OutlinedTextFieldDefaults.colors(
                                                unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                                            )
                                        )
                                        
                                        DropdownMenu(
                                            expanded = showCountryPicker,
                                            onDismissRequest = { showCountryPicker = false },
                                            modifier = Modifier.background(DarkCardBg).heightIn(max = 300.dp)
                                        ) {
                                            countryCodes.forEach { (code, name) ->
                                                DropdownMenuItem(
                                                    text = {
                                                        Row {
                                                            Text(code, color = NetflixRed, fontWeight = FontWeight.Bold)
                                                            Spacer(modifier = Modifier.width(8.dp))
                                                            Text(name, color = Color.White)
                                                        }
                                                    },
                                                    onClick = {
                                                        regPhoneCode = code
                                                        showCountryPicker = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                    
                                    Spacer(modifier = Modifier.width(8.dp))
                                    
                                    OutlinedTextField(
                                        value = regPhone,
                                        onValueChange = { regPhone = it },
                                        placeholder = { Text("رقم الهاتف") },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                        modifier = Modifier.weight(0.65f),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = NetflixRed,
                                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                                        )
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedTextField(
                                value = regPassword,
                                onValueChange = { regPassword = it },
                                label = { Text("كلمة المرور") },
                                singleLine = true,
                                visualTransformation = if (showRegPassword) VisualTransformation.None else PasswordVisualTransformation(),
                                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = NetflixRed) },
                                trailingIcon = {
                                    IconButton(onClick = { showRegPassword = !showRegPassword }) {
                                        Icon(
                                            imageVector = if (showRegPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = null,
                                            tint = Color.Gray
                                        )
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = NetflixRed,
                                    focusedLabelColor = NetflixRed,
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                                )
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedTextField(
                                value = regConfirmPassword,
                                onValueChange = { regConfirmPassword = it },
                                label = { Text("تأكيد كلمة المرور") },
                                singleLine = true,
                                visualTransformation = if (showRegPassword) VisualTransformation.None else PasswordVisualTransformation(),
                                leadingIcon = { Icon(Icons.Default.LockReset, contentDescription = null, tint = NetflixRed) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = NetflixRed,
                                    focusedLabelColor = NetflixRed,
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                                )
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            Button(
                                onClick = {
                                    if (regEmail.isBlank() || regUsername.isBlank() || regPhone.isBlank() || regPassword.isBlank()) {
                                        localFormError = "يرجى إكمال كافة البيانات الأساسية"
                                    } else if (regPassword != regConfirmPassword) {
                                        localFormError = "كلمتا المرور غير متطابقتين"
                                    } else {
                                        localFormError = null
                                        val fullPhone = regPhoneCode + regPhone.trim().removePrefix("0")
                                        onRegister(regEmail, regUsername, regPassword, fullPhone)
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = NetflixRed),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                            ) {
                                Icon(Icons.Default.PersonAdd, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("إنشاء حسابي الآن", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
}

@Composable
private fun AccountDetailRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = icon, contentDescription = null, tint = NetflixRed, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Text(text = label, color = Color.Gray, fontSize = 13.sp)
        }
        Text(text = value, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
}
