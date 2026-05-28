package com.example.unisphere.ui.composables

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.ui.text.input.VisualTransformation

@Composable
fun UniSphereTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    trailingIcon: @Composable (() -> Unit)? = null, // Aggiunto slot per icone di coda (es. GPS o Password)
    singleLine: Boolean = true,
    minLines: Int = 1,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    isError: Boolean = false,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        leadingIcon = leadingIcon?.let { { Icon(imageVector = it, contentDescription = null) } },
        trailingIcon = trailingIcon, // Collega l'icona di coda al componente Material 3
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        singleLine = singleLine,
        minLines = minLines,
        maxLines = maxLines,
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        isError = isError
    )
}

@Composable
fun UniSphereSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    trailingAction: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        if (trailingAction != null) {
            trailingAction()
        }
    }
}

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

@Composable
fun UniSphereEmptyState(
    icon: ImageVector,
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    actionButton: @Composable (() -> Unit)? = null
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = description,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            lineHeight = 18.sp
        )
        if (actionButton != null) {
            Spacer(modifier = Modifier.height(20.dp))
            actionButton()
        }
    }
}

@Composable
fun UniSphereButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        // Il bottone si disattiva da solo se c'è un caricamento in corso
        enabled = enabled && !isLoading,
        modifier = modifier.height(52.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(24.dp),
                strokeWidth = 2.5.dp
            )
        } else {
            Text(
                text = text,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun UniSphereAvatar(
    username: String,
    profilePictureUri: String?,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
    showBorder: Boolean = false,
    borderColor: Color = Color(0xFFFFB300),
    onClick: (() -> Unit)? = null,
    badge: @Composable (BoxScope.() -> Unit)? = null
) {
    val avatarModifier = modifier
        .size(size)
        .clip(CircleShape)
        .then(if (showBorder) Modifier.border(1.5.dp, borderColor, CircleShape) else Modifier)
        .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)

    Box(contentAlignment = Alignment.TopEnd) {
        if (!profilePictureUri.isNullOrEmpty()) {
            AsyncImage(
                model = profilePictureUri,
                contentDescription = null,
                modifier = avatarModifier,
                contentScale = ContentScale.Crop
            )
        } else {
            val initial = username.take(1).uppercase()
            Box(
                modifier = avatarModifier.background(
                    Brush.linearGradient(colors = listOf(Color(0xFFECEFF1), Color(0xFFCFD8DC)))
                ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initial,
                    fontSize = (size.value * 0.4f).sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
        badge?.invoke(this)
    }
}

@Composable
fun UniSphereAlertDialog(
    title: String,
    text: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    dismissText: String? = null, // Opzionale: se è null, il tasto annulla sparisce (es. dialog di solo errore)
    isConfirmEnabled: Boolean = true
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium) },
        text = { Text(text = text, style = MaterialTheme.typography.bodyMedium) },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = isConfirmEnabled
            ) {
                Text(confirmText, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = dismissText?.let {
            {
                TextButton(onClick = onDismiss) {
                    Text(it, color = MaterialTheme.colorScheme.outline)
                }
            }
        }
    )
}