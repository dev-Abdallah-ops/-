package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface FinanceDao {
    // Incomes
    @Query("SELECT * FROM incomes ORDER BY timestamp DESC")
    fun getAllIncomes(): Flow<List<Income>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIncome(income: Income)

    @Update
    suspend fun updateIncome(income: Income)

    @Delete
    suspend fun deleteIncome(income: Income)


    // Expenses
    @Query("SELECT * FROM expenses ORDER BY timestamp DESC")
    fun getAllExpenses(): Flow<List<Expense>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: Expense)

    @Update
    suspend fun updateExpense(expense: Expense)

    @Delete
    suspend fun deleteExpense(expense: Expense)


    // Bills
    @Query("SELECT * FROM bills")
    fun getAllBills(): Flow<List<Bill>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBill(bill: Bill)

    @Update
    suspend fun updateBill(bill: Bill)

    @Delete
    suspend fun deleteBill(bill: Bill)


    // Goals
    @Query("SELECT * FROM goals")
    fun getAllGoals(): Flow<List<Goal>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoal(goal: Goal)

    @Update
    suspend fun updateGoal(goal: Goal)

    @Delete
    suspend fun deleteGoal(goal: Goal)


    // AppSettings (Always single row with ID = 1)
    @Query("SELECT * FROM app_settings WHERE id = 1 LIMIT 1")
    fun getSettings(): Flow<AppSettings?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSettings(settings: AppSettings)


    // Custom Categories
    @Query("SELECT * FROM custom_categories")
    fun getAllCustomCategories(): Flow<List<CustomCategory>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomCategory(category: CustomCategory)

    @Delete
    suspend fun deleteCustomCategory(category: CustomCategory)


    // Async Clear Functions
    @Query("DELETE FROM incomes")
    suspend fun clearIncomes()

    @Query("DELETE FROM expenses")
    suspend fun clearExpenses()

    @Query("DELETE FROM bills")
    suspend fun clearBills()

    @Query("DELETE FROM goals")
    suspend fun clearGoals()

    // Highly Optimized Range Queries and Room performance optimization
    @Query("SELECT * FROM expenses WHERE timestamp BETWEEN :start AND :end ORDER BY timestamp DESC")
    fun getExpensesByTimeRange(start: Long, end: Long): Flow<List<Expense>>

    @Query("SELECT * FROM incomes WHERE timestamp BETWEEN :start AND :end ORDER BY timestamp DESC")
    fun getIncomesByTimeRange(start: Long, end: Long): Flow<List<Income>>

    @Query("SELECT SUM(amount) FROM expenses WHERE category = :cat")
    suspend fun getExpensesSumByCategory(cat: String): Double?

    @Query("SELECT SUM(amount) FROM expenses WHERE timestamp BETWEEN :start AND :end")
    suspend fun getExpensesSumByTimeRange(start: Long, end: Long): Double?
}
