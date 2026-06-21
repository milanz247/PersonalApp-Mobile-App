package com.example.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import com.example.data.model.Transaction
import com.example.ui.AppViewModel
import com.example.ui.components.*
import com.example.ui.theme.*
import java.time.LocalDate
import java.time.Instant
import java.time.ZoneId
import android.app.DatePickerDialog
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.animation.core.animateFloatAsState
import java.util.Calendar

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TransactionsScreen(viewModel: AppViewModel, modifier: Modifier = Modifier) {
    val transactions by viewModel.transactionsState.collectAsState()
    val accounts by viewModel.accountsState.collectAsState()
    val categories by viewModel.categoriesState.collectAsState()
    val settings by viewModel.settingsState.collectAsState()
    val context = LocalContext.current

    // Maps for O(1) lookups
    val accountsMap = remember(accounts) { accounts.associateBy { it.id } }
    val categoriesMap = remember(categories) { categories.associateBy { it.id } }

    var showAddDialog by remember { mutableStateOf(false) }

    // Transaction dialog parameters
    var txnType by remember { mutableStateOf("EXPENSE") } // "EXPENSE" | "INCOME"
    var txnAmount by remember { mutableStateOf("") }
    var txnAccountId by remember { mutableLongStateOf(0L) }
    var txnCategoryId by remember { mutableStateOf<Long?>(null) }
    var txnNote by remember { mutableStateOf("") }

    // Filters
    var filterType by remember { mutableStateOf("ALL") } // "ALL" | "INCOME" | "EXPENSE" | "TRANSFER"
    var searchQuery by remember { mutableStateOf("") }
    var filterDate by remember { mutableStateOf<LocalDate?>(null) }
    var filterCategoryIds by remember { mutableStateOf(emptySet<Long>()) }
    var filterAccountIds by remember { mutableStateOf(emptySet<Long>()) }
    var showFilters by remember { mutableStateOf(false) }

    val showDatePicker = {
        val calendar = Calendar.getInstance()
        val current = filterDate ?: LocalDate.now()
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                filterDate = LocalDate.of(year, month + 1, dayOfMonth)
            },
            current.year,
            current.monthValue - 1,
            current.dayOfMonth
        ).show()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Headers with add button
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "LEDGER JOURNALS",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Transactions",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            PremiumButton(
                text = "Add Tx",
                onClick = {
                    txnAccountId = accounts.firstOrNull()?.id ?: 0L
                    txnCategoryId = categories.firstOrNull { it.type == txnType }?.id
                    showAddDialog = true
                },
                icon = Icons.Default.Add
            )
        }

        // Search Bar with a beautiful modern borderless search feel
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search description or note...", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant) },
            modifier = Modifier.fillMaxWidth().testTag("search_input"),
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PremiumIndigo,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface
            )
        )

        // Filter Toggle Button Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Transactions & Filters",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            TextButton(
                onClick = { showFilters = !showFilters },
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                modifier = Modifier.height(32.dp)
            ) {
                Text(
                    text = if (showFilters) "Hide Filters" else "Show Advanced Filters",
                    fontSize = 12.sp,
                    color = PremiumIndigo
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = if (showFilters) Icons.Default.KeyboardArrowUp else Icons.Default.FilterList,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = PremiumIndigo
                )
            }
        }

        // Advanced Filters Area
        androidx.compose.animation.AnimatedVisibility(visible = showFilters) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Filter chips bar (Transaction Type)
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("ALL", "INCOME", "EXPENSE", "TRANSFER").forEach { f ->
                        val active = filterType == f
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (active) PremiumIndigo else MaterialTheme.colorScheme.surfaceVariant)
                                .border(
                                    width = 1.dp,
                                    color = if (active) PremiumIndigo else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable { filterType = f }
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = f,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (active) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Account Filter bar
                if (accounts.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        accounts.forEach { acc ->
                            val active = filterAccountIds.contains(acc.id)
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (active) PremiumIndigo else MaterialTheme.colorScheme.surfaceVariant)
                                    .border(
                                        width = 1.dp,
                                        color = if (active) PremiumIndigo else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable {
                                        filterAccountIds = if (active) {
                                            filterAccountIds - acc.id
                                        } else {
                                            filterAccountIds + acc.id
                                        }
                                    }
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Acc: ${acc.name}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (active) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Category Filter bar
                if (categories.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        categories.forEach { cat ->
                            val active = filterCategoryIds.contains(cat.id)
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (active) PremiumIndigo else MaterialTheme.colorScheme.surfaceVariant)
                                    .border(
                                        width = 1.dp,
                                        color = if (active) PremiumIndigo else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable {
                                        filterCategoryIds = if (active) {
                                            filterCategoryIds - cat.id
                                        } else {
                                            filterCategoryIds + cat.id
                                        }
                                    }
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = cat.name,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (active) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Date/Day Filter bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = null,
                            tint = if (filterDate != null) PremiumIndigo else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = if (filterDate != null) {
                                "Filtered Day: ${filterDate.toString()}"
                            } else {
                                "All Days journal view"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = if (filterDate != null) FontWeight.Bold else FontWeight.Normal,
                            color = if (filterDate != null) PremiumIndigo else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
        
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (filterDate != null) {
                            TextButton(
                                onClick = { filterDate = null },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(30.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(12.dp), tint = SystemRed)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Clear Day", fontSize = 10.sp, color = SystemRed, fontWeight = FontWeight.Bold)
                            }
                        }
        
                        OutlinedButton(
                            onClick = { showDatePicker() },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                            modifier = Modifier.height(30.dp),
                            border = BorderStroke(1.dp, if (filterDate != null) PremiumIndigo else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                        ) {
                            Icon(imageVector = Icons.Default.FilterList, contentDescription = "Select date", modifier = Modifier.size(12.dp), tint = if (filterDate != null) PremiumIndigo else MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (filterDate != null) "Change Day" else "Filter Day",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (filterDate != null) PremiumIndigo else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        val filteredList = remember(transactions, filterType, searchQuery, filterDate, filterCategoryIds, filterAccountIds) {
            transactions.filter { tx ->
                if (tx.deletedAt != null) return@filter false
                if (filterType != "ALL" && tx.type != filterType) return@filter false
                if (filterCategoryIds.isNotEmpty() && !filterCategoryIds.contains(tx.categoryId)) return@filter false
                if (filterAccountIds.isNotEmpty() && !filterAccountIds.contains(tx.fromAccountId) && !filterAccountIds.contains(tx.toAccountId)) return@filter false
                if (searchQuery.isNotBlank() && tx.note?.contains(searchQuery, true) != true) return@filter false
                if (filterDate != null) {
                    val txDate = java.time.Instant.ofEpochMilli(tx.date)
                        .atZone(java.time.ZoneId.systemDefault())
                        .toLocalDate()
                    if (txDate != filterDate) return@filter false
                }
                true
            }
        }

        if (filteredList.isEmpty()) {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                EmptyState(
                    title = "No matching transactions",
                    description = "No transactions found for the selected filters. Adjust your search or add a new transaction.",
                    icon = Icons.Default.SearchOff
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(items = filteredList, key = { it.id }) { tx ->
                    val corCat = categoriesMap[tx.categoryId]
                    val accountName = (accountsMap[tx.fromAccountId] ?: accountsMap[tx.toAccountId])?.name ?: "System Ledger"

                    var showDeleteAlert by remember { mutableStateOf(false) }

                    var offsetX by remember { mutableStateOf(0f) }
                    val animatedOffsetX by animateFloatAsState(targetValue = offsetX)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Min),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val tagColor = remember(corCat?.color) {
                            try {
                                Color(android.graphics.Color.parseColor(corCat?.color ?: "#737373"))
                            } catch(e: Exception) {
                                SystemBlue
                            }
                        }

                        // Continuous timeline connection line with category-colored indicator node
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .width(28.dp)
                                .fillMaxHeight()
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(1.5.dp)
                                    .weight(1f)
                                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                            )
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(tagColor, CircleShape)
                                    .border(1.5.dp, MaterialTheme.colorScheme.background, CircleShape)
                            )
                            Box(
                                modifier = Modifier
                                    .width(1.5.dp)
                                    .weight(1f)
                                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                            )
                        }

                        // Sliding Card Wrapper with backing tactile Action container
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(IntrinsicSize.Min)
                        ) {
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(SystemRed)
                                    .padding(end = 16.dp),
                                contentAlignment = Alignment.CenterEnd
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Swipe backer",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "DELETE",
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }

                            PremiumCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .offset(x = animatedOffsetX.dp)
                                    .pointerInput(Unit) {
                                        detectHorizontalDragGestures(
                                            onDragEnd = {
                                                if (offsetX < -120f) {
                                                    showDeleteAlert = true
                                                }
                                                offsetX = 0f
                                            },
                                            onDragCancel = { offsetX = 0f },
                                            onHorizontalDrag = { change, dragAmount ->
                                                change.consume()
                                                offsetX = (offsetX + dragAmount).coerceIn(-180f, 0f)
                                            }
                                        )
                                    }
                                    .combinedClickable(
                                        onClick = {},
                                        onLongClick = { showDeleteAlert = true }
                                    )
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)
                                ) {
                                    // Colored category tag along the left edge
                                    Box(
                                        modifier = Modifier
                                            .width(5.dp)
                                            .fillMaxHeight()
                                            .background(tagColor)
                                    )

                                    Row(
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(vertical = 12.dp, horizontal = 12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            modifier = Modifier.weight(1f),
                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            CategoryIconBadge(
                                                iconName = corCat?.icon ?: "settings",
                                                colorHex = corCat?.color ?: "#737373"
                                            )
                                            Column {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    Text(
                                                        text = corCat?.name ?: tx.type,
                                                        style = MaterialTheme.typography.titleSmall,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                    Box(
                                                        modifier = Modifier
                                                            .size(6.dp)
                                                            .background(
                                                                color = if (tx.type == "INCOME") SystemGreen else if (tx.type == "EXPENSE") SystemRed else SystemBlue,
                                                                shape = CircleShape
                                                            )
                                                    )
                                                }
                                                Text(
                                                    text = "${accountName}${if (!tx.note.isNullOrBlank()) " • ${tx.note}" else ""}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 1
                                                )
                                            }
                                        }

                                        AmountText(
                                            amount = tx.amount,
                                            currencySymbol = settings.currencySymbol,
                                            type = tx.type,
                                            showSignSignifier = true,
                                            fontSize = 13.sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Delete confirmation AlertDialog
                    if (showDeleteAlert) {
                        AlertDialog(
                            onDismissRequest = { showDeleteAlert = false },
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier.border(1.5.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(24.dp)),
                            containerColor = MaterialTheme.colorScheme.surface,
                            title = {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(56.dp)
                                            .background(SystemRed.copy(alpha = 0.1f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = null,
                                            tint = SystemRed,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(14.dp))
                                    Text(
                                        "Delete Ledger Entry",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            },
                            text = {
                                Text(
                                    "Are you sure you want to revert this transaction record permanently? This reverses the balance changes previously made to your cash stores.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        showDeleteAlert = false
                                        viewModel.deleteTransaction(tx.id)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = SystemRed),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Delete Entry", fontWeight = FontWeight.Bold)
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showDeleteAlert = false }) {
                                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
                                }
                            }
                        )
                    }
                }
            }
        }

        // Add Dialog Modal form
        if (showAddDialog) {
            AlertDialog(
                onDismissRequest = { showAddDialog = false },
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.border(1.5.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(24.dp)),
                containerColor = MaterialTheme.colorScheme.surface,
                title = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .background(PremiumIndigo.copy(alpha = 0.1f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.SwapHoriz,
                                contentDescription = null,
                                tint = PremiumIndigo,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            "Log Transaction",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(top = 8.dp).verticalScroll(rememberScrollState())
                    ) {
                        // Type toggle
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("EXPENSE", "INCOME").forEach { type ->
                                val sel = txnType == type
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
                                        .clickable {
                                            txnType = type
                                            txnCategoryId = categories.firstOrNull { it.type == type }?.id
                                        }
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
                            value = txnAmount,
                            onValueChange = { txnAmount = it },
                            label = { Text("Amount") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().testTag("transaction_amount_input"),
                            colors = premiumTextFieldColors(),
                            singleLine = true
                        )

                        Text(
                            "Target Account",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            accounts.forEach { acc ->
                                val matches = txnAccountId == acc.id
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (matches) PremiumIndigo else MaterialTheme.colorScheme.surfaceVariant)
                                        .border(
                                            width = 1.dp,
                                            color = if (matches) PremiumIndigo else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .clickable { txnAccountId = acc.id }
                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = acc.name,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (matches) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Text(
                            "Ledger Category",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        var categoryExpanded by remember { mutableStateOf(false) }
                        val activeCategories = categories.filter { it.type == txnType }
                        val selectedCategory = activeCategories.find { it.id == txnCategoryId } ?: activeCategories.firstOrNull()

                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = selectedCategory?.name ?: "Select Category",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Category") },
                                trailingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = "Expand Category dropdown",
                                        tint = PremiumIndigo
                                    )
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth(),
                                colors = premiumTextFieldColors(),
                                singleLine = true
                            )

                            // Invisible tap interceptor
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .clickable { categoryExpanded = !categoryExpanded }
                            )

                            DropdownMenu(
                                expanded = categoryExpanded,
                                onDismissRequest = { categoryExpanded = false },
                                modifier = Modifier
                                    .fillMaxWidth(0.9f)
                                    .background(MaterialTheme.colorScheme.surface)
                                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                            ) {
                                if (activeCategories.isEmpty()) {
                                    DropdownMenuItem(
                                        text = { Text("No categories available", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                        onClick = { categoryExpanded = false }
                                    )
                                } else {
                                    activeCategories.forEach { cat ->
                                        DropdownMenuItem(
                                            text = {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    val bulletColor = try {
                                                        Color(android.graphics.Color.parseColor(cat.color))
                                                    } catch (e: Exception) {
                                                        Color.Gray
                                                    }
                                                    Box(
                                                        modifier = Modifier
                                                            .size(10.dp)
                                                            .clip(CircleShape)
                                                            .background(bulletColor)
                                                    )
                                                    Text(cat.name, style = MaterialTheme.typography.bodyMedium)
                                                }
                                            },
                                            onClick = {
                                                txnCategoryId = cat.id
                                                categoryExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        OutlinedTextField(
                            value = txnNote,
                            onValueChange = { txnNote = it },
                            label = { Text("Note description (e.g. Lunch)") },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth(),
                            colors = premiumTextFieldColors(),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                },
                confirmButton = {
                    PremiumButton(
                        text = "Add Record",
                        onClick = {
                            val ob = txnAmount.toDoubleOrNull() ?: 0.0
                            if (ob > 0.0 && txnAccountId > 0L) {
                                viewModel.addTransaction(
                                    type = txnType,
                                    amount = ob,
                                    accountId = txnAccountId,
                                    categoryId = txnCategoryId,
                                    date = System.currentTimeMillis(),
                                    note = txnNote
                                )
                                showAddDialog = false
                                txnAmount = ""
                                txnNote = ""
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
