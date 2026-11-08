package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AllInclusive
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.example.R

/** Hour windows offered by the time filter; null = everything the app keeps (36 hours). */
val TimeFilterOptions: List<Int?> = listOf(1, 3, 6, 12, 24, null)

private val ActiveRed = Color(0xFFE11D48)

/** "৬" for 6: numbers inside Bengali labels read naturally in Bengali digits. */
fun bengaliDigits(n: Int): String = n.toString().map { if (it in '0'..'9') '০' + (it - '0') else it }.joinToString("")

/**
 * Filter-row chip that limits news to the last N hours. Red while a window is selected, like the
 * keyword button, so a narrowed list is never mistaken for everything.
 */
@Composable
fun TimeFilterChip(maxAgeHours: Int?, onSelect: (Int?) -> Unit, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }
    val scheme = MaterialTheme.colorScheme
    val active = maxAgeHours != null
    val shape = RoundedCornerShape(50)

    Box(modifier = modifier) {
        Row(
            modifier = Modifier
                .clip(shape)
                .background(if (active) ActiveRed.copy(alpha = 0.14f) else Color.Transparent)
                .border(1.dp, if (active) ActiveRed else scheme.outlineVariant, shape)
                .clickable { expanded = true }
                .padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Schedule,
                contentDescription = null,
                tint = if (active) ActiveRed else scheme.onSurfaceVariant,
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = if (active) stringResource(R.string.time_filter_hours, bengaliDigits(maxAgeHours!!))
                else stringResource(R.string.time_filter_label),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                color = if (active) ActiveRed else scheme.onSurfaceVariant
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            offset = DpOffset(0.dp, 6.dp),
            shape = RoundedCornerShape(20.dp),
            containerColor = scheme.surface,
            tonalElevation = 0.dp,
            shadowElevation = 12.dp,
            border = BorderStroke(1.dp, scheme.outlineVariant),
            modifier = Modifier.widthIn(min = 230.dp)
        ) {
            Row(
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(34.dp).background(scheme.primary.copy(alpha = 0.14f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Schedule, contentDescription = null, tint = scheme.primary, modifier = Modifier.size(18.dp))
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(stringResource(R.string.time_menu_title), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = scheme.onSurface)
                    Text(stringResource(R.string.time_menu_subtitle), style = MaterialTheme.typography.labelSmall, color = scheme.outline)
                }
            }
            HorizontalDivider(color = scheme.outlineVariant.copy(alpha = 0.6f), modifier = Modifier.padding(vertical = 4.dp))
            TimeFilterOptions.forEach { hours ->
                TimeOptionRow(
                    label = if (hours == null) stringResource(R.string.time_filter_all)
                    else stringResource(R.string.time_filter_last_hours, bengaliDigits(hours)),
                    icon = if (hours == null) Icons.Default.AllInclusive else Icons.Default.Schedule,
                    selected = hours == maxAgeHours,
                    onClick = {
                        onSelect(hours)
                        expanded = false
                    }
                )
            }
            Spacer(modifier = Modifier.size(4.dp))
        }
    }
}

@Composable
private fun TimeOptionRow(label: String, icon: ImageVector, selected: Boolean, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) scheme.primary.copy(alpha = 0.12f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = if (selected) scheme.primary else scheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) scheme.primary else scheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        if (selected) Icon(Icons.Default.CheckCircle, contentDescription = null, tint = scheme.primary, modifier = Modifier.size(18.dp))
    }
}
