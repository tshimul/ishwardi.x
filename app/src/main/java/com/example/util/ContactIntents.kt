package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.example.data.model.ProviderEntity

object ContactIntents {

    fun dialNumber(context: Context, phoneNumber: String) {
        val cleanNumber = phoneNumber.replace(Regex("[^0-9+]"), "")
        if (cleanNumber.isBlank()) {
            Toast.makeText(context, "No valid phone number provided", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            val intent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:$cleanNumber")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Unable to launch dialer", Toast.LENGTH_SHORT).show()
        }
    }

    fun openWhatsApp(context: Context, rawNumber: String, optionalMessage: String = "Hello from Ishwardi.X") {
        var cleanNumber = rawNumber.replace(Regex("[^0-9+]"), "")
        if (cleanNumber.isBlank()) {
            Toast.makeText(context, "WhatsApp number not available", Toast.LENGTH_SHORT).show()
            return
        }

        // Format Bangladesh local mobile number to international format if starting with 01
        if (cleanNumber.startsWith("01") && cleanNumber.length == 11) {
            cleanNumber = "88$cleanNumber"
        } else if (cleanNumber.startsWith("+880")) {
            cleanNumber = cleanNumber.removePrefix("+")
        }

        try {
            val encodedMsg = Uri.encode(optionalMessage)
            val uri = Uri.parse("https://api.whatsapp.com/send?phone=$cleanNumber&text=$encodedMsg")
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Could not open WhatsApp. Please check if installed.", Toast.LENGTH_SHORT).show()
        }
    }

    fun openFacebook(context: Context, facebookUrl: String) {
        val cleanUrl = facebookUrl.trim()
        if (cleanUrl.isBlank()) {
            Toast.makeText(context, "No Facebook link available", Toast.LENGTH_SHORT).show()
            return
        }

        val urlToOpen = if (!cleanUrl.startsWith("http://") && !cleanUrl.startsWith("https://")) {
            "https://$cleanUrl"
        } else {
            cleanUrl
        }

        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(urlToOpen)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Could not open link", Toast.LENGTH_SHORT).show()
        }
    }

    fun openMap(context: Context, queryOrCoords: String, label: String = "") {
        val clean = queryOrCoords.trim()
        if (clean.isBlank()) {
            Toast.makeText(context, "Location coordinates not available", Toast.LENGTH_SHORT).show()
            return
        }

        val mapUri = if (clean.matches(Regex("^-?\\d+(\\.\\d+)?[,\\s]+-?\\d+(\\.\\d+)?$"))) {
            val parts = clean.split(Regex("[,\\s]+"))
            val lat = parts[0]
            val lng = parts[1]
            val encodedLabel = Uri.encode(label.ifBlank { "Service Location" })
            Uri.parse("geo:$lat,$lng?q=$lat,$lng($encodedLabel)")
        } else {
            val searchQuery = "$clean, Ishwardi, Bangladesh"
            Uri.parse("geo:0,0?q=${Uri.encode(searchQuery)}")
        }

        try {
            val intent = Intent(Intent.ACTION_VIEW, mapUri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            // Fallback to web browser Google Maps
            try {
                val webUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=${Uri.encode("$clean, Ishwardi")}")
                val webIntent = Intent(Intent.ACTION_VIEW, webUri).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(webIntent)
            } catch (ex: Exception) {
                Toast.makeText(context, "No map application available", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun shareProvider(context: Context, provider: ProviderEntity) {
        val shareText = buildString {
            appendLine("⭐ ${provider.name}")
            if (provider.banglaName.isNotBlank()) {
                appendLine("(${provider.banglaName})")
            }
            appendLine("🔧 Service: ${provider.primaryCategory}")
            appendLine("📞 Contact: ${provider.contactNumber}")
            if (provider.additionalContact.isNotBlank()) {
                appendLine("📱 Alt: ${provider.additionalContact}")
            }
            if (provider.locationArea.isNotBlank()) {
                appendLine("📍 Location: ${provider.locationArea}")
            }
            if (provider.chargeEstimate.isNotBlank()) {
                appendLine("💰 Charge: ${provider.chargeEstimate}")
            }
            if (provider.description.isNotBlank()) {
                appendLine("📝 Info: ${provider.description}")
            }
            appendLine()
            appendLine("Shared via Ishwardi.X - Ishwardi at Your Fingertips")
        }

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Contact for ${provider.name} - Ishwardi.X")
            putExtra(Intent.EXTRA_TEXT, shareText)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val chooser = Intent.createChooser(intent, "Share Service Provider via")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }
}
