package com.freehands.assistant.ui.dialog

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.freehands.assistant.commands.CommandExecutor

/**
 * Dialog for confirming critical commands
 */
@Composable
fun CommandConfirmationDialog(
    commandType: CommandExecutor.CommandType,
    commandDetails: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            elevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                // Icon
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colors.primary,
                    modifier = Modifier
                        .size(48.dp)
                        .align(Alignment.CenterHorizontally)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Title
                Text(
                    text = "Подтверждение действия",
                    style = MaterialTheme.typography.h6,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Command details
                Text(
                    text = commandDetails,
                    style = MaterialTheme.typography.body1,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Command type info
                Text(
                    text = getCommandTypeDescription(commandType),
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Отмена")
                    }
                    
                    Button(
                        onClick = {
                            onConfirm()
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Подтвердить")
                    }
                }
            }
        }
    }
}

/**
 * Get description for command type
 */
private fun getCommandTypeDescription(commandType: CommandExecutor.CommandType): String {
    return when (commandType) {
        CommandExecutor.CommandType.WIFI_TOGGLE -> "Изменение состояния Wi-Fi"
        CommandExecutor.CommandType.BLUETOOTH_TOGGLE -> "Изменение состояния Bluetooth"
        CommandExecutor.CommandType.BRIGHTNESS_CHANGE -> "Изменение яркости экрана"
        CommandExecutor.CommandType.DND_TOGGLE -> "Изменение режима 'Не беспокоить'"
        CommandExecutor.CommandType.APP_LAUNCH -> "Запуск приложения"
        CommandExecutor.CommandType.APP_CLOSE -> "Закрытие приложения"
        CommandExecutor.CommandType.PHONE_CALL -> "Совершение телефонного звонка"
        CommandExecutor.CommandType.SEND_SMS -> "Отправка SMS-сообщения"
        CommandExecutor.CommandType.OPEN_SETTINGS -> "Открытие системных настроек"
        CommandExecutor.CommandType.VOLUME_CHANGE -> "Изменение громкости"
    }
}
