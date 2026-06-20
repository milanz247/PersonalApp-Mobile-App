package com.example.data.dao

import androidx.room.*
import com.example.data.model.*
import com.example.data.model.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface AppSettingsDao {
    @Query("SELECT * FROM app_settings WHERE id = 1 LIMIT 1")
    fun getSettingsFlow(): Flow<AppSettings?>

    @Query("SELECT * FROM app_settings WHERE id = 1 LIMIT 1")
    suspend fun getSettings(): AppSettings?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(settings: AppSettings)
}

@Dao
interface AccountDao {
    @Query("SELECT * FROM accounts ORDER BY type DESC, id ASC")
    fun getAllAccountsFlow(): Flow<List<Account>>

    @Query("SELECT * FROM accounts ORDER BY type DESC, id ASC")
    suspend fun getAllAccounts(): List<Account>

    @Query("SELECT * FROM accounts WHERE id = :id LIMIT 1")
    suspend fun getAccountById(id: Long): Account?

    @Query("SELECT * FROM accounts WHERE type = 'WALLET' LIMIT 1")
    suspend fun getWalletAccount(): Account?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: Account): Long

    @Update
    suspend fun updateAccount(account: Account)

    @Query("DELETE FROM accounts WHERE id = :id")
    suspend fun deleteAccountById(id: Long)
}

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY isSystem DESC, id ASC")
    fun getAllCategoriesFlow(): Flow<List<Category>>

    @Query("SELECT * FROM categories ORDER BY isSystem DESC, id ASC")
    suspend fun getAllCategories(): List<Category>

    @Query("SELECT * FROM categories WHERE id = :id LIMIT 1")
    suspend fun getCategoryById(id: Long): Category?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: Category): Long

    @Update
    suspend fun updateCategory(category: Category)

    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun deleteCategoryById(id: Long)
}

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions WHERE deletedAt IS NULL ORDER BY date DESC, createdAt DESC")
    fun getAllTransactionsFlow(): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE deletedAt IS NULL ORDER BY date DESC, createdAt DESC")
    suspend fun getAllTransactionsAndDeleted(): List<Transaction>

    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    suspend fun getTransactionById(id: Long): Transaction?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: Transaction): Long

    @Update
    suspend fun updateTransaction(transaction: Transaction)
}

@Dao
interface DebtDao {
    @Query("SELECT * FROM debts WHERE deletedAt IS NULL ORDER BY dueDate ASC, id DESC")
    fun getAllDebtsFlow(): Flow<List<Debt>>

    @Query("SELECT * FROM debts WHERE deletedAt IS NULL ORDER BY dueDate ASC, id DESC")
    suspend fun getAllDebts(): List<Debt>

    @Query("SELECT * FROM debts WHERE id = :id LIMIT 1")
    suspend fun getDebtById(id: Long): Debt?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDebt(debt: Debt): Long

    @Update
    suspend fun updateDebt(debt: Debt)
}

@Dao
interface BudgetDao {
    @Query("SELECT * FROM budgets WHERE monthYear = :monthYear")
    fun getBudgetsForMonthFlow(monthYear: String): Flow<List<Budget>>

    @Query("SELECT * FROM budgets WHERE monthYear = :monthYear")
    suspend fun getBudgetsForMonth(monthYear: String): List<Budget>

    @Query("SELECT * FROM budgets WHERE categoryId = :categoryId AND monthYear = :monthYear LIMIT 1")
    suspend fun getBudgetForCategoryAndMonth(categoryId: Long, monthYear: String): Budget?

    @Query("SELECT * FROM budgets")
    suspend fun getAllBudgets(): List<Budget>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateBudget(budget: Budget)

    @Query("DELETE FROM budgets WHERE id = :id")
    suspend fun deleteBudgetById(id: Long)
}

@Dao
interface RecurringTransactionDao {
    @Query("SELECT * FROM recurring_transactions ORDER BY id DESC")
    fun getAllRecurringFlow(): Flow<List<RecurringTransaction>>

    @Query("SELECT * FROM recurring_transactions WHERE status = 'ACTIVE'")
    suspend fun getActiveRecurring(): List<RecurringTransaction>

    @Query("SELECT * FROM recurring_transactions WHERE id = :id LIMIT 1")
    suspend fun getRecurringById(id: Long): RecurringTransaction?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecurring(recurring: RecurringTransaction): Long

    @Update
    suspend fun updateRecurring(recurring: RecurringTransaction)

    @Query("DELETE FROM recurring_transactions WHERE id = :id")
    suspend fun deleteRecurringById(id: Long)
}
