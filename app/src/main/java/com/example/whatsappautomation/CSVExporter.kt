package com.example.whatsappautomation

import com.example.whatsappautomation.data.Contact
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CSVExporter {

    fun generateCSV(contacts: List<Contact>): String {
        // Header
        val header = "Name,Phone Number,Source,Tag,Capture Date\n"
        
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

        val rows = contacts.joinToString("\n") { contact ->
            val name = escapeCsv(contact.name)
            val phone = escapeCsv(contact.phoneNumber)
            val source = escapeCsv(contact.sourceApp)
            val tags = escapeCsv(contact.tags)
            val date = dateFormat.format(Date(contact.captureDate))
            
            "$name,$phone,$source,$tags,$date"
        }

        return header + rows
    }

    // Escapes special characters so the CSV doesn't break
    private fun escapeCsv(value: String): String {
        if (value.isBlank()) return ""
        var cleanValue = value
        // Wrap in quotes if it contains a comma, quote, or newline
        if (cleanValue.contains(",") || cleanValue.contains("\"") || cleanValue.contains("\n")) {
            cleanValue = cleanValue.replace("\"", "\"\"") // Escape quotes by doubling them
            return "\"$cleanValue\""
        }
        return cleanValue
    }
}
