package com.shanufx.hotspotx.util

import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.pow

object FormatUtils {

    fun formatBytes(bytes: Long): String {
        if (bytes < 0) return "0 B"
        if (bytes < 1024) return "$bytes B"
        val exp = (log10(bytes.toDouble()) / log10(1024.0)).toInt().coerceIn(1, 4)
        val unit = "KMGT"[exp - 1]
        val value = bytes / 1024.0.pow(exp.toDouble())
        return "%.1f %siB".format(value, unit)
    }

    fun formatSpeed(bytesPerSecond: Long): String {
        return "${formatBytes(bytesPerSecond)}/s"
    }

    fun formatDuration(ms: Long): String {
        if (ms <= 0) return "0s"
        val seconds = ms / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        return when {
            hours > 0 -> "${hours}h ${minutes % 60}m"
            minutes > 0 -> "${minutes}m ${seconds % 60}s"
            else -> "${seconds}s"
        }
    }

    fun formatTime(epochMs: Long): String =
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(epochMs))

    fun formatDateTime(epochMs: Long): String =
        SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(epochMs))

    fun formatDate(epochMs: Long): String =
        SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date(epochMs))

    fun minutesToTimeString(minutes: Int): String {
        val h = minutes / 60
        val m = minutes % 60
        return "%02d:%02d".format(h, m)
    }

    fun timeStringToMinutes(time: String): Int {
        val parts = time.split(":")
        return if (parts.size == 2) {
            (parts[0].toIntOrNull() ?: 0) * 60 + (parts[1].toIntOrNull() ?: 0)
        } else 0
    }

    fun dayMaskToShortLabels(mask: Int): String {
        val days = listOf("Su", "Mo", "Tu", "We", "Th", "Fr", "Sa")
        return days.filterIndexed { i, _ -> mask and (1 shl i) != 0 }.joinToString(" ")
    }

    fun passwordStrength(password: String): Float {
        if (password.length < 8) return 0f
        var score = 0f
        if (password.length >= 12) score += 0.3f
        if (password.any { it.isUpperCase() }) score += 0.2f
        if (password.any { it.isLowerCase() }) score += 0.1f
        if (password.any { it.isDigit() }) score += 0.2f
        if (password.any { !it.isLetterOrDigit() }) score += 0.2f
        return score.coerceIn(0f, 1f)
    }

    fun passwordStrengthLabel(score: Float): String = when {
        score < 0.3f -> "Weak"
        score < 0.6f -> "Fair"
        score < 0.8f -> "Strong"
        else -> "Very Strong"
    }

    fun startOfDayMs(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    fun startOfMonthMs(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    fun startOfWeekMs(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
}
