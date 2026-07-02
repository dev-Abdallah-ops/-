package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.GeminiAdvisor
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Tawffer", appName)
  }

  @Test
  fun `test gemini advisor handles network exception without crashing`() = runBlocking {
    val result = GeminiAdvisor.askFinancialChat(
      chatHistory = listOf("مرحبا" to true),
      incomeTotal = 1000.0,
      expenseTotal = 500.0,
      billsTotal = 100.0,
      goalsTotal = 200.0,
      remainingLimit = 500.0,
      topSpendingCategory = "طعام",
      currencySymbol = "EGP",
      languageName = "Arabic",
      transactionListText = "",
      upcomingBillsText = "",
      goalProgressText = "",
      comparisonText = ""
    )
    assertNotNull(result)
  }
}
