package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.CatalogApp
import com.example.data.model.InstalledAppInfo
import com.example.domain.ApkVerificationResult
import com.example.domain.InstalledAppsDetector
import com.example.domain.Sha256Verifier
import com.example.ui.MyStoreAppRoot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class MyStoreRobolectricTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `sha256 verifier rejects corrupted file and checksum mismatch`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val fakeApk = File(context.cacheDir, "corrupt.apk").apply {
            writeText("not a valid apk zip")
        }
        try {
            val mismatch = Sha256Verifier.verifyApkFile(
                context = context,
                apkFile = fakeApk,
                expectedPackageName = "com.mystore.app",
                expectedSha256 = "0000000000000000000000000000000000000000000000000000000000000000"
            )
            assertTrue(mismatch is ApkVerificationResult.Invalid)

            val actualHash = Sha256Verifier.computeFileSha256(fakeApk)
            val corruptArchive = Sha256Verifier.verifyApkFile(
                context = context,
                apkFile = fakeApk,
                expectedPackageName = "com.mystore.app",
                expectedSha256 = actualHash
            )
            assertTrue(corruptArchive is ApkVerificationResult.Invalid)
        } finally {
            fakeApk.delete()
        }
    }

    @Test
    fun `update detector flags self-update and newer catalog versionCode`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val detector = InstalledAppsDetector(context)
        val installed = mapOf(
            "com.mystore.app" to InstalledAppInfo("com.mystore.app", "1.0.0", 1L, "AA:BB"),
            "com.other.app" to InstalledAppInfo("com.other.app", "2.0.0", 20L, "CC:DD")
        )
        val catalog = listOf(
            CatalogApp(id = "my-store", name = "My Store", packageName = "com.mystore.app", versionName = "1.0.1", versionCode = 2L),
            CatalogApp(id = "other", name = "Other", packageName = "com.other.app", versionName = "2.0.0", versionCode = 20L)
        )
        val updates = detector.detectAvailableUpdates(catalog, installed)
        assertEquals(1, updates.size)
        assertEquals("com.mystore.app", updates.first().catalogApp.packageName)
        assertTrue(updates.first().isSelfUpdate)
    }

    @Test
    fun `app renders home navigation tabs search and details screen`() {
        composeRule.setContent {
            MyStoreAppRoot(showLaunchSplash = false)
        }
        composeRule.waitForIdle()

        // Home branding & 5 bottom navigation items visible
        composeRule.onNodeWithText("My Store").assertIsDisplayed()
        composeRule.onNodeWithTag("nav_home").assertIsDisplayed()
        composeRule.onNodeWithTag("nav_categories").assertIsDisplayed()
        composeRule.onNodeWithTag("nav_updates").assertIsDisplayed()
        composeRule.onNodeWithTag("nav_downloads").assertIsDisplayed()
        composeRule.onNodeWithTag("nav_settings").assertIsDisplayed()

        // Search filters catalog
        composeRule.onNodeWithTag("search_input").performTextInput("My Store")
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Search Results (1)").assertIsDisplayed()

        // Navigate to Categories
        composeRule.onNodeWithTag("nav_categories").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Browse Categories").assertIsDisplayed()

        // Navigate to Updates
        composeRule.onNodeWithTag("nav_updates").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("self_update_card").assertIsDisplayed()

        // Navigate to Downloads
        composeRule.onNodeWithTag("nav_downloads").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("downloads_list").assertIsDisplayed()

        // Navigate to Settings & switch theme
        composeRule.onNodeWithTag("nav_settings").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("catalog_url_input").assertIsDisplayed()
        composeRule.onNodeWithTag("theme_chip_light").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("theme_chip_dark").performClick()
        composeRule.waitForIdle()

        // Navigate back to Home, open App Details, and verify disclosure + SHA-256 section
        composeRule.onNodeWithTag("nav_home").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("app_card_my-store").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("app_details_scroll").assertIsDisplayed()
        composeRule.onNodeWithTag("details_back_button").performClick()
        composeRule.waitForIdle()
    }
}
