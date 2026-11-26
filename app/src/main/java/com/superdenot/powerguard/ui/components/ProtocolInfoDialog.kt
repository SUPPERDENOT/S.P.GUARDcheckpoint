package com.superdenot.powerguard.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.superdenot.powerguard.domain.model.ProtocolExplanationCatalog
import com.superdenot.powerguard.ui.components.PowerActionButton
import com.superdenot.powerguard.ui.components.POWER_BUTTON_CLOSE_DELAY_MS

internal const val INFO_BADGE_DIAMETER_DP = 16f
internal const val INFO_BADGE_ICON_DIAMETER_DP = 11f
internal const val INFO_BADGE_GAP_DP = 4f

@Composable
internal fun InfoBadge(
    contentDescription: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(INFO_BADGE_DIAMETER_DP.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f))
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.Outlined.Info,
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(INFO_BADGE_ICON_DIAMETER_DP.dp)
        )
    }
}

@Composable
fun ProtocolInfoDialog(
    protocolLabel: String,
    onDismiss: () -> Unit
) {
    val explanation = ProtocolExplanationCatalog.get(protocolLabel)
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth().widthIn(max = 420.dp).heightIn(max = 560.dp),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.onSurface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            tonalElevation = 0.dp,
            shadowElevation = 8.dp
        ) {
            Column(
                Modifier.fillMaxWidth().heightIn(max = 560.dp).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(explanation.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(protocolLabel, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                }
                Column(
                    modifier = Modifier.fillMaxWidth().weight(1f, fill = false)
                        .heightIn(max = 420.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Klasifikasi sumber", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                    Text(explanation.powerRange, style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(2.dp))
                    Text("Penjelasan", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                    Text(explanation.summary, style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(2.dp))
                    Text("Daya aktual sisi baterai", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                    Text(ProtocolExplanationCatalog.batterySidePowerExplanation, style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(2.dp))
                    Text("Panduan membandingkan aksesori", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                    Text(ProtocolExplanationCatalog.comparisonGuidance, style = MaterialTheme.typography.bodyMedium)
                }
                androidx.compose.foundation.layout.Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.End
                ) {
                    PowerActionButton(onClick = onDismiss, clickDelayMillis = POWER_BUTTON_CLOSE_DELAY_MS) {
                        Text("Mengerti")
                    }
                }
            }
        }
    }
}
