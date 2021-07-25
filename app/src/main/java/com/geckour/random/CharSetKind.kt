package com.geckour.random

enum class CharSetKind(val charSet: List<Char>) {
    NUMBER((0x30..0x39).map { Char(it) }),
    ALPHABET((0x41..0x5A).map { Char(it) } + (0x61..0x7A).map { Char(it) }),
    ASCII_SYMBOL((0x21..0x2F).map { Char(it) } + (0x3A..0x40).map { Char(it) } + (0x5B..0x60).map { Char(it) } + (0x7A..0x7E).map { Char(it) })
}