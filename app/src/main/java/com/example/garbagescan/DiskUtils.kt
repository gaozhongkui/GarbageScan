package com.example.garbagescan

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Environment
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

object DiskUtils {

    fun formatData(str: String?, j: Long): String? {
        return if (j == 0L) {
            ""
        } else try {
            SimpleDateFormat(str).format(Date(j))
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    fun parseTime(seconds: Long, showSeconds: Boolean): String {
        val day = TimeUnit.SECONDS.toDays(seconds).toInt()
        val hours = TimeUnit.SECONDS.toHours(seconds) - TimeUnit.DAYS.toHours(
            TimeUnit.SECONDS.toDays(seconds)
        )
        val minute = TimeUnit.SECONDS.toMinutes(seconds) - TimeUnit.HOURS.toMinutes(
            TimeUnit.SECONDS.toHours(seconds)
        )
        val second = TimeUnit.SECONDS.toSeconds(seconds) - TimeUnit.MINUTES.toSeconds(
            TimeUnit.SECONDS.toMinutes(seconds)
        )
        val builder = StringBuilder()
        if (day > 0) {
            builder.append(day).append("d")
        }
        if (hours > 0) {
            builder.append(hours).append("h")
        }
        if (minute > 0) {
            builder.append(minute).append("min")
        }
        if (second > 0 && showSeconds) {
            builder.append(second).append("s")
        }
        return builder.toString()
    }

    fun isSystemApp(info: ApplicationInfo): Boolean {
        return info.flags and 1 > 0
    }

    fun formatFileSize(j: Long, z: Boolean): String {
        val strings = formatFileSizeArray(j, z)
        return strings[0] + strings[1]
    }

    fun formatSizeThousand(size: Long, pointed: Boolean): String {
        val strings = formatSizeWithThousand(size, pointed)
        return strings[0] + strings[1]
    }

    fun formatSizeWithThousand(size: Long, pointed: Boolean): Array<String?> {
        val decimalFormat = if (pointed) {
            DecimalFormat("0", DecimalFormatSymbols.getInstance(Locale.US))
        } else {
            DecimalFormat("0.0", DecimalFormatSymbols.getInstance(Locale.US))
        }
        decimalFormat.isGroupingUsed = false
        val strArray = arrayOfNulls<String>(2)
        if (size <= 0) {
            strArray[0] = "0"
            strArray[1] = "B"
            return strArray
        }
        if (size < 1000) {
            strArray[0] = decimalFormat.format(size)
            strArray[1] = "B"
            return strArray
        }
        if (size < 1000000) {
            strArray[0] = decimalFormat.format((size.toFloat() / 1000.0f).toDouble())
            strArray[1] = "KB"
            return strArray
        }
        if (size < 1000000000) {
            strArray[0] = decimalFormat.format((size * 1.0f / 1000000).toDouble())
            strArray[1] = "MB"
            return strArray
        }
        strArray[0] = DecimalFormat("0.0", DecimalFormatSymbols.getInstance(Locale.US)).format((size * 1.0f / 1000000000).toDouble())
        strArray[1] = "GB"
        return strArray
    }

    fun formatFileSizeArray(j: Long, z: Boolean): Array<String?> {
        val decimalFormat: DecimalFormat = if (z) {
            DecimalFormat("0")
        } else {
            DecimalFormat("0.0")
        }
        decimalFormat.isGroupingUsed = false
        val strArray = arrayOfNulls<String>(2)
        if (j <= 0) {
            strArray[0] = "0"
            strArray[1] = "B"
            return strArray
        }
        if (j < 1024) {
            strArray[0] = decimalFormat.format(j)
            strArray[1] = "B"
            return strArray
        }
        if (j < 1024000) {
            strArray[0] = decimalFormat.format((j.toFloat() / 1024.0f).toDouble())
            strArray[1] = "KB"
            return strArray
        }
        if (j < 1048576000) {
            strArray[0] = decimalFormat.format(((j shr 10).toFloat() / 1024.0f).toDouble())
            strArray[1] = "MB"
            return strArray
        }
        strArray[0] = DecimalFormat("0.0").format(((j shr 20).toFloat() / 1024.0f).toDouble())
        strArray[1] = "GB"
        return strArray
    }
}