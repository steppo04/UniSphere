package com.example.unisphere.ui.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun UniSphereListItem(
    headlineText: String,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    leadingBarColor: Color? = null, // Colore opzionale della barra verticale
    onClick: (() -> Unit)? = null,  // Callback opzionale per il click
    trailingContent: @Composable (() -> Unit)? = null // Slot destro riutilizzabile (Slot API)
) {
    // Configura il modificatore di base gestendo il click solo se necessario
    val baseModifier = if (onClick != null) {
        modifier
            .fillMaxWidth()
            .clickable { onClick() }
    } else {
        modifier.fillMaxWidth()
    }

    Row(
        modifier = baseModifier
            .padding(vertical = 10.dp, horizontal = 8.dp)
            .height(IntrinsicSize.Min), // Vincola la barra verticale ad allinearsi perfettamente all'altezza dei testi
        verticalAlignment = Alignment.Top
    ) {
        // --- BARRA VERTICALE COLORATA (Renderizzata solo se il colore viene fornito) ---
        if (leadingBarColor != null) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(2.dp))
                    .background(leadingBarColor)
            )
            Spacer(modifier = Modifier.width(16.dp))
        }

        // --- CONTENUTO INFORMATIVO CENTRALE ---
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = headlineText,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (!supportingText.isNullOrBlank()) {
                Text(
                    text = supportingText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // --- CONTENUTO DESTRO DINAMICO (Orari, Pulsanti, Icone, ecc.) ---
        if (trailingContent != null) {
            Box(
                modifier = Modifier.padding(start = 8.dp),
                contentAlignment = Alignment.TopEnd
            ) {
                trailingContent()
            }
        }
    }
}