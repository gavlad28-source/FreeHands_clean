package com.freehands.assistant.ui.main

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.freehands.assistant.service.AssistantForegroundService

/**
 * Main screen of FreeHands assistant
 */
@Composable
fun MainScreen(
    onSettingsClick: () -> Unit = {}
) {
    var isServiceRunning by remember { mutableStateOf(false) }
    var currentStatus by remember { mutableStateOf("Остановлен") }
    var commandHistory by remember { mutableStateOf(listOf<CommandHistoryItem>()) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("FreeHands Assistant") },
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.Settings, "Настройки")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    // TODO: Toggle service
                    isServiceRunning = !isServiceRunning
                },
                backgroundColor = if (isServiceRunning) MaterialTheme.colors.error else MaterialTheme.colors.primary
            ) {
                Icon(
                    imageVector = if (isServiceRunning) Icons.Default.Stop else Icons.Default.PlayArrow,
                    contentDescription = if (isServiceRunning) "Остановить" else "Запустить"
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Status Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                elevation = 4.dp
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Animated microphone icon
                    AnimatedMicrophoneIcon(isActive = isServiceRunning)
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = if (isServiceRunning) "Активен" else "Неактивен",
                        style = MaterialTheme.typography.h5,
                        fontWeight = FontWeight.Bold,
                        color = if (isServiceRunning) Color.Green else Color.Gray
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = currentStatus,
                        style = MaterialTheme.typography.body2,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                    )
                    
                    if (isServiceRunning) {
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(
                            text = "Скажите: \"Привет, брат\"",
                            style = MaterialTheme.typography.caption,
                            color = MaterialTheme.colors.primary
                        )
                    }
                }
            }
            
            // Quick actions
            QuickActionsSection()
            
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            
            // Command history
            Text(
                text = "История команд",
                style = MaterialTheme.typography.subtitle1,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            
            if (commandHistory.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Нет выполненных команд",
                        style = MaterialTheme.typography.body2,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.4f)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(commandHistory) { item ->
                        CommandHistoryCard(item)
                    }
                }
            }
        }
    }
}

@Composable
fun AnimatedMicrophoneIcon(isActive: Boolean) {
    val scale by animateFloatAsState(
        targetValue = if (isActive) 1.2f else 1.0f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(1000),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        )
    )
    
    Icon(
        imageVector = Icons.Default.Mic,
        contentDescription = null,
        tint = if (isActive) MaterialTheme.colors.primary else Color.Gray,
        modifier = Modifier.size((64 * scale).dp)
    )
}

@Composable
fun QuickActionsSection() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = "Быстрые действия",
            style = MaterialTheme.typography.subtitle1,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            QuickActionButton(
                icon = Icons.Default.Wifi,
                label = "Wi-Fi",
                onClick = { /* TODO */ },
                modifier = Modifier.weight(1f)
            )
            
            QuickActionButton(
                icon = Icons.Default.Bluetooth,
                label = "Bluetooth",
                onClick = { /* TODO */ },
                modifier = Modifier.weight(1f)
            )
            
            QuickActionButton(
                icon = Icons.Default.Brightness6,
                label = "Яркость",
                onClick = { /* TODO */ },
                modifier = Modifier.weight(1f)
            )
            
            QuickActionButton(
                icon = Icons.Default.DoNotDisturb,
                label = "DND",
                onClick = { /* TODO */ },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun QuickActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.aspectRatio(1f),
        contentPadding = PaddingValues(8.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.caption
            )
        }
    }
}

@Composable
fun CommandHistoryCard(item: CommandHistoryItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = getIconForCommand(item.command),
                contentDescription = null,
                tint = if (item.success) Color.Green else Color.Red,
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.command,
                    style = MaterialTheme.typography.body1
                )
                Text(
                    text = item.timestamp,
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                )
            }
            
            if (!item.success) {
                Icon(
                    imageVector = Icons.Default.Error,
                    contentDescription = "Ошибка",
                    tint = Color.Red,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

fun getIconForCommand(command: String): androidx.compose.ui.graphics.vector.ImageVector {
    return when {
        command.contains("wi-fi", ignoreCase = true) -> Icons.Default.Wifi
        command.contains("bluetooth", ignoreCase = true) -> Icons.Default.Bluetooth
        command.contains("яркость", ignoreCase = true) -> Icons.Default.Brightness6
        command.contains("не беспокоить", ignoreCase = true) -> Icons.Default.DoNotDisturb
        command.contains("открой", ignoreCase = true) -> Icons.Default.Launch
        command.contains("позвони", ignoreCase = true) -> Icons.Default.Phone
        else -> Icons.Default.Check
    }
}

data class CommandHistoryItem(
    val command: String,
    val timestamp: String,
    val success: Boolean
)
