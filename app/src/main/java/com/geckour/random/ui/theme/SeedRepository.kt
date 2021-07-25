package com.geckour.random.ui.theme

import android.content.SharedPreferences
import androidx.core.content.edit

class SeedRepository(private val preferences: SharedPreferences) {

    companion object {

        private const val KEY_INITIAL_SEED = "key_initial_seed"
        private const val KEY_POINTED_SEED = "key_pointed_seed"
    }

    fun setInitializedTime(time: Long = System.currentTimeMillis()) {
        if (preferences.contains(KEY_INITIAL_SEED).not()) {
            preferences.edit(commit = true) {
                putLong(KEY_INITIAL_SEED, time)
            }
        }
    }

    fun setTapPoints(points: List<Float>) {
        preferences.edit(commit = true) {
            putString(KEY_POINTED_SEED, points.joinToString(":"))
        }
    }

    fun setupFinished(): Boolean = preferences.contains(KEY_INITIAL_SEED) && preferences.contains(KEY_POINTED_SEED)

    fun getSeed(): Long =
        preferences.getLong(KEY_INITIAL_SEED, System.currentTimeMillis()) +
                (preferences.getString(KEY_POINTED_SEED, null)?.hashCode()?.toLong() ?: throw IllegalStateException())
}