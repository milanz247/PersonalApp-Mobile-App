package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.ui.AppViewModel
import com.example.ui.components.*
import com.example.ui.theme.*
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun DashboardScreen(
    viewModel: AppViewModel,
    modifier: Modifier = Modifier,
    onNavigateToTransactions: () -> Unit = {}
) {
    val accounts by viewModel.accountsState.collectAsState()
    val categories by viewModel.categoriesState.collectAsState()
    val transactions by viewModel.transactionsState.collectAsState()
    val debts by viewModel.debtsState.collectAsState()
    val settings by viewModel.settingsState.collectAsState()
    val currentMonthYear by viewModel.currentMonthYear.collectAsState()

    // Aggregate Calculations
    val totalBalance = accounts.sumOf { it.balance }

    // Filter current month active transactions
    val activeTransactions = remember(transactions, currentMonthYear) {
        transactions.filter { tx ->
            if (tx.deletedAt != null) return@filter false
            val txDate = Instant.ofEpochMilli(tx.date).atZone(ZoneId.systemDefault()).toLocalDate()
            val txMonthYear = txDate.format(DateTimeFormatter.ofPattern("yyyy-MM"))
            txMonthYear == currentMonthYear
        }
    }

    val totalIncome = remember(activeTransactions) {
        activeTransactions.filter { it.type == "INCOME" }.sumOf { it.amount }
    }
    val totalExpense = remember(activeTransactions) {
        activeTransactions.filter { it.type == "EXPENSE" }.sumOf { it.amount }
    }

    // Spend distribution
    val categorySpending = remember(activeTransactions) {
        val map = mutableMapOf<Long, Double>()
        activeTransactions.filter { it.type == "EXPENSE" && it.categoryId != null }.forEach { tx ->
            map[tx.categoryId!!] = (map[tx.categoryId] ?: 0.0) + tx.amount
        }
        map
    }

    // Budget check overspend limits
    val budgets by viewModel.budgetsState.collectAsState()
    val overspendAlerts = remember(budgets, categorySpending) {
        budgets.mapNotNull { b ->
            val spent = categorySpending[b.categoryId] ?: 0.0
            if (spent > b.amount) {
                val cat = categories.firstOrNull { it.id == b.categoryId }
                if (cat != null) {
                    Pair(cat, spent - b.amount)
                } else null
            } else null
        }
    }

    // Balance, Income, Expense Entrance Animations (Count-up numbers)
    var animationTriggered by remember { mutableStateOf(false) }
    LaunchedEffect(totalBalance, totalIncome, totalExpense) {
        animationTriggered = true
    }

    val animatedBalance by animateFloatAsState(
        targetValue = if (animationTriggered) totalBalance.toFloat() else 0f,
        animationSpec = tween(durationMillis = 1100, easing = FastOutSlowInEasing),
        label = "bal_anim"
    )
    val animatedIncome by animateFloatAsState(
        targetValue = if (animationTriggered) totalIncome.toFloat() else 0f,
        animationSpec = tween(durationMillis = 1100, easing = FastOutSlowInEasing),
        label = "inc_anim"
    )
    val animatedExpense by animateFloatAsState(
        targetValue = if (animationTriggered) totalExpense.toFloat() else 0f,
        animationSpec = tween(durationMillis = 1100, easing = FastOutSlowInEasing),
        label = "exp_anim"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Welcoming Title Brand Header
        Column(modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)) {
            Text(
                text = "WELCOME BACK",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = settings.userName,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        // Total Balance Hero Card (Dynamic Gradient Canvas with Asymmetric Lines)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(PremiumIndigo, Color(0xFF1E293B)),
                        start = androidx.compose.ui.geometry.Offset(0f, 0f),
                        end = androidx.compose.ui.geometry.Offset(1000f, 1000f)
                    )
                )
                .testTag("total_balance_card")
        ) {
            // Asymmetric modern background lines / overlays
            androidx.compose.foundation.Canvas(modifier = Modifier.matchParentSize()) {
                val width = size.width
                val height = size.height
                drawCircle(
                    color = Color.White.copy(alpha = 0.05f),
                    radius = width * 0.45f,
                    center = androidx.compose.ui.geometry.Offset(width * 0.95f, height * 0.15f)
                )
                drawCircle(
                    color = Color.White.copy(alpha = 0.03f),
                    radius = width * 0.3f,
                    center = androidx.compose.ui.geometry.Offset(width * 0.1f, height * 0.85f)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "TOTAL NET BALANCE",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.2.sp,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                    Box(
                        modifier = Modifier
                            .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = settings.currencyCode,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Animated Balance Counter text
                Text(
                    text = "${settings.currencySymbol}${String.format("%,.2f", animatedBalance)}",
                    style = MaterialTheme.typography.displayLarge,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(SystemGreen, CircleShape)
                    )
                    Text(
                        text = "Encrypted local ledger synchronized",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = SystemGreen
                    )
                }
            }
        }

        // Monthly Income vs Expenses row tracker
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Income Premium Card
            PremiumCard(modifier = Modifier.weight(1f)) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "MONTHLY INCOME",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${settings.currencySymbol}${String.format("%,.0f", animatedIncome)}",
                        style = MaterialTheme.typography.titleLarge,
                        color = SystemGreen,
                        fontFamily = FontFamily.Monospace
                    )
                    PremiumProgressBar(
                        progress = 1.0f,
                        color = SystemGreen,
                        height = 4.dp
                    )
                }
            }

            // Expenses Premium Card
            PremiumCard(modifier = Modifier.weight(1f)) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "MONTHLY EXPENSES",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${settings.currencySymbol}${String.format("%,.0f", animatedExpense)}",
                        style = MaterialTheme.typography.titleLarge,
                        color = SystemRed,
                        fontFamily = FontFamily.Monospace
                    )
                    val progressRatio = if (totalIncome > 0) (totalExpense / totalIncome).coerceIn(0.0, 1.0).toFloat() else 0.25f
                    PremiumProgressBar(
                        progress = progressRatio,
                        color = SystemRed,
                        height = 4.dp
                    )
                }
            }
        }

        // Overspend Warnings & Progressive Budget Indicators
        if (overspendAlerts.isNotEmpty()) {
            PremiumCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Alert",
                            tint = SystemRed,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "BUDGET OVERSPEND LIMIT EXCEEDED",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.2.sp,
                            color = SystemRed
                        )
                    }

                    overspendAlerts.forEach { alert ->
                        val cat = alert.first
                        val exceededBy = alert.second
                        val budget = budgets.firstOrNull { it.categoryId == cat.id }
                        val budgetLimit = budget?.amount ?: exceededBy
                        val spent = budgetLimit + exceededBy
                        val progress = if (budgetLimit > 0) (spent / budgetLimit).toFloat() else 1.5f
                        
                        val categoryColor = try {
                            Color(android.graphics.Color.parseColor(cat.color))
                        } catch (e: Exception) {
                            SystemRed
                        }

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(categoryColor, CircleShape)
                                    )
                                    Text(
                                        text = cat.name.uppercase(),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                // Interactive/Dynamic percentage flag badge
                                Box(
                                    modifier = Modifier
                                        .background(SystemRed.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "${(progress * 100).toInt()}% spent",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = SystemRed
                                    )
                                }
                            }

                            // Progressive smooth-corner linear progress bar
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth(progress.coerceIn(0f, 1f))
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(SystemRed)
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Spent: ${settings.currencySymbol}${String.format("%,.0f", spent)}",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Limit exceeded by: ${settings.currencySymbol}${String.format("%,.0f", exceededBy)}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = SystemRed
                                )
                            }
                        }
                    }
                }
            }
        }

        // Donut Chart Spending breakdown
        PremiumCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "EXPENSE BY CATEGORY",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                CategoryDonutChart(
                    categorySpending = categorySpending,
                    categories = categories,
                    currencySymbol = settings.currencySymbol
                )
            }
        }

        // Line Chart Trends
        PremiumCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "MONTHLY SPENDING TRENDS",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                MonthlyTrendLineChart(
                    transactions = transactions,
                    currencySymbol = settings.currencySymbol
                )
            }
        }

        // Recent Transaction lists
        SectionHeader(
            title = "Recent Statement Log",
            actionText = "See Full Ledger",
            onActionClick = onNavigateToTransactions
        )

        val rawRecentList = remember(transactions) {
            transactions.filter { it.deletedAt == null }.take(5)
        }

        if (rawRecentList.isEmpty()) {
            PremiumCard(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No recent transactions booked yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                rawRecentList.forEach { tx ->
                    val corCat = categories.firstOrNull { it.id == tx.categoryId }
                    val accountName = (accounts.firstOrNull { it.id == tx.fromAccountId } ?: accounts.firstOrNull { it.id == tx.toAccountId })?.name ?: "Cash Wallet"

                    PremiumCard(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = onNavigateToTransactions
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CategoryIconBadge(
                                    iconName = corCat?.icon ?: "settings",
                                    colorHex = corCat?.color ?: "#737373"
                                )
                                Column {
                                    Text(
                                        text = corCat?.name ?: tx.type,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "${accountName}${if (!tx.note.isNullOrBlank()) " • ${tx.note}" else ""}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            val prefix = if (tx.type == "EXPENSE") "-" else if (tx.type == "INCOME") "+" else ""
                            AmountText(
                                amount = tx.amount,
                                currencySymbol = settings.currencySymbol,
                                type = tx.type,
                                showSignSignifier = true,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }

        // Outstanding Loans Debts lists
        val pendingDebts = remember(debts) {
            debts.filter { it.deletedAt == null && it.status != "SETTLED" }.take(3)
        }

        if (pendingDebts.isNotEmpty()) {
            SectionHeader(title = "Ongoing Debts & Reciepts")
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                pendingDebts.forEach { debt ->
                    val colorBadge = if (debt.type == "LENT") SystemBlue else SystemAmber
                    PremiumCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Box(modifier = Modifier.size(6.dp).background(colorBadge, CircleShape))
                                    Text(
                                        text = debt.personName,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Text(
                                    text = "Owed: ${settings.currencySymbol}${String.format("%.2f", debt.amount)} • Remaining: ${settings.currencySymbol}${String.format("%.2f", debt.remainingAmount)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            StatusPill(text = debt.status, color = colorBadge)
                        }
                    }
                }
            }
        }
    }
}
