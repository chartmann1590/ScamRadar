package com.scamradar.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.scamradar.app.data.datastore.UserPrefs
import com.scamradar.app.ui.navigation.ScamRadarNavHost
import com.scamradar.app.ui.theme.ScamRadarTheme
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val userPrefs = UserPrefs(applicationContext)
        val filesDir = applicationContext.filesDir

        setContent {
            ScamRadarTheme {
                ScamRadarNavHost(
                    userPrefs = userPrefs,
                    filesDir = filesDir,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
