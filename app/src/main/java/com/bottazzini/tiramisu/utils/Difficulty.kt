package com.bottazzini.tiramisu.utils

enum class Difficulty(
    val key: String,
    val displayName: String,
    val redeals: Int,
    val obbligato: Boolean
) {
    FACILE("facile", "🟢 Facile", redeals = 2, obbligato = false),
    NORMALE("normale", "🟡 Normale", redeals = 1, obbligato = false),
    DIFFICILE("difficile", "🔴 Difficile", redeals = 1, obbligato = true);

    companion object {
        fun fromKey(key: String): Difficulty =
            entries.firstOrNull { it.key == key } ?: NORMALE
    }
}
