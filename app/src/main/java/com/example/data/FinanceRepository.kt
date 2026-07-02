package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.util.Calendar
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

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

    // New high performance queries
    fun getExpensesByTimeRange(start: Long, end: Long): Flow<List<Expense>> =
        financeDao.getExpensesByTimeRange(start, end)

    fun getIncomesByTimeRange(start: Long, end: Long): Flow<List<Income>> =
        financeDao.getIncomesByTimeRange(start, end)

    suspend fun getExpensesSumByCategory(cat: String): Double =
        financeDao.getExpensesSumByCategory(cat) ?: 0.0

    suspend fun getExpensesSumByTimeRange(start: Long, end: Long): Double =
        financeDao.getExpensesSumByTimeRange(start, end) ?: 0.0

    /**
     * Highly robust month-by-month billing reset and missing transaction generator.
     * Centralized to prevent logic duplication between UI ViewModel and background Worker.
     */
    suspend fun syncRecurringTransactions(lastMonthStr: String): String {
        val currentIncomes = financeDao.getAllIncomes().first()
        val currentExpenses = financeDao.getAllExpenses().first()
        val currentBills = financeDao.getAllBills().first()

        val currentMillis = System.currentTimeMillis()
        val calNow = Calendar.getInstance()
        calNow.timeInMillis = currentMillis
        val currentMonth = calNow.get(Calendar.MONTH)
        val currentYear = calNow.get(Calendar.YEAR)

        // 1. Reset bills paid status at start of a new month using SharedPreferences
        val currentMonthStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM", Locale.US))

        if (lastMonthStr.isNotEmpty() && lastMonthStr != currentMonthStr) {
            currentBills.forEach { bill ->
                if (bill.isPaid) {
                    financeDao.updateBill(bill.copy(isPaid = false))
                }
            }
        }

        // 2. Process Recurring Incomes
        currentIncomes.filter { it.isRecurring }.forEach { baseIncome ->
            val calBase = Calendar.getInstance()
            calBase.timeInMillis = baseIncome.timestamp
            val baseMonth = calBase.get(Calendar.MONTH)
            val baseYear = calBase.get(Calendar.YEAR)

            val loopCal = Calendar.getInstance()
            loopCal.timeInMillis = baseIncome.timestamp

            while (true) {
                loopCal.add(Calendar.MONTH, 1)
                if (loopCal.get(Calendar.YEAR) > currentYear ||
                    (loopCal.get(Calendar.YEAR) == currentYear && loopCal.get(Calendar.MONTH) > currentMonth)) {
                    break
                }

                val targetM = loopCal.get(Calendar.MONTH)
                val targetY = loopCal.get(Calendar.YEAR)

                val alreadyExists = currentIncomes.any { inc ->
                    val c = Calendar.getInstance()
                    c.timeInMillis = inc.timestamp
                    inc.name == baseIncome.name &&
                    inc.amount == baseIncome.amount &&
                    inc.category == baseIncome.category &&
                    inc.isRecurring &&
                    c.get(Calendar.MONTH) == targetM &&
                    c.get(Calendar.YEAR) == targetY
                }

                if (!alreadyExists) {
                    val clonedCal = Calendar.getInstance()
                    clonedCal.set(targetY, targetM, calBase.get(Calendar.DAY_OF_MONTH).coerceAtMost(loopCal.getActualMaximum(Calendar.DAY_OF_MONTH)), 
                               calBase.get(Calendar.HOUR_OF_DAY), calBase.get(Calendar.MINUTE))
                    financeDao.insertIncome(
                        Income(
                            name = baseIncome.name,
                            amount = baseIncome.amount,
                            category = baseIncome.category,
                            timestamp = clonedCal.timeInMillis,
                            isRecurring = true,
                            notes = baseIncome.notes
                        )
                    )
                }
            }

            // Check specifically for current month
            val currentMonthExists = currentIncomes.any { inc ->
                val c = Calendar.getInstance()
                c.timeInMillis = inc.timestamp
                inc.name == baseIncome.name &&
                inc.amount == baseIncome.amount &&
                inc.category == baseIncome.category &&
                inc.isRecurring &&
                c.get(Calendar.MONTH) == currentMonth &&
                c.get(Calendar.YEAR) == currentYear
            }
            val isAfterBaseMonth = currentYear > baseYear || (currentYear == baseYear && currentMonth > baseMonth)
            if (!currentMonthExists && isAfterBaseMonth) {
                val clonedCal = Calendar.getInstance()
                clonedCal.set(currentYear, currentMonth, calBase.get(Calendar.DAY_OF_MONTH).coerceAtMost(calNow.getActualMaximum(Calendar.DAY_OF_MONTH)), 
                           calBase.get(Calendar.HOUR_OF_DAY), calBase.get(Calendar.MINUTE))
                financeDao.insertIncome(
                    Income(
                        name = baseIncome.name,
                        amount = baseIncome.amount,
                        category = baseIncome.category,
                        timestamp = clonedCal.timeInMillis,
                        isRecurring = true,
                        notes = baseIncome.notes
                    )
                )
            }
        }

        // 3. Process Recurring Expenses
        currentExpenses.filter { it.isRecurring }.forEach { baseExpense ->
            val calBase = Calendar.getInstance()
            calBase.timeInMillis = baseExpense.timestamp
            val baseMonth = calBase.get(Calendar.MONTH)
            val baseYear = calBase.get(Calendar.YEAR)

            val loopCal = Calendar.getInstance()
            loopCal.timeInMillis = baseExpense.timestamp

            while (true) {
                loopCal.add(Calendar.MONTH, 1)
                if (loopCal.get(Calendar.YEAR) > currentYear ||
                    (loopCal.get(Calendar.YEAR) == currentYear && loopCal.get(Calendar.MONTH) > currentMonth)) {
                    break
                }

                val targetM = loopCal.get(Calendar.MONTH)
                val targetY = loopCal.get(Calendar.YEAR)

                val alreadyExists = currentExpenses.any { exp ->
                    val c = Calendar.getInstance()
                    c.timeInMillis = exp.timestamp
                    exp.name == baseExpense.name &&
                    exp.amount == baseExpense.amount &&
                    exp.category == baseExpense.category &&
                    exp.isRecurring &&
                    c.get(Calendar.MONTH) == targetM &&
                    c.get(Calendar.YEAR) == targetY
                }

                if (!alreadyExists) {
                    val clonedCal = Calendar.getInstance()
                    clonedCal.set(targetY, targetM, calBase.get(Calendar.DAY_OF_MONTH).coerceAtMost(loopCal.getActualMaximum(Calendar.DAY_OF_MONTH)), 
                               calBase.get(Calendar.HOUR_OF_DAY), calBase.get(Calendar.MINUTE))
                    financeDao.insertExpense(
                        Expense(
                            name = baseExpense.name,
                            amount = baseExpense.amount,
                            category = baseExpense.category,
                            timestamp = clonedCal.timeInMillis,
                            isRecurring = true,
                            notes = baseExpense.notes
                        )
                    )
                }
            }

            // Check specifically for current month
            val currentMonthExists = currentExpenses.any { exp ->
                val c = Calendar.getInstance()
                c.timeInMillis = exp.timestamp
                exp.name == baseExpense.name &&
                exp.amount == baseExpense.amount &&
                exp.category == baseExpense.category &&
                exp.isRecurring &&
                c.get(Calendar.MONTH) == currentMonth &&
                c.get(Calendar.YEAR) == currentYear
            }
            val isAfterBaseMonth = currentYear > baseYear || (currentYear == baseYear && currentMonth > baseMonth)
            if (!currentMonthExists && isAfterBaseMonth) {
                val clonedCal = Calendar.getInstance()
                clonedCal.set(currentYear, currentMonth, calBase.get(Calendar.DAY_OF_MONTH).coerceAtMost(calNow.getActualMaximum(Calendar.DAY_OF_MONTH)), 
                           calBase.get(Calendar.HOUR_OF_DAY), calBase.get(Calendar.MINUTE))
                financeDao.insertExpense(
                    Expense(
                        name = baseExpense.name,
                        amount = baseExpense.amount,
                        category = baseExpense.category,
                        timestamp = clonedCal.timeInMillis,
                        isRecurring = true,
                        notes = baseExpense.notes
                    )
                )
            }
        }
        return currentMonthStr
    }
}
