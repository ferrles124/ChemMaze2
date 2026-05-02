package com.chemmaze.model

data class Level(
    val id: Int,
    val title: String,
    val formula: String,        // display formula e.g. "CH₃OH"
    val moleculeName: String,
    val rows: Int,
    val cols: Int,
    val walls: List<Pair<Int, Int>>,
    val atoms: List<Atom>,
    val bonds: List<Bond>,
    val reactiveCells: List<ReactiveCell> = emptyList()
)
