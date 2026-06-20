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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

        walletAccount?.let { wall ->
            Text(
                text = "CASH IN HAND & POCKET",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            PremiumCard(
                modifier = Modifier.fillMaxWidth().testTag("wallet_card")
            ) {
                Column {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .background(SystemGreen.copy(alpha = 0.08f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccountBalanceWallet,
                                    contentDescription = null,
                                    tint = SystemGreen,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = wall.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "On-Device Paper Ledger",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        AmountText(
                            amount = wall.balance,
                            currencySymbol = settings.currencySymbol,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = { viewModel.zeroCashWallet() },
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Reset Wallet to Zero",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Commercial Bank accounts list
        if (bankAccountsList.isNotEmpty()) {
            Text(
                text = "COMMERCIAL SAVINGS & CHECKING",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            bankAccountsList.forEach { bank ->
                var showDeleteConfirm by remember { mutableStateOf(false) }

                // Customized physical-credit-card look-and-feel card
                PremiumCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .background(SystemBlue.copy(alpha = 0.08f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccountBalance,
                                    contentDescription = null,
                                    tint = SystemBlue,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = bank.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${bank.bankName} • ${bank.accountNumber ?: "No account details"}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AmountText(
                                amount = bank.balance,
                                currencySymbol = settings.currencySymbol,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(
                                onClick = { showDeleteConfirm = true },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete account",
                                    tint = SystemRed,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                // Delete Confirmation Popups
                if (showDeleteConfirm) {
                    AlertDialog(
                        onDismissRequest = { showDeleteConfirm = false },
                        shape = RoundedCornerShape(16.dp),
                        containerColor = MaterialTheme.colorScheme.surface,
                        title = {
                            Text(
                                "Delete Bank Card",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        text = {
                            Text(
                                "Are you sure you want to permanently detach '${bank.name}'? All underlying ledger journals referencing this entity will lose their account balance mapping.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        confirmButton = {
                            TextButton(onClick = {
                                showDeleteConfirm = false
                                viewModel.deleteBankAccount(bank.id)
                            }) {
                                Text("Delete", color = SystemRed, fontWeight = FontWeight.Bold)
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
        } else if (bankAccountsList.isEmpty()) {
            EmptyState(
                title = "No Commercial Banks Linked",
                description = "Securely map and keep track of unlimited checking and credit accounts offline.",
                icon = Icons.Default.AccountBalance,
                actionLabel = "Add First Account",
                onActionClick = { showAddBankDialog = true }
            )
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
