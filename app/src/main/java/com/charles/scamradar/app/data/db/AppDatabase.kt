package com.charles.scamradar.app.data.db

import android.content.Context

class AppDatabase private constructor(context: Context) {

    private val dao = ScanHistoryDao(context.applicationContext)

    fun scanHistoryDao(): ScanHistoryDao = dao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AppDatabase(context).also { INSTANCE = it }
            }
        }
    }
}
