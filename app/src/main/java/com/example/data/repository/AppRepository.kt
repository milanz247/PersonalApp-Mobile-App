package com.example.data.repository

import androidx.room.withTransaction
import com.example.data.database.AppDatabase
import com.example.data.model.*
import com.example.data.model.Transaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import java.time.LocalDate
import java.time.ZoneId
import java.time.Instant
import java.util.Date

class AppRepository(private val db: AppDatabase) {

    // DAOs
    val settingsDao = db.appSettingsDao()
    val accountDao = db.accountDao()
    val categoryDao = db.categoryDao()
    val transactionDao = db.transactionDao()
    val debtDao = db.debtDao()
    val budgetDao = db.budgetDao()
    val recurringDao = db.recurringTransactionDao()

    // Exposed Flows
    val settingsFlow: Flow<AppSettings?> = settingsDao.getSettingsFlow()
    val accountsFlow: Flow<List<Account>> = accountDao.getAllAccountsFlow()
    val categoriesFlow: Flow<List<Category>> = categoryDao.getAllCategoriesFlow()
    val transactionsFlow: Flow<List<Transaction>> = transactionDao.getAllTransactionsFlow()
    val debtsFlow: Flow<List<Debt>> = debtDao.getAllDebtsFlow()
    val recurringFlow: Flow<List<RecurringTransaction>> = recurringDao.getAllRecurringFlow()

    // Initialize or seed default wallet and categories if they don't exist
    suspend fun seedDatabaseIfNecessary() {
        db.withTransaction {
            // 1. Seed or fetch AppSettings
            val existingSettings = settingsDao.getSettings()
            if (existingSettings == null) {
                settingsDao.insertOrUpdate(AppSettings())
            }

            // 2. Ensure exactly ONE Wallet account exists
            val wallet = accountDao.getWalletAccount()
            if (wallet == null) {
                accountDao.insertAccount(
                    Account(
                        name = "Cash Wallet",
                        type = "WALLET",
                        balance = 0.0 // Starting balance is 0.0 for initial setup
                    )
                )
            }

            // 3. Seed Categories if empty
            val existingCategories = categoryDao.getAllCategories()
            if (existingCategories.isEmpty()) {
                val systemCategories = listOf(
                    Category(name = "Bank Transfer", type = "TRANSFER", isSystem = true, icon = "repeat", color = "#94A3B8"),
                    Category(name = "Bank/ATM Fees", type = "EXPENSE", isSystem = true, icon = "info", color = "#64748B"),
                    Category(name = "Adjustment", type = "EXPENSE", isSystem = true, icon = "settings", color = "#94A3B8"),
                    Category(name = "Loans & Debts", type = "EXPENSE", isSystem = true, icon = "account_balance", color = "#B91C1C"),
                    Category(name = "Internal Transfer", type = "TRANSFER", isSystem = true, icon = "repeat", color = "#94A3B8")
                )

                val defaultIncomeCategories = listOf(
                    Category(name = "Salary", type = "INCOME", isSystem = false, icon = "payments", color = "#10B981"),
                    Category(name = "Freelance/Other", type = "INCOME", isSystem = false, icon = "laptop_mac", color = "#3B82F6")
                )

                val defaultExpenseCategories = listOf(
                    Category(name = "Boarding Rent", type = "EXPENSE", isSystem = false, icon = "home", color = "#EF4444"),
                    Category(name = "Utility Bills (Elec/Water)", type = "EXPENSE", isSystem = false, icon = "bolt", color = "#F59E0B"),
                    Category(name = "Mobile & Data", type = "EXPENSE", isSystem = false, icon = "phone_android", color = "#6366F1"),
                    Category(name = "Daily Meals (Ude/Dawal/Ra)", type = "EXPENSE", isSystem = false, icon = "restaurant", color = "#F97316"),
                    Category(name = "Tea & Snacks (Ice Cream)", type = "EXPENSE", isSystem = false, icon = "local_coffee_shop", color = "#FBBF24"),
                    Category(name = "Cigarettes", type = "EXPENSE", isSystem = false, icon = "smoke_free", color = "#475569"),
                    Category(name = "Groceries", type = "EXPENSE", isSystem = false, icon = "shopping_cart", color = "#84CC16"),
                    Category(name = "General Shopping", type = "EXPENSE", isSystem = false, icon = "shopping_bag", color = "#EC4899"),
                    Category(name = "Transport (Bus/PickMe)", type = "EXPENSE", isSystem = false, icon = "directions_bus", color = "#06B6D4"),
                    Category(name = "Hotel Stays", type = "EXPENSE", isSystem = false, icon = "hotel", color = "#8B5CF6"),
                    Category(name = "Charity (Pinta dunna)", type = "EXPENSE", isSystem = false, icon = "favorite", color = "#F43F5E"),
                    Category(name = "Treats for Friends", type = "EXPENSE", isSystem = false, icon = "group", color = "#D946EF"),
                    Category(name = "Loan Repayments", type = "EXPENSE", isSystem = false, icon = "account_balance", color = "#B91C1C")
                )

                for (cat in systemCategories + defaultIncomeCategories + defaultExpenseCategories) {
                    categoryDao.insertCategory(cat)
                }
            }
        }
    }

    // Settings
    suspend fun updateSettings(settings: AppSettings) {
        settingsDao.insertOrUpdate(settings)
    }

    suspend fun getSettings(): AppSettings {
        return settingsDao.getSettings() ?: AppSettings()
    }

    // Accounts
    suspend fun addBankAccount(name: String, bankName: String, branchName: String, accountNumber: String, balance: Double): Result<Long> {
        val account = Account(
            name = name,
            type = "BANK",
            bankName = bankName,
            branchName = branchName,
            accountNumber = accountNumber,
            balance = balance
        )
        val id = accountDao.insertAccount(account)
        return Result.success(id)
    }

    suspend fun editBankAccount(id: Long, name: String, bankName: String, branchName: String, accountNumber: String): Result<Unit> {
        val account = accountDao.getAccountById(id) ?: return Result.failure(Exception("Account not found"))
        if (account.type == "WALLET") {
            return Result.failure(Exception("Wallet account cannot be modified."))
        }
        val updated = account.copy(
            name = name,
            bankName = bankName,
            branchName = branchName,
            accountNumber = accountNumber
        )
        accountDao.updateAccount(updated)
        return Result.success(Unit)
    }

    suspend fun deleteBankAccount(id: Long): Result<Unit> {
        val account = accountDao.getAccountById(id) ?: return Result.failure(Exception("Account not found"))
        if (account.type == "WALLET") {
            return Result.failure(Exception("Wallet account cannot be deleted."))
        }
        // Business Rule: Block if account has any transactions
        val hasTransactions = transactionDao.getAllTransactionsAndDeleted().any {
            (it.deletedAt == null) && (it.fromAccountId == id || it.toAccountId == id)
        }
        if (hasTransactions) {
            return Result.failure(Exception("Cannot delete account: it has active transactions. Please delete or move the transactions first."))
        }
        accountDao.deleteAccountById(id)
        return Result.success(Unit)
    }

    // Custom Transfer Business logic
    suspend fun transferFunds(fromAccountId: Long, toAccountId: Long, amount: Double, fee: Double, date: Long, note: String?): Result<Unit> {
        if (fromAccountId == toAccountId) {
            return Result.failure(Exception("Source and destination accounts must be different."))
        }
        return db.withTransaction {
            val source = accountDao.getAccountById(fromAccountId) ?: return@withTransaction Result.failure(Exception("Source account not found"))
            val destination = accountDao.getAccountById(toAccountId) ?: return@withTransaction Result.failure(Exception("Destination account not found"))

            val required = amount + fee
            if (source.balance < required) {
                return@withTransaction Result.failure(Exception("Insufficient balance. Available: ${source.balance}, required: $required"))
            }

            // Find categories for transfer and fees
            val categories = categoryDao.getAllCategories()
            val transferCategory = categories.firstOrNull { it.isSystem && it.name == "Internal Transfer" }
                ?: categories.firstOrNull { it.type == "TRANSFER" }
            val feeCategory = categories.firstOrNull { it.isSystem && it.name == "Bank/ATM Fees" }
                ?: categories.firstOrNull { it.name.contains("fee", true) }

            // Update Balances
            accountDao.updateAccount(source.copy(balance = source.balance - required))
            accountDao.updateAccount(destination.copy(balance = destination.balance + amount))

            // Create TRANSFER transaction
            transactionDao.insertTransaction(
                Transaction(
                    fromAccountId = fromAccountId,
                    toAccountId = toAccountId,
                    categoryId = transferCategory?.id,
                    type = "TRANSFER",
                    amount = amount,
                    fee = 0.0,
                    date = date,
                    note = note ?: "Internal Transfer"
                )
            )

            // Create FEE transaction if required
            if (fee > 0.0) {
                transactionDao.insertTransaction(
                    Transaction(
                        fromAccountId = fromAccountId,
                        categoryId = feeCategory?.id,
                        type = "EXPENSE",
                        amount = fee,
                        date = date,
                        note = "Fund transfer fee for: $note"
                    )
                )
            }

            Result.success(Unit)
        }
    }

    // Transactions
    suspend fun addTransaction(type: String, amount: Double, accountId: Long, categoryId: Long?, date: Long, note: String?): Result<Long> {
        return db.withTransaction {
            val account = accountDao.getAccountById(accountId) ?: return@withTransaction Result.failure(Exception("Account not found"))
            if (type == "EXPENSE") {
                if (account.balance < amount) {
                    return@withTransaction Result.failure(Exception("Insufficient balance. Available: ${account.balance}, required: $amount"))
                }
                accountDao.updateAccount(account.copy(balance = account.balance - amount))
                val tId = transactionDao.insertTransaction(
                    Transaction(
                        fromAccountId = accountId,
                        categoryId = categoryId,
                        type = "EXPENSE",
                        amount = amount,
                        date = date,
                        note = note
                    )
                )
                Result.success(tId)
            } else { // INCOME
                accountDao.updateAccount(account.copy(balance = account.balance + amount))
                val tId = transactionDao.insertTransaction(
                    Transaction(
                        toAccountId = accountId,
                        categoryId = categoryId,
                        type = "INCOME",
                        amount = amount,
                        date = date,
                        note = note
                    )
                )
                Result.success(tId)
            }
        }
    }

    // Soft delete transaction & Reverse impact
    suspend fun deleteTransaction(transactionId: Long): Result<Unit> {
        return db.withTransaction {
            val tx = transactionDao.getTransactionById(transactionId) ?: return@withTransaction Result.failure(Exception("Transaction not found"))
            if (tx.deletedAt != null) {
                return@withTransaction Result.success(Unit) // Already deleted
            }

            // 1. Double balance reversal mechanism
            when (tx.type) {
                "EXPENSE" -> {
                    val fromAcct = tx.fromAccountId?.let { accountDao.getAccountById(it) }
                    if (fromAcct != null) {
                        accountDao.updateAccount(fromAcct.copy(balance = fromAcct.balance + tx.amount))
                    }
                }
                "INCOME" -> {
                    val toAcct = tx.toAccountId?.let { accountDao.getAccountById(it) }
                    if (toAcct != null) {
                        // Avoid negative balance block on reversal, just subtract
                        accountDao.updateAccount(toAcct.copy(balance = toAcct.balance - tx.amount))
                    }
                }
                "TRANSFER" -> {
                    val fromAcct = tx.fromAccountId?.let { accountDao.getAccountById(it) }
                    val toAcct = tx.toAccountId?.let { accountDao.getAccountById(it) }
                    // Revert source (+ amount + fee)
                    if (fromAcct != null) {
                        accountDao.updateAccount(fromAcct.copy(balance = fromAcct.balance + tx.amount + tx.fee))
                    }
                    // Revert destination (- amount)
                    if (toAcct != null) {
                        accountDao.updateAccount(toAcct.copy(balance = toAcct.balance - tx.amount))
                    }
                }
            }

            // 2. Cascade logic for linked debts
            if (tx.debtId != null) {
                val debtVal = debtDao.getDebtById(tx.debtId)
                if (debtVal != null) {
                    val isOriginalCreation = tx.categoryId?.let { categoryDao.getCategoryById(it)?.name == "Loans & Debts" } ?: false
                    if (isOriginalCreation || tx.note?.contains("Initial", true) == true) {
                        // Original debt creation TX was soft-deleted -> soft-delete debt and all payments!
                        debtDao.updateDebt(debtVal.copy(deletedAt = System.currentTimeMillis()))
                        
                        // Find and soft-delete all other transactions matching this debtId, reversing them
                        val allTx = transactionDao.getAllTransactionsAndDeleted().filter { it.debtId == debtVal.id && it.deletedAt == null && it.id != transactionId }
                        for (otherTx in allTx) {
                            transactionDao.updateTransaction(otherTx.copy(deletedAt = System.currentTimeMillis()))
                            // Reverse other components
                            when (otherTx.type) {
                                "EXPENSE" -> {
                                    val fa = otherTx.fromAccountId?.let { accountDao.getAccountById(it) }
                                    if (fa != null) accountDao.updateAccount(fa.copy(balance = fa.balance + otherTx.amount))
                                }
                                "INCOME" -> {
                                    val ta = otherTx.toAccountId?.let { accountDao.getAccountById(it) }
                                    if (ta != null) accountDao.updateAccount(ta.copy(balance = ta.balance - otherTx.amount))
                                }
                            }
                        }
                    } else {
                        // This is a payment transaction -> increase debt's remaining amount back
                        val newRemaining = debtVal.remainingAmount + tx.amount
                        val newStatus = when {
                            newRemaining >= debtVal.amount -> "PENDING"
                            newRemaining <= 0 -> "SETTLED"
                            else -> "PARTIALLY_PAID"
                        }
                        debtDao.updateDebt(debtVal.copy(remainingAmount = newRemaining, status = newStatus))
                    }
                }
            }

            // Mark this transaction as soft-deleted
            transactionDao.updateTransaction(tx.copy(deletedAt = System.currentTimeMillis()))
            Result.success(Unit)
        }
    }

    // Categories
    suspend fun addCategory(name: String, type: String, icon: String, color: String): Result<Long> {
        val category = Category(
            name = name,
            type = type,
            isSystem = false,
            icon = icon,
            color = color
        )
        val id = categoryDao.insertCategory(category)
        return Result.success(id)
    }

    suspend fun deleteCategory(id: Long): Result<Unit> {
        val category = categoryDao.getCategoryById(id) ?: return Result.failure(Exception("Category not found"))
        if (category.isSystem) {
            return Result.failure(Exception("System categories cannot be edited or deleted."))
        }
        categoryDao.deleteCategoryById(id)
        return Result.success(Unit)
    }

    // Debts
    suspend fun addDebt(type: String, personName: String, amount: Double, accountId: Long, fee: Double, dueDate: Long?, description: String?, contactEmail: String?, contactPhone: String?): Result<Long> {
        return db.withTransaction {
            val account = accountDao.getAccountById(accountId) ?: return@withTransaction Result.failure(Exception("Account not found"))
            val categories = categoryDao.getAllCategories()
            val debtCategory = categories.firstOrNull { it.isSystem && it.name == "Loans & Debts" }
                ?: categories.firstOrNull { it.name.contains("debt", true) }

            if (type == "BORROWED") {
                // Borrowed: money comes IN
                accountDao.updateAccount(account.copy(balance = account.balance + amount))

                val curTime = System.currentTimeMillis()
                val debtId = debtDao.insertDebt(
                    Debt(
                        accountId = accountId,
                        personName = personName,
                        type = "BORROWED",
                        amount = amount,
                        remainingAmount = amount,
                        dueDate = dueDate,
                        status = "PENDING",
                        description = description,
                        contactEmail = contactEmail,
                        contactPhone = contactPhone
                    )
                )

                // Insert linked INCOME transaction
                transactionDao.insertTransaction(
                    Transaction(
                        toAccountId = accountId,
                        categoryId = debtCategory?.id,
                        debtId = debtId,
                        type = "INCOME",
                        amount = amount,
                        date = curTime,
                        note = "Initial Borrowed loan from: $personName"
                    )
                )

                Result.success(debtId)
            } else {
                // Lent: money goes OUT
                val required = amount + fee
                if (account.balance < required) {
                    return@withTransaction Result.failure(Exception("Insufficient balance. Available: ${account.balance}, required: $required"))
                }

                // Balance deduction
                accountDao.updateAccount(account.copy(balance = account.balance - required))

                val curTime = System.currentTimeMillis()
                val debtId = debtDao.insertDebt(
                    Debt(
                        accountId = accountId,
                        personName = personName,
                        type = "LENT",
                        amount = amount,
                        remainingAmount = amount,
                        dueDate = dueDate,
                        status = "PENDING",
                        description = description,
                        contactEmail = contactEmail,
                        contactPhone = contactPhone
                    )
                )

                // Insert linked EXPENSE transaction
                transactionDao.insertTransaction(
                    Transaction(
                        fromAccountId = accountId,
                        categoryId = debtCategory?.id,
                        debtId = debtId,
                        type = "EXPENSE",
                        amount = amount,
                        date = curTime,
                        note = "Initial Lent loan to: $personName"
                    )
                )

                // Optional Fee
                if (fee > 0.0) {
                    val feeCategory = categories.firstOrNull { it.isSystem && it.name == "Bank/ATM Fees" }
                    transactionDao.insertTransaction(
                        Transaction(
                            fromAccountId = accountId,
                            categoryId = feeCategory?.id,
                            type = "EXPENSE",
                            amount = fee,
                            date = curTime,
                            note = "Lending transaction fee for loan to: $personName"
                        )
                    )
                }

                Result.success(debtId)
            }
        }
    }

    suspend fun recordDebtPayment(debtId: Long, paymentAmount: Double, accountId: Long, fee: Double): Result<Unit> {
        return db.withTransaction {
            val debt = debtDao.getDebtById(debtId) ?: return@withTransaction Result.failure(Exception("Debt records not found."))
            val account = accountDao.getAccountById(accountId) ?: return@withTransaction Result.failure(Exception("Account not found."))
            val categories = categoryDao.getAllCategories()
            val debtCategory = categories.firstOrNull { it.isSystem && it.name == "Loans & Debts" }

            if (paymentAmount > debt.remainingAmount) {
                return@withTransaction Result.failure(Exception("Payment cannot exceed remaining debt amount ($${debt.remainingAmount})."))
            }

            if (debt.type == "BORROWED") {
                // Pay back Borrowed debt: money goes OUT
                val required = paymentAmount + fee
                if (account.balance < required) {
                    return@withTransaction Result.failure(Exception("Insufficient balance in account. Available: ${account.balance}, requires: $required"))
                }

                // Deduct balance
                accountDao.updateAccount(account.copy(balance = account.balance - required))

                // Insert EXPENSE transaction
                transactionDao.insertTransaction(
                    Transaction(
                        fromAccountId = accountId,
                        categoryId = debtCategory?.id,
                        debtId = debtId,
                        type = "EXPENSE",
                        amount = paymentAmount,
                        date = System.currentTimeMillis(),
                        note = "Repayment of borrowed debt: ${debt.personName}"
                    )
                )

                // Fee expense if any
                if (fee > 0.0) {
                    val feeCategory = categories.firstOrNull { it.isSystem && it.name == "Bank/ATM Fees" }
                    transactionDao.insertTransaction(
                        Transaction(
                            fromAccountId = accountId,
                            categoryId = feeCategory?.id,
                            type = "EXPENSE",
                            amount = fee,
                            date = System.currentTimeMillis(),
                            note = "Fee for repaiyng borrowed debt: ${debt.personName}"
                        )
                    )
                }
            } else {
                // Receive payment on Lent debt: money comes IN
                accountDao.updateAccount(account.copy(balance = account.balance + paymentAmount))

                // Insert INCOME transaction
                transactionDao.insertTransaction(
                    Transaction(
                        toAccountId = accountId,
                        categoryId = debtCategory?.id,
                        debtId = debtId,
                        type = "INCOME",
                        amount = paymentAmount,
                        date = System.currentTimeMillis(),
                        note = "Received payment for lent debt: ${debt.personName}"
                    )
                )

                // Optional fee deducted separately
                if (fee > 0.0) {
                    if (account.balance < fee) {
                        return@withTransaction Result.failure(Exception("Insufficient balance to cover fee."))
                    }
                    accountDao.updateAccount(account.copy(balance = account.balance - fee))
                    val feeCategory = categories.firstOrNull { it.isSystem && it.name == "Bank/ATM Fees" }
                    transactionDao.insertTransaction(
                        Transaction(
                            fromAccountId = accountId,
                            categoryId = feeCategory?.id,
                            type = "EXPENSE",
                            amount = fee,
                            date = System.currentTimeMillis(),
                            note = "Fee for payment received of lent debt: ${debt.personName}"
                        )
                    )
                }
            }

            // Update remaining amount + status
            val newRemaining = (debt.remainingAmount - paymentAmount).coerceAtLeast(0.0)
            val newStatus = when {
                newRemaining <= 0.0 -> "SETTLED"
                newRemaining < debt.amount -> "PARTIALLY_PAID"
                else -> "PENDING"
            }
            debtDao.updateDebt(debt.copy(remainingAmount = newRemaining, status = newStatus))
            Result.success(Unit)
        }
    }

    suspend fun updateDebtReminderMark(debtId: Long, timestamp: Long) {
        val debt = debtDao.getDebtById(debtId) ?: return
        debtDao.updateDebt(debt.copy(lastReminderSentAt = timestamp))
    }

    suspend fun updateDebtInitialSentMark(debtId: Long) {
        val debt = debtDao.getDebtById(debtId) ?: return
        debtDao.updateDebt(debt.copy(initialNotificationSent = true))
    }

    // Budgets
    suspend fun getBudgetsForMonth(monthYear: String): Flow<List<Budget>> = budgetDao.getBudgetsForMonthFlow(monthYear)

    suspend fun updateOrCreateBudget(categoryId: Long, monthYear: String, amount: Double) {
        val existing = budgetDao.getBudgetForCategoryAndMonth(categoryId, monthYear)
        if (existing != null) {
            budgetDao.insertOrUpdateBudget(existing.copy(amount = amount))
        } else {
            budgetDao.insertOrUpdateBudget(Budget(categoryId = categoryId, monthYear = monthYear, amount = amount))
        }
    }

    // Recurring Transactions
    suspend fun addRecurringTransaction(accountId: Long, categoryId: Long, amount: Double, description: String?, type: String, frequency: String, startDate: Long): Result<Long> {
        val recurring = RecurringTransaction(
            accountId = accountId,
            categoryId = categoryId,
            amount = amount,
            description = description,
            type = type,
            frequency = frequency,
            startDate = startDate,
            nextDate = startDate,
            status = "ACTIVE"
        )
        val id = recurringDao.insertRecurring(recurring)
        return Result.success(id)
    }

    suspend fun toggleRecurringStatus(id: Long) {
        val rec = recurringDao.getRecurringById(id) ?: return
        val newStatus = if (rec.status == "ACTIVE") "PAUSED" else "ACTIVE"
        recurringDao.updateRecurring(rec.copy(status = newStatus))
    }

    suspend fun deleteRecurring(id: Long) {
        recurringDao.deleteRecurringById(id)
    }

    // Logic to manually execute a recurring transaction rule right now
    suspend fun executeRecurringNow(id: Long): Result<Unit> {
        return db.withTransaction {
            val rec = recurringDao.getRecurringById(id) ?: return@withTransaction Result.failure(Exception("Recurring plan not found."))
            val result = triggerRecurringExecution(rec)
            if (result.isSuccess) {
                Result.success(Unit)
            } else {
                result
            }
        }
    }

    // Shared execution logic (used by WorkManager background worker & manual run)
    suspend fun triggerRecurringExecution(rec: RecurringTransaction): Result<Unit> {
        val account = accountDao.getAccountById(rec.accountId) ?: return Result.failure(Exception("Account not found for recurring transaction"))
        val curTime = System.currentTimeMillis()

        if (rec.type == "EXPENSE") {
            if (account.balance < rec.amount) {
                return Result.failure(Exception("Insufficient balance in ${account.name} to auto-execute expense. (Available: ${account.balance}, required: ${rec.amount})"))
            }
            accountDao.updateAccount(account.copy(balance = account.balance - rec.amount))
            transactionDao.insertTransaction(
                Transaction(
                    fromAccountId = rec.accountId,
                    categoryId = rec.categoryId,
                    type = "EXPENSE",
                    amount = rec.amount,
                    date = curTime,
                    note = rec.description ?: "Auto-Executed Recurring Expense"
                )
            )
        } else { // INCOME
            accountDao.updateAccount(account.copy(balance = account.balance + rec.amount))
            transactionDao.insertTransaction(
                Transaction(
                    toAccountId = rec.accountId,
                    categoryId = rec.categoryId,
                    type = "INCOME",
                    amount = rec.amount,
                    date = curTime,
                    note = rec.description ?: "Auto-Executed Recurring Income"
                )
            )
        }

        // Calculate next Date based on frequency
        val currentLocalDate = epochToLocalDate(rec.nextDate)
        val nextLocalDate = when (rec.frequency) {
            "DAILY" -> currentLocalDate.plusDays(1)
            "WEEKLY" -> currentLocalDate.plusWeeks(1)
            "MONTHLY" -> currentLocalDate.plusMonths(1)
            "YEARLY" -> currentLocalDate.plusYears(1)
            else -> currentLocalDate.plusMonths(1)
        }

        val updated = rec.copy(
            lastExecutedAt = curTime,
            nextDate = localDateToEpoch(nextLocalDate)
        )
        recurringDao.updateRecurring(updated)
        return Result.success(Unit)
    }

    // Date Utilities
    private fun epochToLocalDate(epochMillis: Long): LocalDate {
        return Instant.ofEpochMilli(epochMillis)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
    }

    private fun localDateToEpoch(localDate: LocalDate): Long {
        return localDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    suspend fun getTransactionListDirect(): List<Transaction> {
        return transactionDao.getAllTransactionsAndDeleted()
    }

    suspend fun getDebtListDirect(): List<Debt> {
        return debtDao.getAllDebts()
    }
}
