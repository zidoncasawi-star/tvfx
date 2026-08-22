package com.example.tv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.Surface
import androidx.compose.foundation.Image
import com.example.R
import com.example.model.MainTab
import com.example.tv.theme.TvFocusBorder
import com.example.tv.theme.TvPanel
import com.example.tv.theme.TvRed
import com.example.tv.theme.TvTextGray
import com.example.tv.theme.TvTextWhite

private data class TvNavItem(val tab: MainTab, val icon: androidx.compose.ui.graphics.vector.ImageVector)

private val NAV_ITEMS = listOf(
    TvNavItem(MainTab.HOME, Icons.Default.Home),
    TvNavItem(MainTab.LIVE_TV, Icons.Default.Tv),
    TvNavItem(MainTab.MOVIES, Icons.Default.Movie),
    TvNavItem(MainTab.SERIES, Icons.Default.VideoLibrary),
    TvNavItem(MainTab.FAVORITES, Icons.Default.Favorite)
)

/**
 * الشريط الجانبي الأيقوني — نفس بنية .icon-rail في تطبيق سطح المكتب (68dp، شعار أعلاه، أزرار
 * دائرية، اللون الأحمر للتبويب النشط). على التلفاز التركيز (D-pad) هو معادِل hover/click.
 */
@Composable
fun TvNavRail(
    selectedTab: MainTab,
    onSelectTab: (MainTab) -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .width(76.dp)
            .background(TvPanel)
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(id = R.drawable.app_logo),
            contentDescription = "FXTV Player",
            contentScale = ContentScale.Fit,
            modifier = Modifier.size(46.dp)
        )
        Spacer(modifier = Modifier.padding(top = 18.dp))

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            NAV_ITEMS.forEach { item ->
                TvRailButton(
                    icon = item.icon,
                    active = selectedTab == item.tab,
                    onClick = { onSelectTab(item.tab) }
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            TvRailButton(
                icon = Icons.Default.AccountCircle,
                active = selectedTab == MainTab.USER_ACCOUNT,
                onClick = { onSelectTab(MainTab.USER_ACCOUNT) }
            )
            TvRailButton(
                icon = Icons.Default.Settings,
                active = false,
                onClick = onOpenSettings
            )
        }
    }
}

@Composable
private fun TvRailButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    active: Boolean,
    onClick: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    val bg = when {
        active -> TvRed
        focused -> Color.White.copy(alpha = 0.12f)
        else -> Color.Transparent
    }
    Surface(
        onClick = onClick,
        modifier = Modifier
            .size(44.dp)
            .onFocusChanged { focused = it.isFocused },
        shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(12.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = bg,
            focusedContainerColor = if (active) TvRed else Color.White.copy(alpha = 0.14f)
        ),
        border = ClickableSurfaceDefaults.border(
            border = Border(androidx.compose.foundation.BorderStroke(0.dp, Color.Transparent)),
            focusedBorder = Border(androidx.compose.foundation.BorderStroke(2.dp, TvFocusBorder))
        )
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (active || focused) TvTextWhite else TvTextGray,
            modifier = Modifier.padding(11.dp)
        )
    }
}
