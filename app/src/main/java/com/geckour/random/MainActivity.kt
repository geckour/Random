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
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.geckour.random.ConfigRepository.Companion.DEFAULT_DIGIT
import com.geckour.random.ui.theme.RandomTheme
import org.koin.android.ext.android.get
import kotlin.random.Random

class MainActivity : ComponentActivity() {

    companion object {

        fun newIntent(context: Context) = Intent(context, MainActivity::class.java)
    }

    private val digit = mutableStateOf(DEFAULT_DIGIT)
    private val charsetKinds = mutableStateOf(CharsetKind.values().toList())
    private val customCharset = mutableStateOf("")
    private val customCharsetEnabled = mutableStateOf(false)

    private lateinit var configRepository: ConfigRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        configRepository = get()
        restoreConfig()

        setContent {
            RandomTheme {
                // A surface container using the 'background' color from the theme
                Surface(color = MaterialTheme.colors.background) {
                    Generator(
                        get<SeedRepository>().getSeed(),
                        digit,
                        charsetKinds,
                        customCharset,
                        customCharsetEnabled
                    ) { password ->
                        getSystemService(ClipboardManager::class.java).setPrimaryClip(
                            ClipData.newPlainText(
                                getString(R.string.label_clipboard),
                                password
                            )
                        )
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()

        restoreConfig()
    }

    override fun onPause() {
        super.onPause()

        configRepository.setDigit(digit.value)
        configRepository.setEnabledCharsetKinds(charsetKinds.value)
        configRepository.setCustomCharset(customCharset.value)
        configRepository.setCustomCharsetEnabled(customCharsetEnabled.value)
    }

    private fun restoreConfig() {
        digit.value = configRepository.getDigit()
        charsetKinds.value = configRepository.getEnabledCharsetKinds()
        customCharset.value = configRepository.getCustomCharset()
        customCharsetEnabled.value = configRepository.getCustomCharsetEnabled()
    }
}

@Composable
fun Generator(
    seed: Long,
    digit: MutableState<Int>,
    charsetKinds: MutableState<List<CharsetKind>>,
    customCharset: MutableState<String>,
    customCharsetEnabled: MutableState<Boolean>,
    onCopyPassword: (password: String) -> Unit
) {
    val generateInvoked = remember { mutableStateOf(0L) }

    val wrappedPassword = remember { mutableStateOf("") }
    val counter = remember { mutableStateOf(0) }

    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 48.dp)
            .fillMaxWidth()
            .wrapContentHeight(align = Alignment.Top)
    ) {
        Digit(digit = digit)
        Charsets(charsetKinds = charsetKinds)
        CustomCharsets(customCharset = customCharset, customCharsetEnabled = customCharsetEnabled)
        Generate { generateInvoked.value = System.currentTimeMillis() }
        PasswordDisplay(
            password = makePassword(
                seed = seed,
                digit = digit.value,
                charsetKinds = charsetKinds.value,
                customCharset = if (customCharsetEnabled.value) customCharset.value.toList() else emptyList(),
                forceGenerate = generateInvoked.value
            ).apply {
                wrappedPassword.value = ""
                counter.value = 0
            },
            wrappedPassword = wrappedPassword,
            counter = counter,
            onCopyPassword = onCopyPassword
        )
    }
}

@Composable
fun Digit(digit: MutableState<Int>) {
    var text by remember { mutableStateOf(digit.value.toString()) }
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
        label = { Text(text = stringResource(R.string.label_password_length)) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        textStyle = TextStyle(color = textColor)
    )
}

@Composable
fun Charsets(charsetKinds: MutableState<List<CharsetKind>>) {
    Column(modifier = Modifier.padding(top = 12.dp)) {
        CharsetKind.values().forEach { charSetKind ->
            CharsetCheckbox(text = stringResource(charSetKind.labelResId), enabled = charsetKinds.value.contains(charSetKind)) { checked ->
                charsetKinds.value = (if (checked) charsetKinds.value + charSetKind else charsetKinds.value - charSetKind).distinct()
            }
        }
    }
}

@Composable
fun CharsetCheckbox(text: String, enabled: Boolean, onCheckStateChange: (checked: Boolean) -> Unit) {
    var checked by remember { mutableStateOf(enabled) }
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
fun CustomCharsets(customCharset: MutableState<String>, customCharsetEnabled: MutableState<Boolean>) {
    Column {
        Row(modifier = Modifier
            .fillMaxWidth()
            .clickable { customCharsetEnabled.value = customCharsetEnabled.value.not() }
        ) {
            Checkbox(
                modifier = Modifier.padding(vertical = 4.dp),
                checked = customCharsetEnabled.value,
                onCheckedChange = {
                    customCharsetEnabled.value = it
                }
            )
            Text(modifier = Modifier.padding(4.dp), text = stringResource(R.string.label_charset_custom))
        }
        OutlinedTextField(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp),
            value = customCharset.value,
            onValueChange = {
                customCharset.value = it
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
            .padding(top = 16.dp)
    ) {
        Button(modifier = Modifier.align(Alignment.CenterEnd), onClick = onGenerateInvoked) {
            Text(text = stringResource(R.string.label_button_generate), fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun PasswordDisplay(
    password: String?,
    wrappedPassword: MutableState<String>,
    counter: MutableState<Int>,
    onCopyPassword: (password: String) -> Unit
) {
    if (password.isNullOrBlank()) return
    val passwordFontSize = 20.sp
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 48.dp)
    ) {
        Button(
            modifier = Modifier.align(Alignment.CenterHorizontally),
            onClick = { onCopyPassword(password) }) {
            Text(text = stringResource(R.string.label_button_copy), fontWeight = FontWeight.Bold)
        }
        SelectionContainer(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 16.dp)
        ) {
            Text(
                text = wrappedPassword.value,
                fontSize = passwordFontSize,
                fontFamily = FontFamily.Monospace,
                textAlign = TextAlign.Center,
                softWrap = false,
                onTextLayout = {
                    if (counter.value < password.length) {
                        if (it.didOverflowWidth) {
                            wrappedPassword.value = wrappedPassword.value.dropLast(1) + '\n'
                            counter.value--
                        }
                        wrappedPassword.value += password[counter.value++]
                    }
                }
            )
        }
    }
}

private fun makePassword(
    seed: Long,
    digit: Int,
    charsetKinds: List<CharsetKind>,
    customCharset: List<Char> = emptyList(),
    forceGenerate: Long
): String? {
    val random = Random(seed + System.currentTimeMillis())
    val charSet = (customCharset + charsetKinds.map { it.charset }.flatten()).distinct()
    var result = ""
    if (charSet.isEmpty() || digit < (charsetKinds.size + if (customCharset.isEmpty()) 0 else 1)) return null

    while (result.containsAllCharsets(charsetKinds, customCharset.joinToString("")).not()) {
        result = ""
        repeat(digit) {
            result += charSet[random.nextInt(charSet.size)]
        }
    }

    return result
}

private fun String.containsAllCharsets(charsetKinds: List<CharsetKind>, customCharset: String): Boolean {
    charsetKinds.forEach { charSetKind ->
        if (charSetKind.charset.all { this.contains(it).not() }) return false
    }
    if (customCharset.isNotEmpty() && customCharset.all { this.contains(it).not() }) return false

    return true
}