package com.chemmaze.model

/**
 * An atom on the grid.
 *
 * [symbol]    Base element: "C", "O", "N", "S", "P", "Cl", "F" …
 * [hydrogens] Number of H atoms currently attached (0–4)
 * [row],[col] Grid position
 *
 * Display label = symbol + (if hydrogens>0 → "H" + subscript)
 * e.g.  symbol="C", hydrogens=3  →  "CH₃"
 *       symbol="O", hydrogens=1  →  "OH"
 *       symbol="N", hydrogens=2  →  "NH₂"
 */
data class Atom(
    val id: Int,
    val symbol: String,
    var hydrogens: Int = 0,
    var row: Int,
    var col: Int
) {
    /** Human-readable label shown on the atom circle */
    val label: String get() = when (hydrogens) {
        0    -> symbol
        1    -> "${symbol}H"
        else -> "${symbol}H${hydrogens.toSubscript()}"
    }

    /** Max hydrogens this element can carry (valence-based cap) */
    val maxHydrogens: Int get() = when (symbol) {
        "C"  -> 4
        "N"  -> 3
        "O"  -> 2
        "S"  -> 2
        "P"  -> 3
        "Cl" -> 1
        "F"  -> 1
        else -> 0
    }
}

private fun Int.toSubscript(): String = when (this) {
    2    -> "₂"
    3    -> "₃"
    4    -> "₄"
    else -> this.toString()
}
