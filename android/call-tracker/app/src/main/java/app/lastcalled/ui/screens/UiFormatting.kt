package app.lastcalled.ui.screens

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.lastcalled.ui.theme.Sage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.DateFormat
import java.util.Date
import kotlin.math.min

@Composable
fun ContactAvatar(
    name: String,
    photoUri: String?,
    modifier: Modifier = Modifier,
    size: Int = 48,
) {
    val context = LocalContext.current
    val image = produceState<android.graphics.Bitmap?>(initialValue = null, photoUri) {
        value = photoUri?.let { uri ->
            withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openInputStream(Uri.parse(uri))
                        ?.use { BitmapFactory.decodeStream(it) }
                }.getOrNull()
            }
        }
    }

    Box(
        modifier = modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(Sage),
        contentAlignment = Alignment.Center,
    ) {
        val bitmap = image.value
        if (bitmap != null) {
            androidx.compose.foundation.Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "$name contact photo",
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(size.dp),
            )
        } else {
            Text(
                text = initials(name),
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
                fontSize = (size / 3).sp,
            )
        }
    }
}

fun formatCallDate(timestampMillis: Long): String =
    DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
        .format(Date(timestampMillis))

fun formatDuration(seconds: Long): String {
    val totalSeconds = seconds.coerceAtLeast(0L)
    val hours = totalSeconds / 3_600
    val minutes = (totalSeconds % 3_600) / 60
    val remainingSeconds = totalSeconds % 60
    return when {
        hours > 0 -> "${hours}h ${minutes}m ${remainingSeconds}s"
        minutes > 0 -> "${minutes}m ${remainingSeconds}s"
        else -> "${remainingSeconds}s"
    }
}

fun initials(name: String): String {
    val parts = name.trim().split(Regex("\\s+")).filter(String::isNotBlank)
    if (parts.isEmpty()) return "?"
    return parts.take(min(2, parts.size)).joinToString("") { it.first().uppercase() }
}
