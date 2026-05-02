package com.chemmaze.model

/**
 * A special reactive cell on the grid.
 *
 * When an atom slides INTO this cell, the reaction fires immediately.
 *
 * [type] determines what happens:
 *   "add_H"    → hydrogens++ (capped at maxHydrogens)
 *   "remove_H" → hydrogens-- (capped at 0)
 *   "add_O"    → replaces atom with O variant — not supported yet, reserved
 *   "acid"     → alias for add_H (proton donor)
 *   "base"     → alias for remove_H (proton acceptor)
 *   "block"    → atom cannot pass through (acts like wall for ALL atoms)
 *   "filter_H" → only atoms with hydrogens > 0 can pass; others are blocked
 *
 * [row],[col] grid position of this cell
 * [uses]      how many times this cell can fire (-1 = unlimited)
 */
data class ReactiveCell(
    val row: Int,
    val col: Int,
    val type: String,
    val uses: Int = -1
) {
    var usesLeft: Int = uses
}
