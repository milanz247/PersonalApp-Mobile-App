package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "app_settings")
data class AppSettings(
    @PrimaryKey val id: Long = 1,
    val currencySymbol: String = "Rs.",
    val currencyCode: String = "LKR",
    val timezone: String = "Asia/Colombo",
    val dateFormat: String = "DD/MM/YYYY", // Or "MM/DD/YYYY", "YYYY-MM-DD"
    val avatarPath: String? = null,
    val userName: String = "User",
    val identityNumber: String? = null,
    val appLockEnabled: Boolean = false,
    val appLockPin: String? = null, // Hashed PIN code
    val biometricEnabled: Boolean = false,
    val googleAccountEmail: String? = null,
    val lastBackupAt: Long? = null, // Timestamp in millis
    val autoBackupEnabled: Boolean = false,
    
    // Debt auto-send configuration
    val debtAutoSendInitial: Boolean = false,
    val debtReminderDaysBefore: Int = 2,
    val debtInitialMessageTemplate: String = "Hi {person_name}, recorded a {type} of {amount} via {account_name} on {date}. Due by: {due_date}.",
    val debtReminderMessageTemplate: String = "Hi {person_name}, this is a gentle reminder for the pending {type} of {remaining_amount} which is due on {due_date}.",
    
    // Onboarding
    val hasCompletedOnboarding: Boolean = false,
    
    // Theming
    val themePref: String = "SYSTEM" // "SYSTEM", "LIGHT", "DARK"
)

@Entity(tableName = "accounts")
data class Account(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: String, // "WALLET" | "BANK"
    val bankName: String? = null,
    val branchName: String? = null,
    val accountNumber: String? = null,
    val balance: Double,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: String, // "INCOME" | "EXPENSE" | "TRANSFER"
    val isSystem: Boolean = false,
    val icon: String, // Icon name matching vector icon
    val color: String // Hex color format "#EF4444"
) {
    fun isSystemCategory(): Boolean = isSystem
}

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fromAccountId: Long? = null, // For EXPENSE, TRANSFER
    val toAccountId: Long? = null,   // For INCOME, TRANSFER
    val categoryId: Long? = null,
    val debtId: Long? = null,
    val type: String,                // "EXPENSE" | "INCOME" | "TRANSFER"
    val amount: Double,
    val fee: Double = 0.0,
    val date: Long,                  // Epoch milli
    val note: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null      // Support soft-delete via timestamp (null means active)
)

@Entity(tableName = "debts")
data class Debt(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val accountId: Long,
    val personName: String,
    val type: String,                // "BORROWED" | "LENT"
    val amount: Double,
    val remainingAmount: Double,
    val dueDate: Long? = null,       // Epoch milli
    val status: String = "PENDING",  // "PENDING" | "PARTIALLY_PAID" | "SETTLED"
    val description: String? = null,
    val contactEmail: String? = null,
    val contactPhone: String? = null,
    val initialNotificationSent: Boolean = false,
    val lastReminderSentAt: Long? = null, // Epoch milli
    val deletedAt: Long? = null      // Support soft-delete via timestamp
)

@Entity(
    tableName = "budgets",
    indices = [Index(value = ["categoryId", "monthYear"], unique = true)]
)
data class Budget(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val categoryId: Long,
    val amount: Double,
    val monthYear: String            // Format "yyyy-MM" (e.g. "2026-06")
)

@Entity(tableName = "recurring_transactions")
data class RecurringTransaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val accountId: Long,
    val categoryId: Long,
    val amount: Double,
    val description: String? = null,
    val type: String,                // "INCOME" | "EXPENSE"
    val frequency: String,           // "DAILY" | "WEEKLY" | "MONTHLY" | "YEARLY"
    val startDate: Long,             // Epoch milli
    val lastExecutedAt: Long? = null,// Epoch milli
    val nextDate: Long,              // Epoch milli (date on which next execution will happen)
    val status: String = "ACTIVE"    // "ACTIVE" | "PAUSED"
)
