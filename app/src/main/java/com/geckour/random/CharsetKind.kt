package com.geckour.random

import androidx.annotation.StringRes

enum class CharsetKind(val charset: List<Char>, @StringRes val labelResId: Int) {
    NUMBER((0x30..0x39).map { Char(it) }, R.string.label_charset_number),
    ALPHABET_UPPER((0x41..0x5A).map { Char(it) }, R.string.label_charset_alphabet_upper),
    ALPHABET_LOWER((0x61..0x7A).map { Char(it) }, R.string.label_charset_alphabet_lower),
    ASCII_SYMBOL((0x21..0x2F).map { Char(it) } + (0x3A..0x40).map { Char(it) } + (0x5B..0x60).map { Char(it) } + (0x7A..0x7E).map { Char(it) }, R.string.label_charset_ascii_symbol)
}