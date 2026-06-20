package com.example.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Category
import com.example.ui.AppViewModel
import com.example.ui.components.*
import com.example.ui.theme.*

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CategoriesScreen(viewModel: AppViewModel, modifier: Modifier = Modifier) {
    val categories by viewModel.categoriesState.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }

    // Form inputs
    var catName by remember { mutableStateOf("") }
    var catType by remember { mutableStateOf("EXPENSE") } // "EXPENSE" | "INCOME"
    var catColor by remember { mutableStateOf("#6366F1") }
    var catIcon by remember { mutableStateOf("payments") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Upper Title Header section
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "CLASSIFICATION CODES",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Categories",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            PremiumButton(
                text = "Add Category",
                onClick = { showAddDialog = true },
                icon = Icons.Default.Add
            )
        }

        if (categories.isEmpty()) {
            EmptyState(
                title = "No Custom Categories",
                description = "Define spending categories with colorful visual indices to map your statement flow.",
                icon = Icons.Default.Add,
                actionLabel = "Add Custom Category",
                onActionClick = { showAddDialog = true }
            )
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(categories) { cat ->
                    var showDeleteAlert by remember { mutableStateOf(false) }

                    // Custom grid card
                    PremiumCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .combinedClickable(
                                onClick = {},
                                onLongClick = { showDeleteAlert = true }
                            )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            CategoryIconBadge(
                                iconName = cat.icon,
                                colorHex = cat.color,
                                modifier = Modifier.size(36.dp)
                            )

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = cat.name,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = cat.type,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (cat.type == "INCOME") SystemGreen else if (cat.type == "EXPENSE") SystemRed else SystemBlue,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }

                    // Delete confirm alert
                    if (showDeleteAlert) {
                        AlertDialog(
                            onDismissRequest = { showDeleteAlert = false },
                            shape = RoundedCornerShape(16.dp),
                            containerColor = MaterialTheme.colorScheme.surface,
                            title = {
                                Text(
                                    "Delete Category",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            text = {
                                Text(
                                    "Are you sure you want to delete '${cat.name}' permanently? Any transaction logs mapping back to this category will become un-categorized.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            confirmButton = {
                                TextButton(onClick = {
                                    showDeleteAlert = false
                                    viewModel.deleteCategory(cat.id)
                                }) {
                                    Text("Delete", color = SystemRed, fontWeight = FontWeight.Bold)
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showDeleteAlert = false }) {
                                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        )
                    }
                }
            }
        }

        // Add dialog
        if (showAddDialog) {
            AlertDialog(
                onDismissRequest = { showAddDialog = false },
                shape = RoundedCornerShape(16.dp),
                containerColor = MaterialTheme.colorScheme.surface,
                title = {
                    Text(
                        "Create Category",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .verticalScroll(rememberScrollState())
                            .padding(top = 8.dp)
                    ) {
                        // Type toggle
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("EXPENSE", "INCOME").forEach { type ->
                                val sel = catType == type
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (sel) PremiumIndigo else MaterialTheme.colorScheme.surfaceVariant)
                                        .border(
                                            width = 1.dp,
                                            color = if (sel) PremiumIndigo else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .clickable { catType = type }
                                        .padding(10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = type,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (sel) Color.White else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = catName,
                            onValueChange = { catName = it },
                            label = { Text("Category name") },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().testTag("category_name_input"),
                            colors = premiumTextFieldColors(),
                            singleLine = true
                        )

                        // Color picker title
                        Text(
                            text = "Aesthetic Theme Swatch",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // Real flowable color matrices
                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            PRESET_COLORS.forEach { colorString ->
                                val selected = catColor == colorString
                                val systemColorColor = Color(android.graphics.Color.parseColor(colorString))
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(systemColorColor)
                                        .border(
                                            width = if (selected) 2.5.dp else 0.dp,
                                            color = if (selected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                            shape = CircleShape
                                        )
                                        .clickable { catColor = colorString }
                                )
                            }
                        }

                        // Icon picker title
                        Text(
                            text = "Glyph Emblem Marker",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // Real flowable icon picker
                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            PRESET_ICONS.forEach { iconName ->
                                val selected = catIcon == iconName
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (selected) PremiumIndigo.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant)
                                        .border(
                                            width = 1.dp,
                                            color = if (selected) PremiumIndigo else Color.Transparent,
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .clickable { catIcon = iconName },
                                    contentAlignment = Alignment.Center
                                ) {
                                    val iconColor = if (selected) PremiumIndigo else MaterialTheme.colorScheme.onSurfaceVariant
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
                                        else -> Icons.Filled.Star
                                    }
                                    Icon(
                                        imageVector = iconVector,
                                        contentDescription = null,
                                        tint = iconColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    PremiumButton(
                        text = "Save Category",
                        onClick = {
                            if (catName.isNotBlank()) {
                                viewModel.addCategory(catName, catType, catColor, catIcon)
                                showAddDialog = false
                                catName = ""
                            }
                        }
                    )
                },
                dismissButton = {
                    TextButton(onClick = { showAddDialog = false }) {
                        Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            )
        }
    }
}
