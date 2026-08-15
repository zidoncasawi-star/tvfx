package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.media.MediaPlayer
import com.example.R
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.NetflixRed
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// مدة intro.mp3 الفعلية ~9.9 ثانية — شاشة البداية يجب أن تبقى معروضة لمدته الكاملة كي لا يُقطع
// الصوت في المنتصف؛ شريط التقدّم البصري مضبوط على نفس هذه المدة تماماً ليكتملا معاً
private const val SPLASH_DURATION_MS = 9900

@Composable
fun SplashScreen(onFinish: () -> Unit) {
    val context = LocalContext.current
    val scale = remember { Animatable(0.6f) }
    val alpha = remember { Animatable(0f) }
    val progress = remember { Animatable(0f) }

    DisposableEffect(Unit) {
        val player = MediaPlayer.create(context, R.raw.intro)
        player?.start()
        onDispose {
            runCatching {
                if (player?.isPlaying == true) player.stop()
                player?.release()
            }
        }
    }

    LaunchedEffect(Unit) {
        launch {
            scale.animateTo(
                targetValue = 1f,
                animationSpec = tween(1200, easing = FastOutSlowInEasing)
            )
        }
        launch {
            alpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(1000)
            )
        }
        launch {
            progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(SPLASH_DURATION_MS, easing = LinearEasing)
            )
        }
        delay(SPLASH_DURATION_MS.toLong())
        onFinish()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                painter = painterResource(id = R.drawable.app_logo),
                contentDescription = "FXTV Player",
                contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                modifier = Modifier
                    .size(100.dp)
                    .scale(scale.value)
                    .alpha(alpha.value)
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .scale(scale.value)
                    .alpha(alpha.value)
            ) {
                Text(
                    text = "FLIX",
                    color = NetflixRed,
                    fontWeight = FontWeight.Black,
                    fontSize = 60.sp,
                    letterSpacing = 4.sp
                )
                Text(
                    text = "TV",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 60.sp,
                    letterSpacing = 4.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Premium Streaming Experience",
                color = Color.White.copy(alpha = 0.5f * alpha.value),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.alpha(alpha.value)
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 64.dp)
                .alpha(alpha.value)
        ) {
            Text(
                text = "Loading...",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .width(160.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White.copy(alpha = 0.15f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(progress.value)
                        .clip(RoundedCornerShape(2.dp))
                        .background(NetflixRed)
                )
            }
        }
    }
}
