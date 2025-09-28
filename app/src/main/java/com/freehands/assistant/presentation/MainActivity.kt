package com.freehands.assistant.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.freehands.assistant.R
import com.freehands.assistant.presentation.theme.FreeHandsTheme
import com.freehands.assistant.presentation.viewmodel.VoiceCommandViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FreeHandsTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    VoiceCommandScreen()
                }
            }
        }
    }
}

@Composable
fun VoiceCommandScreen(
    viewModel: VoiceCommandViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Status text
        Text(
            text = when (uiState) {
                is VoiceCommandViewModel.VoiceCommandUiState.Idle -> "Tap the mic to start"
                is VoiceCommandViewModel.VoiceCommandUiState.Ready -> "Ready to listen"
                is VoiceCommandViewModel.VoiceCommandUiState.Listening -> "Listening..."
                is VoiceCommandViewModel.VoiceCommandUiState.Processing -> "Processing..."
                is VoiceCommandViewModel.VoiceCommandUiState.Success -> "Command executed successfully"
                is VoiceCommandViewModel.VoiceCommandUiState.Error -> "Error occurred"
            },
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 32.dp)
        )
        
        // Result/Error message
        when (uiState) {
            is VoiceCommandViewModel.VoiceCommandUiState.Success -> {
                Text(
                    text = (uiState as VoiceCommandViewModel.VoiceCommandUiState.Success).message,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 32.dp)
                )
            }
            is VoiceCommandViewModel.VoiceCommandUiState.Error -> {
                Text(
                    text = (uiState as VoiceCommandViewModel.VoiceCommandUiState.Error).message,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 32.dp)
                )
            }
            else -> {}
        }
        
        // Microphone button
        val buttonColor = when (uiState) {
            is VoiceCommandViewModel.VoiceCommandUiState.Listening -> MaterialTheme.colorScheme.primary
            is VoiceCommandViewModel.VoiceCommandUiState.Error -> MaterialTheme.colorScheme.error
            else -> MaterialTheme.colorScheme.primary
        }
        
        val iconRes = when (uiState) {
            is VoiceCommandViewModel.VoiceCommandUiState.Listening -> R.drawable.ic_mic_on
            else -> R.drawable.ic_mic_off
        }
        
        val buttonAction = {
            when (uiState) {
                is VoiceCommandViewModel.VoiceCommandUiState.Listing -> viewModel.stopListening()
                else -> viewModel.startListening()
            }
        }
        
        IconButton(
            onClick = buttonAction,
            modifier = Modifier
                .size(120.dp)
                .padding(16.dp)
        ) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = "Microphone",
                tint = buttonColor,
                modifier = Modifier.fillMaxSize()
            )
        }
        
        // Help text
        Text(
            text = when (uiState) {
                is VoiceCommandViewModel.VoiceCommandUiState.Listening -> "Tap to stop listening"
                else -> "Tap and speak your command"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 16.dp)
        )
    }
}
