package com.geckour.random

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.Button
import androidx.compose.material.Checkbox
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.geckour.random.ui.theme.RandomTheme
import com.geckour.random.ui.theme.SeedRepository
import org.koin.android.ext.android.get
import kotlin.random.Random

class MainActivity : ComponentActivity() {

    companion object {

        fun newIntent(context: Context) = Intent(context, MainActivity::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RandomTheme {
                // A surface container using the 'background' color from the theme
                Surface(color = MaterialTheme.colors.background) {
                    Generator(seed = get<SeedRepository>().getSeed()) { password ->
                        getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText("Generated password", password))
                    }
                }
            }
        }
    }
}

private const val DEFAULT_DIGIT = 24

@Composable
fun Generator(seed: Long, onCopyPassword: (password: String) -> Unit) {
    val digit = remember { mutableStateOf(DEFAULT_DIGIT) }
    val charSetKinds = remember { mutableStateOf(CharSetKind.values().toList()) }
    val customCharSet = remember { mutableStateOf("") }
    val customCharSetEnabled = remember { mutableStateOf(false) }
    val generateInvoked = remember { mutableStateOf(0L) }

    Column(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth()
            .wrapContentHeight(align = Alignment.Top)
    ) {
        Digit(digit = digit)
        CharSets(charSetKinds = charSetKinds)
        CustomCharSets(customCharSet = customCharSet, customCharSetEnabled = customCharSetEnabled)
        Generate { generateInvoked.value = System.currentTimeMillis() }
        PasswordDisplay(
            makePassword(
                seed = seed,
                digit = digit.value,
                charSetKinds = charSetKinds.value,
                customCharSet = if (customCharSetEnabled.value) customCharSet.value.toList() else emptyList(),
                forceGenerate = generateInvoked.value
            ),
            onCopyPassword = onCopyPassword
        )
    }
}

@Composable
fun Digit(digit: MutableState<Int>) {
    var text by remember { mutableStateOf(DEFAULT_DIGIT.toString()) }
    var textColor by remember { mutableStateOf(Color.White) }
    OutlinedTextField(
        modifier = Modifier.fillMaxWidth(),
        value = text,
        onValueChange = {
            text = it
            it.toIntOrNull()?.let { numbered ->
                digit.value = numbered
                textColor = if (numbered > 0) Color.White else Color.Red
            } ?: run {
                digit.value = 0
                textColor = Color.Red
            }
        },
        label = { Text(text = "Password Length") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        textStyle = TextStyle(color = textColor)
    )
}

@Composable
fun CharSets(charSetKinds: MutableState<List<CharSetKind>>) {
    Column(modifier = Modifier.padding(top = 12.dp)) {
        CharSetKind.values().forEach { charSetKind ->
            CharSetCheckbox(text = charSetKind.name) { checked ->
                charSetKinds.value = (if (checked) charSetKinds.value + charSetKind else charSetKinds.value - charSetKind).distinct()
            }
        }
    }
}

@Composable
fun CharSetCheckbox(text: String, onCheckStateChange: (checked: Boolean) -> Unit) {
    var checked by remember { mutableStateOf(true) }
    Row(modifier = Modifier
        .fillMaxWidth()
        .clickable {
            checked = checked.not()
            onCheckStateChange(checked)
        }
    ) {
        Checkbox(
            modifier = Modifier.padding(vertical = 4.dp),
            checked = checked,
            onCheckedChange = {
                checked = it
                onCheckStateChange(it)
            }
        )
        Text(modifier = Modifier.padding(4.dp), text = text)
    }
}

@Composable
fun CustomCharSets(customCharSet: MutableState<String>, customCharSetEnabled: MutableState<Boolean>) {
    Column {
        Row(modifier = Modifier
            .fillMaxWidth()
            .clickable { customCharSetEnabled.value = customCharSetEnabled.value.not() }
        )  {
            Checkbox(
                modifier = Modifier.padding(vertical = 4.dp),
                checked = customCharSetEnabled.value,
                onCheckedChange = {
                    customCharSetEnabled.value = it
                }
            )
            Text(modifier = Modifier.padding(4.dp), text = "Custom Characters")
        }
        OutlinedTextField(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp),
            value = customCharSet.value,
            onValueChange = {
                customCharSet.value = it
            },
            label = { Text(text = "") }
        )
    }
}

@Composable
fun Generate(onGenerateInvoked: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
    ) {
        Button(modifier = Modifier.align(Alignment.CenterEnd), onClick = onGenerateInvoked) {
            Text(text = "GENERATE")
        }
    }
}

@Composable
fun PasswordDisplay(password: String, onCopyPassword: (password: String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 48.dp)
    ) {
        Button(
            modifier = Modifier.align(Alignment.CenterHorizontally),
            onClick = { onCopyPassword(password) }) {
            Text(text = "COPY")
        }
        SelectionContainer(
            modifier = Modifier
                .padding(top = 16.dp)
                .align(Alignment.CenterHorizontally)
        ) {
            Text(
                text = password,
                fontSize = 20.sp,
                fontFamily = FontFamily.Monospace,
                overflow = TextOverflow.Clip
            )
        }
    }
}

private fun makePassword(
    seed: Long,
    digit: Int,
    charSetKinds: List<CharSetKind>,
    customCharSet: List<Char> = emptyList(),
    forceGenerate: Long
): String {
    val random = Random(seed + System.currentTimeMillis())
    val charSet = (customCharSet + charSetKinds.toList().map { it.charSet }.flatten()).distinct()
    var result = ""
    if (charSet.isEmpty()) return result

    while (result.containsAllCharSets(charSetKinds, customCharSet.joinToString("")).not()) {
        result = ""
        repeat(digit) {
            result += charSet[random.nextInt(charSet.size)]
        }
    }

    return result
}

private fun String.containsAllCharSets(charSetKinds: List<CharSetKind>, customCharSet: String): Boolean {
    charSetKinds.forEach { charSetKind ->
        if (charSetKind.charSet.all { this.contains(it).not() }) return false
    }
    if (customCharSet.isNotEmpty() && customCharSet.all { this.contains(it).not() }) return false

    return true
}