package com.aiaccounting.app.ui

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import com.aiaccounting.app.data.CategoryStatistic
import com.aiaccounting.app.databinding.ViewExpensePieChartBinding
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.ValueFormatter

class ExpensePieChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val binding: ViewExpensePieChartBinding
    private val pieChart: PieChart

    companion object {
        private const val MIN_DISPLAY_PERCENTAGE = 0.06f
        private const val ANIMATION_DURATION = 1000
        private const val MAX_CATEGORIES = 5
    }
    private val chartColors = listOf(
        Color.rgb(255, 99, 132),    // Vibrant Red/Pink
        Color.rgb(54, 162, 235),    // Sky Blue
        Color.rgb(255, 206, 86),    // Sunny Yellow
        Color.rgb(75, 192, 192),    // Teal
        Color.rgb(153, 102, 255),   // Purple
        Color.rgb(255, 159, 64),    // Orange
        Color.rgb(46, 204, 113),    // Emerald Green
        Color.rgb(231, 76, 60),     // Crimson Red
        Color.rgb(52, 152, 219),    // Dodger Blue
        Color.rgb(241, 196, 15),    // Gold
        Color.rgb(155, 89, 182),    // Amethyst
        Color.rgb(26, 188, 156),    // Turquoise
        Color.rgb(230, 126, 34),    // Carrot Orange
        Color.rgb(149, 165, 166)    // Concrete Gray
    )

    init {
        binding = ViewExpensePieChartBinding.inflate(LayoutInflater.from(context), this, true)
        pieChart = binding.chartPie
        setupChart()
    }
    private fun setupChart() {
        pieChart.apply {
            setUsePercentValues(true)
            description.isEnabled = false
            setExtraOffsets(22f, 0f, 22f, 10f)

            isDrawHoleEnabled = true
            setHoleColor(Color.WHITE)
            holeRadius = 58f
            setTransparentCircleColor(Color.rgb(245, 245, 245))
            setTransparentCircleAlpha(110)
            transparentCircleRadius = 61f

            setDrawCenterText(true)
            setCenterTextSize(16f)
            setCenterTextColor(Color.rgb(60, 60, 60))
            setCenterTextTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD))

            setDrawEntryLabels(true)
            setEntryLabelColor(Color.rgb(60, 60, 60))
            setEntryLabelTextSize(11f)

            isRotationEnabled = false
            isHighlightPerTapEnabled = false
            setTouchEnabled(false)

            legend.apply {
                isEnabled = true
                verticalAlignment = Legend.LegendVerticalAlignment.BOTTOM
                horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
                orientation = Legend.LegendOrientation.HORIZONTAL
                setDrawInside(true)
                xEntrySpace = 10f
                yEntrySpace = 5f
                yOffset = 4f
                textSize = 12f
                textColor = Color.rgb(80, 80, 80)
                form = Legend.LegendForm.CIRCLE
                formSize = 12f
                formToTextSpace = 5f
                isWordWrapEnabled = true
            }

            animateY(ANIMATION_DURATION, Easing.EaseInOutCubic)
        }
    }

    fun updateChart(statistics: List<CategoryStatistic>, periodLabel: String = "") {
        if (statistics.isEmpty()) {
            clearChart("暂无数据")
            return
        }
        val validStatistics = statistics.filter { it.total > 0 }
        if (validStatistics.isEmpty()) {
            clearChart("暂无数据")
            return
        }
        val totalAmount = validStatistics.sumOf { it.total.toDouble() }.toFloat()
        val processedStatistics = processStatisticsForDisplay(validStatistics, totalAmount)
        val entries = processedStatistics.map { stat ->
            PieEntry(stat.displayValue, stat.category).apply {
                data = mapOf(
                    "category" to stat.category,
                    "actualPercentage" to (stat.actualValue / totalAmount * 100)
                )
            }
        }

        val dataSet = PieDataSet(entries, "").apply {
            setDrawIcons(false)
            sliceSpace = 3f
            colors = chartColors
            setValueTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL))
            setDrawValues(true)
            setUsingSliceColorAsValueLineColor(true)
            valueLineColor = Color.rgb(120, 120, 120)
            valueLinePart1OffsetPercentage = 80f
            valueLinePart1Length = 0.35f
            valueLinePart2Length = 0.45f
            valueLineWidth = 1.8f
            yValuePosition = PieDataSet.ValuePosition.OUTSIDE_SLICE
            xValuePosition = PieDataSet.ValuePosition.OUTSIDE_SLICE
        }

        val data = PieData(dataSet).apply {
            setValueFormatter(object : ValueFormatter() {
                override fun getPieLabel(value: Float, pieEntry: PieEntry): String {
                    val dataMap = pieEntry.data as? Map<*, *>
                    val categoryName = dataMap?.get("category") as? String ?: ""
                    val actualPercentage = dataMap?.get("actualPercentage") as? Float ?: value
                    return String.format("%.1f%%", actualPercentage)
                }
            })
            setValueTextSize(11f)
            setValueTextColor(Color.rgb(60, 60, 60))
        }

        pieChart.data = data
        pieChart.centerText = periodLabel
        pieChart.invalidate()
    }

    fun clearChart(message: String = "") {
        pieChart.clear()
        pieChart.centerText = message
        pieChart.invalidate()
    }

    private fun processStatisticsForDisplay(
        statistics: List<CategoryStatistic>,
        totalAmount: Float
    ): List<DisplayStatistic> {
        val sortedStats = statistics.sortedByDescending { it.total }
        val displayStats = if (sortedStats.size > MAX_CATEGORIES) {
            val topCategories = sortedStats.take(MAX_CATEGORIES)
            val otherCategories = sortedStats.drop(MAX_CATEGORIES)
            val otherTotal = otherCategories.sumOf { it.total.toDouble() }.toFloat()
            
            val result = topCategories.map { stat ->
                DisplayStatistic(
                    category = stat.category,
                    actualValue = stat.total.toFloat(),
                    displayValue = stat.total.toFloat()
                )
            }.toMutableList()
            
            if (otherTotal > 0) {
                result.add(
                    DisplayStatistic(
                        category = "其他",
                        actualValue = otherTotal,
                        displayValue = otherTotal
                    )
                )
            }
            result
        } else {
            sortedStats.map { stat ->
                DisplayStatistic(
                    category = stat.category,
                    actualValue = stat.total.toFloat(),
                    displayValue = stat.total.toFloat()
                )
            }
        }
        
        return applyMinimumDisplayPercentage(displayStats, totalAmount)
    }

    private fun applyMinimumDisplayPercentage(
        statistics: List<DisplayStatistic>,
        totalAmount: Float
    ): List<DisplayStatistic> {
        val minDisplayValue = totalAmount * MIN_DISPLAY_PERCENTAGE
        var totalAdjustment = 0f

        val adjustedStats = statistics.map { stat ->
            if (stat.displayValue < minDisplayValue) {
                val adjustment = minDisplayValue - stat.displayValue
                totalAdjustment += adjustment
                stat.copy(displayValue = minDisplayValue)
            } else {
                stat
            }
        }
        
        if (totalAdjustment > 0) {
            val largeCategories = adjustedStats.filter { it.displayValue > minDisplayValue }
            val largeCategoriesTotal = largeCategories.sumOf { it.displayValue.toDouble() }.toFloat()
            if (largeCategoriesTotal > 0) {
                return adjustedStats.map { stat ->
                    if (stat.displayValue > minDisplayValue) {
                        val reductionRatio = totalAdjustment / largeCategoriesTotal
                        val newValue = stat.displayValue * (1 - reductionRatio)
                        stat.copy(displayValue = maxOf(newValue, minDisplayValue))
                    } else {
                        stat
                    }
                }
            }
        }
        return adjustedStats
    }

    private data class DisplayStatistic(
        val category: String,
        val actualValue: Float,
        val displayValue: Float
    )
}
