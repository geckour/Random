package com.geckour.random

import android.content.SharedPreferences
import androidx.core.content.edit

class SeedRepository(private val preferences: SharedPreferences) {

    companion object {

        private const val KEY_SEED_INITIAL = "key_seed_initial"
        private const val KEY_SEED_POINTED = "key_seed_pointed"
    }

    fun setInitializedTime(time: Long = System.currentTimeMillis()) {
        if (preferences.contains(KEY_SEED_INITIAL).not()) {
            preferences.edit(commit = true) {
                putLong(KEY_SEED_INITIAL, time)
            }
        }
    }

    fun setTapPoints(points: List<Float>) {
        preferences.edit(commit = true) {
            putString(KEY_SEED_POINTED, points.joinToString(":"))
        }
    }

    fun setupFinished(): Boolean = preferences.contains(KEY_SEED_INITIAL) && preferences.contains(KEY_SEED_POINTED)

    fun getSeed(): Long =
        preferences.getLong(KEY_SEED_INITIAL, System.currentTimeMillis()) +
                (preferences.getString(KEY_SEED_POINTED, null)?.hashCode()?.toLong() ?: throw IllegalStateException())
}