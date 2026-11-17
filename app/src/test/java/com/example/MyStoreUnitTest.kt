package com.example

import com.example.data.local.SettingsDataStore
import com.example.data.model.CatalogApp
import com.example.data.model.CatalogResponse
import com.example.data.remote.CatalogRemoteDataSource
import com.example.data.repository.CatalogRepository
import com.example.domain.Sha256Verifier
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class MyStoreUnitTest {

    @Test
    fun `normalizeCatalogInput converts owner repo shorthand to GitHub Pages HTTPS URL`() {
        assertEquals(
            "https://alice.github.io/my-store/data/catalog.json",
            SettingsDataStore.normalizeCatalogInput("Alice/my-store")
        )
        assertEquals(
            "https://alice.github.io/my-store/data/catalog.json",
            SettingsDataStore.normalizeCatalogInput("https://alice.github.io/my-store")
        )
        assertEquals(
            "https://alice.github.io/my-store/data/catalog.json",
            SettingsDataStore.normalizeCatalogInput("https://alice.github.io/my-store/data/catalog.json")
        )
    }

    @Test
    fun `resolveUrl enforces HTTPS and resolves relative catalog assets`() {
        val siteBase = CatalogRemoteDataSource.deriveSiteBaseUrl(
            "https://alice.github.io/my-store/data/catalog.json"
        )
        assertEquals("https://alice.github.io/my-store/", siteBase)
        assertEquals(
            "https://alice.github.io/my-store/assets/mystore-logo.png",
            CatalogRemoteDataSource.resolveUrl("assets/mystore-logo.png", siteBase)
        )
        assertEquals(
            "",
            CatalogRemoteDataSource.resolveUrl("http://insecure.example.com/app.apk", siteBase)
        )
    }

    @Test
    fun `buildCategoryList dynamically includes new categories from catalog JSON`() {
        val apps = listOf(
            CatalogApp(id = "1", name = "A", packageName = "a.b", category = "Photography"),
            CatalogApp(id = "2", name = "B", packageName = "c.d", category = "Tools")
        )
        val categories = CatalogRepository.buildCategoryList(
            listOf("All Apps", "Productivity", "Tools"),
            apps
        )
        assertEquals(listOf("All Apps", "Productivity", "Tools", "Photography"), categories)
    }

    @Test
    fun `sha256 verifier computes accurate digest`() {
        val temp = File.createTempFile("mystore-test", ".bin")
        try {
            temp.writeText("my-store-verified-apk-bytes")
            val digest = Sha256Verifier.computeFileSha256(temp)
            assertEquals(64, digest.length)
            assertEquals(digest, Sha256Verifier.computeFileSha256(temp))
        } finally {
            temp.delete()
        }
    }

    @Test
    fun `catalog JSON schema parses cleanly and blocks non-HTTPS APK links`() {
        val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
        val adapter = moshi.adapter(CatalogResponse::class.java)
        val parsed = adapter.fromJson(
            """
            {
              "schemaVersion": 1,
              "storeName": "My Store",
              "categories": ["All Apps", "Tools"],
              "apps": [
                {
                  "id": "demo",
                  "name": "Demo",
                  "packageName": "com.example.demo",
                  "apkUrl": "http://insecure.com/demo.apk",
                  "versionCode": 2
                },
                {
                  "id": "secure",
                  "name": "Secure",
                  "packageName": "com.example.secure",
                  "apkUrl": "https://github.com/u/r/releases/download/v1/app.apk",
                  "versionCode": 3
                }
              ]
            }
            """.trimIndent()
        )!!
        assertEquals(2, parsed.apps.size)
        assertFalse(parsed.apps[0].hasDownloadableApk)
        assertTrue(parsed.apps[1].hasDownloadableApk)
    }
}
