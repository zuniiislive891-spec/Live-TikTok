package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FlashlightOff
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Web
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.service.ExecutionResult
import com.example.service.PcConnectionState
import com.example.service.PcResult
import com.example.ui.components.PcDownloadDialog
import com.example.ui.viewmodel.RobbyViewModel
import kotlinx.coroutines.launch

@Composable
fun DeviceControlScreen(
    viewModel: RobbyViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()
    val userProfile by viewModel.userProfile.collectAsState()
    val isUrdu = userProfile.language == "ur"

    var selectedControlTab by remember { mutableIntStateOf(0) } // 0 = Mobile, 1 = PC Control
    var isFlashlightOn by remember { mutableStateOf(false) }
    var toastMessage by remember { mutableStateOf<String?>(null) }
    var showPcDownloadDialog by remember { mutableStateOf(false) }
    var showScriptInstructions by remember { mutableStateOf(false) }

    // PC Controller states
    val pcHost by viewModel.pcController.pcHost.collectAsState()
    val pcPort by viewModel.pcController.pcPort.collectAsState()
    val pcConnectionState by viewModel.pcController.connectionState.collectAsState()
    val pcLastStatusMessage by viewModel.pcController.lastStatusMessage.collectAsState()

    var pcHostInput by remember(pcHost) { mutableStateOf(pcHost) }
    var typeTextInput by remember { mutableStateOf("") }
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Apps,
                contentDescription = "Apps",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
            Text(
                text = if (isUrdu) "کنٹرول سینٹر (موبائل اور کمپیوٹر)" else "APP & PC FULL CONTROL HUB",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                ),
                color = MaterialTheme.colorScheme.primary
            )
        }

        // Mode Selector: Mobile Controls vs PC Full Control
        TabRow(
            selectedTabIndex = selectedControlTab,
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.primary,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedControlTab]),
                    color = MaterialTheme.colorScheme.primary
                )
            },
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
        ) {
            Tab(
                selected = selectedControlTab == 0,
                onClick = { selectedControlTab = 0 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(imageVector = Icons.Default.PhoneAndroid, contentDescription = null, modifier = Modifier.size(16.dp))
                        Text(
                            text = if (isUrdu) "موبائل کنٹرول" else "Mobile Apps",
                            fontWeight = if (selectedControlTab == 0) FontWeight.Bold else FontWeight.Normal,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp
                        )
                    }
                },
                modifier = Modifier.testTag("tab_mobile_control")
            )
            Tab(
                selected = selectedControlTab == 1,
                onClick = { selectedControlTab = 1 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(imageVector = Icons.Default.Computer, contentDescription = null, modifier = Modifier.size(16.dp))
                        Text(
                            text = if (isUrdu) "کمپیوٹر فل کنٹرول" else "PC Full Control",
                            fontWeight = if (selectedControlTab == 1) FontWeight.Bold else FontWeight.Normal,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp
                        )
                    }
                },
                modifier = Modifier.testTag("tab_pc_control")
            )
        }

        // =====================================================================
        // TAB 1: MOBILE APPS & SYSTEM HARDWARE
        // =====================================================================
        if (selectedControlTab == 0) {
            // 1. Hardware Utilities Card (Flashlight & Timers)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = if (isUrdu) "ہارڈ ویئر اور ٹولز" else "MOBILE HARDWARE UTILITIES",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    // Flashlight Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(if (isFlashlightOn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isFlashlightOn) Icons.Default.FlashlightOn else Icons.Default.FlashlightOff,
                                    contentDescription = "Torch",
                                    tint = if (isFlashlightOn) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = if (isUrdu) "فلیش لائٹ / ٹارچ" else "Device Flashlight",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (isFlashlightOn) "Active / روشن" else "Standby / بند",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Switch(
                            checked = isFlashlightOn,
                            onCheckedChange = { checked ->
                                isFlashlightOn = checked
                                val res = viewModel.actionExecutor.toggleFlashlight(checked)
                                toastMessage = if (checked) "Flashlight ON" else "Flashlight OFF"
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                checkedTrackColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.testTag("flashlight_switch")
                        )
                    }

                    // Quick Timers
                    Text(
                        text = if (isUrdu) "فوری ٹائمرز:" else "Quick Timers:",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val timers = listOf(
                            60 to (if (isUrdu) "1 منٹ" else "1 Min"),
                            300 to (if (isUrdu) "5 منٹ" else "5 Min"),
                            900 to (if (isUrdu) "15 منٹ" else "15 Min")
                        )
                        timers.forEach { (sec, label) ->
                            Surface(
                                onClick = {
                                    viewModel.actionExecutor.setTimer(sec, "Robby $label Timer")
                                    toastMessage = "Timer started for $label"
                                },
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("timer_preset_$sec")
                            ) {
                                Row(
                                    modifier = Modifier.padding(vertical = 10.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Timer,
                                        contentDescription = "Timer",
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.size(4.dp))
                                    Text(
                                        text = label,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 2. Mobile App Launch Matrix
            Text(
                text = if (isUrdu) "موبائل ایپس لانچ میٹرکس (صوتی اور 1-ٹیپ)" else "MOBILE APPLICATION LAUNCH MATRIX",
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            val mobileApps = listOf(
                AppLauncherItem("YouTube", "com.google.android.youtube", Icons.Default.PlayArrow, if (isUrdu) "یوٹیوب ویڈیوز" else "Videos / Live"),
                AppLauncherItem("Chrome", "com.android.chrome", Icons.Default.Web, if (isUrdu) "گوگل کروم" else "Fast Web"),
                AppLauncherItem("Camera", null, Icons.Default.CameraAlt, if (isUrdu) "کیمرہ ریکارڈنگ" else "HD Capture"),
                AppLauncherItem("Maps", "com.google.android.apps.maps", Icons.Default.Map, if (isUrdu) "گوگل میپس" else "Navigation"),
                AppLauncherItem("Phone", null, Icons.Default.Phone, if (isUrdu) "ڈائلر / فون" else "Dialer / Calls"),
                AppLauncherItem("Calculator", "com.google.android.calculator", Icons.Default.Calculate, if (isUrdu) "کیلکولیٹر" else "Math / Utility"),
                AppLauncherItem("Clock", "com.google.android.deskclock", Icons.Default.Timer, if (isUrdu) "گھڑی اور الارم" else "Alarms"),
                AppLauncherItem("Settings", "com.android.settings", Icons.Default.Settings, if (isUrdu) "سسٹم سیٹنگز" else "OS Config")
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                mobileApps.chunked(2).forEach { pair ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        pair.forEach { item ->
                            Surface(
                                onClick = {
                                    val res = viewModel.actionExecutor.openApp(item.name, item.packageName)
                                    toastMessage = when (res) {
                                        is ExecutionResult.Success -> res.message
                                        is ExecutionResult.Failure -> res.error
                                    }
                                },
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("app_launch_${item.name.lowercase()}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = item.icon,
                                            contentDescription = item.name,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = item.name,
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = item.desc,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }
                        }
                        if (pair.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        // =====================================================================
        // TAB 2: PC FULL CONTROL (کمپیوٹر فل کنٹرول)
        // =====================================================================
        if (selectedControlTab == 1) {
            // 1. Connection Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(imageVector = Icons.Default.Computer, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            Text(
                                text = if (isUrdu) "پی سی کنکشن سیٹنگز" else "PC CONNECTION LINK",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        // Connection Status Badge
                        val (statusBadgeText, badgeColor) = when (pcConnectionState) {
                            PcConnectionState.CONNECTED -> "CONNECTED 🟢" to Color(0xFF00E676)
                            PcConnectionState.CONNECTING -> "CONNECTING 🟡" to Color(0xFFFFD600)
                            PcConnectionState.ERROR -> "OFFLINE 🔴" to Color(0xFFFF5252)
                            PcConnectionState.DISCONNECTED -> "STANDBY ⚪" to MaterialTheme.colorScheme.onSurfaceVariant
                        }
                        Text(
                            text = statusBadgeText,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = badgeColor
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = pcHostInput,
                            onValueChange = {
                                pcHostInput = it
                                viewModel.pcController.updatePcAddress(it, pcPort)
                            },
                            label = { Text(if (isUrdu) "کمپیوٹر IP (مثلاً 192.168.1.5)" else "PC IP (e.g. 192.168.1.5)", fontSize = 11.sp) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("pc_ip_input_field")
                        )

                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    val res = viewModel.pcController.pingPc(pcHostInput, pcPort)
                                    toastMessage = when (res) {
                                        is PcResult.Success -> res.message
                                        is PcResult.Failure -> res.error
                                    }
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier.testTag("ping_pc_button")
                        ) {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = "Ping", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = if (isUrdu) "کنیکٹ" else "Ping", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Text(
                        text = pcLastStatusMessage,
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 2. PC Apps Launchers (1-Tap & Voice Directives)
            Text(
                text = if (isUrdu) "کمپیوٹر ایپس لانچ میٹرکس" else "PC APPLICATION LAUNCH MATRIX",
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            val pcAppItems = listOf(
                PcAppAction("YouTube", Icons.Default.PlayArrow, "open_url", "https://www.youtube.com", if (isUrdu) "یوٹیوب براؤزر" else "Browser Tab"),
                PcAppAction("Chrome", Icons.Default.Web, "open_app", "chrome", if (isUrdu) "گوگل کروم" else "Browser"),
                PcAppAction("Notepad", Icons.Default.Code, "open_app", "notepad", if (isUrdu) "نوٹ پیڈ" else "Text Editor"),
                PcAppAction("Calculator", Icons.Default.Calculate, "open_app", "calc", if (isUrdu) "کیلکولیٹر" else "Desktop Calc"),
                PcAppAction("Explorer", Icons.Default.Folder, "open_app", "explorer", if (isUrdu) "فائل مینیجر" else "Files"),
                PcAppAction("CMD / Terminal", Icons.Default.Terminal, "open_app", "cmd", if (isUrdu) "کمانڈ پرامپٹ" else "Terminal"),
                PcAppAction("Spotify", Icons.Default.MusicNote, "open_app", "spotify", if (isUrdu) "اسپاٹیفائی" else "Music"),
                PcAppAction("VS Code", Icons.Default.Code, "open_app", "vscode", if (isUrdu) "وی ایس کوڈ" else "Code Editor")
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                pcAppItems.chunked(2).forEach { pair ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        pair.forEach { item ->
                            Surface(
                                onClick = {
                                    coroutineScope.launch {
                                        val res = viewModel.pcController.sendCommand(item.action, item.target)
                                        toastMessage = when (res) {
                                            is PcResult.Success -> res.message
                                            is PcResult.Failure -> res.error
                                        }
                                    }
                                },
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("pc_launch_${item.name.lowercase().replace(" ", "_")}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = item.icon,
                                            contentDescription = item.name,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = item.name,
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = item.desc,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 3. PC Media & Master Volume Controls
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = if (isUrdu) "پی سی میڈیا اور والیم کنٹرول" else "PC MEDIA & VOLUME REMOTE",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val mediaActions = listOf(
                            Triple("vol_up", Icons.Default.VolumeUp, if (isUrdu) "آواز +" else "Vol +"),
                            Triple("vol_down", Icons.Default.VolumeDown, if (isUrdu) "آواز -" else "Vol -"),
                            Triple("mute", Icons.Default.VolumeMute, if (isUrdu) "میوٹ" else "Mute"),
                            Triple("play_pause", Icons.Default.PlayArrow, if (isUrdu) "پلے/پاز" else "Play/Pause")
                        )

                        mediaActions.forEach { (cmd, icon, label) ->
                            Surface(
                                onClick = {
                                    coroutineScope.launch {
                                        val res = viewModel.pcController.sendCommand("media", cmd)
                                        toastMessage = when (res) {
                                            is PcResult.Success -> res.message
                                            is PcResult.Failure -> res.error
                                        }
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("pc_media_$cmd")
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = label,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = label,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 4. PC Power & Security Controls
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = if (isUrdu) "پی سی پاور اور سیکیورٹی (لاک/شٹ ڈاؤن)" else "PC POWER & SECURITY DIRECTIVES",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    val res = viewModel.pcController.sendCommand("power", "lock")
                                    toastMessage = when (res) {
                                        is PcResult.Success -> res.message
                                        is PcResult.Failure -> res.error
                                    }
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("pc_lock_button")
                        ) {
                            Icon(imageVector = Icons.Default.Lock, contentDescription = "Lock", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = if (isUrdu) "اسکرین لاک" else "Lock PC", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    val res = viewModel.pcController.sendCommand("power", "shutdown")
                                    toastMessage = when (res) {
                                        is PcResult.Success -> res.message
                                        is PcResult.Failure -> res.error
                                    }
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("pc_shutdown_button")
                        ) {
                            Icon(imageVector = Icons.Default.PowerSettingsNew, contentDescription = "Shutdown", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = if (isUrdu) "شٹ ڈاؤن" else "Shutdown", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    val res = viewModel.pcController.sendCommand("power", "restart")
                                    toastMessage = when (res) {
                                        is PcResult.Success -> res.message
                                        is PcResult.Failure -> res.error
                                    }
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.tertiary,
                                contentColor = MaterialTheme.colorScheme.onTertiary
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("pc_restart_button")
                        ) {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = "Restart", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = if (isUrdu) "ری سٹارٹ" else "Restart", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // 5. Remote Typing / Keystrokes to PC
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = if (isUrdu) "کمپیوٹر پر کی بورڈ ٹائپنگ بھیجیں" else "REMOTE KEYSTROKES & TYPING",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = typeTextInput,
                            onValueChange = { typeTextInput = it },
                            placeholder = { Text(if (isUrdu) "پی سی پر جو لکھنا ہو درج کریں..." else "Type text to send to PC...", fontSize = 12.sp) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            ),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                            keyboardActions = KeyboardActions(onSend = {
                                if (typeTextInput.isNotBlank()) {
                                    coroutineScope.launch {
                                        val res = viewModel.pcController.sendCommand("type", typeTextInput)
                                        typeTextInput = ""
                                        toastMessage = when (res) {
                                            is PcResult.Success -> res.message
                                            is PcResult.Failure -> res.error
                                        }
                                    }
                                }
                            }),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("pc_type_text_input")
                        )

                        Button(
                            onClick = {
                                if (typeTextInput.isNotBlank()) {
                                    coroutineScope.launch {
                                        val res = viewModel.pcController.sendCommand("type", typeTextInput)
                                        typeTextInput = ""
                                        toastMessage = when (res) {
                                            is PcResult.Success -> res.message
                                            is PcResult.Failure -> res.error
                                        }
                                    }
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("pc_send_type_button")
                        ) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.Send, contentDescription = "Send", modifier = Modifier.size(16.dp))
                        }
                    }

                    // Quick Special Keys
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("enter" to "⏎ Enter", "esc" to "⎋ Esc", "space" to "␣ Space", "desktop" to "Win+D").forEach { (key, label) ->
                            Surface(
                                onClick = {
                                    coroutineScope.launch {
                                        viewModel.pcController.sendCommand("key", key)
                                    }
                                },
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(vertical = 6.dp),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }

            // 6. Python PC Bridge Setup Guide (1-Click Copy Script)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isUrdu) "پی سی سیٹ اپ گائیڈ اور پائتھن اسکرپٹ" else "PC BRIDGE SERVER SETUP SCRIPT",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        IconButton(
                            onClick = { showScriptInstructions = !showScriptInstructions },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = if (showScriptInstructions) Icons.Default.Refresh else Icons.Default.Code,
                                contentDescription = "Toggle",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Text(
                        text = if (isUrdu) {
                            "اپنے کمپیوٹر پر پائتھن اسکرپٹ چلائیں تاکہ روبائی اے آئی آپ کے پی سی کو مکمل کنٹرول کر سکے۔ کسی انسٹالیشن کی ضرورت نہیں۔"
                        } else {
                            "Run the lightweight 1-file Python script on your Windows, Mac, or Linux computer to enable full PC control with zero external packages."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val script = viewModel.pcController.getPythonBridgeScript()
                                clipboardManager.setText(AnnotatedString(script))
                                Toast.makeText(context, "robby_pc_bridge.py copied to clipboard!", Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("copy_pc_script_button")
                        ) {
                            Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = if (isUrdu) "اسکرپٹ کاپی کریں" else "Copy Python Script", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = { showPcDownloadDialog = true },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("open_pc_portal_dialog_button")
                        ) {
                            Icon(imageVector = Icons.Default.Download, contentDescription = "Download", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = if (isUrdu) "ڈاؤن لوڈ پورٹل" else "PC Downloads", fontSize = 11.sp)
                        }
                    }

                    AnimatedVisibility(visible = showScriptInstructions) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                                .padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = if (isUrdu) "طریقہ کار:" else "Quick Steps:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = if (isUrdu) "1. اوپر 'اسکرپٹ کاپی کریں' دبائیں۔\n2. کمپیوٹر پر ایک فائل بنائیں robby_pc_bridge.py اور کوڈ پیسٹ کریں۔\n3. ٹرمینل میں چلائیں: python robby_pc_bridge.py\n4. اسکرین پر دکھایا گیا IP اوپر درج کریں اور 'کنیکٹ' دبائیں۔"
                                       else "1. Tap 'Copy Python Script'.\n2. Save as robby_pc_bridge.py on your computer.\n3. Run: python robby_pc_bridge.py\n4. Enter the shown IP above and tap Ping.",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        // Status Toast Feedback
        if (toastMessage != null) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = toastMessage.orEmpty(),
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }

    if (showPcDownloadDialog) {
        PcDownloadDialog(
            isUrdu = isUrdu,
            actionExecutor = viewModel.actionExecutor,
            onDismiss = { showPcDownloadDialog = false }
        )
    }
}

private data class AppLauncherItem(
    val name: String,
    val packageName: String?,
    val icon: ImageVector,
    val desc: String
)

private data class PcAppAction(
    val name: String,
    val icon: ImageVector,
    val action: String,
    val target: String,
    val desc: String
)
