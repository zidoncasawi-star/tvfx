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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import com.example.model.PlaylistEntity
import com.example.model.UserAccountEntity
import com.example.tv.theme.TvBg
import com.example.tv.theme.TvBorder
import com.example.tv.theme.TvPanel
import com.example.tv.theme.TvRed
import com.example.tv.theme.TvTextGray
import com.example.tv.theme.TvTextWhite
import com.example.tv.ui.components.TvFocusable
import com.example.tv.ui.components.TvOutlineButton
import com.example.tv.ui.components.TvPrimaryButton

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
    onAddNewPlaylist: () -> Unit = {},
    onLogout: () -> Unit
) {
    val expiryText = remember(account.expiresAt) {
        if (account.expiresAt > 0L) {
            java.text.SimpleDateFormat("MMM d, yyyy", java.util.Locale.ENGLISH).format(java.util.Date(account.expiresAt))
        } else "—"
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TvBg)
            .verticalScroll(rememberScrollState())
            .padding(40.dp),
        contentAlignment = Alignment.TopCenter
    ) {
    Column(modifier = Modifier.width(640.dp)) {
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
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    if (account.isActivated) androidx.compose.ui.graphics.Color(0x262ECC71) else androidx.compose.ui.graphics.Color(0x14FFFFFF),
                    RoundedCornerShape(8.dp)
                )
                .padding(14.dp)
        ) {
            Text(
                if (account.isActivated) "Subscription Active" else "Waiting for activation",
                color = if (account.isActivated) androidx.compose.ui.graphics.Color(0xFF2ECC71) else TvTextGray,
                fontSize = 13.sp
            )
        }

        Spacer(Modifier.height(10.dp))
        Column(modifier = Modifier.fillMaxWidth()) {
            TvSettingsRow("Email") { Text(account.email.ifBlank { "—" }, color = TvTextGray, fontSize = 13.sp) }
            TvSettingsRow("Activation Code") { Text(account.activationCode, color = TvTextWhite, fontSize = 13.sp) }
            TvSettingsRow("Subscription Status") {
                Text(
                    if (account.isActivated) "Active" else "Waiting",
                    color = if (account.isActivated) androidx.compose.ui.graphics.Color(0xFF2ECC71) else TvTextGray,
                    fontSize = 13.sp
                )
            }
            TvSettingsRow("Expires On") { Text(expiryText, color = TvTextGray, fontSize = 13.sp) }
            TvSettingsRow("Xtream Server") { Text(if (account.xtreamHost.isNotBlank()) "Connected" else "Not configured", color = TvTextGray, fontSize = 13.sp) }

            if (playlists.size > 1) {
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

        Spacer(Modifier.height(16.dp))
        TvOutlineButton(text = "+ Add New Playlist", onClick = onAddNewPlaylist)

        Spacer(Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TvOutlineButton(text = "🔄 Check Activation Status", onClick = onCheckActivation)
            TvOutlineButton(text = "🔄 Content Update", onClick = onContentUpdate)
            TvPrimaryButton(text = "Logout", onClick = onLogout)
        }
    }
    }
}
