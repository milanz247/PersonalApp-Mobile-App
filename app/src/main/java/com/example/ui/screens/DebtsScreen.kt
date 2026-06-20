package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Debt
import com.example.ui.AppViewModel
import com.example.ui.components.*
import com.example.ui.theme.*
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DebtsScreen(viewModel: AppViewModel, modifier: Modifier = Modifier) {
    val debts by viewModel.debtsState.collectAsState()
    val accounts by viewModel.accountsState.collectAsState()
    val settings by viewModel.settingsState.collectAsState()
    val transactions by viewModel.transactionsState.collectAsState()
    val context = LocalContext.current

    var showAddDialog by remember { mutableStateOf(false) }
    var viewingDebtDetail by remember { mutableStateOf<Debt?>(null) }

    // Add inputs
    var dType by remember { mutableStateOf("BORROWED") } // OR "LENT"
    var dPersonName by remember { mutableStateOf("") }
    var dAmount by remember { mutableStateOf("") }
    var dAccountId by remember { mutableLongStateOf(0L) }
    var dFee by remember { mutableStateOf("0.00") }
    var dDescription by remember { mutableStateOf("") }
    var dContactEmail by remember { mutableStateOf("") }
    var dContactPhone by remember { mutableStateOf("") }

    // Record repayment state variables
    var showRepayDialog by remember { mutableStateOf(false) }
    var repayAmount by remember { mutableStateOf("") }
    var repayAccountId by remember { mutableLongStateOf(0L) }
    var repayFee by remember { mutableStateOf("0.00") }

    val totalBorrowedPending = debts.filter { it.deletedAt == null && it.type == "BORROWED" && it.status != "SETTLED" }.sumOf { it.remainingAmount }
    val totalLentPending = debts.filter { it.deletedAt == null && it.type == "LENT" && it.status != "SETTLED" }.sumOf { it.remainingAmount }

    val activeDebtDetail = viewingDebtDetail?.let { active ->
        debts.firstOrNull { it.id == active.id && it.deletedAt == null }
    }

    val currentDebt = activeDebtDetail

    if (currentDebt == null) {
        // List dashboard View
        Column(
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "TRUSTS & LOANS",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Debt Ledger",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                PremiumButton(
                    text = "Add Loan",
                    onClick = {
                        dAccountId = accounts.firstOrNull()?.id ?: 0L
                        showAddDialog = true
                    },
                    icon = Icons.Default.Add
                )
            }

            // High Fidelity Balance Stats Tracker for Borrower vs Lender
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                // Borrowed pending summary card
                PremiumCard(modifier = Modifier.weight(1f)) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "YOU OWE (BORROWED)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${settings.currencySymbol}${String.format("%,.2f", totalBorrowedPending)}",
                            style = MaterialTheme.typography.titleLarge,
                            color = SystemAmber,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Lent pending summary card
                PremiumCard(modifier = Modifier.weight(1f)) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "OWED TO YOU (LENT)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${settings.currencySymbol}${String.format("%,.2f", totalLentPending)}",
                            style = MaterialTheme.typography.titleLarge,
                            color = SystemBlue,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Text(
                text = "ACTIVE BALANCES",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            val activeDebts = debts.filter { it.deletedAt == null }
            if (activeDebts.isEmpty()) {
                EmptyState(
                    title = "No Debts or Loans Recorded",
                    description = "Keep track of peer-to-peer micro-loans and settling journals offline securely.",
                    icon = Icons.Default.Handshake,
                    actionLabel = "Log Debt Record",
                    onActionClick = {
                        dAccountId = accounts.firstOrNull()?.id ?: 0L
                        showAddDialog = true
                    }
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    activeDebts.forEach { debt ->
                        val colorBadge = if (debt.type == "LENT") SystemBlue else SystemAmber
                        PremiumCard(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { viewingDebtDetail = debt }
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .background(colorBadge.copy(alpha = 0.08f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (debt.type == "LENT") Icons.Default.CallMade else Icons.Default.CallReceived,
                                            contentDescription = null,
                                            tint = colorBadge,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = debt.personName,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Principal: ${settings.currencySymbol}${String.format("%.2f", debt.amount)}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Column(
                                    horizontalAlignment = Alignment.End,
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    AmountText(
                                        amount = debt.remainingAmount,
                                        currencySymbol = settings.currencySymbol,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        type = if (debt.status == "SETTLED") "INCOME" else null
                                    )
                                    StatusPill(text = debt.status, color = colorBadge)
                                }
                            }
                        }
                    }
                }
            }
        }
    } else {
        // Detail View Screen
        Column(
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewingDebtDetail = null }) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Text("Loan Ledger Details", style = MaterialTheme.typography.titleMedium)
                Box(modifier = Modifier.width(48.dp))
            }

            PremiumCard(modifier = Modifier.fillMaxWidth()) {
                val colorBadge = if (currentDebt.type == "LENT") SystemBlue else SystemAmber
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "DEBT HOLDER",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = currentDebt.personName,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(colorBadge.copy(alpha = 0.08f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (currentDebt.type == "LENT") Icons.Default.CallMade else Icons.Default.CallReceived,
                                contentDescription = null,
                                tint = colorBadge,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("Total Outlay Principal", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${settings.currencySymbol}${String.format("%,.2f", currentDebt.amount)}",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Outstanding Balance", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${settings.currencySymbol}${String.format("%,.2f", currentDebt.remainingAmount)}",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = colorBadge
                            )
                        }
                    }

                    if (currentDebt.description != null && currentDebt.description.isNotBlank()) {
                        Column {
                            Text("Memo Description", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = currentDebt.description, style = MaterialTheme.typography.bodyMedium)
                        }
                    }

                    // Contact action panel with secure external intents
                    if ((currentDebt.contactEmail != null && currentDebt.contactEmail.isNotBlank()) ||
                        (currentDebt.contactPhone != null && currentDebt.contactPhone.isNotBlank())) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Reach Out & Remind", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                            currentDebt.contactEmail?.let { email ->
                                Row(
                                    modifier = Modifier.clickable {
                                        try {
                                            val mailIntent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$email")).apply {
                                                putExtra(Intent.EXTRA_SUBJECT, "[Ledger Notification] Pending Balance Settlement Update")
                                                putExtra(Intent.EXTRA_TEXT, "Hi ${currentDebt.personName},\n\nThis is a status update notification indicating that your outstanding loan balance with me is currently ${settings.currencySymbol}${String.format("%,.2f", currentDebt.remainingAmount)}.\n\nThank you!")
                                            }
                                            context.startActivity(mailIntent)
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Cannot launch Mail interface.", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Email, contentDescription = null, tint = PremiumIndigo, modifier = Modifier.size(16.dp))
                                    Text(text = email, style = MaterialTheme.typography.bodyMedium, color = PremiumIndigo, fontWeight = FontWeight.SemiBold)
                                }
                            }

                            currentDebt.contactPhone?.let { phone ->
                                Row(
                                    modifier = Modifier.clickable {
                                        try {
                                            val smsUri = Uri.parse("smsto:$phone")
                                            val smsIntent = Intent(Intent.ACTION_SENDTO, smsUri).apply {
                                                putExtra("sms_body", "Hello, this is a ledger status reminder that your current outstanding loan balance is ${settings.currencySymbol}${String.format("%,.2f", currentDebt.remainingAmount)}.")
                                            }
                                            context.startActivity(smsIntent)
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Cannot launch SMS interface.", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Sms, contentDescription = null, tint = PremiumIndigo, modifier = Modifier.size(16.dp))
                                    Text(text = phone, style = MaterialTheme.typography.bodyMedium, color = PremiumIndigo, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Current Settlement State", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        StatusPill(text = currentDebt.status, color = colorBadge)
                    }
                }
            }

            // Quick actions repayment panel
            if (currentDebt.status != "SETTLED") {
                PremiumButton(
                    text = "Record Payment / Settle Partial",
                    onClick = {
                        repayAccountId = accounts.firstOrNull()?.id ?: 0L
                        showRepayDialog = true
                    },
                    modifier = Modifier.fillMaxWidth(),
                    icon = Icons.Default.Paid
                )
            }

            Text(
                text = "Ledger History",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Repayment history list rows filtered dynamically
            val repaymentLogs = remember(transactions, currentDebt) {
                transactions.filter { it.deletedAt == null && it.debtId == currentDebt.id }
            }

            if (repaymentLogs.isEmpty()) {
                PremiumCard(modifier = Modifier.fillMaxWidth()) {
                    Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        Text("No payment transactions posted yet.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    repaymentLogs.forEach { log ->
                        PremiumCard(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .background(SystemGreen.copy(alpha = 0.08f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = SystemGreen, modifier = Modifier.size(16.dp))
                                    }
                                    Column {
                                        val formD = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(log.date))
                                        Text("Repayment Processed", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                        Text("Date: $formD", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                                AmountText(amount = log.amount, currencySymbol = settings.currencySymbol, fontSize = 14.sp)
                            }
                        }
                    }
                }
            }
        }
    }

    // Debt record addition dialog
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            shape = RoundedCornerShape(16.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                Text(
                    "Provision Loan Document",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Type split
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("BORROWED" to "Borrow", "LENT" to "Lend").forEach { pair ->
                            val sel = dType == pair.first
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
                                    .clickable { dType = pair.first }
                                    .padding(10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = pair.second,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (sel) Color.White else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = dPersonName,
                        onValueChange = { dPersonName = it },
                        label = { Text("Person Name") },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        colors = premiumTextFieldColors(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = dAmount,
                        onValueChange = { dAmount = it },
                        label = { Text("Principal Amount") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        colors = premiumTextFieldColors(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = dDescription,
                        onValueChange = { dDescription = it },
                        label = { Text("Memo / Purpose") },
                        shape = RoundedCornerShape(12.dp),
                        colors = premiumTextFieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = dContactEmail,
                        onValueChange = { dContactEmail = it },
                        label = { Text("Email Address") },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        colors = premiumTextFieldColors(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = dContactPhone,
                        onValueChange = { dContactPhone = it },
                        label = { Text("Phone Number") },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        colors = premiumTextFieldColors(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                PremiumButton(
                    text = "Save Loan",
                    onClick = {
                        val amt = dAmount.toDoubleOrNull() ?: 0.0
                        val fee = dFee.toDoubleOrNull() ?: 0.0
                        if (amt > 0.0 && dPersonName.isNotBlank() && dAccountId > 0L) {
                            viewModel.addDebt(
                                type = dType,
                                personName = dPersonName,
                                amount = amt,
                                accountId = dAccountId,
                                fee = fee,
                                dueDate = null,
                                description = dDescription,
                                contactEmail = dContactEmail,
                                contactPhone = dContactPhone
                            )
                            showAddDialog = false
                            dPersonName = ""
                            dAmount = ""
                            dDescription = ""
                            dContactEmail = ""
                            dContactPhone = ""
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

    // Repayment Dialog interface
    if (showRepayDialog && activeDebtDetail != null) {
        val validDebt = activeDebtDetail
        AlertDialog(
            onDismissRequest = { showRepayDialog = false },
            shape = RoundedCornerShape(16.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                Text(
                    "Record Settle Transaction",
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
                        text = "Remaining Owed Balance: ${settings.currencySymbol}${String.format("%,.2f", validDebt.remainingAmount)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = repayAmount,
                        onValueChange = { repayAmount = it },
                        label = { Text("Payment/Settlement Amount") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().testTag("repay_amount_input"),
                        singleLine = true
                    )

                    Text(
                        "Select cash store mapping",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        accounts.forEach { acc ->
                            val matches = repayAccountId == acc.id
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (matches) PremiumIndigo else MaterialTheme.colorScheme.surfaceVariant)
                                    .border(
                                        width = 1.dp,
                                        color = if (matches) PremiumIndigo else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable { repayAccountId = acc.id }
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
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amt = repayAmount.toDoubleOrNull() ?: 0.0
                        val fee = repayFee.toDoubleOrNull() ?: 0.0
                        if (amt > 0.0 && repayAccountId > 0L) {
                            viewModel.recordDebtPayment(validDebt.id, amt, repayAccountId, fee)
                            showRepayDialog = false
                            repayAmount = ""
                        }
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PremiumIndigo)
                ) {
                    Text("Record Payment", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRepayDialog = false }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }
}
