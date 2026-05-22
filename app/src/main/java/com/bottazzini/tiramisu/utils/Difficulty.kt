package com.bottazzini.tiramisu.utils

enum class Difficulty(
    val key: String,
    val displayName: String,
    val redeals: Int,
    val obbligato: Boolean,
    val strictTableau: Boolean,
    /**
     * PoC D: when true, a drag onto an empty pile transfers only the top card
     * even if a longer run would otherwise be eligible. Used to make empty
     * piles less of a free "unloading bay" at Normale/Difficile.
     */
    val emptyPileSingleCard: Boolean
) {
    FACILE   ("facile",    "🟢 Facile",    redeals = 1, obbligato = false, strictTableau = false, emptyPileSingleCard = false),
    NORMALE  ("normale",   "🟡 Normale",   redeals = 1, obbligato = false, strictTableau = true,  emptyPileSingleCard = true),
    DIFFICILE("difficile", "🔴 Difficile", redeals = 0, obbligato = true,  strictTableau = true,  emptyPileSingleCard = true);

    companion object {
        fun fromKey(key: String): Difficulty =
            entries.firstOrNull { it.key == key } ?: NORMALE
    }
}
