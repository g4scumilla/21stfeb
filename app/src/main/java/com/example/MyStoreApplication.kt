package com.example

import android.app.Application
import com.example.data.local.MyStoreDatabase
import com.example.data.local.SettingsDataStore
import com.example.data.remote.CatalogRemoteDataSource
import com.example.data.repository.CatalogRepository
import com.example.domain.InstalledAppsDetector
import com.example.installer.ApkDownloadManager
import com.example.installer.ApkInstallerManager
import com.example.installer.StoreNotificationHelper
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

class MyStoreApplication : Application() {

    lateinit var database: MyStoreDatabase
        private set
    lateinit var settingsDataStore: SettingsDataStore
        private set
    lateinit var okHttpClient: OkHttpClient
        private set
    lateinit var catalogRepository: CatalogRepository
        private set
    lateinit var installedAppsDetector: InstalledAppsDetector
        private set
    lateinit var notificationHelper: StoreNotificationHelper
        private set
    lateinit var downloadManager: ApkDownloadManager
        private set
    lateinit var installerManager: ApkInstallerManager
        private set

    override fun onCreate() {
        super.onCreate()
        database = MyStoreDatabase.getInstance(this)
        settingsDataStore = SettingsDataStore(this)
        okHttpClient = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()

        val remoteDataSource = CatalogRemoteDataSource(this, okHttpClient)
        catalogRepository = CatalogRepository(
            dao = database.myStoreDao(),
            remoteDataSource = remoteDataSource,
            settingsDataStore = settingsDataStore
        )
        installedAppsDetector = InstalledAppsDetector(this)
        notificationHelper = StoreNotificationHelper(this).apply {
            createChannels()
        }
        downloadManager = ApkDownloadManager(
            context = this,
            okHttpClient = okHttpClient,
            dao = database.myStoreDao(),
            settingsDataStore = settingsDataStore,
            notificationHelper = notificationHelper
        )
        installerManager = ApkInstallerManager(
            context = this,
            installedAppsDetector = installedAppsDetector
        )
    }
}
