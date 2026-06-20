package com.example.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import com.example.data.model.Account
import com.example.ui.AppViewModel
import com.example.ui.components.*
import com.example.ui.theme.*

@Composable
fun AccountsScreen(viewModel: AppViewModel, modifier: Modifier = Modifier) {
    val accounts by viewModel.accountsState.collectAsState()
    val settings by viewModel.settingsState.collectAsState()

    var showAddBankDialog by remember { mutableStateOf(false) }
    var showTransferDialog by remember { mutableStateOf(false) }

    // Dialog Input states
    var bankAccountName by remember { mutableStateOf("") }
    var bankNameInput by remember { mutableStateOf("") }
    var branchNameInput by remember { mutableStateOf("") }
    var accountNumberInput by remember { mutableStateOf("") }
    var openingBalanceInput by remember { mutableStateOf("") }

    // Transfer inputs
    var fromAccountSelected by remember { mutableLongStateOf(0L) }
    var toAccountSelected by remember { mutableLongStateOf(0L) }
    var transferAmountInput by remember { mutableStateOf("") }
    var transferFeeInput by remember { mutableStateOf("0.00") }
    var transferNoteInput by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Core Header & Top Action buttons
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "VALUABLES & STORES",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "My Accounts",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Transfer button
                PremiumButton(
                    text = "Transfer",
                    onClick = { showTransferDialog = true },
                    icon = Icons.Default.SwapHoriz
                )
                // Add Bank button
                PremiumButton(
                    text = "Add Bank",
                    onClick = { showAddBankDialog = true },
                    icon = Icons.Default.Add
                )
            }
        }

        // Wallets Core cash ledger first
        val walletAccount = remember(accounts) { accounts.firstOrNull { it.type == "WALLET" } }
        val bankAccountsList = remember(accounts) { accounts.filter { it.type == "BANK" } }

        // QuickBooks-Style Centered Circular Dashboards (Ceylon Ledger Editions)
        val walletBalance = walletAccount?.balance ?: 0.0
        val totalBankBalance = bankAccountsList.sumOf { it.balance }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Circle 1: Pocket Ledger
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(105.dp)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    SystemGreen.copy(alpha = 0.12f),
                                    SystemGreen.copy(alpha = 0.01f)
                                )
                            ),
                            shape = CircleShape
                        )
                        .border(2.5.dp, SystemGreen.copy(alpha = 0.8f), CircleShape)
                        .padding(6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "POCKET",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = SystemGreen,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${settings.currencySymbol}${String.format("%,.0f", walletBalance)}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1
                        )
                        Text(
                            text = "Cash Wallet",
                            fontSize = 8.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Small vertical separator in center
            Box(
                modifier = Modifier
                    .height(42.dp)
                    .width(1.dp)
                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
            )

            // Circle 2: Bank Ledger
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(105.dp)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    PremiumIndigo.copy(alpha = 0.12f),
                                    PremiumIndigo.copy(alpha = 0.01f)
                                )
                            ),
                            shape = CircleShape
                        )
                        .border(2.5.dp, PremiumIndigo.copy(alpha = 0.8f), CircleShape)
                        .padding(6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "BANKS",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = PremiumIndigo,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${settings.currencySymbol}${String.format("%,.0f", totalBankBalance)}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1
                        )
                        Text(
                            text = "Ledger Assets",
                            fontSize = 8.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Virtual Accounts & Wallets custom spring-stacked layout
        val accountsList = remember(walletAccount, bankAccountsList) {
            listOfNotNull(walletAccount) + bankAccountsList
        }

        var isStackExpanded by remember { mutableStateOf(false) }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "MY VIRTUAL WALLETS & CARDS",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            TextButton(
                onClick = { isStackExpanded = !isStackExpanded },
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Icon(
                    imageVector = if (isStackExpanded) Icons.Default.Refresh else Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = PremiumIndigo
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (isStackExpanded) "Stack Cards" else "Expand Wallet",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = PremiumIndigo
                )
            }
        }

        if (accountsList.isEmpty()) {
            EmptyState(
                title = "No Wallets or Banks Found",
                description = "Securely setup cash stores and banking cards offline.",
                icon = Icons.Default.AccountBalanceWallet,
                actionLabel = "Add Bank",
                onActionClick = { showAddBankDialog = true }
            )
        } else {
            if (!isStackExpanded) {
                // Collapsed physical stacked look: Cards overlap using negative offset + smaller scale scaling
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                        .clickable { isStackExpanded = true }
                        .padding(bottom = 12.dp)
                ) {
                    accountsList.forEachIndexed { index, account ->
                        val visualIndex = index
                        val yOffset = (40 * visualIndex).dp
                        val scale = (1f - (0.04f * visualIndex)).coerceIn(0.8f, 1f)
                        
                        val gradientColors = when (account.type) {
                            "WALLET" -> listOf(Color(0xFF047857), Color(0xFF10B981)) // Emerald Green
                            else -> when (index % 4) {
                                0 -> listOf(Color(0xFF1E1B4B), Color(0xFF312E81)) // Dark Indigo
                                1 -> listOf(Color(0xFF0F172A), Color(0xFF1E293B)) // Deep Slate
                                2 -> listOf(Color(0xFF581C87), Color(0xFF6B21A8)) // Royal Amethyst
                                else -> listOf(Color(0xFF881337), Color(0xFF9F1239)) // Rosewood Crimson
                            }
                        }

                        val digits = if (account.type == "WALLET") "CASH WALLET" else account.accountNumber ?: "•••• •••• •••• 1234"
                        
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .offset(y = yOffset)
                                .graphicsLayer {
                                    scaleX = scale
                                    scaleY = scale
                                    translationY = yOffset.toPx()
                                }
                                .zIndex((accountsList.size - visualIndex).toFloat())
                                .aspectRatio(1.586f)
                                .clip(RoundedCornerShape(20.dp)),
                            elevation = CardDefaults.cardElevation(defaultElevation = (12 - 2 * visualIndex).dp)
                        ) {
                            VirtualCardItem(
                                name = account.name,
                                bankName = account.bankName ?: "Cash Ledger",
                                accountNumber = digits,
                                balance = account.balance,
                                currencySymbol = settings.currencySymbol,
                                holderName = settings.userName,
                                tagType = if (account.type == "WALLET") "CASH STORE" else "DEBIT ACCOUNT",
                                gradientColors = gradientColors
                            )
                        }
                    }
                }
                
                Text(
                    text = "Tap on the card stack to slide open the physical wallet",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                )
            } else {
                // Stack expanded layout: Cards laid out sequentially with action control banners
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    accountsList.forEachIndexed { index, account ->
                        val gradientColors = when (account.type) {
                            "WALLET" -> listOf(Color(0xFF047857), Color(0xFF10B981))
                            else -> when (index % 4) {
                                0 -> listOf(Color(0xFF1E1B4B), Color(0xFF312E81))
                                1 -> listOf(Color(0xFF0F172A), Color(0xFF1E293B))
                                2 -> listOf(Color(0xFF581C87), Color(0xFF6B21A8))
                                else -> listOf(Color(0xFF881337), Color(0xFF9F1239))
                            }
                        }

                        val digits = if (account.type == "WALLET") "CASH WALLET" else account.accountNumber ?: "•••• •••• •••• 1234"
                        var showDeleteConfirm by remember { mutableStateOf(false) }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f), RoundedCornerShape(24.dp))
                                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f), RoundedCornerShape(24.dp))
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            VirtualCardItem(
                                name = account.name,
                                bankName = account.bankName ?: "Cash Ledger",
                                accountNumber = digits,
                                balance = account.balance,
                                currencySymbol = settings.currencySymbol,
                                holderName = settings.userName,
                                tagType = if (account.type == "WALLET") "CASH STORE" else "DEBIT ACCOUNT",
                                gradientColors = gradientColors
                            )

                            // Action footer for each card
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (account.type == "WALLET") {
                                    TextButton(
                                        onClick = { viewModel.zeroCashWallet() },
                                        colors = ButtonDefaults.textButtonColors(contentColor = SystemRed)
                                    ) {
                                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(15.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Reset Cash Wallet to Zero", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                } else {
                                    TextButton(
                                        onClick = { showDeleteConfirm = true },
                                        colors = ButtonDefaults.textButtonColors(contentColor = SystemRed)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(15.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Delete Bank Account", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        if (showDeleteConfirm) {
                            AlertDialog(
                                onDismissRequest = { showDeleteConfirm = false },
                                shape = RoundedCornerShape(24.dp),
                                containerColor = MaterialTheme.colorScheme.surface,
                                title = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .background(SystemRed.copy(alpha = 0.1f), CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = null, tint = SystemRed, modifier = Modifier.size(18.dp))
                                        }
                                        Text("Delete '${account.name}'?", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                    }
                                },
                                text = {
                                    Text(
                                        "Are you sure you want to permanently detach this card and delete associated savings entries? Ledger journal records balance mappings will be adjusted.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                },
                                confirmButton = {
                                    Button(
                                        onClick = {
                                            showDeleteConfirm = false
                                            viewModel.deleteBankAccount(account.id)
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = SystemRed),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text("Delete account", fontWeight = FontWeight.Bold)
                                    }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showDeleteConfirm = false }) {
                                        Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }

        // Add Dialog Sheet
        if (showAddBankDialog) {
            AlertDialog(
                onDismissRequest = { showAddBankDialog = false },
                shape = RoundedCornerShape(20.dp),
                containerColor = MaterialTheme.colorScheme.surface,
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(PremiumIndigo.copy(alpha = 0.1f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountBalance,
                                contentDescription = null,
                                tint = PremiumIndigo,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Text(
                            "Create Bank Account",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        OutlinedTextField(
                            value = bankAccountName,
                            onValueChange = { bankAccountName = it },
                            label = { Text("Display Label (e.g. Savings)") },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().testTag("bank_name_input"),
                            colors = premiumTextFieldColors(),
                            singleLine = true
                        )
                        var bankDropdownExpanded by remember { mutableStateOf(false) }
                        val lankanBanks = listOf(
                            "Bank of Ceylon (BOC)" to ("BOC" to Color(0xFFEAB308)),
                            "People's Bank" to ("PB" to Color(0xFF10B981)),
                            "Commercial Bank of Ceylon" to ("CBC" to Color(0xFF3B82F6)),
                            "Hatton National Bank (HNB)" to ("HNB" to Color(0xFFEC4899)),
                            "Sampath Bank" to ("SAMP" to Color(0xFFF59E0B)),
                            "Seylan Bank" to ("SEY" to Color(0xFF6366F1)),
                            "Nations Trust Bank (NTB)" to ("NTB" to Color(0xFF111827)),
                            "DFCC Bank" to ("DFCC" to Color(0xFF8B5CF6)),
                            "National Savings Bank (NSB)" to ("NSB" to Color(0xFFEF4444)),
                            "Union Bank of Colombo" to ("UBC" to Color(0xFF06B6D4)),
                            "Pan Asia Bank" to ("PAB" to Color(0xFF14B8A6)),
                            "Sanasa Development Bank" to ("SDB" to Color(0xFFF43F5E)),
                            "Cargills Bank" to ("CBL" to Color(0xFF84CC16))
                        )
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = bankNameInput,
                                onValueChange = { },
                                readOnly = true,
                                label = { Text("Bank Name") },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth(),
                                colors = premiumTextFieldColors(),
                                singleLine = true,
                                trailingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = "Expand bank dropdown",
                                        tint = PremiumIndigo
                                    )
                                }
                            )
                            // Invisible tap interceptor
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .clickable { bankDropdownExpanded = !bankDropdownExpanded }
                            )
                            
                            DropdownMenu(
                                expanded = bankDropdownExpanded,
                                onDismissRequest = { bankDropdownExpanded = false },
                                modifier = Modifier
                                    .fillMaxWidth(0.85f)
                                    .background(MaterialTheme.colorScheme.surface)
                                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                            ) {
                                lankanBanks.forEach { bPair ->
                                    val fullName = bPair.first
                                    val (initials, color) = bPair.second
                                    DropdownMenuItem(
                                        text = {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(32.dp)
                                                        .background(color.copy(alpha = 0.15f), CircleShape)
                                                        .border(1.1.dp, color, CircleShape),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = initials,
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = color
                                                    )
                                                }
                                                Text(
                                                    text = fullName,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                        },
                                        onClick = {
                                            bankNameInput = fullName
                                            bankDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                        OutlinedTextField(
                            value = branchNameInput,
                            onValueChange = { branchNameInput = it },
                            label = { Text("Branch Code/Location") },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth(),
                            colors = premiumTextFieldColors(),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = accountNumberInput,
                            onValueChange = { accountNumberInput = it },
                            label = { Text("Account Number") },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth(),
                            colors = premiumTextFieldColors(),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = openingBalanceInput,
                            onValueChange = { openingBalanceInput = it },
                            label = { Text("Opening Balance (${settings.currencySymbol})") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth(),
                            colors = premiumTextFieldColors(),
                            singleLine = true
                        )
                    }
                },
                confirmButton = {
                    PremiumButton(
                        text = "Add Account",
                        onClick = {
                            val ob = openingBalanceInput.toDoubleOrNull() ?: 0.0
                            if (bankAccountName.isNotBlank() && bankNameInput.isNotBlank()) {
                                viewModel.addBankAccount(bankAccountName, bankNameInput, branchNameInput, accountNumberInput, ob)
                                bankAccountName = ""
                                bankNameInput = ""
                                branchNameInput = ""
                                accountNumberInput = ""
                                openingBalanceInput = ""
                                showAddBankDialog = false
                            }
                        }
                    )
                },
                dismissButton = {
                    TextButton(onClick = { showAddBankDialog = false }) {
                        Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            )
        }

        // Funds Transfer Dialog sheet
        if (showTransferDialog) {
            val validAccFirst = accounts.firstOrNull()?.id ?: 0L
            val validAccSecond = accounts.drop(1).firstOrNull()?.id ?: 0L
            if (fromAccountSelected == 0L) fromAccountSelected = validAccFirst
            if (toAccountSelected == 0L) toAccountSelected = validAccSecond

            AlertDialog(
                onDismissRequest = { showTransferDialog = false },
                shape = RoundedCornerShape(16.dp),
                containerColor = MaterialTheme.colorScheme.surface,
                title = {
                    Text(
                        "Inter-Account Transfer",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            "Source Wallet/Bank",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            accounts.take(3).forEach { acc ->
                                val selected = fromAccountSelected == acc.id
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (selected) PremiumIndigo else MaterialTheme.colorScheme.surfaceVariant)
                                        .border(
                                            width = 1.dp,
                                            color = if (selected) PremiumIndigo else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .clickable { fromAccountSelected = acc.id }
                                        .padding(8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = acc.name,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1
                                    )
                                }
                            }
                        }

                        Text(
                            "Destination Wallet/Bank",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            accounts.take(3).forEach { acc ->
                                val selected = toAccountSelected == acc.id
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (selected) PremiumIndigo else MaterialTheme.colorScheme.surfaceVariant)
                                        .border(
                                            width = 1.dp,
                                            color = if (selected) PremiumIndigo else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .clickable { toAccountSelected = acc.id }
                                        .padding(8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = acc.name,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = transferAmountInput,
                            onValueChange = { transferAmountInput = it },
                            label = { Text("Transfer Amount") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth(),
                            colors = premiumTextFieldColors(),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = transferFeeInput,
                            onValueChange = { transferFeeInput = it },
                            label = { Text("Transaction Fee") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth(),
                            colors = premiumTextFieldColors(),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = transferNoteInput,
                            onValueChange = { transferNoteInput = it },
                            label = { Text("Short journal memo (e.g. ATM withdrawal)") },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth(),
                            colors = premiumTextFieldColors(),
                            singleLine = true
                        )
                    }
                },
                confirmButton = {
                    PremiumButton(
                        text = "Post Transfer",
                        onClick = {
                            val amt = transferAmountInput.toDoubleOrNull() ?: 0.0
                            val fee = transferFeeInput.toDoubleOrNull() ?: 0.0
                            if (amt > 0.0) {
                                viewModel.transferFunds(fromAccountSelected, toAccountSelected, amt, fee, transferNoteInput)
                                showTransferDialog = false
                                transferAmountInput = ""
                                transferFeeInput = "0.00"
                                transferNoteInput = ""
                            }
                        }
                    )
                },
                dismissButton = {
                    TextButton(onClick = { showTransferDialog = false }) {
                        Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            )
        }
    }
}

@Composable
fun VirtualCardItem(
    name: String,
    bankName: String,
    accountNumber: String,
    balance: Double,
    currencySymbol: String,
    holderName: String,
    tagType: String,
    gradientColors: List<Color>,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(Brush.linearGradient(colors = gradientColors))
    ) {
        // Translucent radial visual accent overlay
        androidx.compose.foundation.Canvas(modifier = Modifier.matchParentSize()) {
            val w = size.width
            val h = size.height
            drawCircle(
                color = Color.White.copy(alpha = 0.04f),
                radius = w * 0.45f,
                center = androidx.compose.ui.geometry.Offset(w * 0.95f, h * 0.15f)
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.03f),
                radius = w * 0.28f,
                center = androidx.compose.ui.geometry.Offset(w * 0.12f, h * 0.88f)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Row: Brand labels & smart gold chip representation
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = bankName.uppercase(),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        letterSpacing = 1.2.sp
                    )
                    Text(
                        text = tagType,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }

                // Simulated smart micro-chip
                Box(
                    modifier = Modifier
                        .size(width = 34.dp, height = 24.dp)
                        .background(Color(0xFFF1C40F).copy(alpha = 0.9f), RoundedCornerShape(6.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                ) {
                    androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height
                        drawLine(Color.Black.copy(alpha = 0.12f), androidx.compose.ui.geometry.Offset(w * 0.5f, 0f), androidx.compose.ui.geometry.Offset(w * 0.5f, h), 1f)
                        drawLine(Color.Black.copy(alpha = 0.12f), androidx.compose.ui.geometry.Offset(0f, h * 0.5f), androidx.compose.ui.geometry.Offset(w, h * 0.5f), 1f)
                    }
                }
            }

            // Middle Row: Card digits / Details formatted cleanly
            Column {
                Text(
                    text = accountNumber.chunked(4).joinToString("   "),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.85f),
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.2.sp
                )
            }

            // Bottom Row: Holder name and balance pairing
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = "CARDHOLDER",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White.copy(alpha = 0.5f),
                        letterSpacing = 0.8.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = holderName.uppercase(),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "CURRENT BALANCE",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White.copy(alpha = 0.5f),
                        letterSpacing = 0.8.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${currencySymbol}${String.format("%,.2f", balance)}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}
