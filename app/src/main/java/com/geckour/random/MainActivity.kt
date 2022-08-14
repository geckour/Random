package com.geckour.random

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.icu.text.BreakIterator
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.Checkbox
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onPlaced
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.android.ext.android.get
import kotlin.math.log2
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.random.Random

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

    @OptIn(ExperimentalMaterialApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        passwordFontFamily = FontFamily(Font(R.font.ricty_discord_bold, weight = FontWeight.Bold), Font(R.font.ricty_discord_regular))

        configRepository = get()
        restoreConfig()

        setContent {
            RandomTheme {
                val interactionSource = remember { MutableInteractionSource() }
                val ripple = rememberRipple()
                val rippleEnabled = remember { mutableStateOf(false) }
                val forceGenerate = remember { mutableStateOf(-1L) }
                val password = makeAndCopyPassword(
                    seed = get<SeedRepository>().getSeed(),
                    digit = digit.value,
                    charsetKinds = charsetKinds.value,
                    customCharset = if (customCharsetEnabled.value) customCharset.value else "",
                    onCopyPassword = { password ->
                        getSystemService(ClipboardManager::class.java).setPrimaryClip(
                            ClipData.newPlainText(
                                getString(R.string.label_clipboard),
                                password
                            )
                        )
                    },
                    forceGenerate = forceGenerate.value
                )
                // A surface container using the 'background' color from the theme
                Surface(color = MaterialTheme.colors.background) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable(enabled = rippleEnabled.value, indication = ripple, interactionSource = interactionSource, onClick = {})
                    )
                    Generator(
                        password,
                        digit,
                        charsetKinds,
                        customCharset,
                        customCharsetEnabled,
                        ::storeConfig,
                        interactionSource,
                        rippleEnabled,
                    ) { forceGenerate.value = System.currentTimeMillis() }
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
    password: String?,
    digit: MutableState<Int>,
    charsetKinds: MutableState<List<CharsetKind>>,
    customCharset: MutableState<String>,
    customCharsetEnabled: MutableState<Boolean>,
    onStoreConfig: () -> Unit,
    surfaceInteractionSource: MutableInteractionSource,
    rippleEnabled: MutableState<Boolean>,
    onForceGenerate: () -> Unit
) {

    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 24.dp)
            .fillMaxSize()
    ) {
        val coroutineScope = rememberCoroutineScope()
        Digit(digit = digit)
        Charsets(charsetKinds = charsetKinds)
        CustomCharsets(customCharset = customCharset, customCharsetEnabled = customCharsetEnabled)
        Entropy(
            digit = digit.value,
            charsetKindCount = (charsetKinds.value.flatMap { it.charset }.map { it.code } +
                    if (customCharsetEnabled.value) customCharset.value.toGraphemeList() else emptyList()).distinct().size
        )
        PasswordDisplay(
            password = password,
            onGenerateAndCopyPassword = { center ->
                onStoreConfig()
                onForceGenerate()
                coroutineScope.launch {
                    val press = PressInteraction.Press(center)
                    rippleEnabled.value = true
                    surfaceInteractionSource.tryEmit(press)
                    delay(250)
                    surfaceInteractionSource.tryEmit(PressInteraction.Release(press))
                    rippleEnabled.value = false
                }
            }
        )
    }
}

@Composable
fun ColumnScope.Digit(digit: MutableState<Int>) {
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
    Spacer(modifier = Modifier.height(12.dp))
    CharsetKind.values().forEach { charSetKind ->
        CharsetCheckbox(text = stringResource(charSetKind.labelResId), enabled = charsetKinds.value.contains(charSetKind)) { checked ->
            charsetKinds.value = (if (checked) charsetKinds.value + charSetKind else charsetKinds.value - charSetKind).distinct()
        }
    }
}

@Composable
fun CharsetCheckbox(text: String, enabled: Boolean, onCheckStateChange: (checked: Boolean) -> Unit) {
    var checked by remember { mutableStateOf(enabled) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                checked = checked.not()
                onCheckStateChange(checked)
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = {
                checked = it
                onCheckStateChange(it)
            }
        )
        Text(text = text)
    }
}

@Composable
fun CustomCharsets(customCharset: MutableState<String>, customCharsetEnabled: MutableState<Boolean>) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { customCharsetEnabled.value = customCharsetEnabled.value.not() }
    ) {
        Checkbox(
            modifier = Modifier.alignByBaseline(),
            checked = customCharsetEnabled.value,
            onCheckedChange = {
                customCharsetEnabled.value = it
            }
        )
        Column {
            Text(text = stringResource(R.string.label_charset_custom))
            OutlinedTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 4.dp),
                value = customCharset.value,
                maxLines = 1,
                onValueChange = {
                    customCharset.value = it
                },
                label = { Text(text = "") }
            )
        }
    }
}

@Composable
fun Entropy(digit: Int, charsetKindCount: Int) {
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
    Text(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
        text = stringResource(strengthMessageRes, entropy ?: Float.POSITIVE_INFINITY),
        color = strengthMessageColor,
        fontSize = 14.sp,
        textAlign = TextAlign.Center
    )
}

@Composable
fun PasswordDisplay(
    password: String?,
    onGenerateAndCopyPassword: (copyButtonCenter: Offset) -> Unit
) {
    var generateAndCopyButtonCenterOffset by remember { mutableStateOf(Offset(0f, 0f)) }
    var wrappedPassword by remember { mutableStateOf(password) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 24.dp)
    ) {
        if (password.isNullOrBlank().not()) {
            wrappedPassword = password

            SelectionContainer(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
                    .padding(top = 4.dp)
            ) {
                Text(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState()),
                    text = checkNotNull(wrappedPassword),
                    fontSize = 20.sp,
                    fontFamily = passwordFontFamily,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    softWrap = false,
                    onTextLayout = {
                        if (it.didOverflowWidth) {
                            var currentTextWidth = 0f
                            var result = ""
                            it.layoutInput.text.forEachIndexed { index, c ->
                                val charWidth = it.getBoundingBox(index).width
                                currentTextWidth += charWidth
                                result += if (currentTextWidth > it.layoutInput.constraints.maxWidth) {
                                    currentTextWidth = charWidth
                                    "\n$c"
                                } else {
                                    c
                                }
                            }
                            wrappedPassword = result
                        }
                    }
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(1f)
            )
        }
        Button(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .onPlaced { layoutCoordinates -> generateAndCopyButtonCenterOffset = layoutCoordinates.boundsInWindow().center },
            onClick = { onGenerateAndCopyPassword(generateAndCopyButtonCenterOffset) }
        ) {
            Text(text = stringResource(R.string.label_button_generate_and_copy), fontWeight = FontWeight.Bold)
        }
    }
}

private fun makeAndCopyPassword(
    seed: Long,
    digit: Int,
    charsetKinds: List<CharsetKind>,
    customCharset: String = "",
    onCopyPassword: (password: String) -> Unit,
    forceGenerate: Long
): String? {
    if (forceGenerate < 0) return null

    val random = Random(seed + System.currentTimeMillis())
    val charSet = (customCharset + charsetKinds.map { it.charset }.flatten().joinToString(""))
    var result = ""
    if (charSet.isEmpty() || digit < (charsetKinds.size + if (customCharset.isEmpty()) 0 else 1) || digit > DIGIT_MAX) return null

    val chars = charSet.toGraphemeList()
    while (result.containsAllCharsets(charsetKinds, customCharset).not()) {
        result = ""
        repeat(digit) {
            result += chars[random.nextInt(chars.size)]
        }
    }

    onCopyPassword(result)

    return result
}

private fun calcPasswordEntropy(digit: Int, charsetKindCount: Int): Float? =
    if (charsetKindCount == 0) 0f
    else {
        charsetKindCount.toDouble().pow(digit).let {
            if (it.isInfinite()) null else (log2(it) * 10).roundToInt() / 10f
        }
    }

private fun String.containsAllCharsets(charsetKinds: List<CharsetKind>, customCharset: String): Boolean {
    charsetKinds.forEach { charSetKind ->
        if (charSetKind.charset.all { this.contains(it).not() }) return false
    }
    if (customCharset.isNotEmpty()) {
        val chars = customCharset.toGraphemeList()
        val target = this.toGraphemeList()

        return chars.any { target.contains(it) }
    }

    return true
}

private fun String.toGraphemeList(): List<String> {
    val breakIterator = BreakIterator.getCharacterInstance().apply { setText(this@toGraphemeList) }
    val chars = mutableListOf<String>()
    var start = breakIterator.first()
    var end = breakIterator.next()
    while (end != BreakIterator.DONE) {
        chars.add(this.substring(start, end))
        start = end
        end = breakIterator.next()
    }

    return chars
}