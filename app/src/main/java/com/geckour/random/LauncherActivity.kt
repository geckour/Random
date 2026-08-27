package com.geckour.random

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import com.geckour.random.ui.theme.RandomTheme
import org.koin.android.ext.android.get

class LauncherActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RandomTheme {
                // A surface container using the 'background' color from the theme
                Surface(color = MaterialTheme.colorScheme.background) {}
            }
        }
        val intent = if (get<SeedRepository>().setupFinished()) MainActivity.newIntent(this) else SetupActivity.newIntent(this)
        startActivity(intent)
    }
}