package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.PremiumIndigo
import com.example.ui.theme.SystemGreen
import com.example.ui.theme.SystemRed

// Subtle scale-down animation on press for a high-end tactile experience
@Composable
fun Modifier.bounceClick(enabled: Boolean = true, onClick: () -> Unit): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "bounce"
    )

    return this
        .scale(scale)
        .clickable(
            interactionSource = interactionSource,
            indication = LocalIndication.current,
            enabled = enabled,
            onClick = onClick
        )
}

@Composable
fun PremiumCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    border: BorderStroke? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background == Color(0xFF0B0F19)
    val finalBorder = border ?: BorderStroke(
        width = 1.dp,
        color = if (isDark) Color(0xFF1F2937) else Color(0xFFE5E7EB)
    )
    val containerColor = if (isDark) Color(0xFF111827) else Color(0xFFFFFFFF)

    Card(
        modifier = if (onClick != null) modifier.bounceClick(onClick = onClick) else modifier,
        shape = RoundedCornerShape(16.dp),
        border = finalBorder,
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        content = content
    )
}

@Composable
fun AmountText(
    amount: Double,
    currencySymbol: String,
    modifier: Modifier = Modifier,
    type: String? = null, // "INCOME", "EXPENSE", or null (neutral)
    fontSize: androidx.compose.ui.unit.TextUnit = 16.sp,
    fontWeight: FontWeight = FontWeight.Bold,
    showSignSignifier: Boolean = false
) {
    val color = when (type) {
        "INCOME" -> SystemGreen
        "EXPENSE" -> SystemRed
        else -> MaterialTheme.colorScheme.onSurface
    }

    val sign = if (showSignSignifier) {
        if (type == "INCOME") "+ " else if (type == "EXPENSE") "- " else ""
    } else ""

    Text(
        text = "$sign$currencySymbol${String.format("%,.2f", amount)}",
        color = color,
        fontSize = fontSize,
        fontWeight = fontWeight,
        fontFamily = FontFamily.Monospace, // Monospaced numbers for tabular alignment
        modifier = modifier
    )
}

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        if (actionText != null && onActionClick != null) {
            Text(
                text = actionText,
                color = PremiumIndigo,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { onActionClick() }
            )
        }
    }
}

@Composable
fun premiumTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = PremiumIndigo,
    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
    focusedContainerColor = MaterialTheme.colorScheme.surface,
    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
    focusedLabelColor = PremiumIndigo,
    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
)

@Composable
fun PremiumButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    colors: ButtonColors = ButtonDefaults.buttonColors(
        containerColor = PremiumIndigo,
        contentColor = Color.White
    )
) {
    Button(
        onClick = onClick,
        modifier = modifier.bounceClick(enabled = enabled, onClick = onClick),
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        colors = colors,
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.5.sp
            )
        }
    }
}

@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null
) {
    PremiumButton(
        text = text,
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        icon = icon,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    )
}

@Composable
fun EmptyState(
    title: String,
    description: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .background(PremiumIndigo.copy(alpha = 0.08f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = PremiumIndigo,
                modifier = Modifier.size(50.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        if (actionLabel != null && onActionClick != null) {
            Spacer(modifier = Modifier.height(24.dp))
            PremiumButton(
                text = actionLabel,
                onClick = onActionClick
            )
        }
    }
}

@Composable
fun StatusPill(
    text: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.08f))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = color,
            fontSize = 9.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 0.5.sp
        )
    }
}

@Composable
fun PremiumProgressBar(
    progress: Float, // 0.0f to 1.0f
    color: Color,
    modifier: Modifier = Modifier,
    height: Dp = 8.dp
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "progress"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(animatedProgress)
                .clip(CircleShape)
                .background(color)
        )
    }
}

@Composable
fun SkeletonRow(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha), CircleShape)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier
                    .width(120.dp)
                    .height(14.dp)
                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha), RoundedCornerShape(4.dp))
            )
            Spacer(modifier = Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .width(80.dp)
                    .height(10.dp)
                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha), RoundedCornerShape(4.dp))
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Box(
            modifier = Modifier
                .width(60.dp)
                .height(14.dp)
                .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha), RoundedCornerShape(4.dp))
        )
    }
}

// -------------------------------------------------------------
// CENTRALIZED HELPERS FOR COLOR & ICON PICKERS
val PRESET_COLORS = listOf(
    "#10B981", "#3B82F6", "#EF4444", "#F59E0B", "#6366F1",
    "#F97316", "#FBBF24", "#475569", "#84CC16", "#EC4899",
    "#06B6D4", "#8B5CF6", "#F43F5E", "#D946EF", "#14B8A6"
)

val PRESET_ICONS = listOf(
    "payments", "laptop_mac", "home", "bolt", "phone_android",
    "restaurant", "local_cafe", "smoke_free", "shopping_cart",
    "shopping_bag", "directions_bus", "hotel", "favorite", "group", "account_balance"
)

@Composable
fun CategoryIconBadge(iconName: String, colorHex: String, modifier: Modifier = Modifier) {
    val tintColor = try {
        Color(android.graphics.Color.parseColor(colorHex))
    } catch (e: Exception) {
        Color(0xFF6366F1)
    }

    val iconVector = when (iconName) {
        "payments" -> Icons.Filled.Payments
        "laptop_mac" -> Icons.Filled.LaptopMac
        "home" -> Icons.Filled.Home
        "bolt" -> Icons.Filled.Bolt
        "phone_android" -> Icons.Filled.PhoneAndroid
        "restaurant" -> Icons.Filled.Restaurant
        "local_cafe" -> Icons.Filled.LocalCafe
        "smoke_free" -> Icons.Filled.SmokingRooms
        "shopping_cart" -> Icons.Filled.ShoppingCart
        "shopping_bag" -> Icons.Filled.ShoppingBag
        "directions_bus" -> Icons.Filled.DirectionsBus
        "hotel" -> Icons.Filled.Hotel
        "favorite" -> Icons.Filled.Favorite
        "group" -> Icons.Filled.Group
        "account_balance" -> Icons.Filled.AccountBalance
        "repeat" -> Icons.Filled.Repeat
        "settings" -> Icons.Filled.Settings
        "info" -> Icons.Filled.Info
        else -> Icons.Filled.Star
    }

    Box(
        modifier = modifier
            .size(36.dp)
            .background(tintColor.copy(alpha = 0.12f), CircleShape)
            .border(1.dp, tintColor.copy(alpha = 0.25f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = iconVector,
            contentDescription = null,
            tint = tintColor,
            modifier = Modifier.size(18.dp)
        )
    }
}
