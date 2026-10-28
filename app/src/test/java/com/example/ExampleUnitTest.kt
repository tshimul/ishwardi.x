package com.example

import com.example.data.model.ProviderEntity
import com.example.util.CsvParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

  @Test
  fun testProviderModelAllCategories() {
    val provider = ProviderEntity(
      name = "Noshid",
      primaryCategory = "Plumbing",
      additionalCategories = "Water Services, Gas Services",
      contactNumber = "555-0123",
      locationArea = "Ishwardi"
    )

    val allCategories = provider.getAllCategories()
    assertEquals(3, allCategories.size)
    assertTrue(allCategories.contains("Plumbing"))
    assertTrue(allCategories.contains("Water Services"))
    assertTrue(allCategories.contains("Gas Services"))
  }

  @Test
  fun testCsvParserValidAndErrorRows() {
    val sampleCsv = """
      Name,BanglaName,Category,AdditionalCategories,Contact,AltContact,Location,Address,Landmark,Charge,Description,WhatsApp,Facebook,WorkingHours,Verified
      Tariqul Islam,,Electricians,,01712-345678,,Rooppur,,,,,01712345678,,,true
      ,বাংলা নাম,Plumbing,,01700-000000,,Ishwardi Sadar,,,,,,,,false
      Missing Category,,,,,01700-111111,,Dashuria,,,,,,,,false
    """.trimIndent()

    val result = CsvParser.parseProviderCsv(sampleCsv)

    // First row should be valid
    assertEquals(1, result.validProviders.size)
    assertEquals("Tariqul Islam", result.validProviders[0].name)
    assertEquals("Electricians", result.validProviders[0].primaryCategory)
    assertTrue(result.validProviders[0].isVerified)

    // Row 2 has empty Name, Row 3 has empty Category and Contact
    assertTrue(result.rowErrors.isNotEmpty())
  }

  @Test
  fun testCsvExportFormatting() {
    val providers = listOf(
      ProviderEntity(
        name = "Noshid",
        banglaName = "নশিদ",
        primaryCategory = "Plumbing",
        contactNumber = "555-0123",
        locationArea = "Ishwardi"
      )
    )

    val csv = CsvParser.exportToCsv(providers)
    assertTrue(csv.contains("Name,BanglaName,Category"))
    assertTrue(csv.contains("Noshid"))
    assertTrue(csv.contains("555-0123"))
    assertTrue(csv.contains("Plumbing"))
  }
}
