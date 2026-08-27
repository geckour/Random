package com.geckour.random

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MotionEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.geckour.random.ui.theme.RandomTheme
import org.koin.android.ext.android.get

class SetupActivity : ComponentActivity() {

    companion object {

        fun newIntent(context: Context) = Intent(context, SetupActivity::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RandomTheme {
                // A surface container using the 'background' color from the theme
                Surface(color = MaterialTheme.colorScheme.background) {
                    Greeting(
                        modifier = Modifier.padding(WindowInsets.systemBars.asPaddingValues()),
                        message = getString(R.string.greeting_message),
                    ) { points ->
                        get<SeedRepository>().setTapPoints(points)
                        startActivity(MainActivity.newIntent(this))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun Greeting(
    modifier: Modifier = Modifier,
    message: String,
    onCompleteTapping: (points: List<Float>) -> Unit,
) {
    var tappedPoints by remember { mutableStateOf(listOf<Float>()) }
    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInteropFilter { event ->
                when (event.action) {
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                        tappedPoints = tappedPoints + event.x
                        tappedPoints = tappedPoints + event.y
                        if (tappedPoints.size >= 10) {
                            onCompleteTapping(tappedPoints)
                        }
                    }
                }

                return@pointerInteropFilter true
            }
    ) {
        Text(modifier = Modifier
            .align(Alignment.Center)
            .padding(8.dp), text = message, fontSize = 18.sp)
    }
}