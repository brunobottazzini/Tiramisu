package com.bottazzini.tiramisu.utils

enum class Difficulty(
    val key: String,
    val displayName: String,
    val redeals: Int,
    val obbligato: Boolean,
    val strictTableau: Boolean
) {
    FACILE   ("facile",    "🟢 Facile",    redeals = 1, obbligato = false, strictTableau = false),
    NORMALE  ("normale",   "🟡 Normale",   redeals = 1, obbligato = false, strictTableau = true),
    DIFFICILE("difficile", "🔴 Difficile", redeals = 0, obbligato = true,  strictTableau = true);

    companion object {
        fun fromKey(key: String): Difficulty =
            entries.firstOrNull { it.key == key } ?: NORMALE
    }
}
