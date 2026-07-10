package com.example.data

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.graphics.pdf.PdfDocument
import android.graphics.Paint
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Typeface
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object InvoiceGenerator {

    fun generateInvoicePdf(
        context: Context,
        order: Order,
        products: List<Product>
    ): Uri? {
        val pdfDocument = PdfDocument()
        
        // A4 page size
        val pageWidth = 595
        val pageHeight = 842
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        val paint = Paint()
        
        // Draw Header background
        paint.color = Color.parseColor("#1B5E20") // Dark green primary
        canvas.drawRect(0f, 0f, pageWidth.toFloat(), 90f, paint)

        // Header Title
        paint.color = Color.WHITE
        paint.textSize = 22f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("ZYL VOR BAZAAR", 30f, 40f, paint)

        paint.textSize = 12f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("Premium Organic & Electronic Marketplace", 30f, 60f, paint)
        canvas.drawText("TAX INVOICE / BILL OF SUPPLY", 30f, 78f, paint)

        // Invoice Metadata
        paint.color = Color.BLACK
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 11f
        canvas.drawText("Invoice Number: INV-${order.orderId.uppercase()}", 30f, 120f, paint)
        
        val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
        val formattedDate = sdf.format(Date(order.orderDate))
        canvas.drawText("Invoice Date: $formattedDate", 30f, 138f, paint)
        canvas.drawText("Order ID: ${order.orderId}", 30f, 156f, paint)

        // Divider
        paint.color = Color.LTGRAY
        canvas.drawLine(30f, 170f, (pageWidth - 30).toFloat(), 170f, paint)

        // Customer details
        paint.color = Color.BLACK
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 12f
        canvas.drawText("Billed To:", 30f, 195f, paint)

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 10f
        canvas.drawText("Name: ${order.email.substringBefore("@")}", 30f, 212f, paint)
        canvas.drawText("Email: ${order.email}", 30f, 227f, paint)
        
        // Wrap delivery address text
        val addressLines = wrapText(order.deliveryAddress, (pageWidth - 60).toFloat(), paint)
        var yPos = 242f
        canvas.drawText("Delivery Address:", 30f, yPos, paint)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        for (line in addressLines) {
            yPos += 14f
            canvas.drawText(line, 30f, yPos, paint)
        }
        
        yPos += 20f
        paint.color = Color.LTGRAY
        canvas.drawLine(30f, yPos, (pageWidth - 30).toFloat(), yPos, paint)

        // Table Header
        yPos += 25f
        paint.color = Color.parseColor("#E8F5E9") // light green
        canvas.drawRect(30f, yPos - 15f, (pageWidth - 30).toFloat(), yPos + 10f, paint)

        paint.color = Color.BLACK
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 10f
        canvas.drawText("Item Name", 40f, yPos, paint)
        canvas.drawText("Price", 320f, yPos, paint)
        canvas.drawText("Qty", 410f, yPos, paint)
        canvas.drawText("Total", 480f, yPos, paint)

        // Parse itemsSummary
        val orderLines = parseItemsSummary(order.itemsSummary, products)
        yPos += 25f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        
        for (line in orderLines) {
            paint.color = Color.BLACK
            // Product Name wrapping
            val nameLines = wrapText(line.name, 270f, paint)
            var tempY = yPos
            for (nl in nameLines) {
                canvas.drawText(nl, 40f, tempY, paint)
                tempY += 14f
            }
            
            canvas.drawText("₹${String.format(Locale.US, "%.2f", line.price)}", 320f, yPos, paint)
            canvas.drawText(line.quantity.toString(), 410f, yPos, paint)
            canvas.drawText("₹${String.format(Locale.US, "%.2f", line.price * line.quantity)}", 480f, yPos, paint)
            
            yPos = maxOf(tempY, yPos + 20f)
            
            paint.color = Color.parseColor("#EEEEEE")
            canvas.drawLine(30f, yPos - 5f, (pageWidth - 30).toFloat(), yPos - 5f, paint)
            yPos += 15f
        }

        // Summary details
        yPos += 10f
        paint.color = Color.BLACK
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Payment Summary", 320f, yPos, paint)
        yPos += 18f

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("Items Amount:", 320f, yPos, paint)
        val itemsAmt = order.itemsAmount.takeIf { it > 0.0 } ?: (order.totalAmount - order.deliveryCharge)
        canvas.drawText("₹${String.format(Locale.US, "%.2f", itemsAmt)}", 480f, yPos, paint)
        yPos += 16f

        canvas.drawText("Delivery Charge:", 320f, yPos, paint)
        canvas.drawText("₹${String.format(Locale.US, "%.2f", order.deliveryCharge)}", 480f, yPos, paint)
        yPos += 16f

        if (order.couponApplied.isNotBlank()) {
            canvas.drawText("Discount (${order.couponApplied}):", 320f, yPos, paint)
            val discount = itemsAmt * 0.20
            canvas.drawText("-₹${String.format(Locale.US, "%.2f", discount)}", 480f, yPos, paint)
            yPos += 16f
        }

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Total Amount Paid:", 320f, yPos, paint)
        canvas.drawText("₹${String.format(Locale.US, "%.2f", order.totalAmount)}", 480f, yPos, paint)
        
        yPos += 30f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 9f
        paint.color = Color.GRAY
        canvas.drawText("Payment Mode: ${order.paymentMode}", 30f, yPos, paint)
        if (order.paymentTransactionId.isNotBlank()) {
            canvas.drawText("Transaction ID: ${order.paymentTransactionId}", 30f, yPos + 12f, paint)
        }

        // Footer
        paint.color = Color.LTGRAY
        canvas.drawLine(30f, pageHeight - 60f, (pageWidth - 30).toFloat(), pageHeight - 60f, paint)

        paint.color = Color.GRAY
        paint.textSize = 9f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
        canvas.drawText("This is a computer-generated invoice and requires no signature.", 30f, pageHeight - 45f, paint)
        canvas.drawText("Thank you for shopping at Zyl Vor Bazaar!", 30f, pageHeight - 32f, paint)

        pdfDocument.finishPage(page)

        // Save PDF to public Downloads
        val filename = "Invoice_${order.orderId}.pdf"
        var uri: Uri? = null

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val resolver = context.contentResolver
                uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                if (uri != null) {
                    resolver.openOutputStream(uri)?.use { out ->
                        pdfDocument.writeTo(out)
                    }
                }
            } else {
                val downloadFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (!downloadFolder.exists()) {
                    downloadFolder.mkdirs()
                }
                val file = File(downloadFolder, filename)
                FileOutputStream(file).use { out ->
                    pdfDocument.writeTo(out)
                }
                uri = Uri.fromFile(file)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            pdfDocument.close()
        }
        
        return uri
    }

    private fun wrapText(text: String, maxWidth: Float, paint: Paint): List<String> {
        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var currentLine = ""
        
        for (word in words) {
            val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
            val width = paint.measureText(testLine)
            if (width > maxWidth) {
                lines.add(currentLine)
                currentLine = word
            } else {
                currentLine = testLine
            }
        }
        if (currentLine.isNotEmpty()) {
            lines.add(currentLine)
        }
        return lines
    }

    private class InvoiceLine(val name: String, val price: Double, val quantity: Int)

    private fun parseItemsSummary(itemsSummary: String, products: List<Product>): List<InvoiceLine> {
        val list = mutableListOf<InvoiceLine>()
        if (itemsSummary.isBlank()) return list
        
        val items = itemsSummary.split(",")
        for (item in items) {
            val raw = item.trim()
            if (raw.isBlank()) continue
            
            val qty = Regex("""(?:x|Qty:\s*)\s*(\d+)""", RegexOption.IGNORE_CASE)
                .find(raw)
                ?.groupValues
                ?.getOrNull(1)
                ?.toIntOrNull()
                ?: 1
                
            val cleanName = raw
                .substringBefore(" x")
                .substringBefore(" Qty:")
                .substringBefore(" (")
                .trim()
                .ifBlank { raw }
                
            val matchingProd = products.find {
                it.name.equals(cleanName, ignoreCase = true) ||
                    cleanName.contains(it.name, ignoreCase = true) ||
                    it.name.contains(cleanName, ignoreCase = true)
            }
            val price = matchingProd?.price ?: 0.0
            list.add(InvoiceLine(cleanName, price, qty))
        }
        return list
    }
}
