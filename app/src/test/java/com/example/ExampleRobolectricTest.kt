package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read app name and tagline from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    val tagline = context.getString(R.string.tagline)
    val developerCredit = context.getString(R.string.developer_credit)

    assertEquals("Ishwardi.X", appName)
    assertEquals("Ishwardi at Your Fingertips", tagline)
    assertEquals("Developed by ART", developerCredit)
  }
}
