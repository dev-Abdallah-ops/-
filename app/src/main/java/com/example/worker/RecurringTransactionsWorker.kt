package com.example.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.data.AppDatabase
import com.example.data.Income
import com.example.data.Expense
import kotlinx.coroutines.flow.first
import java.util.Calendar

class RecurringTransactionsWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        try {
            val database = AppDatabase.getDatabase(applicationContext)
            val dao = database.financeDao()

            // Fetch current records
            val incomes = dao.getAllIncomes().first()
            val expenses = dao.getAllExpenses().first()

            val calendarNow = Calendar.getInstance()
            val currentMonth = calendarNow.get(Calendar.MONTH)
            val currentYear = calendarNow.get(Calendar.YEAR)

            // Process Recurring Incomes
            val recurringIncomes = incomes.filter { it.isRecurring }
            for (income in recurringIncomes) {
                // If there is no transaction of this EXACT details recorded in this month, auto-create it
                val alreadyExistsThisMonth = incomes.any {
                    it.isRecurring &&
                    it.name == income.name &&
                    it.category == income.category &&
                    it.amount == income.amount &&
                    run {
                        val c = Calendar.getInstance().apply { timeInMillis = it.timestamp }
                        c.get(Calendar.MONTH) == currentMonth && c.get(Calendar.YEAR) == currentYear
                    }
                }
                if (!alreadyExistsThisMonth) {
                    dao.insertIncome(
                        Income(
                            name = income.name,
                            amount = income.amount,
                            category = income.category,
                            timestamp = calendarNow.timeInMillis,
                            isRecurring = true,
                            notes = "Auto-generated monthly recurring income"
                        )
                    )
                }
            }

            // Process Recurring Expenses
            val recurringExpenses = expenses.filter { it.isRecurring }
            for (expense in recurringExpenses) {
                // If there is no transaction of this EXACT details recorded in this month, auto-create it
                val alreadyExistsThisMonth = expenses.any {
                    it.isRecurring &&
                    it.name == expense.name &&
                    it.category == expense.category &&
                    it.amount == expense.amount &&
                    run {
                        val c = Calendar.getInstance().apply { timeInMillis = it.timestamp }
                        c.get(Calendar.MONTH) == currentMonth && c.get(Calendar.YEAR) == currentYear
                    }
                }
                if (!alreadyExistsThisMonth) {
                    dao.insertExpense(
                        Expense(
                            name = expense.name,
                            amount = expense.amount,
                            category = expense.category,
                            timestamp = calendarNow.timeInMillis,
                            isRecurring = true,
                            notes = "Auto-generated monthly recurring expense"
                        )
                    )
                }
            }

            return Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            return Result.retry()
        }
    }
}
