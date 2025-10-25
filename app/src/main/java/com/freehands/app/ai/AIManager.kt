package com.freehands.app.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AIManager {
    suspend fun requestResponse(prompt: String): String = withContext(Dispatchers.IO) {
        // Online AI placeholder
        ""AI ответ на: ""
    }
}
