package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.UserProfileEntity
import com.example.ui.theme.NetflixRed

@Composable
fun ProfileManagementDialog(
    profiles: List<UserProfileEntity>,
    activeProfile: UserProfileEntity?,
    onSwitchProfile: (UserProfileEntity) -> Unit,
    onCreateProfile: (name: String, avatarColorHex: String, isKids: Boolean, pinCode: String) -> Unit,
    onDeleteProfile: (UserProfileEntity) -> Unit,
    onDismiss: () -> Unit
) {
    var isCreatingNew by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    var newIsKids by remember { mutableStateOf(false) }
    var newPinCode by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf("#E50914") }

    val colorOptions = listOf("#E50914", "#0084FF", "#00C853", "#AA00FF", "#FFD600", "#FF6D00")

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier.fillMaxWidth(0.95f),
        containerColor = Color(0xFF1E1E24),
        titleContentColor = Color.White,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.AccountCircle, contentDescription = null, tint = NetflixRed)
                    Text(
                        text = if (isCreatingNew) "إضافة بروفايل جديد" else "إدارة بروفايلات المستخدمين",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = Color.Gray)
                }
            }
        },
        text = {
            if (isCreatingNew) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("اسم البروفايل") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NetflixRed,
                            unfocusedBorderColor = Color.Gray,
                            focusedLabelColor = NetflixRed,
                            unfocusedLabelColor = Color.LightGray,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("اختر لون الأيقونة:", fontSize = 12.sp, color = Color.LightGray)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(colorOptions) { colorHex ->
                            val parsedColor = try {
                                Color(android.graphics.Color.parseColor(colorHex))
                            } catch (e: Exception) {
                                NetflixRed
                            }
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(parsedColor)
                                    .border(
                                        width = if (selectedColor == colorHex) 3.dp else 0.dp,
                                        color = if (selectedColor == colorHex) Color.White else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .clickable { selectedColor = colorHex }
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("وضع الأطفال", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Text("تقييد المحتوى المناسب للأعمار الصغيرة فقط", fontSize = 10.sp, color = Color.Gray)
                        }
                        Switch(
                            checked = newIsKids,
                            onCheckedChange = { newIsKids = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = NetflixRed, checkedTrackColor = NetflixRed.copy(alpha = 0.5f))
                        )
                    }

                    OutlinedTextField(
                        value = newPinCode,
                        onValueChange = { if (it.length <= 4) newPinCode = it },
                        label = { Text("رمز PIN لحماية البروفايل (اختياري)") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NetflixRed,
                            unfocusedBorderColor = Color.Gray,
                            focusedLabelColor = NetflixRed,
                            unfocusedLabelColor = Color.LightGray,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("اختر البروفايل النشط للتبديل إليه:", fontSize = 12.sp, color = Color.LightGray)

                    profiles.forEach { profile ->
                        val isCurrent = activeProfile?.id == profile.id
                        val avatarColor = try {
                            Color(android.graphics.Color.parseColor(profile.avatarColorHex))
                        } catch (e: Exception) {
                            NetflixRed
                        }

                        Card(
                            onClick = {
                                if (!isCurrent) {
                                    onSwitchProfile(profile)
                                    onDismiss()
                                }
                            },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isCurrent) NetflixRed.copy(alpha = 0.15f) else Color(0xFF282830)
                            ),
                            border = if (isCurrent) androidx.compose.foundation.BorderStroke(1.dp, NetflixRed) else null,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(avatarColor),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = profile.name.take(1).uppercase(),
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp
                                        )
                                    }

                                    Column {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = profile.name,
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp
                                            )
                                            if (profile.isKids) {
                                                Surface(
                                                    color = Color(0xFF0084FF),
                                                    shape = RoundedCornerShape(4.dp)
                                                ) {
                                                    Text(
                                                        text = "أطفال",
                                                        fontSize = 9.sp,
                                                        color = Color.White,
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                        }

                                        if (profile.pinCode.isNotEmpty()) {
                                            Text(
                                                text = "محمي برمز PIN",
                                                fontSize = 10.sp,
                                                color = Color.Gray
                                            )
                                        }
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (isCurrent) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = NetflixRed,
                                            modifier = Modifier.padding(end = 8.dp)
                                        )
                                    }

                                    if (profiles.size > 1 && !isCurrent) {
                                        IconButton(onClick = { onDeleteProfile(profile) }) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "حذف البروفايل",
                                                tint = Color.Gray,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Button(
                        onClick = { isCreatingNew = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2C2C36)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("إنشاء بروفايل جديد", color = Color.White)
                    }
                }
            }
        },
        confirmButton = {
            if (isCreatingNew) {
                Button(
                    onClick = {
                        if (newName.isNotBlank()) {
                            onCreateProfile(newName, selectedColor, newIsKids, newPinCode)
                            isCreatingNew = false
                            newName = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NetflixRed)
                ) {
                    Text("حفظ البروفايل", color = Color.White)
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    if (isCreatingNew) {
                        isCreatingNew = false
                    } else {
                        onDismiss()
                    }
                }
            ) {
                Text(if (isCreatingNew) "تراجع" else "إلغاء", color = Color.Gray)
            }
        }
    )
}
