package com.example.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Carpenter
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Emergency
import androidx.compose.material.icons.filled.FormatPaint
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Handyman
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.LocalPharmacy
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Plumbing
import androidx.compose.material.icons.filled.PropaneTank
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

object CategoryIconHelper {

    fun getIconForCategory(categoryName: String): ImageVector {
        val lower = categoryName.lowercase()
        return when {
            lower.contains("plumb") -> Icons.Default.Plumbing
            lower.contains("electri") -> Icons.Default.ElectricBolt
            lower.contains("internet") || lower.contains("wifi") || lower.contains("broadband") -> Icons.Default.Wifi
            lower.contains("gas") -> Icons.Default.PropaneTank
            lower.contains("water") -> Icons.Default.WaterDrop
            lower.contains("mobile") || lower.contains("telecom") || lower.contains("phone") -> Icons.Default.Smartphone
            lower.contains("mechanic") || lower.contains("vehicle repair") -> Icons.Default.Build
            lower.contains("electron") || lower.contains("tv") -> Icons.Default.Tv
            lower.contains("computer") || lower.contains("it") -> Icons.Default.Computer
            lower.contains("ac") || lower.contains("refriger") -> Icons.Default.AcUnit
            lower.contains("appliance") -> Icons.Default.Kitchen
            lower.contains("construct") || lower.contains("building") -> Icons.Default.Handyman
            lower.contains("carpent") || lower.contains("furniture") -> Icons.Default.Carpenter
            lower.contains("paint") -> Icons.Default.FormatPaint
            lower.contains("clean") || lower.contains("housekeep") -> Icons.Default.CleaningServices
            lower.contains("transport") || lower.contains("rental") -> Icons.Default.DirectionsCar
            lower.contains("ambulance") || lower.contains("emergency") -> Icons.Default.Emergency
            lower.contains("doctor") || lower.contains("clinic") -> Icons.Default.MedicalServices
            lower.contains("hospital") || lower.contains("diagnostic") -> Icons.Default.LocalHospital
            lower.contains("pharmac") || lower.contains("medicine") -> Icons.Default.LocalPharmacy
            lower.contains("legal") || lower.contains("lawyer") -> Icons.Default.Gavel
            lower.contains("educat") || lower.contains("coach") || lower.contains("tutor") -> Icons.Default.School
            lower.contains("photo") || lower.contains("event") -> Icons.Default.CameraAlt
            lower.contains("restaur") || lower.contains("food") -> Icons.Default.Restaurant
            lower.contains("courier") || lower.contains("deliver") -> Icons.Default.LocalShipping
            lower.contains("govern") || lower.contains("public") -> Icons.Default.AccountBalance
            else -> Icons.Default.MoreHoriz
        }
    }

    fun getColorForCategory(categoryName: String): Color {
        val lower = categoryName.lowercase()
        return when {
            lower.contains("emergency") || lower.contains("ambulance") -> Color(0xFFDC2626)
            lower.contains("doctor") || lower.contains("hospital") || lower.contains("pharmac") -> Color(0xFF0284C7)
            lower.contains("electri") -> Color(0xFFD97706)
            lower.contains("plumb") || lower.contains("water") -> Color(0xFF0284C7)
            lower.contains("gas") -> Color(0xFFEA580C)
            lower.contains("internet") || lower.contains("computer") -> Color(0xFF059669)
            lower.contains("mechanic") || lower.contains("construct") -> Color(0xFF475569)
            lower.contains("clean") -> Color(0xFF0D9488)
            lower.contains("legal") -> Color(0xFF7C3AED)
            lower.contains("educat") -> Color(0xFF4F46E5)
            lower.contains("restaur") -> Color(0xFFE11D48)
            else -> Color(0xFF00668B)
        }
    }
}
