package com.quickmsg.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quickmsg.app.data.QuickMessage
import com.quickmsg.app.data.QuickMessages
import com.quickmsg.app.service.TemplateProcessor

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QuickSendScreen(
    partnerName: String,
    partnerPhone: String,
    lastMessage: String?,
    lastTime: String?,
    onEditPartner: () -> Unit,
    onDeletePartner: () -> Unit,
    onOpenChat: () -> Unit,
    onSend: (String) -> Unit
) {
    var selectedCategory by remember { mutableStateOf("Status") }
    var customMessage by remember { mutableStateOf("") }

    val categoryColors = mapOf(
        "Status" to Color(0xFF25D366),
        "Quick Reply" to Color(0xFF34B7F1),
        "Busy" to Color(0xFFFF9800),
        "Love" to Color(0xFFE91E63),
        "Food" to Color(0xFFFF5722),
        "Templates" to Color(0xFF9C27B0)
    )

    val messages = QuickMessages.byCategory(selectedCategory)
    val accentColor = categoryColors[selectedCategory] ?: Color(0xFF25D366)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Partner card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFE91E63).copy(alpha = 0.08f)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFFE91E63)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Favorite,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = partnerName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = partnerPhone,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onOpenChat) {
                    Icon(
                        Icons.Default.Chat,
                        contentDescription = "Open chat",
                        tint = Color(0xFF25D366)
                    )
                }
                IconButton(onClick = onEditPartner) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Edit contact",
                        tint = Color(0xFFE91E63)
                    )
                }
                IconButton(onClick = onDeletePartner) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete contact",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        // Last sent
        AnimatedVisibility(visible = lastMessage != null, enter = fadeIn(), exit = fadeOut()) {
            if (lastMessage != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Schedule,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Last: \"${lastMessage.take(30)}\" ${lastTime ?: ""}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Custom message
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = customMessage,
                onValueChange = { customMessage = it },
                placeholder = { Text("Type a custom message...") },
                modifier = Modifier.weight(1f),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = {
                    if (customMessage.isNotBlank()) {
                        onSend(customMessage)
                        customMessage = ""
                    }
                }
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send",
                    tint = Color(0xFF25D366)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Category chips
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            QuickMessages.categories.forEach { category ->
                val color = categoryColors[category] ?: Color.Gray
                FilterChip(
                    selected = selectedCategory == category,
                    onClick = { selectedCategory = category },
                    label = {
                        Text(
                            category,
                            fontSize = 13.sp,
                            fontWeight = if (selectedCategory == category) FontWeight.Bold
                            else FontWeight.Normal
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = color.copy(alpha = 0.2f),
                        selectedLabelColor = color
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Message grid
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(messages.size) { index ->
                val msg = messages[index]
                val displayText = if (TemplateProcessor.hasTemplateVars(msg.text)) {
                    TemplateProcessor.previewTemplate(msg.text)
                } else {
                    msg.text
                }
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSend(msg.text) },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = accentColor.copy(alpha = 0.1f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        if (msg.emoji.isNotEmpty()) {
                            Text(
                                text = msg.emoji,
                                fontSize = 20.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                        Text(
                            text = displayText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}
