package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Category
import com.example.data.model.Transaction
import com.example.ui.theme.*
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun CategoryDonutChart(
    categorySpending: Map<Long, Double>,
    categories: List<Category>,
    currencySymbol: String,
    modifier: Modifier = Modifier
) {
    val totalSpend = categorySpending.values.sum()
    val isDark = MaterialTheme.colorScheme.background == Color(0xFF0B0F19)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (totalSpend <= 0) {
            // High-fidelity empty state for donut
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .background(Color.Transparent, CircleShape)
                    .border(1.5.dp, MaterialTheme.colorScheme.outline, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "No recorded",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                    Text(
                        text = "expenses yet",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                }
            }
            return
        }

        // Preparation
        val sliceRates = mutableListOf<Float>()
        val sliceColors = mutableListOf<Color>()
        val names = mutableListOf<String>()

        var index = 0
        val sortedList = categorySpending.toList().sortedByDescending { it.second }
        for ((catId, amount) in sortedList) {
            val cat = categories.firstOrNull { it.id == catId }
            val name = cat?.name ?: "Unknown"
            val colorStr = cat?.color ?: "#737373"
            val color = try {
                Color(android.graphics.Color.parseColor(colorStr))
            } catch (e: Exception) {
                Color.Gray
            }

            sliceRates.add((amount / totalSpend).toFloat())
            sliceColors.add(color)
            names.add(name)
            index++
        }

        // Animate the sweep entrance on start
        var animationStarted by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) {
            animationStarted = true
        }
        val animationProgress by animateFloatAsState(
            targetValue = if (animationStarted) 1f else 0f,
            animationSpec = tween(durationMillis = 1000),
            label = "donut_sweep"
        )

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // Circle Donut Draw
            Box(
                modifier = Modifier.size(130.dp),
                contentAlignment = Alignment.Center
            ) {
                var startAngle = -90f // Start from top
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeWidth = 20.dp.toPx()
                    for (i in sliceRates.indices) {
                        val sweepAngle = sliceRates[i] * 360f * animationProgress
                        drawArc(
                            color = sliceColors[i],
                            startAngle = startAngle,
                            sweepAngle = sweepAngle,
                            useCenter = false,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )
                        startAngle += sliceRates[i] * 360f // Continue full angle to keep positioning solid
                    }
                }

                // Inner circle content
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "TOTAL SPENT",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "$currencySymbol${String.format("%,.2f", totalSpend)}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Legend Scroll Box
            Column(
                modifier = Modifier
                    .padding(start = 12.dp)
                    .widthIn(max = 160.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                sortedList.take(4).forEachIndexed { i, (catId, amount) ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(sliceColors[i], CircleShape)
                        )
                        Column {
                            Text(
                                text = names[i],
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "$currencySymbol${String.format("%,.0f", amount)} (${String.format("%.0f", sliceRates[i] * 100)}%)",
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MonthlyTrendLineChart(
    transactions: List<Transaction>,
    currencySymbol: String,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    val isDark = MaterialTheme.colorScheme.background == Color(0xFF0B0F19)
    val gridColor = if (isDark) Color(0xFF1F2937) else Color(0xFFE5E7EB)
    val textLabelColor = MaterialTheme.colorScheme.onSurfaceVariant

    // 1. Calculate values for the last 30 days
    val today = LocalDate.now()
    val daysList = (0..29).map { today.minusDays(it.toLong()) }.reversed()
    val formatter = DateTimeFormatter.ofPattern("dd/MM")

    val dailyIncome = mutableListOf<Double>()
    val dailyExpense = mutableListOf<Double>()

    for (day in daysList) {
        val startOfDay = day.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val endOfDay = day.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

        val checkTx = transactions.filter {
            it.deletedAt == null && it.date in startOfDay until endOfDay
        }

        val dayIncome = checkTx.filter { it.type == "INCOME" }.sumOf { it.amount }
        val dayExpense = checkTx.filter { it.type == "EXPENSE" }.sumOf { it.amount }

        dailyIncome.add(dayIncome)
        dailyExpense.add(dayExpense)
    }

    val maxAmount = (dailyIncome.maxOrNull() ?: 1.0)
        .coerceAtLeast(dailyExpense.maxOrNull() ?: 1.0)
        .coerceAtLeast(100.0) // Safe default minimum height

    // Animate the line drawing progress
    var drawAnimationStarted by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        drawAnimationStarted = true
    }
    val drawProgress by animateFloatAsState(
        targetValue = if (drawAnimationStarted) 1f else 0f,
        animationSpec = tween(durationMillis = 1200),
        label = "line_draw"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        // Legend Headers
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Last 30 Days Trend",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(modifier = Modifier.size(6.dp).background(SystemGreen, CircleShape))
                    Text("Income", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(modifier = Modifier.size(6.dp).background(SystemRed, CircleShape))
                    Text("Expense", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(170.dp)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val width = size.width
                val height = size.height

                val paddingLeft = 32.dp.toPx()
                val paddingBottom = 20.dp.toPx()

                val chartWidth = width - paddingLeft
                val chartHeight = height - paddingBottom

                // Draw Grid & Y-Axis Labels
                val gridRows = 4
                for (i in 0..gridRows) {
                    val y = chartHeight * i / gridRows
                    drawLine(
                        color = gridColor,
                        start = androidx.compose.ui.geometry.Offset(paddingLeft, y),
                        end = androidx.compose.ui.geometry.Offset(width, y)
                    )

                    val labelAmount = maxAmount - (maxAmount * i / gridRows)
                    val labelText = "$currencySymbol${String.format("%.0f", labelAmount)}"
                    drawText(
                        textMeasurer = textMeasurer,
                        text = labelText,
                        topLeft = androidx.compose.ui.geometry.Offset(2f, y - 6.dp.toPx()),
                        style = androidx.compose.ui.text.TextStyle(
                            color = textLabelColor,
                            fontSize = 8.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    )
                }

                // Draw X-Axis Date markers (3 of them)
                val testIndex = listOf(0, 14, 29)
                for (idx in testIndex) {
                    val x = paddingLeft + (chartWidth * idx / 29)
                    val labelDate = daysList[idx].format(formatter)
                    drawText(
                        textMeasurer = textMeasurer,
                        text = labelDate,
                        topLeft = androidx.compose.ui.geometry.Offset(x - 12.dp.toPx(), chartHeight + 4.dp.toPx()),
                        style = androidx.compose.ui.text.TextStyle(color = textLabelColor, fontSize = 8.sp)
                    )
                }

                // Path construction
                val incomePath = Path()
                val expensePath = Path()

                val stepX = chartWidth / 29f

                for (idx in 0..29) {
                    val x = paddingLeft + (idx * stepX)
                    
                    val incRatio = (dailyIncome[idx] / maxAmount).toFloat()
                    val expRatio = (dailyExpense[idx] / maxAmount).toFloat()

                    val yInc = chartHeight - (incRatio * chartHeight)
                    val yExp = chartHeight - (expRatio * chartHeight)

                    if (idx == 0) {
                        incomePath.moveTo(x, yInc)
                        expensePath.moveTo(x, yExp)
                    } else {
                        incomePath.lineTo(x, yInc)
                        expensePath.lineTo(x, yExp)
                    }
                }

                // Smooth horizontal drawing sweep
                clipRect(right = paddingLeft + chartWidth * drawProgress) {
                    // Draw curves
                    drawPath(
                        path = incomePath,
                        color = SystemGreen,
                        style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                    )

                    drawPath(
                        path = expensePath,
                        color = SystemRed,
                        style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
            }
        }
    }
}
