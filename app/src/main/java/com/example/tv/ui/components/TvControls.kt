package com.example.tv.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.OutlinedButton
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import androidx.tv.material3.Button
import com.example.tv.theme.TvBorder
import com.example.tv.theme.TvCard
import com.example.tv.theme.TvFocusBorder
import com.example.tv.theme.TvRed
import com.example.tv.theme.TvTextGray
import com.example.tv.theme.TvTextWhite

/** زر أساسي أحمر — يطابق .btn-primary في تطبيق سطح المكتب */
@Composable
fun TvPrimaryButton(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        colors = ButtonDefaults.colors(
            containerColor = TvRed,
            contentColor = TvTextWhite,
            focusedContainerColor = Color(0xFFFF1A24),
            focusedContentColor = TvTextWhite
        ),
        // بدون حدّ مرئي عند التركيز، الفرق بين درجتي الأحمر (TvRed <-> #FF1A24) شبه غير مرئي على
        // شاشة التلفاز — يبدو للمستخدم أن الزر غير مركَّز عليه إطلاقاً رغم أنه هو المركَّز فعلياً
        border = ButtonDefaults.border(
            border = Border(androidx.compose.foundation.BorderStroke(0.dp, Color.Transparent)),
            focusedBorder = Border(androidx.compose.foundation.BorderStroke(3.dp, TvFocusBorder))
        ),
        shape = ButtonDefaults.shape(shape = RoundedCornerShape(8.dp))
    ) {
        Text(text)
    }
}

/** زر مفرّغ — يطابق .btn-outline */
@Composable
fun TvOutlineButton(
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        colors = androidx.tv.material3.OutlinedButtonDefaults.colors(
            containerColor = Color.White.copy(alpha = 0.06f),
            contentColor = TvTextWhite,
            focusedContainerColor = Color.White.copy(alpha = 0.16f),
            focusedContentColor = TvTextWhite
        ),
        border = androidx.tv.material3.OutlinedButtonDefaults.border(
            border = Border(androidx.compose.foundation.BorderStroke(1.dp, TvBorder)),
            focusedBorder = Border(androidx.compose.foundation.BorderStroke(2.dp, TvFocusBorder))
        ),
        shape = androidx.tv.material3.OutlinedButtonDefaults.shape(shape = RoundedCornerShape(8.dp))
    ) {
        Text(text)
    }
}

/**
 * حاوية قابلة للتركيز عبر D-pad مع تكبير خفيف وحدّ أحمر عند التركيز — المعادِل على التلفاز
 * لتأثير ":hover { transform: scale(1.05) }" في .poster-card على سطح المكتب.
 */
@Composable
fun TvFocusable(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onFocus: () -> Unit = {},
    shape: RoundedCornerShape = RoundedCornerShape(10.dp),
    content: @Composable (isFocused: Boolean) -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (focused) 1.08f else 1f, label = "tvFocusScale")

    Surface(
        onClick = onClick,
        modifier = modifier
            .scale(scale)
            .onFocusChanged {
                focused = it.isFocused
                if (it.isFocused) onFocus()
            },
        shape = ClickableSurfaceDefaults.shape(shape = shape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = TvCard,
            focusedContainerColor = TvCard,
            pressedContainerColor = TvCard
        ),
        border = ClickableSurfaceDefaults.border(
            border = Border(androidx.compose.foundation.BorderStroke(1.dp, Color.Transparent)),
            focusedBorder = Border(androidx.compose.foundation.BorderStroke(3.dp, TvFocusBorder))
        ),
        // التكبير عند التركيز مُطبَّق يدوياً أعلاه عبر Modifier.scale — نُلغي تكبير Surface المدمج
        // افتراضياً حتى لا يتضاعف الأثران معاً
        scale = ClickableSurfaceDefaults.scale(scale = 1f, focusedScale = 1f, pressedScale = 1f)
    ) {
        content(focused)
    }
}

/** حقل إدخال نصي مهيّأ للتركيز عبر جهاز التحكم — يطابق شكل حقول .login-box input */
@Composable
fun TvTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    isPassword: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    var focused by remember { mutableStateOf(false) }
    val focusRequester = remember { androidx.compose.ui.focus.FocusRequester() }
    val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
    Surface(
        // الضغط على "موافق" بالريموت هنا فقط يُركّز الـ Surface نفسه (بحدّه الأحمر)، لكن لا يفتح
        // لوحة مفاتيح التلفاز تلقائياً — لازم نطلب التركيز صراحة على BasicTextField الداخلي
        // ونستدعي show() على متحكم لوحة المفاتيح البرمجية يدوياً
        onClick = {
            focusRequester.requestFocus()
            keyboardController?.show()
        },
        modifier = modifier
            .onFocusChanged { focused = it.isFocused },
        shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(8.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color(0xFF0F0F0F),
            focusedContainerColor = Color(0xFF0F0F0F)
        ),
        border = ClickableSurfaceDefaults.border(
            border = Border(androidx.compose.foundation.BorderStroke(1.dp, TvBorder)),
            focusedBorder = Border(androidx.compose.foundation.BorderStroke(2.dp, TvFocusBorder))
        )
    ) {
        Box(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            androidx.compose.foundation.text.BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = TvTextWhite),
                cursorBrush = androidx.compose.ui.graphics.SolidColor(TvRed),
                visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                modifier = Modifier
                    .focusRequester(focusRequester)
                    .onFocusChanged { if (it.isFocused) keyboardController?.show() },
                decorationBox = { inner ->
                    if (value.isEmpty()) {
                        Text(placeholder, color = TvTextGray)
                    }
                    inner()
                }
            )
        }
    }
}
