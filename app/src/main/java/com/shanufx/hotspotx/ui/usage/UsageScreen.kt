package com.shanufx.hotspotx.ui.usage

import android.content.Intent
import android.graphics.Color as AndroidColor
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.shanufx.hotspotx.ui.components.GlassCard
import com.shanufx.hotspotx.ui.components.StatRow
import com.shanufx.hotspotx.ui.theme.*

@Composable
fun UsageScreen(vm: UsageViewModel = hiltViewModel()) {
    val ui by vm.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Handle CSV export
    LaunchedEffect(ui.exportUri) {
        ui.exportUri?.let { uri ->
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Export Usage CSV"))
            vm.clearExportUri()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ── Header ────────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Usage & Charts", style = MaterialTheme.typography.titleLarge)
            IconButton(onClick = vm::exportCsv) {
                Icon(Icons.Rounded.FileDownload, contentDescription = "Export CSV", tint = CyanPrimary)
            }
        }

        // ── Summary Cards ─────────────────────────────────────────
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SummaryCard("Today", ui.todayTotalText, Modifier.weight(1f))
            SummaryCard("This Week", ui.weekTotalText, Modifier.weight(1f))
            SummaryCard("This Month", ui.monthTotalText, Modifier.weight(1f))
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SummaryCard("Peak Speed", ui.peakSpeedText, Modifier.weight(1f))
            SummaryCard("Avg Session", ui.avgSessionText, Modifier.weight(1f))
            SummaryCard("Sessions", ui.totalSessionsText, Modifier.weight(1f))
        }

        // ── Real-time Line Chart ──────────────────────────────────
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Text("Real-time Throughput (5 min)", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            if (ui.realtimeUpload.size >= 2) {
                RealtimeLineChart(
                    uploadPoints = ui.realtimeUpload,
                    downloadPoints = ui.realtimeDownload,
                    modifier = Modifier.fillMaxWidth().height(180.dp)
                )
            } else {
                Box(Modifier.fillMaxWidth().height(180.dp), contentAlignment = Alignment.Center) {
                    Text("Waiting for data…", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
            }
        }

        // ── Hourly Bar Chart ──────────────────────────────────────
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Text("Hourly Usage — Today", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            if (ui.hourlyUpload.isNotEmpty()) {
                HourlyBarChart(
                    labels = ui.hourlyLabels,
                    upload = ui.hourlyUpload,
                    download = ui.hourlyDownload,
                    modifier = Modifier.fillMaxWidth().height(180.dp)
                )
            } else {
                NoDataPlaceholder()
            }
        }

        // ── Daily Bar Chart ───────────────────────────────────────
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Text("Daily Usage — Last 30 Days", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            if (ui.dailyUpload.isNotEmpty()) {
                DailyBarChart(
                    labels = ui.dailyLabels,
                    upload = ui.dailyUpload,
                    download = ui.dailyDownload,
                    modifier = Modifier.fillMaxWidth().height(200.dp)
                )
            } else {
                NoDataPlaceholder()
            }
        }

        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun SummaryCard(label: String, value: String, modifier: Modifier) {
    GlassCard(modifier = modifier) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
        Spacer(Modifier.height(4.dp))
        Text(value, style = MaterialTheme.typography.titleMedium, color = CyanPrimary)
    }
}

@Composable
private fun NoDataPlaceholder() {
    Box(Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
        Text("No data yet", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
    }
}

@Composable
private fun RealtimeLineChart(uploadPoints: List<Float>, downloadPoints: List<Float>, modifier: Modifier) {
    AndroidView(
        factory = { ctx ->
            LineChart(ctx).apply {
                description.isEnabled = false
                legend.isEnabled = false
                setTouchEnabled(false)
                setDrawGridBackground(false)
                setBackgroundColor(AndroidColor.TRANSPARENT)
                xAxis.apply {
                    position = XAxis.XAxisPosition.BOTTOM
                    textColor = AndroidColor.parseColor("#8899AA")
                    gridColor = AndroidColor.parseColor("#1AFFFFFF")
                    axisLineColor = AndroidColor.parseColor("#33FFFFFF")
                    setDrawLabels(false)
                }
                axisLeft.apply {
                    textColor = AndroidColor.parseColor("#8899AA")
                    gridColor = AndroidColor.parseColor("#1AFFFFFF")
                    axisLineColor = AndroidColor.TRANSPARENT
                }
                axisRight.isEnabled = false
            }
        },
        update = { chart ->
            val upEntries = uploadPoints.mapIndexed { i, v -> Entry(i.toFloat(), v) }
            val dnEntries = downloadPoints.mapIndexed { i, v -> Entry(i.toFloat(), v) }

            val upSet = LineDataSet(upEntries, "Upload").apply {
                color = AndroidColor.parseColor("#00E5FF")
                setDrawCircles(false)
                lineWidth = 2f
                fillColor = AndroidColor.parseColor("#3000E5FF")
                setDrawFilled(true)
                mode = LineDataSet.Mode.CUBIC_BEZIER
            }
            val dnSet = LineDataSet(dnEntries, "Download").apply {
                color = AndroidColor.parseColor("#CE93D8")
                setDrawCircles(false)
                lineWidth = 2f
                fillColor = AndroidColor.parseColor("#30CE93D8")
                setDrawFilled(true)
                mode = LineDataSet.Mode.CUBIC_BEZIER
            }
            chart.data = LineData(upSet, dnSet)
            chart.invalidate()
        },
        modifier = modifier
    )
}

@Composable
private fun HourlyBarChart(labels: List<String>, upload: List<Float>, download: List<Float>, modifier: Modifier) {
    AndroidView(
        factory = { ctx ->
            BarChart(ctx).apply {
                description.isEnabled = false
                legend.isEnabled = false
                setTouchEnabled(true)
                setDrawGridBackground(false)
                setBackgroundColor(AndroidColor.TRANSPARENT)
                xAxis.apply {
                    position = XAxis.XAxisPosition.BOTTOM
                    textColor = AndroidColor.parseColor("#8899AA")
                    gridColor = AndroidColor.parseColor("#1AFFFFFF")
                    granularity = 1f
                }
                axisLeft.apply {
                    textColor = AndroidColor.parseColor("#8899AA")
                    gridColor = AndroidColor.parseColor("#1AFFFFFF")
                }
                axisRight.isEnabled = false
            }
        },
        update = { chart ->
            val groupSpace = 0.1f
            val barSpace  = 0.02f
            val barWidth  = (1f - groupSpace - 2 * barSpace) / 2f

            val upEntries = upload.mapIndexed { i, v -> BarEntry(i.toFloat(), v) }
            val dnEntries = download.mapIndexed { i, v -> BarEntry(i.toFloat(), v) }

            val upSet = BarDataSet(upEntries, "Upload").apply {
                color = AndroidColor.parseColor("#00E5FF")
            }
            val dnSet = BarDataSet(dnEntries, "Download").apply {
                color = AndroidColor.parseColor("#CE93D8")
            }
            val data = BarData(upSet, dnSet).apply {
                this.barWidth = barWidth
            }
            chart.data = data
            chart.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
            chart.groupBars(0f, groupSpace, barSpace)
            chart.invalidate()
        },
        modifier = modifier
    )
}

@Composable
private fun DailyBarChart(labels: List<String>, upload: List<Float>, download: List<Float>, modifier: Modifier) {
    AndroidView(
        factory = { ctx ->
            BarChart(ctx).apply {
                description.isEnabled = false
                legend.isEnabled = false
                setTouchEnabled(true)
                setDrawGridBackground(false)
                setBackgroundColor(AndroidColor.TRANSPARENT)
                xAxis.apply {
                    position = XAxis.XAxisPosition.BOTTOM
                    textColor = AndroidColor.parseColor("#8899AA")
                    gridColor = AndroidColor.parseColor("#1AFFFFFF")
                    granularity = 1f
                    labelRotationAngle = -45f
                }
                axisLeft.apply {
                    textColor = AndroidColor.parseColor("#8899AA")
                    gridColor = AndroidColor.parseColor("#1AFFFFFF")
                }
                axisRight.isEnabled = false
            }
        },
        update = { chart ->
            val stackedEntries = upload.indices.map { i ->
                BarEntry(i.toFloat(), floatArrayOf(upload[i], download[i]))
            }
            val set = BarDataSet(stackedEntries, "Daily").apply {
                colors = listOf(
                    AndroidColor.parseColor("#00E5FF"),
                    AndroidColor.parseColor("#CE93D8")
                )
                stackLabels = arrayOf("Upload", "Download")
            }
            chart.data = BarData(set)
            chart.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
            chart.invalidate()
        },
        modifier = modifier
    )
}
