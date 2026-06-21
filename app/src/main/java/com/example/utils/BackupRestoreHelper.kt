package com.example.utils

import android.content.Context
import androidx.room.withTransaction
import com.example.data.database.AppDatabase
import com.example.data.model.*
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.io.File

object BackupRestoreHelper {

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    data class BackupData(
        val settings: AppSettings?,
        val accounts: List<Account>,
        val categories: List<Category>,
        val transactions: List<Transaction>,
        val debts: List<Debt>,
        val budgets: List<Budget> = emptyList(),
        val recurring: List<RecurringTransaction> = emptyList()
    )

    data class BackupMetadata(
        val timestamp: Long,
        val userName: String?,
        val accountsCount: Int,
        val transactionsCount: Int
    )

    // Find our secure backup file path that will survive uninstallation, associated with the user payload
    fun getBackupFile(context: Context, userName: String = "default"): File {
        val safeContent = userName.replace(Regex("[^a-zA-Z0-9]"), "_")
        val fileName = "personal_finance_backup_$safeContent.json"
        
        val backupDir = File("/sdcard/Download")
        if (!backupDir.exists()) {
            backupDir.mkdirs()
        }
        val target = if (backupDir.exists() && backupDir.canWrite()) {
            File(backupDir, fileName)
        } else {
            val sdcard = File("/sdcard")
            if (sdcard.exists() && sdcard.canWrite()) {
                File(sdcard, fileName)
            } else {
                // Fallback inside data/data-private files dir
                File(context.filesDir, "Backup_$safeContent.json")
            }
        }
        return target
    }

    // Save backup JSON text to stable target + rolling history for maximum safety
    fun saveBackup(context: Context, backupJson: String, userName: String = "default") {
        val safeContent = userName.replace(Regex("[^a-zA-Z0-9]"), "_")
        val mainFile = getBackupFile(context, userName)
        
        try {
            val parentDir = mainFile.parentFile ?: context.filesDir
            val curTime = System.currentTimeMillis()
            val historyFile = File(parentDir, "personal_finance_backup_${safeContent}_$curTime.json")
            
            historyFile.writeText(backupJson)
            
            // Clean up historical list, keeping only the 3 latest files scoped per user
            val historyFiles = parentDir.listFiles { _, name ->
                name.startsWith("personal_finance_backup_${safeContent}_") && name.endsWith(".json")
            }?.sortedByDescending { it.lastModified() }
            
            if (historyFiles != null && historyFiles.size > 3) {
                for (i in 3 until historyFiles.size) {
                    historyFiles[i].delete()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        mainFile.writeText(backupJson)
    }

    // Retrieve high-fidelity metadata (timestamp + records count) from the backup payload
    fun getLatestBackupMetadata(context: Context): BackupMetadata? {
        try {
            val file = getBackupFile(context)
            if (!file.exists()) return null
            val jsonStr = file.readText()
            val adapter = moshi.adapter(BackupData::class.java)
            val backup = adapter.fromJson(jsonStr) ?: return null
            
            return BackupMetadata(
                timestamp = file.lastModified(),
                userName = backup.settings?.userName,
                accountsCount = backup.accounts.size,
                transactionsCount = backup.transactions.size
            )
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    // Export entire local database as a structured JSON string
    suspend fun exportDatabaseToJson(db: AppDatabase): String {
        val settings = db.appSettingsDao().getSettings()
        val accounts = db.accountDao().getAllAccounts()
        val categories = db.categoryDao().getAllCategories()
        val transactions = db.transactionDao().getAllTransactionsAndDeleted()
        val debts = db.debtDao().getAllDebts()
        val recurringPlan = db.recurringTransactionDao().getActiveRecurring()
        
        val allBudgets = try {
            db.budgetDao().getAllBudgets()
        } catch (e: Exception) {
            emptyList()
        }

        val adapter = moshi.adapter(BackupData::class.java)
        
        val backup = BackupData(
            settings = settings,
            accounts = accounts,
            categories = categories,
            transactions = transactions,
            debts = debts,
            budgets = allBudgets,
            recurring = recurringPlan
        )
        
        return adapter.toJson(backup)
    }

    // Restore database from JSON string, overwriting all local records safely
    suspend fun restoreDatabaseFromJson(db: AppDatabase, jsonStr: String): Boolean {
        try {
            val adapter = moshi.adapter(BackupData::class.java)
            val backup = adapter.fromJson(jsonStr) ?: return false

            db.withTransaction {
                // Clear existing records safely
                db.clearAllTables()

                // Restore elements safely inside the transaction block
                val settingsDao = db.appSettingsDao()
                val accountDao = db.accountDao()
                val categoryDao = db.categoryDao()
                val transactionDao = db.transactionDao()
                val debtDao = db.debtDao()
                val budgetDao = db.budgetDao()
                val recurringDao = db.recurringTransactionDao()

                backup.settings?.let { settingsDao.insertOrUpdate(it) }
                
                for (acc in backup.accounts) {
                    accountDao.insertAccount(acc)
                }

                for (cat in backup.categories) {
                    categoryDao.insertCategory(cat)
                }

                for (tx in backup.transactions) {
                    transactionDao.insertTransaction(tx)
                }

                for (debt in backup.debts) {
                    debtDao.insertDebt(debt)
                }

                for (bgt in backup.budgets) {
                    budgetDao.insertOrUpdateBudget(bgt)
                }

                for (rec in backup.recurring) {
                    recurringDao.insertRecurring(rec)
                }
            }

            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }
}
