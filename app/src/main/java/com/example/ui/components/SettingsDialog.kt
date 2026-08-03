package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.util.LocalizationHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsDialog(
    currentLanguage: String,
    currentVideoPlayer: String,
    currentAccentColorHex: String,
    currentStreamQuality: String,
    currentAutoRefresh: Boolean,
    activePlaylistName: String?,
    onLanguageChange: (String) -> Unit,
    onVideoPlayerChange: (String) -> Unit,
    onAccentColorChange: (String) -> Unit,
    onStreamQualityChange: (String) -> Unit,
    onAutoRefreshChange: (Boolean) -> Unit,
    onClearCache: () -> Unit,
    onDismiss: () -> Unit
) {
    val scrollState = rememberScrollState()

    // Helper translation local shorthand
    fun t(key: String): String {
        return LocalizationHelper.translate(key, currentLanguage)
    }

    val isRtl = currentLanguage == "ar"
    val uriHandler = LocalUriHandler.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF16161A)),
            border = BorderStroke(1.dp, Color(currentAccentColorHex.substring(1).toLong(16) or 0xFF000000).copy(alpha = 0.25f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = null,
                            tint = Color(android.graphics.Color.parseColor(currentAccentColorHex))
                        )
                        Text(
                            text = t("settings_title"),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(32.dp)
                            .background(Color.White.copy(alpha = 0.05f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = t("close"),
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Settings List Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(scrollState),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Language Selection Section
                    SettingsSection(title = t("app_language"), icon = Icons.Default.Language, accentColorHex = currentAccentColorHex) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val languages = listOf("ar" to "العربية", "en" to "English", "fr" to "Français")
                            languages.forEach { (code, name) ->
                                val isSelected = currentLanguage == code
                                val borderCol = if (isSelected) Color(android.graphics.Color.parseColor(currentAccentColorHex)) else Color.White.copy(alpha = 0.08f)
                                val bgCol = if (isSelected) Color(android.graphics.Color.parseColor(currentAccentColorHex)).copy(alpha = 0.15f) else Color.Transparent

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(bgCol)
                                        .clickable { onLanguageChange(code) }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = name,
                                        color = if (isSelected) Color(android.graphics.Color.parseColor(currentAccentColorHex)) else Color.LightGray,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    }

                    // Theme Color Settings
                    SettingsSection(title = t("theme") + " (" + t("accent_color") + ")", icon = Icons.Default.Palette, accentColorHex = currentAccentColorHex) {
                        val colors = listOf(
                            "#E50914" to "أحمر",      // Netflix Red
                            "#FFC107" to "ذهبي",     // Amber Gold
                            "#00E5FF" to "أزرق نيون",  // Neon Blue
                            "#00E676" to "أخضر",     // Emerald Green
                            "#9C27B0" to "بنفسجي"     // Purple
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            colors.forEach { (hex, name) ->
                                val isSelected = currentAccentColorHex.uppercase() == hex.uppercase()
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color(android.graphics.Color.parseColor(hex)))
                                        .clickable { onAccentColorChange(hex) }
                                        .padding(2.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(Color.Black.copy(alpha = 0.3f), CircleShape)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = name,
                                                tint = Color.White,
                                                modifier = Modifier
                                                    .size(16.dp)
                                                    .align(Alignment.Center)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Video Player Preference
                    SettingsSection(title = t("video_player"), icon = Icons.Default.PlayCircle, accentColorHex = currentAccentColorHex) {
                        val players = listOf("ExoPlayer", "VLC Player", "MX Player", "System Player")
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            players.forEach { p ->
                                val isSelected = currentVideoPlayer == p
                                val desc = when (p) {
                                    "ExoPlayer" -> t("exoplayer_desc")
                                    "VLC Player" -> t("vlc_desc")
                                    "MX Player" -> t("mx_desc")
                                    else -> t("system_desc")
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isSelected) Color(android.graphics.Color.parseColor(currentAccentColorHex)).copy(alpha = 0.08f) else Color.White.copy(alpha = 0.02f))
                                        .clickable { onVideoPlayerChange(p) }
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = { onVideoPlayerChange(p) },
                                        colors = RadioButtonDefaults.colors(selectedColor = Color(android.graphics.Color.parseColor(currentAccentColorHex)))
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = p,
                                            color = if (isSelected) Color.White else Color.LightGray,
                                            fontSize = 14.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                        )
                                        Text(
                                            text = desc,
                                            color = Color.Gray,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Default Streaming Quality Selection
                    SettingsSection(title = t("stream_quality"), icon = Icons.Default.HighQuality, accentColorHex = currentAccentColorHex) {
                        val qualities = listOf(
                            "Auto" to t("auto"),
                            "FHD (1080p)" to t("fhd"),
                            "HD (720p)" to t("hd"),
                            "SD (480p)" to t("sd")
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            qualities.forEach { (code, label) ->
                                val isSelected = currentStreamQuality == code
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) Color(android.graphics.Color.parseColor(currentAccentColorHex)) else Color.White.copy(alpha = 0.04f))
                                        .clickable { onStreamQualityChange(code) }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label.substringBefore(" ("),
                                        color = if (isSelected) Color.White else Color.Gray,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }

                    // Auto Refresh EPG Toggle & Clear Cache
                    SettingsSection(title = t("clear_cache"), icon = Icons.Default.DeleteSweep, accentColorHex = currentAccentColorHex) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.03f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = t("clear_cache_desc"),
                                    color = Color.Gray,
                                    fontSize = 12.sp
                                )

                                Button(
                                    onClick = onClearCache,
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(android.graphics.Color.parseColor(currentAccentColorHex))),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth(),
                                    contentPadding = PaddingValues(vertical = 8.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = t("clear_cache"),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    // Active subscription metadata
                    if (activePlaylistName != null) {
                        SettingsSection(title = t("playlists"), icon = Icons.Default.CloudQueue, accentColorHex = currentAccentColorHex) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.White.copy(alpha = 0.03f))
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF4CAF50),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = activePlaylistName,
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = t("server") + ": VIP Stream Link",
                                        color = Color.Gray,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }

                    // Legal: Terms and Conditions / Privacy Policy
                    SettingsSection(title = t("legal"), icon = Icons.Default.Gavel, accentColorHex = currentAccentColorHex) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val legalLinks = listOf(
                                Triple(t("terms_conditions"), Icons.Default.Description, "https://app.flixplayer.pro/terms.php"),
                                Triple(t("privacy_policy"), Icons.Default.PrivacyTip, "https://app.flixplayer.pro/privacy.php")
                            )
                            legalLinks.forEach { (label, icon, url) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color.White.copy(alpha = 0.03f))
                                        .clickable { uriHandler.openUri(url) }
                                        .padding(horizontal = 12.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = null,
                                            tint = Color.LightGray,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = label,
                                            color = Color.White,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.Default.OpenInNew,
                                        contentDescription = null,
                                        tint = Color.Gray,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Footer Actions
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.08f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = t("close"),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsSection(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColorHex: String,
    content: @Composable () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color(android.graphics.Color.parseColor(accentColorHex)),
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
        content()
    }
}
