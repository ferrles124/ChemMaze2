package com.chemmaze.engine

import android.content.Context
import com.chemmaze.model.Atom
import com.chemmaze.model.Bond
import com.chemmaze.model.Level
import com.chemmaze.model.ReactiveCell
import org.json.JSONObject

object LevelLoader {

    fun listLevelFiles(context: Context): List<String> =
        context.assets.list("levels")
            ?.filter { it.endsWith(".json") }
            ?.sorted() ?: emptyList()

    fun load(context: Context, filename: String): Level {
        val raw = context.assets.open("levels/$filename").bufferedReader().use { it.readText() }
        return parse(raw)
    }

    fun loadByIndex(context: Context, index: Int): Level {
        val files = listLevelFiles(context)
        require(index in 1..files.size) { "Level $index not found" }
        return load(context, files[index - 1])
    }

    fun parse(json: String): Level {
        val obj = JSONObject(json)

        val walls = mutableListOf<Pair<Int, Int>>()
        val wa = obj.getJSONArray("walls")
        for (i in 0 until wa.length()) {
            val w = wa.getJSONArray(i); walls.add(Pair(w.getInt(0), w.getInt(1)))
        }

        val atoms = mutableListOf<Atom>()
        val aa = obj.getJSONArray("atoms")
        for (i in 0 until aa.length()) {
            val a = aa.getJSONObject(i)
            atoms.add(Atom(
                id        = a.getInt("id"),
                symbol    = a.getString("symbol"),
                hydrogens = if (a.has("h")) a.getInt("h") else 0,
                row       = a.getInt("row"),
                col       = a.getInt("col")
            ))
        }

        val bonds = mutableListOf<Bond>()
        val ba = obj.getJSONArray("bonds")
        for (i in 0 until ba.length()) {
            val b = ba.getJSONObject(i)
            bonds.add(Bond(
                fromId   = b.getInt("from"),
                toId     = b.getInt("to"),
                fromSide = b.getString("fromSide"),
                toSide   = b.getString("toSide")
            ))
        }

        val reactiveCells = mutableListOf<ReactiveCell>()
        if (obj.has("reactive")) {
            val ra = obj.getJSONArray("reactive")
            for (i in 0 until ra.length()) {
                val r = ra.getJSONObject(i)
                reactiveCells.add(ReactiveCell(
                    row  = r.getInt("row"),
                    col  = r.getInt("col"),
                    type = r.getString("type"),
                    uses = if (r.has("uses")) r.getInt("uses") else -1
                ))
            }
        }

        return Level(
            id            = obj.getInt("id"),
            title         = obj.getString("title"),
            formula       = obj.getString("formula"),
            moleculeName  = obj.getString("moleculeName"),
            rows          = obj.getInt("rows"),
            cols          = obj.getInt("cols"),
            walls         = walls,
            atoms         = atoms,
            bonds         = bonds,
            reactiveCells = reactiveCells
        )
    }
}
