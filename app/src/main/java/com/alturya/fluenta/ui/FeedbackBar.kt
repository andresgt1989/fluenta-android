package com.alturya.fluenta.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.alturya.fluenta.network.ApiClient
import com.alturya.fluenta.network.FeedbackBody
import kotlinx.coroutines.launch

// Lightweight thumbs feedback. Fire-and-forget to POST /api/feedback.
// Drop it into any screen with a `surface` tag to fuel /admin/improve.
@Composable
fun FeedbackBar(surface: String, modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    var sent by remember(surface) { mutableStateOf(false) }

    fun send(rating: Int) {
        if (sent) return
        sent = true
        scope.launch {
            runCatching { ApiClient.api.sendFeedback(FeedbackBody(surface = surface, rating = rating)) }
        }
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (sent) {
            Text("¡Gracias por tu opinión! 🙌", style = MaterialTheme.typography.bodySmall)
        } else {
            Text("¿Te gustó?", style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.width(8.dp))
            TextButton(onClick = { send(5) }) { Text("👍") }
            TextButton(onClick = { send(1) }) { Text("👎") }
        }
    }
}
