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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.geckour.random.ConfigRepository.Companion.DEFAULT_DIGIT
import com.geckour.random.ui.theme.DeepOrange600
import com.geckour.random.ui.theme.Green600
import com.geckour.random.ui.theme.LightBlue600
import com.geckour.random.ui.theme.Lime600
import com.geckour.random.ui.theme.Pink600
import com.geckour.random.ui.theme.RandomTheme
import org.koin.android.ext.android.get
import kotlin.math.log2
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.random.Random
import kotlin.streams.toList

private const val DIGIT_MAX = 1000

private lateinit var passwordFontFamily: FontFamily

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

        passwordFontFamily = FontFamily(Font(R.font.ricty_discord_bold, weight = FontWeight.Bold), Font(R.font.ricty_discord_regular))

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
                        customCharsetEnabled,
                        ::storeConfig
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

        storeConfig()
    }

    private fun restoreConfig() {
        digit.value = configRepository.getDigit()
        charsetKinds.value = configRepository.getEnabledCharsetKinds()
        customCharset.value = configRepository.getCustomCharset()
        customCharsetEnabled.value = configRepository.getCustomCharsetEnabled()
    }

    private fun storeConfig() {
        configRepository.setDigit(digit.value)
        configRepository.setEnabledCharsetKinds(charsetKinds.value)
        configRepository.setCustomCharset(customCharset.value)
        configRepository.setCustomCharsetEnabled(customCharsetEnabled.value)
    }
}

@Composable
fun Generator(
    seed: Long,
    digit: MutableState<Int>,
    charsetKinds: MutableState<List<CharsetKind>>,
    customCharset: MutableState<String>,
    customCharsetEnabled: MutableState<Boolean>,
    onStoreConfig: () -> Unit,
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
        Generate {
            onStoreConfig()
            generateInvoked.value = System.currentTimeMillis()
        }
        PasswordDisplay(
            password = makePassword(
                seed = seed,
                digit = digit.value,
                charsetKinds = charsetKinds.value,
                customCharset = if (customCharsetEnabled.value) customCharset.value else "",
                forceGenerate = generateInvoked.value
            ).apply {
                wrappedPassword.value = ""
                counter.value = 0
            },
            wrappedPassword = wrappedPassword,
            counter = counter,
            onCopyPassword = onCopyPassword,
            digit = digit.value,
            charsetKindCount = (charsetKinds.value.flatMap { it.charset }.map { it.code } + if (customCharsetEnabled.value) customCharset.value
                .codePoints()
                .toList() else emptyList()).distinct().size
        )
    }
}

@Composable
fun Digit(digit: MutableState<Int>) {
    val normalTextColor = MaterialTheme.colors.onBackground
    var text by remember { mutableStateOf(digit.value.toString()) }
    var textColor by remember { mutableStateOf(normalTextColor) }
    OutlinedTextField(
        modifier = Modifier.fillMaxWidth(),
        value = text,
        onValueChange = {
            text = it
            it.toIntOrNull()?.let { numbered ->
                digit.value = numbered
                textColor = if (numbered in 1..DIGIT_MAX) normalTextColor else Color.Red
            } ?: run {
                digit.value = 0
                textColor = Color.Red
            }
        },
        label = { Text(text = stringResource(R.string.label_password_length, DIGIT_MAX)) },
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
    digit: Int,
    charsetKindCount: Int,
    password: String?,
    wrappedPassword: MutableState<String>,
    counter: MutableState<Int>,
    onCopyPassword: (password: String) -> Unit
) {
    if (password.isNullOrBlank()) return

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 48.dp)
    ) {
        val entropy = calcPasswordEntropy(digit, charsetKindCount)
        val (strengthMessageRes, strengthMessageColor) = when (entropy) {
            null -> {
                R.string.message_strength_very_strong to LightBlue600
            }
            in 0f..27.9f -> {
                R.string.message_strength_very_weak to Pink600
            }
            in 28f..35.9f -> {
                R.string.message_strength_weak to DeepOrange600
            }
            in 36f..59.9f -> {
                R.string.message_strength_reasonable to Lime600
            }
            in 60f..127.9f -> {
                R.string.message_strength_strong to Green600
            }
            else -> {
                R.string.message_strength_very_strong to LightBlue600
            }
        }

        Button(
            modifier = Modifier.align(Alignment.CenterHorizontally),
            onClick = { onCopyPassword(password) }) {
            Text(text = stringResource(R.string.label_button_copy), fontWeight = FontWeight.Bold)
        }
        Text(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            text = stringResource(strengthMessageRes, entropy ?: Float.POSITIVE_INFINITY),
            color = strengthMessageColor,
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )
        SelectionContainer(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 4.dp)
        ) {
            val codePoints = password.codePoints().toList()
            Text(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                text = wrappedPassword.value,
                fontSize = 20.sp,
                fontFamily = passwordFontFamily,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                softWrap = false,
                onTextLayout = {
                    if (counter.value < codePoints.size) {
                        if (it.didOverflowWidth) {
                            val droppedCodePoints = wrappedPassword.value.codePoints().toList().dropLast(1).toIntArray()
                            wrappedPassword.value = String(droppedCodePoints, 0, droppedCodePoints.size) + '\n'
                            counter.value--
                        }
                        wrappedPassword.value += String(intArrayOf(codePoints[counter.value++]), 0, 1)
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
    customCharset: String = "",
    forceGenerate: Long
): String? {
    val random = Random(seed + System.currentTimeMillis())
    val charSet = (customCharset + charsetKinds.map { it.charset }.flatten().joinToString(""))
    var result = ""
    if (charSet.isEmpty() || digit < (charsetKinds.size + if (customCharset.isEmpty()) 0 else 1) || digit > DIGIT_MAX) return null

    val codePoints = charSet.codePoints().toList()
    while (result.containsAllCharsets(charsetKinds, customCharset).not()) {
        result = ""
        repeat(digit) {
            result += String(intArrayOf(codePoints[random.nextInt(codePoints.size)]), 0, 1)
        }
    }

    return result
}

private fun calcPasswordEntropy(digit: Int, charsetKindCount: Int): Float? = charsetKindCount.toDouble().pow(digit).let {
    if (it.isInfinite()) null else (log2(it) * 10).roundToInt() / 10f
}

private fun String.containsAllCharsets(charsetKinds: List<CharsetKind>, customCharset: String): Boolean {
    charsetKinds.forEach { charSetKind ->
        if (charSetKind.charset.all { this.contains(it).not() }) return false
    }
    val inputCodePoints = this.codePoints().toList()
    if (customCharset.isNotEmpty() && customCharset.codePoints().toList().all { inputCodePoints.contains(it).not() }) return false

    return true
}