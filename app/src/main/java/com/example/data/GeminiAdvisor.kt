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
        languageName: String,
        transactionListText: String,
        upcomingBillsText: String,
        goalProgressText: String,
        comparisonText: String
    ): String = withContext(Dispatchers.IO) {
        val apiKey = try {
            val key = BuildConfig.GEMINI_API_KEY
            if (!key.isNullOrEmpty() && key != "MY_GEMINI_API_KEY" && key != "YOUR_ANEWKEY_HERE") {
                key
            } else {
                BuildConfig.Anewkey ?: ""
            }
        } catch (e: Exception) {
            ""
        }
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY" || apiKey == "YOUR_ANEWKEY_HERE") {
            return@withContext if (languageName == "Arabic") {
                "يا ريت تظبط مفتاح الـ Gemini API من لوحة الأسرار (Secrets) علشان أقدر أحلل لك مصاريفك وبياناتك وأساعدك بنصايح ذكية تفيدك تظبط ميزانيتك!"
            } else {
                "Please configure the Gemini API key in your Secrets panel to view personalized smart advisor recommendations with deep analytics."
            }
        }

        val prompt = if (languageName == "Arabic") {
            """
                أنت مستشار مالي ذكي وخبير مالي مصري جدع وبشوش في الإدارة المالية الشخصية (إسمك مستشار توفير Tawffer Advisor). ساعد المستخدم بنصائح مالية ممتازة، مشخصة، مشجعة، وقابلة للتطبيق الفوري.
                إليك إحصائيات ميزانية المستخدم الحالية وتاريخ معاملاته بالعملة ($currencySymbol):
                - إجمالي الدخل: $currencySymbol$incomeTotal
                - إجمالي النفقات: $currencySymbol$expenseTotal
                - الفواتير المجدولة: $currencySymbol$billsTotal
                - إجمالي المدخرات للأهداف: $currencySymbol$goalsTotal
                - المتبقي من الحد الأقصى للميزانية الشهرية: $currencySymbol$remainingLimit
                - الفئة الأعلى إنفاقاً: $topSpendingCategory

                [مقارنة مع الشهر السابق وتحليل الأداء]:
                $comparisonText

                [آخر المعاملات المسجلة]:
                $transactionListText

                [الفواتير القريبة المستحقة]:
                $upcomingBillsText

                [أهداف الادخار ونسبة إنجازها]:
                $goalProgressText

                الرجاء تقييم وضعهم المالي بدقة متناهية وتقديم:
                1. ملخص تشجيعي وإيجابي في سطرين باللهجة المصرية الودية والشعبية الخفيفة (مثلاً: "يا بطل، فلوسك متظبطة الشهر ده وعامل شغل عالي..." أو "محتاجين نربط الحزام شوية يا صاحبي...").
                2. بالضبط 3 نصائح عملية ومحددة وقصيرة جداً باللهجة المصرية العامية المبسطة لتقليل الإنفاق، تحسين الادخار، أو إدارة الفواتير القادمة.
                * اعتمد بالكامل على البيانات الحقيقية المذكورة بالأعلى (مثلاً إذا كان هناك فواتير قريبة، نبههم لها؛ أو إذا اقترب هدف ادخار من الاكتمال، شجعهم عليه؛ أو قارن الصرف مع الشهر اللي فات).
                اكتب النصائح بأسلوب ودود جداً، داعم، ومناسب للعرض على شاشة موبايل صغيرة (أقل من 200 كلمة في المجمل).
                اكتب الرد كاملاً باللهجة المصرية العامية الودية الخفيفة مع تنسيقات واضحة وعناوين بارزة.
            """.trimIndent()
        } else {
            """
                You are Tawffer's smart, advanced personal financial advisor. Focus on highly specific, data-driven analytical suggestions.
                Here is the user's detailed financial context (Currency: $currencySymbol):
                - Total Income: $currencySymbol$incomeTotal
                - Total Expenses: $currencySymbol$expenseTotal
                - Bills Scheduled: $currencySymbol$billsTotal
                - Saved in Goals: $currencySymbol$goalsTotal
                - Remaining Limit: $currencySymbol$remainingLimit
                - Top Category: $topSpendingCategory

                [Monthly Comparison & Spending Velocity]:
                $comparisonText

                [Recent Detailed Transactions]:
                $transactionListText

                [Upcoming Urgent Bills]:
                $upcomingBillsText

                [Savings Goals Progress]:
                $goalProgressText

                Analyze their situation and provide:
                1. A brief empathetic summary analyzing their current budget velocity vs previous periods.
                2. Exactly 3 short, data-informed, highly actionable tips addressing upcoming bills, goals near completion, or overspending limits.
                Format clearly using bold styling. Keep under 200 words. Respond in $languageName.
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
            .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=$apiKey")
            .post(body)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errBody = response.body?.string() ?: ""
                    if (BuildConfig.DEBUG) Log.e("GeminiAdvisor", "API error: $errBody")
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
            if (BuildConfig.DEBUG) Log.e("GeminiAdvisor", "Failed to contact Gemini", e)
            if (languageName == "Arabic") {
                "نصيحة ذكية: تابع مصاريفك أول بأول علشان تسيطر على فلوسك وتاخد بالك من الفلوس الصغيرة اللي بتتسرب منك من غير ما تدري."
            } else {
                "Smart Recommendation: Frequently tracking and categorizing small daily expenses is the key to preventing budget leaks."
            }
        }
    }

    suspend fun askFinancialChat(
        chatHistory: List<Pair<String, Boolean>>,
        incomeTotal: Double,
        expenseTotal: Double,
        billsTotal: Double,
        goalsTotal: Double,
        remainingLimit: Double,
        topSpendingCategory: String,
        currencySymbol: String,
        languageName: String,
        transactionListText: String,
        upcomingBillsText: String,
        goalProgressText: String,
        comparisonText: String
    ): String = withContext(Dispatchers.IO) {
        val apiKey = try {
            val key = BuildConfig.GEMINI_API_KEY
            if (!key.isNullOrEmpty() && key != "MY_GEMINI_API_KEY" && key != "YOUR_ANEWKEY_HERE") {
                key
            } else {
                BuildConfig.Anewkey ?: ""
            }
        } catch (e: Exception) {
            ""
        }
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY" || apiKey == "YOUR_ANEWKEY_HERE") {
            return@withContext if (languageName == "Arabic") {
                "يا ريت تظبط مفتاح الـ Gemini API أولاً من لوحة الأسرار (Secrets) عشان نقدر ندردش يا بطل!"
            } else {
                "Please configure the Gemini API key in your Secrets panel to start chat conversations."
            }
        }

        val systemPrompt = if (languageName == "Arabic") {
            """
                أنت مستشار مالي ذكي وخبير مصري ودود في الإدارة المالية الشخصية (اسمه مستشار توفير Tawffer Advisor). 
                أنت تدردش مع المستخدم لمساعدته وتجيب سؤاله بناءً على بياناته المالية الحقيقية أدناه. لا تتجاهلها أبداً، بل استخدمها لتقديم رد ذكي وواقعي جداً!

                إليك إحصائيات ميزانية المستخدم الحالية وتاريخ معاملاته بالعملة ($currencySymbol):
                - إجمالي الدخل: $currencySymbol$incomeTotal
                - إجمالي النفقات: $currencySymbol$expenseTotal
                - الفواتير المجدولة: $currencySymbol$billsTotal
                - إجمالي المدخرات للأهداف: $currencySymbol$goalsTotal
                - المتبقي من الحد الأقصى للميزانية الشهرية: $currencySymbol$remainingLimit
                - الفئة الأعلى إنفاقاً: $topSpendingCategory

                [مقارنة مع الشهر السابق وتحليل الأداء]:
                $comparisonText

                [آخر المعاملات المسجلة]:
                $transactionListText

                [الفواتير القريبة المستحقة]:
                $upcomingBillsText

                [أهداف الادخار ونسبة إنجازها]:
                $goalProgressText

                الرجاء إجابته باللهجة المصرية العامية الودية، بأسلوب خفيف الظل، مشخص ومفيد جداً، مع عمل حسبة رياضية في رأسك بالبيانات السابقة لإرشاده إذا كان بمقدوره الشراء، أو التحذير من شراء شيء إذا كان سيهدر ميزانيته أو يعطل أهدافه المالية.
                اجعل الردود قصيرة ومناسبة للموبايل (أقل من 150 كلمة).
            """.trimIndent()
        } else {
            """
                You are Tawffer's friendly, supportive personal financial chat advisor. You must answer the user's question by directly analyzing their actual, real-time financial database provided below. Do not give generic advice—give precise, mathematical, and data-backed numbers!

                Here are the user's current month budget statistics ($currencySymbol):
                - Total Income: $currencySymbol$incomeTotal
                - Total Expenses: $currencySymbol$expenseTotal
                - Scheduled Bills: $currencySymbol$billsTotal
                - Savings in Goals: $currencySymbol$goalsTotal
                - Remaining Limit: $currencySymbol$remainingLimit
                - Highest Spending Category: $topSpendingCategory

                [Monthly Comparison & Spending Velocity]:
                $comparisonText

                [Recent Detailed Transactions]:
                $transactionListText

                [Upcoming Urgent Bills]:
                $upcomingBillsText

                [Savings Goals Progress]:
                $goalProgressText

                Please respond accurately, kindly, and concisely in English. Reference their real-time numbers (under 150 words total).
            """.trimIndent()
        }

        val jsonRequest = JSONObject().apply {
            // Set system instruction
            put("systemInstruction", JSONObject().apply {
                put("parts", JSONArray().apply {
                    put(JSONObject().apply {
                        put("text", systemPrompt)
                    })
                })
            })

            // Construct contents list with complete chat history (roles: user, model)
            put("contents", JSONArray().apply {
                val limitedHistory = chatHistory.takeLast(20)
                for (msg in limitedHistory) {
                    val textMsg = msg.first
                    val isUser = msg.second
                    put(JSONObject().apply {
                        put("role", if (isUser) "user" else "model")
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                                    put("text", textMsg)
                            })
                        })
                    })
                }
            })

            put("generationConfig", JSONObject().apply {
                put("temperature", 0.7)
            })
        }

        val body = jsonRequest.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=$apiKey")
            .post(body)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errBody = response.body?.string() ?: ""
                    if (BuildConfig.DEBUG) Log.e("GeminiAdvisor", "Chat API error: $errBody")
                    return@withContext if (languageName == "Arabic") {
                        "حصلت مشكلة بسيطة في الاتصال بالـ AI، جرب تسألني تاني يا صاحبي وصلي على النبي! (الخطأ: $errBody)"
                    } else {
                        "Something went wrong communicating with Gemini. Please try again. (Error: $errBody)"
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
            if (BuildConfig.DEBUG) Log.e("GeminiAdvisor", "Failed to contact Gemini for chat", e)
            if (languageName == "Arabic") {
                "فشل الاتصال بالإنترنت، جرب كمان شوية يا بطل. (التفاصيل: ${e.localizedMessage})"
            } else {
                "Connection failed. Please check your internet and try again. (Details: ${e.localizedMessage})"
            }
        }
    }
}
