package com.freehands.assistant.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Settings screen for FreeHands assistant
 */
@Composable
fun SettingsScreen(
    onBack: () -> Unit = {}
) {
    var autoStart by remember { mutableStateOf(false) }
    var wakeWordEnabled by remember { mutableStateOf(true) }
    var wakeWordSensitivity by remember { mutableStateOf(0.5f) }
    var energySaving by remember { mutableStateOf(false) }
    var confirmCommands by remember { mutableStateOf(true) }
    var ttsEnabled by remember { mutableStateOf(true) }
    var ttsSpeed by remember { mutableStateOf(1.0f) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Настройки ассистента") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Назад")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // General Settings
            SettingsSection(title = "Основные") {
                SwitchSetting(
                    title = "Автозапуск",
                    subtitle = "Запускать ассистента при загрузке устройства",
                    checked = autoStart,
                    onCheckedChange = { autoStart = it },
                    icon = Icons.Default.PlayArrow
                )
                
                SwitchSetting(
                    title = "Подтверждение команд",
                    subtitle = "Запрашивать подтверждение для критичных операций",
                    checked = confirmCommands,
                    onCheckedChange = { confirmCommands = it },
                    icon = Icons.Default.Security
                )
                
                SwitchSetting(
                    title = "Энергосбережение",
                    subtitle = "Снизить частоту обработки для экономии батареи",
                    checked = energySaving,
                    onCheckedChange = { energySaving = it },
                    icon = Icons.Default.BatteryChargingFull
                )
            }
            
            Divider()
            
            // Wake Word Settings
            SettingsSection(title = "Ключевое слово") {
                SwitchSetting(
                    title = "Активация по ключевому слову",
                    subtitle = "Слушать фразу 'привет, брат' для активации",
                    checked = wakeWordEnabled,
                    onCheckedChange = { wakeWordEnabled = it },
                    icon = Icons.Default.Mic
                )
                
                if (wakeWordEnabled) {
                    SliderSetting(
                        title = "Чувствительность",
                        subtitle = "Насколько точно должно быть произнесено ключевое слово",
                        value = wakeWordSensitivity,
                        onValueChange = { wakeWordSensitivity = it },
                        valueRange = 0f..1f,
                        icon = Icons.Default.TuneSharp
                    )
                    
                    ClickableSetting(
                        title = "Настроить ключевые слова",
                        subtitle = "Добавить или изменить фразы активации",
                        onClick = { /* TODO */ },
                        icon = Icons.Default.Edit
                    )
                }
            }
            
            Divider()
            
            // Voice Settings
            SettingsSection(title = "Голос и речь") {
                SwitchSetting(
                    title = "Голосовые ответы",
                    subtitle = "Озвучивать ответы ассистента",
                    checked = ttsEnabled,
                    onCheckedChange = { ttsEnabled = it },
                    icon = Icons.Default.VolumeUp
                )
                
                if (ttsEnabled) {
                    SliderSetting(
                        title = "Скорость речи",
                        subtitle = "Как быстро говорит ассистент",
                        value = ttsSpeed,
                        onValueChange = { ttsSpeed = it },
                        valueRange = 0.5f..2.0f,
                        icon = Icons.Default.Speed
                    )
                }
                
                ClickableSetting(
                    title = "Язык распознавания",
                    subtitle = "Русский",
                    onClick = { /* TODO */ },
                    icon = Icons.Default.Language
                )
            }
            
            Divider()
            
            // Security Settings
            SettingsSection(title = "Безопасность") {
                ClickableSetting(
                    title = "Биометрическая защита",
                    subtitle = "Требовать отпечаток для критичных команд",
                    onClick = { /* TODO */ },
                    icon = Icons.Default.Fingerprint
                )
                
                ClickableSetting(
                    title = "Проверить подпись приложения",
                    subtitle = "Убедиться, что приложение не изменено",
                    onClick = { /* TODO */ },
                    icon = Icons.Default.VerifiedUser
                )
                
                ClickableSetting(
                    title = "Очистить данные",
                    subtitle = "Удалить сохраненные настройки и кэш",
                    onClick = { /* TODO */ },
                    icon = Icons.Default.DeleteForever
                )
            }
            
            Divider()
            
            // About
            SettingsSection(title = "О приложении") {
                InfoSetting(
                    title = "Версия",
                    subtitle = "1.0.0",
                    icon = Icons.Default.Info
                )
                
                ClickableSetting(
                    title = "Лицензии с открытым кодом",
                    subtitle = "Vosk, Porcupine, и другие",
                    onClick = { /* TODO */ },
                    icon = Icons.Default.Description
                )
                
                ClickableSetting(
                    title = "Политика конфиденциальности",
                    subtitle = "Как мы обрабатываем ваши данные",
                    onClick = { /* TODO */ },
                    icon = Icons.Default.Policy
                )
            }
        }
    }
}

@Composable
fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.subtitle2,
            color = MaterialTheme.colors.primary,
            modifier = Modifier.padding(16.dp)
        )
        content()
    }
}

@Composable
fun SwitchSetting(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colors.primary,
            modifier = Modifier.size(24.dp)
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.body1
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
            )
        }
        
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
fun SliderSetting(
    title: String,
    subtitle: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colors.primary,
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.body1
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                )
            }
            
            Text(
                text = "%.1f".format(value),
                style = MaterialTheme.typography.body2
            )
        }
        
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 40.dp)
        )
    }
}

@Composable
fun ClickableSetting(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colors.primary,
            modifier = Modifier.size(24.dp)
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.body1
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
            )
        }
        
        IconButton(onClick = onClick) {
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Открыть"
            )
        }
    }
}

@Composable
fun InfoSetting(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colors.primary,
            modifier = Modifier.size(24.dp)
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.body1
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}
