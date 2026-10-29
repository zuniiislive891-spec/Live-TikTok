package com.example.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.FuturisticHudOverlay
import com.example.ui.screens.ChatScreen
import com.example.ui.screens.DeviceControlScreen
import com.example.ui.screens.HudDashboardScreen
import com.example.ui.screens.NotesScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.viewmodel.RobbyViewModel

enum class NavigationTab(
    val titleEn: String,
    val titleUr: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    HUD("Core", "کور", Icons.Filled.GraphicEq, Icons.Outlined.GraphicEq),
    CHAT("Chat", "چیٹ", Icons.Filled.Chat, Icons.Outlined.Chat),
    CONTROL("Apps", "ایپس", Icons.Filled.Apps, Icons.Outlined.Apps),
    NOTES("Notes", "نوٹس", Icons.Filled.EditNote, Icons.Outlined.EditNote),
    SETTINGS("System", "سیٹنگز", Icons.Filled.Settings, Icons.Outlined.Settings)
}

@Composable
fun RobbyMainApp(
    viewModel: RobbyViewModel,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    var currentTab by remember { mutableStateOf(NavigationTab.HUD) }
    val userProfile by viewModel.userProfile.collectAsState()
    val isUrdu = userProfile.language == "ur"

    // BackHandler to return to HUD tab if inside sub-tab
    BackHandler(enabled = currentTab != NavigationTab.HUD) {
        currentTab = NavigationTab.HUD
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                tonalElevation = 8.dp
            ) {
                NavigationTab.values().forEach { tab ->
                    val isSelected = currentTab == tab
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { currentTab = tab },
                        icon = {
                            Icon(
                                imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                contentDescription = tab.titleEn
                            )
                        },
                        label = {
                            Text(
                                text = if (isUrdu) tab.titleUr else tab.titleEn,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                NavigationTab.HUD -> HudDashboardScreen(viewModel = viewModel)
                NavigationTab.CHAT -> ChatScreen(viewModel = viewModel)
                NavigationTab.CONTROL -> DeviceControlScreen(viewModel = viewModel)
                NavigationTab.NOTES -> NotesScreen(viewModel = viewModel)
                NavigationTab.SETTINGS -> SettingsScreen(
                    viewModel = viewModel,
                    onSignOut = onSignOut
                )
            }

            // Futuristic HUD Overlay visualizing AI processing state with haptics
            FuturisticHudOverlay(
                viewModel = viewModel,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
