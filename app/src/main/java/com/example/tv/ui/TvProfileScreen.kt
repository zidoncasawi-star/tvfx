package com.example.tv.ui

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import com.example.model.PlaylistEntity
import com.example.model.UserAccountEntity
import com.example.tv.theme.TvBg
import com.example.tv.theme.TvBorder
import com.example.tv.theme.TvCard
import com.example.tv.theme.TvPanel
import com.example.tv.theme.TvRed
import com.example.tv.theme.TvTextGray
import com.example.tv.theme.TvTextWhite
import com.example.tv.ui.components.TvFocusable
import com.example.tv.ui.components.TvOutlineButton
import com.example.tv.ui.components.TvPrimaryButton
import kotlinx.coroutines.delay

private const val ADMIN_URL = "https://app.flixplayer.pro"

@Composable
private fun TvSettingsRow(label: String, value: @Composable () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = TvTextWhite, fontSize = 14.sp)
        value()
    }
    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(TvBorder))
}

/** شاشة الحساب — نفس بنية #view-profile: أفاتار، بيانات الحساب، كود التفعيل، حالة الاشتراك. */
@Composable
fun TvProfileScreen(
    account: UserAccountEntity,
    playlists: List<PlaylistEntity>,
    onSelectPlaylist: (Long) -> Unit,
    onCheckActivation: () -> Unit,
    onContentUpdate: () -> Unit = {},
    onLogout: () -> Unit
) {
    val expiryText = remember(account.expiresAt) {
        if (account.expiresAt > 0L) {
            java.text.SimpleDateFormat("MMM d, yyyy", java.util.Locale.ENGLISH).format(java.util.Date(account.expiresAt))
        } else "—"
    }
    var showAddPlaylistQr by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier.fillMaxSize().background(TvBg),
        contentAlignment = Alignment.TopCenter
    ) {
        // Modifier.verticalScroll لا يُحرِّك الـ viewport تلقائياً عند تنقّل التركيز بالريموت خارج
        // حدود الشاشة المرئية — LazyColumn وحدها تُبقي العنصر المركَّز ظاهراً دائماً وتُمرِّر تلقائياً
        LazyColumn(
            modifier = Modifier.width(640.dp).fillMaxSize().padding(40.dp)
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(64.dp).clip(CircleShape).background(TvRed),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            account.fullName.take(1).uppercase().ifBlank { "?" },
                            color = TvTextWhite,
                            fontWeight = FontWeight.Black,
                            fontSize = 24.sp
                        )
                    }
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(account.fullName, color = TvTextWhite, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        Text("@${account.username}", color = TvTextGray, fontSize = 13.sp)
                    }
                }
                Spacer(Modifier.height(20.dp))
            }

            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (account.isActivated) Color(0x262ECC71) else Color(0x14FFFFFF),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(14.dp)
                ) {
                    Text(
                        if (account.isActivated) "Subscription Active" else "Waiting for activation",
                        color = if (account.isActivated) Color(0xFF2ECC71) else TvTextGray,
                        fontSize = 13.sp
                    )
                }
                Spacer(Modifier.height(10.dp))
            }

            item { TvSettingsRow("Email") { Text(account.email.ifBlank { "—" }, color = TvTextGray, fontSize = 13.sp) } }
            item { TvSettingsRow("Activation Code") { Text(account.activationCode, color = TvTextWhite, fontSize = 13.sp) } }
            item {
                TvSettingsRow("Subscription Status") {
                    Text(
                        if (account.isActivated) "Active" else "Waiting",
                        color = if (account.isActivated) Color(0xFF2ECC71) else TvTextGray,
                        fontSize = 13.sp
                    )
                }
            }
            item { TvSettingsRow("Expires On") { Text(expiryText, color = TvTextGray, fontSize = 13.sp) } }
            item { TvSettingsRow("Xtream Server") { Text(if (account.xtreamHost.isNotBlank()) "Connected" else "Not configured", color = TvTextGray, fontSize = 13.sp) } }

            if (playlists.size > 1) {
                item {
                    TvSettingsRow("Active Playlist") {
                        Row {
                            playlists.forEach { pl ->
                                TvFocusable(
                                    modifier = Modifier.padding(start = 6.dp),
                                    shape = RoundedCornerShape(14.dp),
                                    onClick = { onSelectPlaylist(pl.id) }
                                ) { _ ->
                                    Box(
                                        modifier = Modifier
                                            .background(if (pl.isActive) TvRed else TvPanel, RoundedCornerShape(14.dp))
                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Text(pl.name, color = TvTextWhite, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(Modifier.height(16.dp))
                TvOutlineButton(text = "+ Add New Playlist", onClick = { showAddPlaylistQr = true })
            }

            item {
                Spacer(Modifier.height(24.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TvOutlineButton(text = "🔄 Check Activation Status", onClick = onCheckActivation)
                    TvOutlineButton(text = "🔄 Content Update", onClick = onContentUpdate)
                    TvPrimaryButton(text = "Logout", onClick = onLogout)
                }
                Spacer(Modifier.height(30.dp))
            }
        }
    }

    if (showAddPlaylistQr) {
        AddPlaylistQrDialog(
            username = account.username,
            activationCode = account.activationCode,
            onDismiss = { showAddPlaylistQr = false }
        )
    }
}

/**
 * يعرض رمز QR يفتح على الهاتف رابطاً يسجّل دخول جلسة الموقع تلقائياً بنفس بيانات حساب هذا
 * الجهاز (عبر login_via_code.php)، فيصل المستخدم مباشرة لصفحة إدارة قوائم تشغيله الفعلية دون
 * الحاجة لكتابة اسم مستخدم أو كلمة مرور يدوياً على شاشة التلفاز.
 */
@Composable
private fun AddPlaylistQrDialog(username: String, activationCode: String, onDismiss: () -> Unit) {
    val closeFocusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        delay(80)
        runCatching { closeFocusRequester.requestFocus() }
    }

    val targetUrl = remember(username, activationCode) {
        "$ADMIN_URL/login_via_code.php?username=${java.net.URLEncoder.encode(username, "UTF-8")}" +
            "&code=${java.net.URLEncoder.encode(activationCode, "UTF-8")}&type=manage"
    }
    val qrImageUrl = remember(targetUrl) {
        "https://api.qrserver.com/v1/create-qr-code/?size=220x220&data=${java.net.URLEncoder.encode(targetUrl, "UTF-8")}"
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.75f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .width(420.dp)
                .background(TvCard, RoundedCornerShape(16.dp))
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Add New Playlist", color = TvTextWhite, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                TvOutlineButton(
                    text = "✕",
                    onClick = onDismiss,
                    modifier = Modifier.focusRequester(closeFocusRequester)
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                "Scan this code with your phone to open your account's playlist page directly:",
                color = TvTextGray,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                lineHeight = 17.sp
            )
            Spacer(Modifier.height(18.dp))
            AsyncImage(
                model = qrImageUrl,
                contentDescription = "Add playlist QR code",
                modifier = Modifier
                    .size(200.dp)
                    .background(Color.White, RoundedCornerShape(10.dp))
                    .padding(10.dp)
            )
        }
    }
}
