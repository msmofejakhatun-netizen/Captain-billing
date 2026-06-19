package com.example.data.util

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import com.example.domain.model.*
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ReportExportUtil {

    private const val TAG = "ReportExportUtil"

    fun shareFile(context: Context, file: File, mimeType: String, title: String) {
        try {
            val authority = "${context.packageName}.provider"
            val uri: Uri = FileProvider.getUriForFile(context, authority, file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, title)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, title))
        } catch (e: Exception) {
            Log.e(TAG, "Failed sharing file", e)
        }
    }

    // PDF GENERATION USING NATIVE PdfDocument
    fun exportToPdf(context: Context, title: String, headers: List<String>, rows: List<List<String>>): File? {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 Size standard pixels
        val page = pdfDocument.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        val paint = Paint().apply {
            isAntiAlias = true
        }

        var y = 40f

        // Title Header
        paint.color = Color.DKGRAY
        paint.textSize = 18f
        paint.isFakeBoldText = true
        canvas.drawText(title.uppercase(), 40f, y, paint)
        y += 20f

        // Timestamps
        paint.textSize = 10f
        paint.isFakeBoldText = false
        paint.color = Color.GRAY
        val dateString = SimpleDateFormat("dd-MMM-yyyy HH:mm", Locale.getDefault()).format(Date())
        canvas.drawText("Generated on: $dateString", 40f, y, paint)
        y += 25f

        // Table Borders or Partition Lines
        paint.color = Color.BLACK
        paint.strokeWidth = 1f
        canvas.drawLine(40f, y, 555f, y, paint)
        y += 20f

        // Draw Headers Column Wise
        paint.isFakeBoldText = true
        paint.textSize = 10f
        paint.color = Color.BLUE
        val colWidth = 515f / headers.size
        for (i in headers.indices) {
            canvas.drawText(headers[i], 40f + (i * colWidth), y, paint)
        }
        y += 15f
        paint.color = Color.BLACK
        canvas.drawLine(40f, y, 555f, y, paint)
        y += 20f

        // Draw Row Data
        paint.isFakeBoldText = false
        paint.color = Color.BLACK
        for (row in rows) {
            for (i in row.indices) {
                val value = row[i]
                // truncate long text to avoid overlapping
                val displayVal = if (value.length > 18) value.substring(0, 15) + "..." else value
                canvas.drawText(displayVal, 40f + (i * colWidth), y, paint)
            }
            y += 20f
            if (y > 800) {
                // simple break if exceeds a single A4 page
                break
            }
        }

        canvas.drawLine(40f, y, 555f, y, paint)
        y += 20f
        paint.color = Color.GRAY
        paint.textSize = 9f
        canvas.drawText("Spice Garden POS Report Footer - Page 1 of 1", 40f, y, paint)

        pdfDocument.finishPage(page)

        val cleanTitle = title.replace(" ", "_").lowercase()
        val file = File(context.cacheDir, "${cleanTitle}_report_${System.currentTimeMillis()}.pdf")
        try {
            pdfDocument.writeTo(FileOutputStream(file))
            pdfDocument.close()
            return file
        } catch (e: Exception) {
            Log.e(TAG, "Writing PDF failed", e)
            pdfDocument.close()
            return null
        }
    }

    // EXCEL / CSV Tabular writer
    fun exportToExcelCsv(context: Context, title: String, headers: List<String>, rows: List<List<String>>): File? {
        val cleanTitle = title.replace(" ", "_").lowercase()
        val file = File(context.cacheDir, "${cleanTitle}_report_${System.currentTimeMillis()}.csv")
        try {
            FileOutputStream(file).use { out ->
                // Write Excel UTF-8 BOM byte sequence so Excel reads characters properly
                out.write(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()))
                
                val writer = out.writer()
                writer.write("$title Report\n")
                val dateString = SimpleDateFormat("dd-MMM-yyyy HH:mm", Locale.getDefault()).format(Date())
                writer.write("Generated on: $dateString\n\n")

                // Write Header Row
                writer.write(headers.joinToString(",") { "\"$it\"" } + "\n")

                // Write Rows
                for (row in rows) {
                    writer.write(row.joinToString(",") { "\"${it.replace("\"", "\"\"")}\"" } + "\n")
                }
                writer.flush()
            }
            return file
        } catch (e: Exception) {
            Log.e(TAG, "Writing CSV/Excel failed", e)
            return null
        }
    }
}
