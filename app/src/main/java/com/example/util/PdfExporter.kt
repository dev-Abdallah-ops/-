package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.example.data.Expense
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class PdfCategoryBreakdown(
    val category: String,
    val amount: Double,
    val percentage: Double
)

object PdfExporter {

    fun exportFinancialReportToPdf(
        context: Context,
        currencySymbol: String,
        totalIncomeStr: String,
        totalExpensesStr: String,
        netBalanceStr: String,
        netBalance: Double,
        categoryBreakdown: List<PdfCategoryBreakdown>,
        topExpenses: List<Expense>,
        isArabic: Boolean
    ) {
        val sdfMonthYear = if (isArabic) SimpleDateFormat("MMMM yyyy", Locale("ar")) else SimpleDateFormat("MMMM yyyy", Locale.ENGLISH)
        val monthYearString = sdfMonthYear.format(Date())

        val sdfFileDate = SimpleDateFormat("yyyy_MM", Locale.US)
        val formattedFileName = "Tawffer_Report_${sdfFileDate.format(Date())}.pdf"

        val pdfDocument = PdfDocument()
        
        // A4 page info (595 x 842)
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        // Define colors matching our premium styling
        val colorPrimaryText = Color.parseColor("#0F172A") // Slate 900
        val colorSecondaryText = Color.parseColor("#475569") // Slate 600
        val colorDivider = Color.parseColor("#E2E8F0") // Slate 200
        val colorCardBg = Color.parseColor("#F8FAFC") // Slate 50
        val colorCardBorder = Color.parseColor("#E2E8F0") // Slate 200
        val colorGreen = Color.parseColor("#10B981") // Emerald 500
        val colorRed = Color.parseColor("#EF4444") // Red 500
        val colorPrimaryTheme = Color.parseColor("#4F46E5") // Indigo 600

        // Create paints
        val textPaint = Paint().apply {
            color = colorPrimaryText
            isAntiAlias = true
        }

        // Draw deep navy accent bar at the top of the page
        val accentBarPaint = Paint().apply {
            color = colorPrimaryTheme
            style = Paint.Style.FILL
        }
        canvas.drawRect(RectF(0f, 0f, 595f, 12f), accentBarPaint)

        // Draw Header Text
        val titleText = if (isArabic) "تطبيق توفير - التقرير المالي الشهري" else "Tawffer - Monthly Financial Report"
        textPaint.apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 20f
            textAlign = if (isArabic) Paint.Align.RIGHT else Paint.Align.LEFT
        }
        val titleX = if (isArabic) 550f else 45f
        canvas.drawText(titleText, titleX, 45f, textPaint)

        // Subtitle (Month/Year)
        textPaint.apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textSize = 12f
            color = colorSecondaryText
        }
        val subtitleText = if (isArabic) "تاريخ التقرير: $monthYearString" else "Report Period: $monthYearString"
        canvas.drawText(subtitleText, titleX, 65f, textPaint)

        // Divider below header
        val linePaint = Paint().apply {
            color = colorDivider
            strokeWidth = 1.5f
            style = Paint.Style.STROKE
        }
        canvas.drawLine(45f, 85f, 550f, 85f, linePaint)

        // --- DRAW SUMMARY CARDS (GRID OF 3) ---
        val cardYTop = 105f
        val cardYBottom = 165f
        val cardWidth = 155f
        val gap = 12.5f

        // Card 1: Income
        val card1Left = 45f
        val card1Right = card1Left + cardWidth
        drawSummaryCard(
            canvas = canvas,
            left = card1Left,
            top = cardYTop,
            right = card1Right,
            bottom = cardYBottom,
            label = if (isArabic) "إجمالي الدخل" else "Total Income",
            value = totalIncomeStr,
            valueColor = colorGreen,
            bgColor = colorCardBg,
            borderColor = colorCardBorder,
            isArabic = isArabic
        )

        // Card 2: Expenses
        val card2Left = card1Right + gap
        val card2Right = card2Left + cardWidth
        drawSummaryCard(
            canvas = canvas,
            left = card2Left,
            top = cardYTop,
            right = card2Right,
            bottom = cardYBottom,
            label = if (isArabic) "إجمالي المصاريف" else "Total Expenses",
            value = totalExpensesStr,
            valueColor = colorRed,
            bgColor = colorCardBg,
            borderColor = colorCardBorder,
            isArabic = isArabic
        )

        // Card 3: Net Balance
        val card3Left = card2Right + gap
        val card3Right = 550f
        val balanceColor = if (netBalance >= 0) colorGreen else colorRed
        drawSummaryCard(
            canvas = canvas,
            left = card3Left,
            top = cardYTop,
            right = card3Right,
            bottom = cardYBottom,
            label = if (isArabic) "صافي الرصيد" else "Net Balance",
            value = netBalanceStr,
            valueColor = balanceColor,
            bgColor = colorCardBg,
            borderColor = colorCardBorder,
            isArabic = isArabic
        )

        // --- CATEGORY BREAKDOWN SECTION ---
        var currentY = 195f

        // Section Title
        textPaint.apply {
            color = colorPrimaryText
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 14f
            textAlign = if (isArabic) Paint.Align.RIGHT else Paint.Align.LEFT
        }
        val section1Title = if (isArabic) "تحليل المصاريف حسب الفئة" else "Expense Breakdown by Category"
        canvas.drawText(section1Title, titleX, currentY, textPaint)

        currentY += 15f
        canvas.drawLine(45f, currentY, 550f, currentY, linePaint)
        
        currentY += 18f

        // Draw Table Header
        textPaint.apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 10f
            color = colorSecondaryText
        }

        if (isArabic) {
            textPaint.textAlign = Paint.Align.RIGHT
            canvas.drawText("الفئة", 550f, currentY, textPaint)
            textPaint.textAlign = Paint.Align.CENTER
            canvas.drawText("النسبة المئوية", 300f, currentY, textPaint)
            textPaint.textAlign = Paint.Align.LEFT
            canvas.drawText("المبلغ", 45f, currentY, textPaint)
        } else {
            textPaint.textAlign = Paint.Align.LEFT
            canvas.drawText("Category", 45f, currentY, textPaint)
            textPaint.textAlign = Paint.Align.CENTER
            canvas.drawText("Percentage", 300f, currentY, textPaint)
            textPaint.textAlign = Paint.Align.RIGHT
            canvas.drawText("Amount", 550f, currentY, textPaint)
        }

        currentY += 8f
        val thinLinePaint = Paint().apply {
            color = colorDivider
            strokeWidth = 0.8f
            style = Paint.Style.STROKE
        }
        canvas.drawLine(45f, currentY, 550f, currentY, thinLinePaint)

        // Draw Category breakdown rows
        textPaint.apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textSize = 10f
            color = colorPrimaryText
        }

        if (categoryBreakdown.isEmpty()) {
            currentY += 20f
            textPaint.textAlign = Paint.Align.CENTER
            canvas.drawText(
                if (isArabic) "لا توجد مصاريف مسجلة هذا الشهر" else "No expenses recorded this month",
                297.5f,
                currentY,
                textPaint
            )
            currentY += 10f
        } else {
            categoryBreakdown.forEach { item ->
                currentY += 20f
                if (isArabic) {
                    textPaint.textAlign = Paint.Align.RIGHT
                    canvas.drawText(item.category, 550f, currentY, textPaint)
                    textPaint.textAlign = Paint.Align.CENTER
                    canvas.drawText(String.format(Locale.getDefault(), "%.1f%%", item.percentage), 300f, currentY, textPaint)
                    textPaint.textAlign = Paint.Align.LEFT
                    val formattedAmt = String.format(Locale.getDefault(), "%,.2f %s", item.amount, currencySymbol)
                    canvas.drawText(formattedAmt, 45f, currentY, textPaint)
                } else {
                    textPaint.textAlign = Paint.Align.LEFT
                    canvas.drawText(item.category, 45f, currentY, textPaint)
                    textPaint.textAlign = Paint.Align.CENTER
                    canvas.drawText(String.format(Locale.US, "%.1f%%", item.percentage), 300f, currentY, textPaint)
                    textPaint.textAlign = Paint.Align.RIGHT
                    val formattedAmt = String.format(Locale.US, "%s%,.2f", currencySymbol, item.amount)
                    canvas.drawText(formattedAmt, 550f, currentY, textPaint)
                }
                
                // Row line divider
                canvas.drawLine(45f, currentY + 6f, 550f, currentY + 6f, thinLinePaint)
            }
            currentY += 6f
        }

        // --- TOP 5 EXPENSES SECTION ---
        currentY += 25f

        // Section Title
        textPaint.apply {
            color = colorPrimaryText
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 14f
            textAlign = if (isArabic) Paint.Align.RIGHT else Paint.Align.LEFT
        }
        val section2Title = if (isArabic) "أعلى 5 مصاريف قيمةً" else "Top 5 Expenses"
        canvas.drawText(section2Title, titleX, currentY, textPaint)

        currentY += 15f
        canvas.drawLine(45f, currentY, 550f, currentY, linePaint)
        
        currentY += 18f

        // Draw Table Header
        textPaint.apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 10f
            color = colorSecondaryText
        }

        if (isArabic) {
            textPaint.textAlign = Paint.Align.RIGHT
            canvas.drawText("الاسم", 550f, currentY, textPaint)
            textPaint.textAlign = Paint.Align.CENTER
            canvas.drawText("الفئة", 300f, currentY, textPaint)
            textPaint.textAlign = Paint.Align.LEFT
            canvas.drawText("المبلغ", 45f, currentY, textPaint)
        } else {
            textPaint.textAlign = Paint.Align.LEFT
            canvas.drawText("Item Name", 45f, currentY, textPaint)
            textPaint.textAlign = Paint.Align.CENTER
            canvas.drawText("Category", 300f, currentY, textPaint)
            textPaint.textAlign = Paint.Align.RIGHT
            canvas.drawText("Amount", 550f, currentY, textPaint)
        }

        currentY += 8f
        canvas.drawLine(45f, currentY, 550f, currentY, thinLinePaint)

        // Draw Rows
        textPaint.apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textSize = 10f
            color = colorPrimaryText
        }

        if (topExpenses.isEmpty()) {
            currentY += 20f
            textPaint.textAlign = Paint.Align.CENTER
            canvas.drawText(
                if (isArabic) "لا توجد مصاريف مسجلة" else "No expenses logged",
                297.5f,
                currentY,
                textPaint
            )
            currentY += 10f
        } else {
            topExpenses.forEach { item ->
                currentY += 20f
                if (isArabic) {
                    textPaint.textAlign = Paint.Align.RIGHT
                    canvas.drawText(item.name, 550f, currentY, textPaint)
                    textPaint.textAlign = Paint.Align.CENTER
                    canvas.drawText(item.category, 300f, currentY, textPaint)
                    textPaint.textAlign = Paint.Align.LEFT
                    val formattedAmt = String.format(Locale.getDefault(), "%,.2f %s", item.amount, currencySymbol)
                    canvas.drawText(formattedAmt, 45f, currentY, textPaint)
                } else {
                    textPaint.textAlign = Paint.Align.LEFT
                    canvas.drawText(item.name, 45f, currentY, textPaint)
                    textPaint.textAlign = Paint.Align.CENTER
                    canvas.drawText(item.category, 300f, currentY, textPaint)
                    textPaint.textAlign = Paint.Align.RIGHT
                    val formattedAmt = String.format(Locale.US, "%s%,.2f", currencySymbol, item.amount)
                    canvas.drawText(formattedAmt, 550f, currentY, textPaint)
                }
                
                // Row line divider
                canvas.drawLine(45f, currentY + 6f, 550f, currentY + 6f, thinLinePaint)
            }
            currentY += 6f
        }

        // --- FOOTER ---
        // Generation date
        val sdfFullDate = if (isArabic) SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("ar")) else SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
        val generationTimeStr = sdfFullDate.format(Date())
        
        textPaint.apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
            textSize = 8f
            color = colorSecondaryText
            textAlign = Paint.Align.CENTER
        }
        val footerText = if (isArabic) {
            "تم التوليد تلقائياً بواسطة تطبيق توفير (Tawffer) في $generationTimeStr - صفحة 1 من 1"
        } else {
            "Automatically generated by Tawffer App on $generationTimeStr - Page 1 of 1"
        }
        canvas.drawText(footerText, 297.5f, 805f, textPaint)

        pdfDocument.finishPage(page)

        // Write to file
        try {
            val pdfFile = File(context.cacheDir, formattedFileName)
            val outputStream = FileOutputStream(pdfFile)
            pdfDocument.writeTo(outputStream)
            outputStream.flush()
            outputStream.close()
            pdfDocument.close()

            // Trigger ShareSheet
            val fileUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                pdfFile
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, fileUri)
                putExtra(Intent.EXTRA_SUBJECT, if (isArabic) "تقرير Tawffer المالي" else "Tawffer Financial Report")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
            val chooserIntent = Intent.createChooser(shareIntent, if (isArabic) "مشاركة التقرير المالي" else "Share Financial Report").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooserIntent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun drawSummaryCard(
        canvas: Canvas,
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        label: String,
        value: String,
        valueColor: Int,
        bgColor: Int,
        borderColor: Int,
        isArabic: Boolean
    ) {
        val bgPaint = Paint().apply {
            color = bgColor
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        val borderPaint = Paint().apply {
            color = borderColor
            strokeWidth = 1f
            style = Paint.Style.STROKE
            isAntiAlias = true
        }

        // Draw rounded rectangle card background
        val rect = RectF(left, top, right, bottom)
        canvas.drawRoundRect(rect, 8f, 8f, bgPaint)
        canvas.drawRoundRect(rect, 8f, 8f, borderPaint)

        // Draw Card Content
        val cardTextPaint = Paint().apply {
            isAntiAlias = true
        }

        // Label
        cardTextPaint.apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textSize = 9f
            color = Color.parseColor("#64748B") // Slate 500
            textAlign = if (isArabic) Paint.Align.RIGHT else Paint.Align.LEFT
        }
        val textX = if (isArabic) right - 10f else left + 10f
        canvas.drawText(label, textX, top + 22f, cardTextPaint)

        // Value
        cardTextPaint.apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 13f
            color = valueColor
        }
        canvas.drawText(value, textX, top + 42f, cardTextPaint)
    }
}
