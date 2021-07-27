package com.geckour.random

import android.content.SharedPreferences
import androidx.core.content.edit
import timber.log.Timber

class ConfigRepository(private val preferences: SharedPreferences) {

    companion object {

        private const val KEY_CONFIG_DIGIT = "key_config_digit"
        private const val KEY_CONFIG_ENABLED_CHARSET_KINDS = "key_config_charset_kinds_enabled"
        private const val KEY_CONFIG_CUSTOM_CHARSET = "key_config_custom_charset"
        private const val KEY_CONFIG_CUSTOM_CHARSET_ENABLED = "key_config_custom_charset_enabled"

        const val DEFAULT_DIGIT = 24
        private val DEFAULT_ENABLED_CHARSET_KINDS = listOf(
            CharsetKind.NUMBER,
            CharsetKind.ALPHABET_UPPER,
            CharsetKind.ALPHABET_LOWER,
            CharsetKind.ASCII_SYMBOL
        )
    }

    fun setDigit(digit: Int) {
        preferences.edit(commit = true) {
            putInt(KEY_CONFIG_DIGIT, digit)
        }
    }

    fun getDigit(): Int = preferences.getInt(KEY_CONFIG_DIGIT, DEFAULT_DIGIT)

    fun setEnabledCharsetKinds(charsetKinds: List<CharsetKind>) {
        preferences.edit(commit = true) {
            putStringSet(KEY_CONFIG_ENABLED_CHARSET_KINDS, charsetKinds.map { it.name }.toSet().let { if (it.isEmpty()) setOf("") else it })
        }
    }

    fun getEnabledCharsetKinds(): List<CharsetKind> =
        preferences.getStringSet(KEY_CONFIG_ENABLED_CHARSET_KINDS, null)?.mapNotNull {
            try {
                CharsetKind.valueOf(it)
            } catch (t: Throwable) {
                Timber.e(t)
                null
            }
        } ?: DEFAULT_ENABLED_CHARSET_KINDS

    fun setCustomCharset(customCharset: String) {
        preferences.edit(commit = true) {
            putString(KEY_CONFIG_CUSTOM_CHARSET, customCharset)
        }
    }

    fun getCustomCharset(): String = preferences.getString(KEY_CONFIG_CUSTOM_CHARSET, null) ?: ""

    fun setCustomCharsetEnabled(enabled: Boolean) {
        preferences.edit(commit = true) {
            putBoolean(KEY_CONFIG_CUSTOM_CHARSET_ENABLED, enabled)
        }
    }

    fun getCustomCharsetEnabled(): Boolean = preferences.getBoolean(KEY_CONFIG_CUSTOM_CHARSET_ENABLED, false)
}