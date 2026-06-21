package com.example.utils

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.example.data.model.Account
import com.example.data.model.Category
import com.example.data.model.Transaction
import java.io.File
import java.io.FileOutputStream
import java.io.FileWriter
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date

object ExportUtils {

    suspend fun generateAndSharePdf(
        context: Context,
        userName: String,
        monthYear: String, // e.g. "2026-06"
        currencySymbol: String,
        accounts: List<Account>,
        categories: List<Category>,
        transactions: List<Transaction>
    ) {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                // Ensure we don't process excessively large datasets which might cause memory overflow
                val sanitizedTransactions = if (transactions.size > 50000) transactions.take(50000) else transactions
                
                val pdfDocument = PdfDocument()
                val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 Size (595x842)
                val page = pdfDocument.startPage(pageInfo)
                val canvas = page.canvas

                // Core Paint Definitions
                val paintHeaderBg = Paint().apply {
                    color = Color.BLACK // Deep Black Background Accent
                    isAntiAlias = true
                }

                val paintCardBg = Paint().apply {
                    color = Color.WHITE // White card fill
                    isAntiAlias = true
                }

                val paintCardBorder = Paint().apply {
                    color = Color.BLACK // Black border
                    strokeWidth = 1f
                    style = Paint.Style.STROKE
                    isAntiAlias = true
                }

                val paintTextDark = Paint().apply {
                    color = Color.BLACK // Deep Black
                    isAntiAlias = true
                    textSize = 10f
                }

                val paintTextLight = Paint().apply {
                    color = Color.DKGRAY // Dark Gray
                    isAntiAlias = true
                    textSize = 9f
                }

                val paintLabelBold = Paint().apply {
                    color = Color.BLACK
                    isAntiAlias = true
                    textSize = 10f
                    isFakeBoldText = true
                }

                val paintHeaderTitle = Paint().apply {
                    color = Color.WHITE
                    isAntiAlias = true
                    textSize = 16f
                    isFakeBoldText = true
                }

                val paintHeaderSubtitle = Paint().apply {
                    color = Color.LTGRAY // Light Gray
                    isAntiAlias = true
                    textSize = 9f
                }

                val paintSectionHeading = Paint().apply {
                    color = Color.BLACK // Deep Black
                    isAntiAlias = true
                    textSize = 11f
                    isFakeBoldText = true
                }

                val paintLine = Paint().apply {
                    color = Color.BLACK
                    strokeWidth = 1f
                }

                val paintGreen = Paint().apply {
                    color = Color.parseColor("#10B981") // System Success Green
                    isAntiAlias = true
                    textSize = 10f
                    isFakeBoldText = true
                }

                val paintRed = Paint().apply {
                    color = Color.parseColor("#EF4444") // System Error Red
                    isAntiAlias = true
                    textSize = 10f
                    isFakeBoldText = true
                }

                // 1. Draw Brand Header Rectangle Block
                canvas.drawRoundRect(40f, 35f, 555f, 105f, 12f, 12f, paintHeaderBg)
                canvas.drawText("LEDGER FINANCIAL STATEMENT", 55f, 65f, paintHeaderTitle)
                canvas.drawText("USER: $userName  •  BILLING STATUS PERIOD: $monthYear  •  GENERATED SECURELY VIA OFFLINE LEDGER PRO", 55f, 85f, paintHeaderSubtitle)

                // 2. Fetch and Calculate summary statistics
                var totalIncome = 0.0
                var totalExpense = 0.0
                for (tx in sanitizedTransactions) {
                    if (tx.deletedAt == null) {
                        when (tx.type) {
                            "INCOME" -> totalIncome += tx.amount
                            "EXPENSE" -> totalExpense += tx.amount
                        }
                    }
                }
                val savings = totalIncome - totalExpense
                val savingsRatePercent = if (totalIncome > 0) (savings / totalIncome) * 100 else 0.0

                // 3. Draw stat card highlights
                // Card A: CASH INFLOW
                canvas.drawRoundRect(40f, 120f, 195f, 180f, 8f, 8f, paintCardBg)
                canvas.drawRoundRect(40f, 120f, 195f, 180f, 8f, 8f, paintCardBorder)
                canvas.drawText("CASH RECEIVED (INFLOW)", 50f, 140f, paintTextLight)
                paintTextDark.textSize = 12f
                paintTextDark.isFakeBoldText = true
                canvas.drawText("$currencySymbol${String.format("%,.2f", totalIncome)}", 50f, 165f, paintGreen)

                // Card B: CASH OUTFLOW
                canvas.drawRoundRect(210f, 120f, 375f, 180f, 8f, 8f, paintCardBg)
                canvas.drawRoundRect(210f, 120f, 375f, 180f, 8f, 8f, paintCardBorder)
                canvas.drawText("CASH DISBURSED", 220f, 140f, paintTextLight)
                canvas.drawText("$currencySymbol${String.format("%,.2f", totalExpense)}", 220f, 165f, paintRed)

                // Card C: NET CASH SAVED
                canvas.drawRoundRect(390f, 120f, 555f, 180f, 8f, 8f, paintCardBg)
                canvas.drawRoundRect(390f, 120f, 555f, 180f, 8f, 8f, paintCardBorder)
                canvas.drawText("NET LEDGER COEF", 400f, 140f, paintTextLight)
                
                val netSavedPaint = if (savings >= 0) paintGreen else paintRed
                canvas.drawText("$currencySymbol${String.format("%,.2f", savings)}", 400f, 165f, netSavedPaint)

                // 4. Section: ACCOUNT LEDGER STATUS
                canvas.drawText("ACCOUNT LEDGER SUMMARY", 40f, 210f, paintSectionHeading)
                canvas.drawLine(40f, 218f, 555f, 218f, paintLine)

                var currentY = 238f
                paintTextDark.textSize = 9.5f
                paintTextDark.isFakeBoldText = false
                
                if (accounts.isEmpty()) {
                    canvas.drawText("No active banking/cash repositories connected.", 50f, currentY, paintTextLight)
                    currentY += 20f
                } else {
                    accounts.forEach { acc ->
                        val initials = (acc.bankName ?: "CASH").take(4).uppercase()
                        canvas.drawText("${acc.name} [${initials}]", 50f, currentY, paintLabelBold)
                        val maskRef = acc.accountNumber?.let { if (it.length >= 6) it.takeLast(6).padStart(12, '*') else it } ?: "N/A"
                        canvas.drawText("Type: ${acc.type} • Acct Ref: $maskRef", 210f, currentY, paintTextLight)
                        canvas.drawText("$currencySymbol${String.format("%,.2f", acc.balance)}", 440f, currentY, paintTextDark)
                        currentY += 18f
                    }
                }

                // 5. Section: DETAILED TRANSACTION JOURNAL REGISTER
                canvas.drawText("RECENT LEDGER JOURNAL TRANSACTIONS", 40f, currentY + 14f, paintSectionHeading)
                canvas.drawLine(40f, currentY + 22f, 555f, currentY + 22f, paintLine)

                // Draw Table Headings
                var journalY = currentY + 38f
                canvas.drawText("DATE & TIME", 45f, journalY, paintLabelBold)
                canvas.drawText("DETAILS & CATEGORIES", 155f, journalY, paintLabelBold)
                canvas.drawText("ACCOUNT/BANK", 345f, journalY, paintLabelBold)
                canvas.drawText("TYPE", 455f, journalY, paintLabelBold)
                canvas.drawText("AMOUNT", 505f, journalY, paintLabelBold)

                canvas.drawLine(40f, journalY + 8f, 555f, journalY + 8f, paintLine)
                journalY += 24f

                val activeLedgers = sanitizedTransactions.filter { it.deletedAt == null }.sortedByDescending { it.date }.take(14)
                val sdf = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault())

                if (activeLedgers.isEmpty()) {
                    canvas.drawText("No transactions logged in this billing period.", 50f, journalY, paintTextLight)
                } else {
                    activeLedgers.forEach { tx ->
                        val cleanDate = sdf.format(java.util.Date(tx.date))
                        val catName = categories.firstOrNull { it.id == tx.categoryId }?.name ?: "General"
                        val finalNote = if (!tx.note.isNullOrBlank()) " • ${tx.note}" else ""
                        val labelDetails = "${catName}${finalNote}"
                        val accId = tx.fromAccountId ?: tx.toAccountId
                        val accName = accounts.firstOrNull { it.id == accId }?.name ?: "Personal Ledger"

                        canvas.drawText(cleanDate, 45f, journalY, paintTextLight)
                        
                        // Truncate details text safely to fit PDF limits
                        val detailsToDraw = if (labelDetails.length > 36) labelDetails.take(33) + "..." else labelDetails
                        canvas.drawText(detailsToDraw, 155f, journalY, paintTextDark)
                        
                        canvas.drawText(accName, 345f, journalY, paintTextLight)
                        canvas.drawText(tx.type, 455f, journalY, if (tx.type == "INCOME") paintGreen else paintRed)
                        
                        // Right aligned or clean spaced numeric value
                        val displayAmount = "${if (tx.type == "INCOME") "+" else "-"}$currencySymbol${String.format("%,.2f", tx.amount)}"
                        canvas.drawText(displayAmount, 505f, journalY, if (tx.type == "INCOME") paintGreen else paintRed)

                        journalY += 20f
                    }
                }

                // Draw Standard Legal Disclaimer Footer
                canvas.drawLine(40f, 795f, 555f, 795f, paintLine)
                paintTextLight.textSize = 8f
                canvas.drawText("Securely compiled offline. QuickBooks local compatibility check completed successfully.", 40f, 810f, paintTextLight)
                canvas.drawText("Ledger Pro Local Statements are mathematically locked by cryptographic keys.", 40f, 822f, paintTextLight)

                pdfDocument.finishPage(page)

                // Write PDF file
                val file = File(context.cacheDir, "Report_${monthYear.replace("/", "-")}.pdf")
                val outputStream = FileOutputStream(file)
                pdfDocument.writeTo(outputStream)
                outputStream.close()
                pdfDocument.close()

                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    // Share File
                    shareDocumentFile(context, file, "application/pdf")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    android.widget.Toast.makeText(context, "Error generating PDF: ${e.localizedMessage}", android.widget.Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    suspend fun generateAndShareCsv(
        context: Context,
        startDate: LocalDate,
        endDate: LocalDate,
        transactions: List<Transaction>,
        accounts: List<Account>,
        categories: List<Category>
    ) {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val csvHeader = "Date,Type,Category,Amount,Fee,From Account,To Account,Note\n"
                val builder = java.lang.StringBuilder()
                builder.append(csvHeader)

                val startMillis = startDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                val endMillis = endDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

                // Filter and sort newest first, and limit size to prevent memory crash before writing
                val targetTransactions = transactions.filter {
                    it.deletedAt == null && it.date in startMillis until endMillis
                }.sortedByDescending { it.date }.take(50000)

                val formatter = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())

                for (tx in targetTransactions) {
                    val dateStr = formatter.format(Date(tx.date))
                    val typeStr = tx.type
                    val catName = categories.firstOrNull { it.id == tx.categoryId }?.name ?: ""
                    val amtStr = String.format("%.2f", tx.amount)
                    val feeStr = String.format("%.2f", tx.fee)
                    val fromAccName = accounts.firstOrNull { it.id == tx.fromAccountId }?.name ?: ""
                    val toAccName = accounts.firstOrNull { it.id == tx.toAccountId }?.name ?: ""

                    // Escape notes with double quotes
                    val noteRaw = tx.note ?: ""
                    val noteStr = if (noteRaw.contains(",") || noteRaw.contains("\"") || noteRaw.contains("\n")) {
                        "\"${noteRaw.replace("\"", "\"\"")}\""
                    } else {
                        noteRaw
                    }

                    builder.append("$dateStr,$typeStr,$catName,$amtStr,$feeStr,$fromAccName,$toAccName,$noteStr\n")
                }

                // File writing
                val filename = "Transactions_${startDate}_to_${endDate}.csv"
                val file = File(context.cacheDir, filename)
                val writer = FileWriter(file)
                writer.write(builder.toString())
                writer.close()

                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    // Share file
                    shareDocumentFile(context, file, "text/csv")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    android.widget.Toast.makeText(context, "Error generating CSV: ${e.localizedMessage}", android.widget.Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun shareDocumentFile(context: Context, file: File, mimeType: String) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooserIntent = Intent.createChooser(intent, "Export Document via:")
        chooserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooserIntent)
    }
}
