package com.example.data

import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object GeminiAdvisor {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun getFinancialInsights(
        incomeTotal: Double,
        expenseTotal: Double,
        billsTotal: Double,
        goalsTotal: Double,
        remainingLimit: Double,
        topSpendingCategory: String,
        currencySymbol: String,
        languageName: String
    ): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext if (languageName == "Arabic") {
                "يا ريت تظبط مفتاح الـ Gemini API من لوحة الأسرار علشان أقدر أحلل لك مصاريفك المرة دي وأساعدك بنصايح تفيدك تظبط ميزانيتك!"
            } else {
                "Please configure the Gemini API key in your Secrets panel to view personalized smart advisor recommendations."
            }
        }

        val prompt = if (languageName == "Arabic") {
            """
                أنت مستشار مالي ذكي وخبير مصري جدع في الإدارة المالية الشخصية للأفراد. ساعد المستخدم بنصائح مالية ممتازة، مشخصة، مشجعة، وقابلة للتطبيق الفوري.
                إليك إحصائيات ميزانية المستخدم الحالية بالعملة ($currencySymbol):
                - إجمالي الدخل: $currencySymbol$incomeTotal
                - إجمالي النفقات: $currencySymbol$expenseTotal
                - الفواتير المجدولة: $currencySymbol$billsTotal
                - إجمالي المدخرات للأهداف: $currencySymbol$goalsTotal
                - المتبقي من الحد الأقصى للميزانية الشهرية: $currencySymbol$remainingLimit
                - الفئة الأعلى إنفاقاً: $topSpendingCategory

                الرجاء تقييم وضعهم المالي وتقديم:
                1. ملخص تشجيعي وإيجابي في سطرين باللهجة المصرية الودية والشعبية الجميلة (مثلاً: "يا بطل، فلوسك متظبطة الشهر ده وعامل شغل عالي..." أو "محتاجين نربط الحزام شوية يا صاحبي...").
                2. بالضبط 3 نصائح عملية ومحددة وقصيرة جداً باللهجة المصرية العامية المبسطة لتقليل الإنفاق على $topSpendingCategory، تحسين الادخار، أو إدارة الفواتير القادمة.
                اكتب النصائح بأسلوب ودود جداً، داعم، ومناسب للعرض على شاشة موبايل صغيرة (أقل من 150 كلمة في المجمل).
                اكتب الرد كاملاً باللهجة المصرية العامية الودية الخفيفة مع تنسيقات واضحة وعناوين بارزة.
            """.trimIndent()
        } else {
            """
                You are an expert, friendly personal financial advisor. Help this user manage their money with empathetic, highly actionable, friendly, and supportive financial insights.
                Here is the user's current month statistics in currency symbol ($currencySymbol):
                - Total Income: $currencySymbol$incomeTotal
                - Total Expenses: $currencySymbol$expenseTotal
                - Scheduled Recurring Bills: $currencySymbol$billsTotal
                - Savings toward Goals: $currencySymbol$goalsTotal
                - Remaining Monthly Budget Limit: $currencySymbol$remainingLimit
                - Highest Spending Category: $topSpendingCategory

                Evaluate their financial health. Provide:
                1. An encouraging, empathetic summary of their situation (1-2 sentences).
                2. Exactly 3 short, specific, highly actionable tips to improve their savings rate, limit spending on $topSpendingCategory, or manage their recurring bills.
                Write it in a clean, friendly, supportive voice. Keep descriptions brief, highly readable on a mobile screen (under 150 words total).
                Write the response in the selected language: $languageName using clear, bulleted formatting and bold key terms.
            """.trimIndent()
        }

        val jsonRequest = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", prompt)
                        })
                    })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("temperature", 0.7)
            })
        }

        val body = jsonRequest.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey")
            .post(body)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errBody = response.body?.string() ?: ""
                    Log.e("GeminiAdvisor", "API error: $errBody")
                    return@withContext if (languageName == "Arabic") {
                        "نصيحة على الماشي: لو قللت مصاريف الـ $topSpendingCategory بنسبة 10% بس الأسبوع ده، هتلاقي معاك قرشين حلوين زيادة على جنب!"
                    } else {
                        "Quick Tip: Reducing your spending in $topSpendingCategory by just 10% this week will immediately give you more financial breathing room!"
                    }
                }
                val respStr = response.body?.string() ?: ""
                val jsonResponse = JSONObject(respStr)
                val candidates = jsonResponse.getJSONArray("candidates")
                val content = candidates.getJSONObject(0).getJSONObject("content")
                val parts = content.getJSONArray("parts")
                parts.getJSONObject(0).getString("text")
            }
        } catch (e: Exception) {
            Log.e("GeminiAdvisor", "Failed to contact Gemini", e)
            if (languageName == "Arabic") {
                "نصيحة ذكية: تابع مصاريفك أول بأول علشان تسيطر على فلوسك وتاخد بالك من الفلوس الصغيرة اللي بتتسرب منك من غير ما تدري."
            } else {
                "Smart Recommendation: Frequently tracking and categorizing small daily expenses is the key to preventing budget leaks."
            }
        }
    }
}
