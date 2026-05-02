package com.chemmaze.engine

import com.chemmaze.model.Atom
import com.chemmaze.model.Bond
import com.chemmaze.model.Level
import com.chemmaze.model.ReactiveCell

class GameEngine(val level: Level) {

    val atoms: MutableList<Atom> = level.atoms.map { it.copy() }.toMutableList()

    // Live reactive cells (tracks remaining uses)
    val reactiveCells: MutableList<ReactiveCell> =
        level.reactiveCells.map { it.copy().also { c -> c.usesLeft = it.uses } }.toMutableList()

    var moveCount: Int = 0
        private set

    var selectedAtomId: Int = -1

    // Snapshot for undo: atoms + reactive cell states
    private data class Snapshot(
        val atoms: List<Atom>,
        val cellUses: List<Int>   // usesLeft per cell, same order as reactiveCells
    )
    private val history: ArrayDeque<Snapshot> = ArrayDeque()

    // ── Queries ───────────────────────────────────────────────────────────────

    fun atomAt(row: Int, col: Int): Atom? =
        atoms.find { it.row == row && it.col == col }

    fun isWall(row: Int, col: Int): Boolean =
        level.walls.contains(Pair(row, col))

    fun inBounds(row: Int, col: Int): Boolean =
        row in 0 until level.rows && col in 0 until level.cols

    fun reactiveCellAt(row: Int, col: Int): ReactiveCell? =
        reactiveCells.find { it.row == row && it.col == col && it.usesLeft != 0 }

    // ── Direction ─────────────────────────────────────────────────────────────

    enum class Direction(val dr: Int, val dc: Int) {
        UP(-1, 0), DOWN(1, 0), LEFT(0, -1), RIGHT(0, 1)
    }

    // ── Movement ──────────────────────────────────────────────────────────────

    /**
     * Slides [atomId] in [dir] until hitting a wall, another atom, or a
     * blocking reactive cell. Fires reactions on every cell passed through
     * AND the final landing cell.
     *
     * Returns true if the atom moved at all.
     */
    fun slide(atomId: Int, dir: Direction): Boolean {
        val atom = atoms.find { it.id == atomId } ?: return false

        // Collect the path cells (excluding start)
        val path = mutableListOf<Pair<Int,Int>>()
        var r = atom.row; var c = atom.col
        while (true) {
            val nr = r + dir.dr; val nc = c + dir.dc
            if (!inBounds(nr, nc)) break
            if (isWall(nr, nc)) break
            if (atomAt(nr, nc) != null) break

            // Check if reactive cell blocks this atom
            val rc = reactiveCellAt(nr, nc)
            if (rc != null && rc.type == "block") break
            if (rc != null && rc.type == "filter_H" && atom.hydrogens == 0) break

            path.add(Pair(nr, nc))
            r = nr; c = nc
        }

        if (path.isEmpty()) return false

        // Save snapshot before move
        saveSnapshot()

        // Fire reactions on each cell in path
        for ((pr, pc) in path) {
            val rc = reactiveCellAt(pr, pc)
            if (rc != null) fireReaction(atom, rc)
        }

        atom.row = r; atom.col = c
        moveCount++
        return true
    }

    private fun fireReaction(atom: Atom, cell: ReactiveCell) {
        when (cell.type) {
            "add_H", "acid" -> {
                if (atom.hydrogens < atom.maxHydrogens) atom.hydrogens++
            }
            "remove_H", "base" -> {
                if (atom.hydrogens > 0) atom.hydrogens--
            }
            // "add_O", "block", "filter_H" handled elsewhere or no-op here
        }
        if (cell.uses > 0) cell.usesLeft--
    }

    // ── Undo ──────────────────────────────────────────────────────────────────

    private fun saveSnapshot() {
        history.addLast(Snapshot(
            atoms = atoms.map { it.copy() },
            cellUses = reactiveCells.map { it.usesLeft }
        ))
    }

    fun undo(): Boolean {
        if (history.isEmpty()) return false
        val snap = history.removeLast()
        atoms.clear(); atoms.addAll(snap.atoms.map { it.copy() })
        snap.cellUses.forEachIndexed { i, u -> reactiveCells[i].usesLeft = u }
        moveCount--
        return true
    }

    fun reset() {
        atoms.clear(); atoms.addAll(level.atoms.map { it.copy() })
        reactiveCells.forEachIndexed { i, rc ->
            rc.usesLeft = level.reactiveCells[i].uses
        }
        history.clear(); moveCount = 0; selectedAtomId = -1
    }

    // ── Win Check ─────────────────────────────────────────────────────────────

    /**
     * Win = all bonds satisfied AND atom labels match what bonds expect.
     * Each bond checks:
     *   1. atom[fromId] is adjacent to atom[toId] in fromSide direction
     *   2. (optional future: label check)
     */
    fun checkWin(): Boolean = level.bonds.all { isBondSatisfied(it) }

    fun isBondSatisfied(bond: Bond): Boolean {
        val a = atoms.find { it.id == bond.fromId } ?: return false
        val b = atoms.find { it.id == bond.toId }   ?: return false
        return when (bond.fromSide) {
            "right" -> b.row == a.row && b.col == a.col + 1
            "left"  -> b.row == a.row && b.col == a.col - 1
            "down"  -> b.col == a.col && b.row == a.row + 1
            "up"    -> b.col == a.col && b.row == a.row - 1
            else    -> false
        }
    }

    fun unsatisfiedAtomIds(): Set<Int> {
        val result = mutableSetOf<Int>()
        for (bond in level.bonds) {
            if (!isBondSatisfied(bond)) {
                result.add(bond.fromId); result.add(bond.toId)
            }
        }
        return result
    }
}
