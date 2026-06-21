package com.example.data

import kotlinx.coroutines.flow.Flow

class FinanceRepository(private val financeDao: FinanceDao) {
    val allIncomes: Flow<List<Income>> = financeDao.getAllIncomes()
    val allExpenses: Flow<List<Expense>> = financeDao.getAllExpenses()
    val allBills: Flow<List<Bill>> = financeDao.getAllBills()
    val allGoals: Flow<List<Goal>> = financeDao.getAllGoals()
    val settings: Flow<AppSettings?> = financeDao.getSettings()
    val customCategories: Flow<List<CustomCategory>> = financeDao.getAllCustomCategories()

    // Incomes CRUD
    suspend fun insertIncome(income: Income) = financeDao.insertIncome(income)
    suspend fun updateIncome(income: Income) = financeDao.updateIncome(income)
    suspend fun deleteIncome(income: Income) = financeDao.deleteIncome(income)

    // Expenses CRUD
    suspend fun insertExpense(expense: Expense) = financeDao.insertExpense(expense)
    suspend fun updateExpense(expense: Expense) = financeDao.updateExpense(expense)
    suspend fun deleteExpense(expense: Expense) = financeDao.deleteExpense(expense)

    // Bills CRUD
    suspend fun insertBill(bill: Bill) = financeDao.insertBill(bill)
    suspend fun updateBill(bill: Bill) = financeDao.updateBill(bill)
    suspend fun deleteBill(bill: Bill) = financeDao.deleteBill(bill)

    // Goals CRUD
    suspend fun insertGoal(goal: Goal) = financeDao.insertGoal(goal)
    suspend fun updateGoal(goal: Goal) = financeDao.updateGoal(goal)
    suspend fun deleteGoal(goal: Goal) = financeDao.deleteGoal(goal)

    // AppSettings CRUD
    suspend fun insertSettings(settings: AppSettings) = financeDao.insertSettings(settings)

    // Custom Categories CRUD
    suspend fun insertCustomCategory(category: CustomCategory) = financeDao.insertCustomCategory(category)
    suspend fun deleteCustomCategory(category: CustomCategory) = financeDao.deleteCustomCategory(category)

    // Clean Database
    suspend fun clearAllData() {
        financeDao.clearIncomes()
        financeDao.clearExpenses()
        financeDao.clearBills()
        financeDao.clearGoals()
    }
}
