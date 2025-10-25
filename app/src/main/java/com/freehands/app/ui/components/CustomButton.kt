package com.freehands.app.ui.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
fun CustomButton(label: String, onClick: () -> Unit) {
    Text(text = label)
}
