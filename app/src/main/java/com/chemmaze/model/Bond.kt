package com.chemmaze.model

/**
 * A required directional adjacency between two atoms.
 *
 * [fromSide] which face of atom [fromId] the bond exits  ("right"|"left"|"up"|"down")
 * [toSide]   which face of atom [toId]   the bond enters ("right"|"left"|"up"|"down")
 *
 * Win condition: atom[fromId] must be in the exact cell indicated by fromSide
 * relative to atom[toId].
 */
data class Bond(
    val fromId: Int,
    val toId: Int,
    val fromSide: String,
    val toSide: String
)
