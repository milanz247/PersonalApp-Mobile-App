package com.example.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.AppViewModel
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.utils.ExportUtils
import java.time.LocalDate
import android.app.DatePickerDialog
import java.util.Calendar
import java.time.format.DateTimeFormatter

@Composable
fun ReportsScreen(viewModel: AppViewModel, modifier: Modifier = Modifier) {
    val accounts by viewModel.accountsState.collectAsState()
    val categories by viewModel.categoriesState.collectAsState()
    val transactions by viewModel.transactionsState.collectAsState()
    val settings by viewModel.settingsState.collectAsState()
    val currentMonthYear by viewModel.currentMonthYear.collectAsState()
    val context = LocalContext.current

    // Default to the current month's start and end date
    val now = LocalDate.now()
    var startDate by remember { mutableStateOf(now.withDayOfMonth(1)) }
    var endDate by remember { mutableStateOf(now.withDayOfMonth(now.lengthOfMonth())) }

    val showStartDatePicker = {
        val calendar = Calendar.getInstance()
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                startDate = LocalDate.of(year, month + 1, dayOfMonth)
            },
            startDate.year,
            startDate.monthValue - 1,
            startDate.dayOfMonth
        ).show()
    }

    val showEndDatePicker = {
        val calendar = Calendar.getInstance()
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                endDate = LocalDate.of(year, month + 1, dayOfMonth)
            },
            endDate.year,
            endDate.monthValue - 1,
            endDate.dayOfMonth
        ).show()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)) {
            Text(
                text = "STATEMENTS & WORKBOOKS",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Ledger Reports",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        // Section A: PDF Compilation Card
        PremiumCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Generate PDF Summary Statement",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Compiles total incomes, total expenses, savings coefficients, account ledger status, and Category percentages into a beautifully formatted, shareable PDF document.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(4.dp))

                Button(
                    onClick = {
                        ExportUtils.generateAndSharePdf(
                            context = context,
                            userName = settings.userName,
                            monthYear = currentMonthYear,
                            currencySymbol = settings.currencySymbol,
                            accounts = accounts,
                            categories = categories,
                            transactions = transactions
                        )
                    },
                    modifier = Modifier.fillMaxWidth().testTag("generate_pdf_button"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PremiumIndigo)
                ) {
                    Icon(imageVector = Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Compile & Share PDF", fontWeight = FontWeight.Bold)
                }
            }
        }

        // Section B: CSV Export Card
        PremiumCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Export Raw Ledger to CSV",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Generates a flat .csv worksheet containing all logged double entry rows (dates, source, destinations, notes, and fees) for standard bookkeeping spreadsheet imports.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(4.dp))

                Button(
                    onClick = {
                        ExportUtils.generateAndShareCsv(
                            context = context,
                            startDate = startDate,
                            endDate = endDate,
                            transactions = transactions,
                            accounts = accounts,
                            categories = categories
                        )
                    },
                    modifier = Modifier.fillMaxWidth().testTag("export_csv_button"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PremiumIndigo)
                ) {
                    Icon(imageVector = Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Export Statement to CSV", fontWeight = FontWeight.Bold)
                }
            }
        }

        // Section C: Interactive Beautiful Bank Statement Sheet / Ledger Statement
        val statementFormatter = remember { DateTimeFormatter.ofPattern("dd MMM yyyy") }
        val rangeTxs = remember(transactions, startDate, endDate) {
            transactions.filter { tx ->
                val txDate = java.time.Instant.ofEpochMilli(tx.date)
                    .atZone(java.time.ZoneId.systemDefault())
                    .toLocalDate()
                !txDate.isBefore(startDate) && !txDate.isAfter(endDate)
            }.sortedBy { it.date }
        }

        val totalIncome = rangeTxs.filter { it.type == "INCOME" }.sumOf { it.amount }
        val totalExpense = rangeTxs.filter { it.type == "EXPENSE" }.sumOf { it.amount }
        val totalFee = rangeTxs.sumOf { it.fee }
        val netChange = totalIncome - totalExpense - totalFee

        PremiumCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column {
                    Text(
                        text = "LEDGER ACCOUNT STATEMENT",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.1.sp,
                        color = PremiumIndigo
                    )
                    Text(
                        text = "Visual statement generator filtering double-entry logs dynamically.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                // Date Picker Selectors Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { showStartDatePicker() },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("FROM DATE", fontSize = 8.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(startDate.format(statementFormatter), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    OutlinedButton(
                        onClick = { showEndDatePicker() },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("TO DATE", fontSize = 8.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(endDate.format(statementFormatter), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Balance summary box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Total Credits (Inflow)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${settings.currencySymbol}${String.format("%,.2f", totalIncome)}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = SystemGreen)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Total Debits (Outflow)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${settings.currencySymbol}${String.format("%,.2f", totalExpense)}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = SystemRed)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Ancillary/Platform Fees", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${settings.currencySymbol}${String.format("%,.2f", totalFee)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("NET STATUTORY BALANCE", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            val balanceColor = if (netChange >= 0) SystemGreen else SystemRed
                            Text(
                                text = "${if (netChange >= 0) "+" else ""}${settings.currencySymbol}${String.format("%,.2f", netChange)}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                color = balanceColor
                            )
                        }
                    }
                }

                // Statement List Table
                Text(
                    text = "CHRONOLOGICAL LEDGER ENTRIES (${rangeTxs.size})",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 0.5.sp
                )

                if (rangeTxs.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No records logged within selected timeline.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Table header
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("DATE & DETAILS", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("AMOUNT", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

                        rangeTxs.forEach { tx ->
                            val txDate = java.time.Instant.ofEpochMilli(tx.date)
                                .atZone(java.time.ZoneId.systemDefault())
                                .toLocalDate()
                            val formattedDate = txDate.format(DateTimeFormatter.ofPattern("dd MMM"))
                            val catName = categories.firstOrNull { it.id == tx.categoryId }?.name ?: tx.type

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp, horizontal = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "$formattedDate • $catName",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                    if (!tx.note.isNullOrBlank()) {
                                        Text(
                                            text = tx.note,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontSize = 9.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1
                                        )
                                    }
                                }

                                val amountLabel = when (tx.type) {
                                    "INCOME" -> "+${settings.currencySymbol}${String.format("%.2f", tx.amount)}"
                                    "EXPENSE" -> "-${settings.currencySymbol}${String.format("%.2f", tx.amount)}"
                                    else -> "${settings.currencySymbol}${String.format("%.2f", tx.amount)}"
                                }
                                val amountColor = when (tx.type) {
                                    "INCOME" -> SystemGreen
                                    "EXPENSE" -> SystemRed
                                    else -> SystemBlue
                                }

                                Text(
                                    text = amountLabel,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = amountColor
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
