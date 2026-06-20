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

    fun generateAndSharePdf(
        context: Context,
        userName: String,
        monthYear: String, // e.g. "2026-06"
        currencySymbol: String,
        accounts: List<Account>,
        categories: List<Category>,
        transactions: List<Transaction>
    ) {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 Size (595x842)
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        val paintText = Paint().apply {
            color = Color.BLACK
            isAntiAlias = true
        }

        val paintHeader = Paint().apply {
            color = Color.DKGRAY
            isAntiAlias = true
            textSize = 20f
            isFakeBoldText = true
        }

        val paintSection = Paint().apply {
            color = Color.BLACK
            isAntiAlias = true
            textSize = 14f
            isFakeBoldText = true
        }

        val paintAccent = Paint().apply {
            color = Color.parseColor("#10B981") // System Green
            isAntiAlias = true
            textSize = 15f
            isFakeBoldText = true
        }

        val paintLine = Paint().apply {
            color = Color.LTGRAY
            strokeWidth = 1f
        }

        // Draw header
        canvas.drawText("PERSONAL FINANCE REPORT", 40f, 60f, paintHeader)
        paintText.textSize = 10f
        canvas.drawText("Generated on: ${LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))}", 40f, 80f, paintText)
        canvas.drawText("Account holder: $userName", 40f, 95f, paintText)
        canvas.drawText("Billing Period: $monthYear", 40f, 110f, paintText)

        canvas.drawLine(40f, 125f, 555f, 125f, paintLine)

        // Calculate statistics
        var totalIncome = 0.0
        var totalExpense = 0.0
        for (tx in transactions) {
            if (tx.deletedAt == null) {
                when (tx.type) {
                    "INCOME" -> totalIncome += tx.amount
                    "EXPENSE" -> totalExpense += tx.amount
                }
            }
        }
        val savings = totalIncome - totalExpense
        val savingsRatePercent = if (totalIncome > 0) (savings / totalIncome) * 100 else 0.0

        // Draw statistics block
        canvas.drawText("SUMMARY STATISTICS", 40f, 155f, paintSection)
        paintText.textSize = 12f
        canvas.drawText("Total Income:", 50f, 185f, paintText)
        canvas.drawText("$currencySymbol${String.format("%.2f", totalIncome)}", 200f, 185f, paintText)

        canvas.drawText("Total Expense:", 50f, 210f, paintText)
        canvas.drawText("$currencySymbol${String.format("%.2f", totalExpense)}", 200f, 210f, paintText)

        canvas.drawText("Net Savings:", 50f, 235f, paintText)
        canvas.drawText("$currencySymbol${String.format("%.2f", savings)}", 200f, 235f, paintAccent)

        canvas.drawText("Savings Rate:", 50f, 260f, paintText)
        canvas.drawText("${String.format("%.1f", savingsRatePercent)}%", 200f, 260f, paintText)

        canvas.drawLine(40f, 280f, 555f, 280f, paintLine)

        // Draw Account balances
        canvas.drawText("ACCOUNT BALANCES", 40f, 310f, paintSection)
        var currentY = 340f
        paintText.textSize = 11f
        for (account in accounts) {
            canvas.drawText(account.name, 50f, currentY, paintText)
            canvas.drawText("${account.type} - $currencySymbol${String.format("%.2f", account.balance)}", 200f, currentY, paintText)
            currentY += 20f
            if (currentY > 440f) break
        }

        canvas.drawLine(40f, 460f, 555f, 460f, paintLine)

        // Category breakdown
        canvas.drawText("CATEGORY BREAKDOWN (EXPENSES)", 40f, 490f, paintSection)
        val catSpentMap = mutableMapOf<Long, Double>()
        for (tx in transactions) {
            if (tx.deletedAt == null && tx.type == "EXPENSE" && tx.categoryId != null) {
                catSpentMap[tx.categoryId] = (catSpentMap[tx.categoryId] ?: 0.0) + tx.amount
            }
        }

        currentY = 520f
        paintText.textSize = 11f
        if (catSpentMap.isEmpty()) {
            canvas.drawText("No expenses recorded for this month.", 50f, currentY, paintText)
        } else {
            for ((catId, amount) in catSpentMap.toList().sortedByDescending { it.second }.take(8)) {
                val catName = categories.firstOrNull { it.id == catId }?.name ?: "Unknown Category"
                canvas.drawText(catName, 50f, currentY, paintText)
                canvas.drawText("$currencySymbol${String.format("%.2f", amount)}", 200f, currentY, paintText)
                currentY += 20f
            }
        }

        // Draw footer
        canvas.drawLine(40f, 790f, 555f, 790f, paintLine)
        paintText.textSize = 8f
        paintText.color = Color.GRAY
        canvas.drawText("This PDF was automatically generated by the Personal Finance App.", 40f, 810f, paintText)

        pdfDocument.finishPage(page)

        // Write PDF file
        val file = File(context.cacheDir, "Report_${monthYear}.pdf")
        val outputStream = FileOutputStream(file)
        pdfDocument.writeTo(outputStream)
        outputStream.close()
        pdfDocument.close()

        // Share File
        shareDocumentFile(context, file, "application/pdf")
    }

    fun generateAndShareCsv(
        context: Context,
        startDate: LocalDate,
        endDate: LocalDate,
        transactions: List<Transaction>,
        accounts: List<Account>,
        categories: List<Category>
    ) {
        val csvHeader = "Date,Type,Category,Amount,Fee,From Account,To Account,Note\n"
        val builder = java.lang.StringBuilder()
        builder.append(csvHeader)

        val startMillis = startDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val endMillis = endDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

        // Filter and sort newest first
        val targetTransactions = transactions.filter {
            it.deletedAt == null && it.date in startMillis until endMillis
        }.sortedByDescending { it.date }

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

        // Share file
        shareDocumentFile(context, file, "text/csv")
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
