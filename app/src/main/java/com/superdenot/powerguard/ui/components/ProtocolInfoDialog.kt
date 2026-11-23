package com.superdenot.powerguard.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.superdenot.powerguard.domain.model.ProtocolExplanationCatalog

@Composable
fun ProtocolInfoDialog(
    protocolLabel: String,
    onDismiss: () -> Unit
) {
    val explanation = ProtocolExplanationCatalog.get(protocolLabel)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(explanation.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(protocolLabel, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
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
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Mengerti") }
        }
    )
}
