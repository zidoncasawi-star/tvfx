package com.example.tv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.example.data.AdminPanelClient
import com.example.tv.theme.TvCard
import com.example.tv.theme.TvFocusBorder
import com.example.tv.theme.TvPanel
import com.example.tv.theme.TvRed
import com.example.tv.theme.TvTextGray
import com.example.tv.theme.TvTextWhite

private const val PREFS_NAME = "tv_notifications_prefs"
private const val KEY_LAST_SEEN_ID = "last_seen_notification_id"

/**
 * الشريط العلوي — يطابق header.topbar في سطح المكتب (شعار | بحث | جرس إشعارات | تسجيل خروج)،
 * ويظهر أعلى كل شاشات المحتوى بدل عدم وجود أي شريط علوي إطلاقاً على التلفاز سابقاً.
 */
@Composable
fun TvTopBar(
    adminUrl: String,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onLogout: () -> Unit,
    hasActiveRecording: Boolean = false,
    onOpenRecordings: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE) }

    var notifications by remember { mutableStateOf<List<AdminPanelClient.AppNotification>>(emptyList()) }
    var showPanel by remember { mutableStateOf(false) }
    var unseenCount by remember { mutableIntStateOf(0) }

    LaunchedEffect(adminUrl) {
        val list = AdminPanelClient.fetchNotifications(adminUrl, platform = "android")
        notifications = list
        val lastSeenId = prefs.getInt(KEY_LAST_SEEN_ID, 0)
        unseenCount = list.count { it.id > lastSeenId }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(TvPanel)
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.weight(1f).widthIn(max = 420.dp)) {
            TvTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = "Search movies, series, channels...",
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(Modifier.weight(1f))

        // مؤشّر تسجيل حالي — يطابق #recIndicatorBtn في سطح المكتب: أيقونة حمراء بجانب جرس
        // الإشعارات مباشرة تظهر فقط أثناء وجود تسجيل قيد التنفيذ فعلياً، وتفتح شاشة التسجيلات
        if (hasActiveRecording) {
            Surface(
                onClick = onOpenRecordings,
                modifier = Modifier.size(42.dp),
                shape = ClickableSurfaceDefaults.shape(shape = CircleShape),
                colors = ClickableSurfaceDefaults.colors(
                    containerColor = TvRed.copy(alpha = 0.22f),
                    focusedContainerColor = TvRed
                ),
                border = ClickableSurfaceDefaults.border(
                    border = Border(androidx.compose.foundation.BorderStroke(0.dp, Color.Transparent)),
                    focusedBorder = Border(androidx.compose.foundation.BorderStroke(2.dp, TvFocusBorder))
                )
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        imageVector = Icons.Default.FiberManualRecord,
                        contentDescription = "Recording in progress",
                        tint = TvRed,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Spacer(Modifier.width(10.dp))
        }

        Box {
            var focused by remember { mutableStateOf(false) }
            Surface(
                onClick = {
                    showPanel = true
                    val maxId = notifications.maxOfOrNull { it.id } ?: 0
                    prefs.edit().putInt(KEY_LAST_SEEN_ID, maxId).apply()
                    unseenCount = 0
                },
                modifier = Modifier.size(42.dp),
                shape = ClickableSurfaceDefaults.shape(shape = CircleShape),
                colors = ClickableSurfaceDefaults.colors(
                    containerColor = Color.White.copy(alpha = 0.06f),
                    focusedContainerColor = Color.White.copy(alpha = 0.18f)
                ),
                border = ClickableSurfaceDefaults.border(
                    border = Border(androidx.compose.foundation.BorderStroke(0.dp, Color.Transparent)),
                    focusedBorder = Border(androidx.compose.foundation.BorderStroke(2.dp, TvFocusBorder))
                )
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(imageVector = Icons.Default.Notifications, contentDescription = "Notifications", tint = TvTextWhite, modifier = Modifier.size(20.dp))
                }
            }
            if (unseenCount > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(16.dp)
                        .background(TvRed, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(if (unseenCount > 9) "9+" else "$unseenCount", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(Modifier.width(14.dp))

        TvOutlineButton(text = "Logout", onClick = onLogout)
    }

    if (showPanel) {
        val panelCloseFocusRequester = remember { FocusRequester() }
        LaunchedEffect(showPanel) {
            panelCloseFocusRequester.requestFocusWithRetry()
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.7f)),
            contentAlignment = Alignment.TopEnd
        ) {
            Box(
                modifier = Modifier
                    .padding(top = 70.dp, end = 24.dp)
                    .width(380.dp)
                    .heightIn(max = 480.dp)
                    .background(TvCard, RoundedCornerShape(12.dp))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Notifications", color = TvTextWhite, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Surface(
                            onClick = { showPanel = false },
                            modifier = Modifier.size(30.dp).focusRequester(panelCloseFocusRequester),
                            shape = ClickableSurfaceDefaults.shape(shape = CircleShape),
                            colors = ClickableSurfaceDefaults.colors(containerColor = Color.White.copy(alpha = 0.08f)),
                            border = ClickableSurfaceDefaults.border(
                                border = Border(androidx.compose.foundation.BorderStroke(0.dp, Color.Transparent)),
                                focusedBorder = Border(androidx.compose.foundation.BorderStroke(2.dp, TvFocusBorder))
                            )
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                Icon(Icons.Default.Close, contentDescription = "Close", tint = TvTextWhite, modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    if (notifications.isEmpty()) {
                        Text("No notifications yet.", color = TvTextGray, fontSize = 13.sp)
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(notifications) { n ->
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color.White.copy(alpha = 0.04f), RoundedCornerShape(8.dp))
                                        .padding(12.dp)
                                ) {
                                    Text(n.title, color = TvTextWhite, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Spacer(Modifier.height(4.dp))
                                    Text(n.content, color = TvTextGray, fontSize = 12.sp, maxLines = 4, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
