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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import com.example.tv.theme.TvBg
import com.example.tv.theme.TvBorder
import com.example.tv.theme.TvRed
import com.example.tv.theme.TvTextGray
import com.example.tv.theme.TvTextWhite
import com.example.tv.ui.components.TvFocusable
import com.example.tv.ui.components.TvOutlineButton
import com.example.tv.ui.components.TvPrimaryButton

@Composable
private fun TvGroupTitle(text: String) {
    Text(
        text.uppercase(),
        color = TvTextGray,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.5.sp,
        modifier = Modifier.padding(top = 22.dp, bottom = 6.dp)
    )
}

@Composable
private fun TvToggleRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = TvTextWhite, fontSize = 14.sp)
        TvFocusable(shape = RoundedCornerShape(20.dp), onClick = { onCheckedChange(!checked) }) { _ ->
            Box(
                modifier = Modifier
                    .width(44.dp)
                    .height(24.dp)
                    .background(if (checked) TvRed else Color.White.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
                    .padding(3.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .align(if (checked) Alignment.CenterEnd else Alignment.CenterStart)
                        .background(Color.White, RoundedCornerShape(50))
                )
            }
        }
    }
    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(TvBorder))
}

/** شاشة الإعدادات — نفس بنية #view-settings (Playback / App / Legal) بنفس التسميات. */
@Composable
fun TvSettingsScreen(
    autoplayEnabled: Boolean,
    onAutoplayChange: (Boolean) -> Unit,
    activePlaylistName: String?,
    onClearCache: () -> Unit,
    onClearFavorites: () -> Unit,
    onLogout: () -> Unit,
    onOpenTerms: () -> Unit,
    onOpenPrivacy: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(TvBg)
            .verticalScroll(rememberScrollState())
            .padding(40.dp)
    ) {
        Text("Settings", color = TvTextWhite, fontWeight = FontWeight.Black, fontSize = 26.sp)

        Column(modifier = Modifier.width(560.dp)) {
            TvGroupTitle("Playback")
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Stream Source", color = TvTextWhite, fontSize = 14.sp)
                Text(activePlaylistName ?: "Direct from Xtream server", color = TvTextGray, fontSize = 13.sp)
            }
            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(TvBorder))
            TvToggleRow("Autoplay Next Episode", autoplayEnabled, onAutoplayChange)

            TvGroupTitle("App")
            Spacer(Modifier.height(4.dp))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                TvOutlineButton(text = "Clear Local Cache", onClick = onClearCache, modifier = Modifier.fillMaxWidth())
                TvOutlineButton(text = "Clear Favorites & History", onClick = onClearFavorites, modifier = Modifier.fillMaxWidth())
                TvPrimaryButton(text = "Logout", onClick = onLogout, modifier = Modifier.fillMaxWidth())
            }

            TvGroupTitle("Legal")
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                TvOutlineButton(text = "Terms and Conditions", onClick = onOpenTerms, modifier = Modifier.fillMaxWidth())
                TvOutlineButton(text = "Privacy Policy", onClick = onOpenPrivacy, modifier = Modifier.fillMaxWidth())
            }

            Spacer(Modifier.height(30.dp))
            Text("FXTV Player TV · v1.0", color = TvTextGray, fontSize = 12.sp, modifier = Modifier.fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
    }
}
