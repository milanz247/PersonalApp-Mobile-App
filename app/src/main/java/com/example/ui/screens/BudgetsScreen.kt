package com.example.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Category
import com.example.ui.AppViewModel
import com.example.ui.components.*
import com.example.ui.theme.*
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun BudgetsScreen(viewModel: AppViewModel, modifier: Modifier = Modifier) {
    val budgets by viewModel.budgetsState.collectAsState()
    val categories by viewModel.categoriesState.collectAsState()
    val transactions by viewModel.transactionsState.collectAsState()
    val settings by viewModel.settingsState.collectAsState()
    val currentMonthYear by viewModel.currentMonthYear.collectAsState()

    var showEditDialog by remember { mutableStateOf<Category?>(null) }
    var changeBudgetAmt by remember { mutableStateOf("") }

    // Aggregate monthly actual expenses
    val expenseCategories = remember(categories) { categories.filter { it.type == "EXPENSE" } }
    val monthlyTransactions = remember(transactions, currentMonthYear) {
        transactions.filter {
            it.deletedAt == null &&
            it.type == "EXPENSE" &&
            Instant.ofEpochMilli(it.date).atZone(ZoneId.systemDefault()).toLocalDate().format(DateTimeFormatter.ofPattern("yyyy-MM")) == currentMonthYear
        }
    }

    val actualSpentMap = remember(monthlyTransactions) {
        val map = mutableMapOf<Long, Double>()
        monthlyTransactions.forEach { tx ->
            if (tx.categoryId != null) {
                map[tx.categoryId] = (map[tx.categoryId] ?: 0.0) + tx.amount
            }
        }
        map
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Headers section
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "LIMITS & THRESHOLDS",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Budgets Plan",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            // Month badge label
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(PremiumIndigo.copy(alpha = 0.12f))
                    .border(1.dp, PremiumIndigo.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text(
                    text = currentMonthYear,
                    color = PremiumIndigo,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        if (expenseCategories.isEmpty()) {
            EmptyState(
                title = "No Expense Categories",
                description = "Define spending classifications first before provisioning threshold budgets.",
                icon = Icons.Default.Info,
                actionLabel = "Modify Categories First",
                onActionClick = {}
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(expenseCategories) { cat ->
                    val allocatedBudget = budgets.firstOrNull { it.categoryId == cat.id }?.amount ?: 0.0
                    val spent = actualSpentMap[cat.id] ?: 0.0
                    val isLockedOver = spent > allocatedBudget && allocatedBudget > 0.0
                    val progress = if (allocatedBudget > 0.0) (spent / allocatedBudget).toFloat() else 0f

                    PremiumCard(
                        modifier = Modifier.fillMaxWidth().testTag("budget_row_${cat.id}")
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CategoryIconBadge(iconName = cat.icon, colorHex = cat.color)
                                    Column {
                                        Text(
                                            text = cat.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Pre-allocated: ${settings.currencySymbol}${String.format("%,.0f", allocatedBudget)}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    AmountText(
                                        amount = spent,
                                        currencySymbol = settings.currencySymbol,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        type = if (isLockedOver) "EXPENSE" else null
                                    )
                                    TextButton(
                                        onClick = {
                                            changeBudgetAmt = if (allocatedBudget > 0.0) allocatedBudget.toString() else ""
                                            showEditDialog = cat
                                        },
                                        contentPadding = PaddingValues(0.dp),
                                        modifier = Modifier.height(28.dp)
                                    ) {
                                        Text(
                                            text = "Set Limit",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = PremiumIndigo,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            // Dynamic LinearProgressIndicator matches M3 design guidelines and matches our colors
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                LinearProgressIndicator(
                                    progress = { progress.coerceAtMost(1f) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                    color = if (isLockedOver) SystemRed else SystemGreen,
                                    trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f),
                                )
                                if (allocatedBudget > 0.0) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        val percent = (progress * 100).toInt()
                                        Text(
                                            text = "$percent% Utilized",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (isLockedOver) SystemRed else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        val left = (allocatedBudget - spent).coerceAtLeast(0.0)
                                        Text(
                                            text = if (isLockedOver) "Exceeded by ${settings.currencySymbol}${String.format("%,.0f", spent - allocatedBudget)}" else "Remaining: ${settings.currencySymbol}${String.format("%,.0f", left)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (isLockedOver) SystemRed else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                } else {
                                    Text(
                                        text = "Unscheduled spending limit. Click Set Limit to define.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Set Budget Dialog popups
        if (showEditDialog != null) {
            val cat = showEditDialog!!
            AlertDialog(
                onDismissRequest = { showEditDialog = null },
                shape = RoundedCornerShape(16.dp),
                containerColor = MaterialTheme.colorScheme.surface,
                title = {
                    Text(
                        "Set Expense Budget",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Text(
                            text = "Set monthly threshold limit for '${cat.name}'. You will receive active warnings if monthly expenses exceed this amount.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedTextField(
                            value = changeBudgetAmt,
                            onValueChange = { changeBudgetAmt = it },
                            label = { Text("Pre-allocated limit") },
                            placeholder = { Text("e.g. 50000") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().testTag("budget_amount_input"),
                            colors = premiumTextFieldColors(),
                            singleLine = true
                        )
                    }
                },
                confirmButton = {
                    PremiumButton(
                        text = "Apply Limit",
                        onClick = {
                            val ob = changeBudgetAmt.toDoubleOrNull() ?: 0.0
                            viewModel.updateBudget(cat.id, ob)
                            showEditDialog = null
                            changeBudgetAmt = ""
                        }
                    )
                },
                dismissButton = {
                    TextButton(onClick = { showEditDialog = null }) {
                        Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            )
        }
    }
}
