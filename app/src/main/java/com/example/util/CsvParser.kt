package com.example.util

import com.example.data.model.ProviderEntity
import java.io.BufferedReader
import java.io.StringReader
import java.util.UUID

data class CsvImportResult(
    val totalRowsProcessed: Int,
    val validProviders: List<ProviderEntity>,
    val rowErrors: List<String>,
    val duplicateWarnings: List<String>
)

object CsvParser {

    /**
     * Parses CSV text with columns:
     * Name, BanglaName, Category, AdditionalCategories, Contact, AltContact, Location, Address, Landmark, Charge, Description, WhatsApp, Facebook, WorkingHours, Verified
     */
    fun parseProviderCsv(csvContent: String): CsvImportResult {
        val validProviders = mutableListOf<ProviderEntity>()
        val rowErrors = mutableListOf<String>()
        val duplicateWarnings = mutableListOf<String>()
        val seenPhones = mutableSetOf<String>()

        val reader = BufferedReader(StringReader(csvContent))
        var lineNumber = 0
        var isHeaderRow = true

        val rows = parseCsvRows(csvContent)

        for (columns in rows) {
            lineNumber++
            if (columns.isEmpty() || columns.all { it.isBlank() }) continue

            // Detect header row
            if (isHeaderRow) {
                val firstCol = columns.firstOrNull()?.trim()?.lowercase() ?: ""
                if (firstCol.contains("name") || firstCol.contains("title")) {
                    isHeaderRow = false
                    continue
                }
                isHeaderRow = false
            }

            if (columns.size < 4) {
                rowErrors.add("Row $lineNumber: Insufficient columns. Minimum required: Name, Category, Contact, Location.")
                continue
            }

            val name = columns.getOrElse(0) { "" }.trim()
            val banglaName = columns.getOrElse(1) { "" }.trim()
            val category = columns.getOrElse(2) { "" }.trim()
            val additionalCategories = columns.getOrElse(3) { "" }.trim()
            val contact = columns.getOrElse(4) { "" }.trim()
            val altContact = columns.getOrElse(5) { "" }.trim()
            val location = columns.getOrElse(6) { "" }.trim()
            val address = columns.getOrElse(7) { "" }.trim()
            val landmark = columns.getOrElse(8) { "" }.trim()
            val charge = columns.getOrElse(9) { "" }.trim()
            val description = columns.getOrElse(10) { "" }.trim()
            val whatsapp = columns.getOrElse(11) { "" }.trim()
            val facebook = columns.getOrElse(12) { "" }.trim()
            val workingHours = columns.getOrElse(13) { "" }.trim()
            val verifiedStr = columns.getOrElse(14) { "false" }.trim().lowercase()

            // Validations
            if (name.isBlank()) {
                rowErrors.add("Row $lineNumber: Missing required Name.")
                continue
            }
            if (category.isBlank()) {
                rowErrors.add("Row $lineNumber: Missing required Category for '$name'.")
                continue
            }
            if (contact.isBlank()) {
                rowErrors.add("Row $lineNumber: Missing required Contact Number for '$name'.")
                continue
            }
            if (location.isBlank()) {
                rowErrors.add("Row $lineNumber: Missing required Location for '$name'.")
                continue
            }

            // Check duplicate phone in current batch
            val normalizedPhone = contact.replace(Regex("[^0-9+]"), "")
            if (seenPhones.contains(normalizedPhone)) {
                duplicateWarnings.add("Row $lineNumber: Duplicate phone '$contact' in import batch for '$name'.")
            } else {
                seenPhones.add(normalizedPhone)
            }

            val isVerified = verifiedStr == "true" || verifiedStr == "1" || verifiedStr == "yes"

            validProviders.add(
                ProviderEntity(
                    id = UUID.randomUUID().toString(),
                    name = name,
                    banglaName = banglaName,
                    primaryCategory = category,
                    additionalCategories = additionalCategories,
                    contactNumber = contact,
                    additionalContact = altContact,
                    locationArea = location,
                    specificAddress = address,
                    landmark = landmark,
                    chargeEstimate = charge,
                    description = description,
                    whatsappNumber = whatsapp,
                    facebookUrl = facebook,
                    workingHours = workingHours,
                    isVerified = isVerified,
                    isActive = true,
                    isDemoData = false
                )
            )
        }

        return CsvImportResult(
            totalRowsProcessed = lineNumber,
            validProviders = validProviders,
            rowErrors = rowErrors,
            duplicateWarnings = duplicateWarnings
        )
    }

    private fun parseCsvRows(content: String): List<List<String>> {
        val result = mutableListOf<List<String>>()
        var currentToken = StringBuilder()
        val currentRow = mutableListOf<String>()
        var insideQuotes = false

        var i = 0
        while (i < content.length) {
            val c = content[i]
            when {
                c == '"' -> {
                    if (insideQuotes && i + 1 < content.length && content[i + 1] == '"') {
                        currentToken.append('"')
                        i++ // Skip escaped quote
                    } else {
                        insideQuotes = !insideQuotes
                    }
                }
                c == ',' && !insideQuotes -> {
                    currentRow.add(currentToken.toString().trim())
                    currentToken = StringBuilder()
                }
                (c == '\r' || c == '\n') && !insideQuotes -> {
                    if (c == '\r' && i + 1 < content.length && content[i + 1] == '\n') {
                        i++
                    }
                    currentRow.add(currentToken.toString().trim())
                    currentToken = StringBuilder()
                    if (currentRow.isNotEmpty() && currentRow.any { it.isNotBlank() }) {
                        result.add(currentRow.toList())
                    }
                    currentRow.clear()
                }
                else -> {
                    currentToken.append(c)
                }
            }
            i++
        }
        if (currentToken.isNotEmpty() || currentRow.isNotEmpty()) {
            currentRow.add(currentToken.toString().trim())
            if (currentRow.any { it.isNotBlank() }) {
                result.add(currentRow.toList())
            }
        }
        return result
    }

    fun exportToCsv(providers: List<ProviderEntity>): String {
        val sb = StringBuilder()
        // Header
        sb.appendLine("Name,BanglaName,Category,AdditionalCategories,Contact,AltContact,Location,Address,Landmark,Charge,Description,WhatsApp,Facebook,WorkingHours,Verified,Active")
        for (p in providers) {
            val row = listOf(
                escapeCsv(p.name),
                escapeCsv(p.banglaName),
                escapeCsv(p.primaryCategory),
                escapeCsv(p.additionalCategories),
                escapeCsv(p.contactNumber),
                escapeCsv(p.additionalContact),
                escapeCsv(p.locationArea),
                escapeCsv(p.specificAddress),
                escapeCsv(p.landmark),
                escapeCsv(p.chargeEstimate),
                escapeCsv(p.description),
                escapeCsv(p.whatsappNumber),
                escapeCsv(p.facebookUrl),
                escapeCsv(p.workingHours),
                if (p.isVerified) "true" else "false",
                if (p.isActive) "true" else "false"
            )
            sb.appendLine(row.joinToString(","))
        }
        return sb.toString()
    }

    private fun escapeCsv(value: String): String {
        if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
            return "\"" + value.replace("\"", "\"\"") + "\""
        }
        return value
    }
}
