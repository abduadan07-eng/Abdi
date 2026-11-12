package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.localization.AppStrings
import com.example.ui.theme.*

@Composable
fun SchoolTopBar(
    title: String,
    subtitle: String? = null,
    currentLang: String,
    onToggleLang: () -> Unit,
    onSignOut: () -> Unit,
    onBack: (() -> Unit)? = null
) {
    Surface(
        color = NavyPrimary,
        tonalElevation = 4.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (onBack != null) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("back_button")
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                }
                Column {
                    Text(
                        text = title,
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (subtitle != null) {
                        Text(
                            text = subtitle,
                            color = BlueLight,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Language Switcher Badge
                AssistChip(
                    onClick = onToggleLang,
                    label = {
                        Text(
                            text = if (currentLang == "so") "SO | EN" else "EN | SO",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Language,
                            contentDescription = "Language",
                            tint = AmberWarning,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = NavyDark
                    ),
                    border = null,
                    modifier = Modifier.testTag("language_toggle_button")
                )

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = onSignOut,
                    modifier = Modifier.testTag("sign_out_button")
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ExitToApp,
                        contentDescription = "Sign Out",
                        tint = Color(0xFFFC8181)
                    )
                }
            }
        }
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
fun StatusBadge(status: String, lang: String = "so") {
    val (label, bg, fg) = when (status.uppercase()) {
        "PRESENT", "JOOGA" -> Triple(AppStrings.get("present", lang), Color(0xFFC6F6D5), Color(0xFF22543D))
        "ABSENT", "MAQAN" -> Triple(AppStrings.get("absent", lang), Color(0xFFFED7D7), Color(0xFF742A2A))
        "LATE", "SOO DAAHAY" -> Triple(AppStrings.get("late", lang), Color(0xFFFEEBC8), Color(0xFF7B341E))
        "EXCUSED", "CUDURDAAR" -> Triple(AppStrings.get("excused", lang), Color(0xFFE2E8F0), Color(0xFF2D3748))
        "PAID", "DHAMEYSTIRAN" -> Triple(AppStrings.get("status_paid", lang), Color(0xFFC6F6D5), Color(0xFF22543D))
        "PARTIAL", "PARTIALLY_PAID" -> Triple(AppStrings.get("status_partial", lang), Color(0xFFFEEBC8), Color(0xFF7B341E))
        "UNPAID" -> Triple(AppStrings.get("status_unpaid", lang), Color(0xFFFED7D7), Color(0xFF742A2A))
        "PUBLISHED" -> Triple(AppStrings.get("status_published", lang), Color(0xFFC6F6D5), Color(0xFF22543D))
        "APPROVED" -> Triple(AppStrings.get("status_approved", lang), Color(0xFFBEE3F8), Color(0xFF2A4365))
        "SUBMITTED" -> Triple(AppStrings.get("status_submitted", lang), Color(0xFFE9D8FD), Color(0xFF44337A))
        "LOCKED" -> Triple(AppStrings.get("status_locked", lang), Color(0xFFEDF2F7), Color(0xFF4A5568))
        else -> Triple(status, Color(0xFFE2E8F0), Color(0xFF2D3748))
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = label,
            color = fg,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
