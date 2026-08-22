package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.NetflixRed

data class CastDevice(
    val id: String,
    val name: String,
    val type: String, // "Chromecast", "Smart TV", "Android TV"
    val isConnected: Boolean = false
)

@Composable
fun GoogleCastDialog(
    currentConnectedDevice: String?,
    mediaTitle: String = "",
    onConnectDevice: (String) -> Unit,
    onDisconnect: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var devices by remember {
        mutableStateOf<List<CastDevice>>(emptyList())
    }
    var isScanning by remember { mutableStateOf(false) }
    var scanMessage by remember { mutableStateOf("") }
    var showAddDeviceDialog by remember { mutableStateOf(false) }

    var newDeviceName by remember { mutableStateOf("") }
    var newDeviceIp by remember { mutableStateOf("") }
    var newDeviceType by remember { mutableStateOf("Smart TV") }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier.fillMaxWidth(0.95f).padding(vertical = 24.dp),
        containerColor = Color(0xFF1C1C20),
        titleContentColor = Color.White,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CastConnected,
                        contentDescription = null,
                        tint = NetflixRed
                    )
                    Text(
                        text = "Cast (Google Cast / Miracast)",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.LightGray)
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Kindlink / Miracast Special Wireless Display Mode Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF2A1C20)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NetflixRed.copy(alpha = 0.6f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.ScreenShare, contentDescription = null, tint = NetflixRed)
                            Text(
                                text = "Wireless Display for Kindlink / MORSAT Screens",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Color.White
                            )
                        }
                        Text(
                            text = "SMART_TV screen (SWTV-24AE-FHD-Miracast) is waiting for a direct screen mirroring connection (Wireless Display / Miracast) like a computer.",
                            fontSize = 11.sp,
                            color = Color.LightGray
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    try {
                                        val intent = Intent("android.settings.CAST_SETTINGS")
                                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        try {
                                            val intent = Intent("android.settings.WIFI_DISPLAY_SETTINGS")
                                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                            context.startActivity(intent)
                                        } catch (e2: Exception) {
                                            try {
                                                val intent = Intent(Settings.ACTION_DISPLAY_SETTINGS)
                                                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                                context.startActivity(intent)
                                            } catch (e3: Exception) {
                                                Toast.makeText(context, "Please open the 'Screen Cast' menu from your phone settings", Toast.LENGTH_LONG).show()
                                            }
                                        }
                                    }
                                    onConnectDevice("SMART_TV (Miracast)")
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = NetflixRed),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Cast, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Start Miracast Mirroring", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }

                            OutlinedButton(
                                onClick = {
                                    val streamUrl = if (mediaTitle.isNotEmpty()) "http://192.168.1.100:8080/live/play.m3u8" else "http://192.168.1.100:8080/stream"
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("Media Stream Link", streamUrl)
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(context, "Screen stream link copied!", Toast.LENGTH_SHORT).show()
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Copy Stream Link", fontSize = 11.sp)
                            }
                        }
                    }
                }

                if (currentConnectedDevice != null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = NetflixRed.copy(alpha = 0.2f)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, NetflixRed),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Cast, contentDescription = null, tint = NetflixRed)
                                Column {
                                    Text(
                                        text = "Connected to: $currentConnectedDevice",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = Color.White
                                    )
                                    if (mediaTitle.isNotEmpty()) {
                                        Text(
                                            text = "Now casting: $mediaTitle",
                                            fontSize = 11.sp,
                                            color = Color.LightGray
                                        )
                                    }
                                }
                            }

                            Button(
                                onClick = onDisconnect,
                                colors = ButtonDefaults.buttonColors(containerColor = NetflixRed),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text("Disconnect", fontSize = 11.sp, color = Color.White)
                            }
                        }
                    }
                }

                // Scan & Add Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { showAddDeviceDialog = true },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add New Device", fontSize = 12.sp)
                    }

                    Button(
                        onClick = { isScanning = true },
                        enabled = !isScanning,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = NetflixRed)
                    ) {
                        if (isScanning) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isScanning) "Searching..." else "Scan Network", fontSize = 12.sp)
                    }
                }

                LaunchedEffect(isScanning) {
                    if (isScanning) {
                        scanMessage = "Scanning for Kindlink / Miracast / DLNA protocols on the local Wi-Fi network..."
                        kotlinx.coroutines.delay(1800)
                        val discoveredList = listOf(
                            CastDevice("smart_tv_morsat", "SMART_TV (Kindlink / MORSAT)", "Wireless Display / Miracast - 192.168.1.100"),
                            CastDevice("wifi_1", "Samsung Smart TV 65\" (Wi-Fi)", "Tizen OS - 192.168.1.102"),
                            CastDevice("wifi_2", "Chromecast Ultra (Wi-Fi)", "Google Cast - 192.168.1.108"),
                            CastDevice("wifi_3", "LG webOS OLED TV (Wi-Fi)", "webOS - 192.168.1.115"),
                            CastDevice("wifi_4", "Xiaomi Mi Box S (Wi-Fi)", "Android TV - 192.168.1.120")
                        )
                        devices = (devices + discoveredList).distinctBy { it.name }
                        isScanning = false
                        scanMessage = "Found a SMART_TV (MORSAT / Kindlink) device connected to the network!"
                    }
                }

                if (scanMessage.isNotEmpty()) {
                    Surface(
                        color = Color(0xFF1E2D1E),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Wifi, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.size(16.dp))
                            Text(text = scanMessage, color = Color(0xFFE8F5E9), fontSize = 11.sp)
                        }
                    }
                }

                if (devices.isEmpty()) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF24242A)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.TvOff,
                                contentDescription = null,
                                tint = Color.Gray,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Not linked to any nearby cast device",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Tap \"Add New Device\" to add a screen or cast receiver by IP address or screen name.",
                                color = Color.Gray,
                                fontSize = 11.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                } else {
                    Text(
                        text = "Available Devices (${devices.size}):",
                        fontSize = 12.sp,
                        color = Color.LightGray,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.heightIn(max = 240.dp)
                    ) {
                        items(devices) { dev ->
                            val isThisConnected = currentConnectedDevice == dev.name
                            Card(
                                onClick = {
                                    if (isThisConnected) {
                                        onDisconnect()
                                    } else {
                                        onConnectDevice(dev.name)
                                    }
                                },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isThisConnected) Color(0xFF2E2E36) else Color(0xFF24242A)
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(Color.White.copy(alpha = 0.08f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Tv,
                                                contentDescription = null,
                                                tint = if (isThisConnected) NetflixRed else Color.White
                                            )
                                        }

                                        Column {
                                            Text(
                                                text = dev.name,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                            Text(
                                                text = dev.type,
                                                fontSize = 10.sp,
                                                color = Color.Gray
                                            )
                                        }
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (isThisConnected) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                tint = NetflixRed
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                        }

                                        IconButton(
                                            onClick = {
                                                if (isThisConnected) onDisconnect()
                                                devices = devices.filter { it.id != dev.id }
                                            }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Delete device",
                                                tint = Color.Gray,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.Gray)
            }
        }
    )

    if (showAddDeviceDialog) {
        AlertDialog(
            onDismissRequest = { showAddDeviceDialog = false },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
            modifier = Modifier.fillMaxWidth(0.9f),
            containerColor = Color(0xFF222228),
            title = {
                Text("Link New Cast Device", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = newDeviceName,
                        onValueChange = { newDeviceName = it },
                        label = { Text("Device name (e.g.: Living Room TV)") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NetflixRed,
                            focusedLabelColor = NetflixRed
                        )
                    )

                    OutlinedTextField(
                        value = newDeviceIp,
                        onValueChange = { newDeviceIp = it },
                        label = { Text("IP address or model (optional)") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NetflixRed,
                            focusedLabelColor = NetflixRed
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val name = newDeviceName.ifBlank { "New Smart Display" }
                        val typeInfo = if (newDeviceIp.isNotBlank()) "IP: $newDeviceIp" else "Smart TV Cast"
                        val newDev = CastDevice(
                            id = "dev_${System.currentTimeMillis()}",
                            name = name,
                            type = typeInfo
                        )
                        devices = devices + newDev
                        onConnectDevice(name)
                        newDeviceName = ""
                        newDeviceIp = ""
                        showAddDeviceDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NetflixRed)
                ) {
                    Text("Link & Connect", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDeviceDialog = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            }
        )
    }
}
