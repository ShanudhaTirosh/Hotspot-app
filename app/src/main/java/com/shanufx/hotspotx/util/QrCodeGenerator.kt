package com.shanufx.hotspotx.util

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.WriterException
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object QrCodeGenerator {

    /**
     * Generates a Wi-Fi QR code in WPA format.
     * The string format is: WIFI:T:WPA;S:<ssid>;P:<password>;;
     * Scannable by Android Camera, iOS Camera, and QR scanner apps.
     */
    suspend fun generateWifiQr(
        ssid: String,
        password: String,
        securityType: String = "WPA",
        sizePx: Int = 512
    ): Bitmap? = withContext(Dispatchers.Default) {
        val escaped = ssid.replace("\\", "\\\\").replace(";", "\\;").replace(",", "\\,")
        val escapedPw = password.replace("\\", "\\\\").replace(";", "\\;").replace(",", "\\,")
        val content = "WIFI:T:$securityType;S:$escaped;P:$escapedPw;;"

        try {
            val hints = mapOf(
                EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
                EncodeHintType.MARGIN to 1,
                EncodeHintType.CHARACTER_SET to "UTF-8"
            )
            val writer = QRCodeWriter()
            val matrix: BitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
            matrixToBitmap(matrix)
        } catch (_: WriterException) {
            null
        }
    }

    private fun matrixToBitmap(matrix: BitMatrix): Bitmap {
        val width = matrix.width
        val height = matrix.height
        val pixels = IntArray(width * height)
        for (y in 0 until height) {
            for (x in 0 until width) {
                pixels[y * width + x] = if (matrix[x, y]) Color.BLACK else Color.WHITE
            }
        }
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
        bmp.setPixels(pixels, 0, width, 0, 0, width, height)
        return bmp
    }
}
