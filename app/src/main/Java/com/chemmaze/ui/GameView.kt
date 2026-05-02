package com.chemmaze.ui

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import com.chemmaze.engine.GameEngine
import com.chemmaze.model.Atom
import com.chemmaze.model.Bond
import com.chemmaze.model.ReactiveCell
import kotlin.math.abs
import kotlin.math.min

class GameView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    var engine: GameEngine? = null
        set(value) { field = value; calculateCell(); invalidate() }

    var onWin: (() -> Unit)? = null
    var onMoveCountChanged: ((Int) -> Unit)? = null

    private var cellSize = 0f
    private var offsetX = 0f
    private var offsetY = 0f

    // Animation
    private var animAtomId = -1
    private var animFromR = 0f; private var animFromC = 0f
    private var animToR = 0f;   private var animToC = 0f
    private var animFrac = 1f
    var isAnimating = false
        private set

    // Touch
    private var touchStartX = 0f; private var touchStartY = 0f

    // ── Paints ────────────────────────────────────────────────────────────────
    private val pBg    = Paint().apply { color = Color.parseColor("#0d1220") }
    private val pGrid  = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#0a0f1e"); style = Paint.Style.STROKE; strokeWidth = 1f }
    private val pWall  = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#1a2640") }
    private val pWallB = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#2a4070"); style = Paint.Style.STROKE; strokeWidth = 1.5f }

    private val pBondOk = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#69f0ae"); style = Paint.Style.STROKE
        strokeWidth = 7f; strokeCap = Paint.Cap.ROUND }
    private val pBondNo = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#2a3a4a"); style = Paint.Style.STROKE
        strokeWidth = 4f; strokeCap = Paint.Cap.ROUND
        pathEffect = DashPathEffect(floatArrayOf(8f, 6f), 0f) }
    private val pStub   = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeWidth = 5f; strokeCap = Paint.Cap.ROUND }

    private val pText   = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER; typeface = Typeface.DEFAULT_BOLD }
    private val pSubText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER; typeface = Typeface.DEFAULT_BOLD }

    // Reactive cell colors by type
    private val reactiveColors = mapOf(
        "add_H"    to Pair(Color.parseColor("#003a50"), Color.parseColor("#00bcd4")),
        "remove_H" to Pair(Color.parseColor("#3a0010"), Color.parseColor("#f06292")),
        "acid"     to Pair(Color.parseColor("#003a50"), Color.parseColor("#00bcd4")),
        "base"     to Pair(Color.parseColor("#1a003a"), Color.parseColor("#ce93d8")),
        "block"    to Pair(Color.parseColor("#2a1000"), Color.parseColor("#ff7043")),
        "filter_H" to Pair(Color.parseColor("#002a00"), Color.parseColor("#66bb6a")),
    )

    // Reactive cell label
    private val reactiveLabels = mapOf(
        "add_H"    to "+H",
        "remove_H" to "-H",
        "acid"     to "H⁺",
        "base"     to "OH⁻",
        "block"    to "✕",
        "filter_H" to "H?",
    )

    // Atom colors: bg, border, text
    private val atomColors = mapOf(
        "C"  to Triple(Color.parseColor("#6a2800"), Color.parseColor("#ff6b35"), Color.parseColor("#ffcba8")),
        "O"  to Triple(Color.parseColor("#3a0a0a"), Color.parseColor("#ef5350"), Color.parseColor("#ff8a80")),
        "N"  to Triple(Color.parseColor("#200a3a"), Color.parseColor("#ab47bc"), Color.parseColor("#ce93d8")),
        "S"  to Triple(Color.parseColor("#302000"), Color.parseColor("#ffca28"), Color.parseColor("#ffee58")),
        "P"  to Triple(Color.parseColor("#1a2000"), Color.parseColor("#aed581"), Color.parseColor("#dcedc8")),
        "Cl" to Triple(Color.parseColor("#003020"), Color.parseColor("#26a69a"), Color.parseColor("#80cbc4")),
        "F"  to Triple(Color.parseColor("#002028"), Color.parseColor("#00e5ff"), Color.parseColor("#80f5ff")),
    )
    private fun atomColor(sym: String) =
        atomColors[sym] ?: Triple(Color.DKGRAY, Color.GRAY, Color.WHITE)

    // ── Layout ────────────────────────────────────────────────────────────────
    override fun onSizeChanged(w: Int, h: Int, ow: Int, oh: Int) { super.onSizeChanged(w,h,ow,oh); calculateCell() }

    private fun calculateCell() {
        val eng = engine ?: return
        val lv = eng.level
        cellSize = min(width * 0.96f / lv.cols, height * 0.96f / lv.rows)
        offsetX = (width - cellSize * lv.cols) / 2f
        offsetY = (height - cellSize * lv.rows) / 2f
    }

    // ── Draw ──────────────────────────────────────────────────────────────────
    override fun onDraw(canvas: Canvas) {
        val eng = engine ?: return
        canvas.drawPaint(pBg)
        drawGrid(canvas, eng)
        drawWalls(canvas, eng)
        drawReactiveCells(canvas, eng)
        drawBondLines(canvas, eng)
        drawBondStubs(canvas, eng)
        drawAtoms(canvas, eng)
    }

    private fun drawGrid(canvas: Canvas, eng: GameEngine) {
        val lv = eng.level
        for (r in 0..lv.rows) {
            val y = offsetY + r * cellSize
            canvas.drawLine(offsetX, y, offsetX + lv.cols * cellSize, y, pGrid)
        }
        for (c in 0..lv.cols) {
            val x = offsetX + c * cellSize
            canvas.drawLine(x, offsetY, x, offsetY + lv.rows * cellSize, pGrid)
        }
    }

    private fun drawWalls(canvas: Canvas, eng: GameEngine) {
        for ((r, c) in eng.level.walls) {
            val x = offsetX + c * cellSize; val y = offsetY + r * cellSize
            canvas.drawRect(x, y, x + cellSize, y + cellSize, pWall)
            canvas.drawRect(x+1f, y+1f, x+cellSize-1f, y+cellSize-1f, pWallB)
        }
    }

    private fun drawReactiveCells(canvas: Canvas, eng: GameEngine) {
        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.CENTER; typeface = Typeface.DEFAULT_BOLD
            textSize = cellSize * 0.28f
        }
        val usedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.CENTER
            textSize = cellSize * 0.18f
            color = Color.parseColor("#445566")
        }

        for (rc in eng.reactiveCells) {
            val x = offsetX + rc.col * cellSize
            val y = offsetY + rc.row * cellSize
            val (bgCol, borderCol) = reactiveColors[rc.type]
                ?: Pair(Color.parseColor("#1a1a2a"), Color.parseColor("#445566"))

            val depleted = rc.usesLeft == 0
            val alpha = if (depleted) 80 else 255

            // Background
            val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = bgCol; this.alpha = alpha
            }
            canvas.drawRect(x+2f, y+2f, x+cellSize-2f, y+cellSize-2f, bgPaint)

            // Border (dashed)
            val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = borderCol; this.alpha = alpha
                style = Paint.Style.STROKE; strokeWidth = 2f
                pathEffect = DashPathEffect(floatArrayOf(6f, 4f), 0f)
            }
            canvas.drawRect(x+3f, y+3f, x+cellSize-3f, y+cellSize-3f, borderPaint)

            // Label
            val label = if (depleted) "✕" else (reactiveLabels[rc.type] ?: rc.type)
            labelPaint.color = if (depleted) Color.parseColor("#445566") else borderCol
            canvas.drawText(label, x + cellSize / 2f,
                y + cellSize / 2f - (labelPaint.descent() + labelPaint.ascent()) / 2f, labelPaint)

            // Uses left indicator
            if (rc.uses > 0 && !depleted) {
                canvas.drawText("×${rc.usesLeft}", x + cellSize - cellSize*0.18f,
                    y + cellSize - cellSize*0.1f, usedPaint)
            }
        }
    }

    private fun drawBondLines(canvas: Canvas, eng: GameEngine) {
        for (bond in eng.level.bonds) {
            val a = eng.atoms.find { it.id == bond.fromId } ?: continue
            val b = eng.atoms.find { it.id == bond.toId }   ?: continue
            val (arF, acF) = animPos(a)
            val (brF, bcF) = animPos(b)
            val ax = offsetX + (acF + 0.5f) * cellSize
            val ay = offsetY + (arF + 0.5f) * cellSize
            val bx = offsetX + (bcF + 0.5f) * cellSize
            val by = offsetY + (brF + 0.5f) * cellSize
            val dr = abs(arF - brF); val dc = abs(acF - bcF)
            if (dr < 1.5f && dc < 1.5f && (dr < 0.1f || dc < 0.1f)) {
                canvas.drawLine(ax, ay, bx, by, if (eng.isBondSatisfied(bond)) pBondOk else pBondNo)
            }
        }
    }

    private fun drawBondStubs(canvas: Canvas, eng: GameEngine) {
        val stubLen = cellSize * 0.20f
        val atomR   = cellSize * 0.36f
        for (bond in eng.level.bonds) {
            val a = eng.atoms.find { it.id == bond.fromId } ?: continue
            val b = eng.atoms.find { it.id == bond.toId }   ?: continue
            val sat = eng.isBondSatisfied(bond)
            val col = if (sat) Color.parseColor("#69f0ae") else Color.parseColor("#445566")
            val (arF, acF) = animPos(a)
            drawStub(canvas, offsetX+(acF+0.5f)*cellSize, offsetY+(arF+0.5f)*cellSize, bond.fromSide, atomR, stubLen, col)
            val (brF, bcF) = animPos(b)
            drawStub(canvas, offsetX+(bcF+0.5f)*cellSize, offsetY+(brF+0.5f)*cellSize, bond.toSide, atomR, stubLen, col)
        }
    }

    private fun drawStub(canvas: Canvas, cx: Float, cy: Float, side: String,
                         atomR: Float, len: Float, color: Int) {
        pStub.color = color
        when (side) {
            "right" -> canvas.drawLine(cx+atomR, cy, cx+atomR+len, cy, pStub)
            "left"  -> canvas.drawLine(cx-atomR, cy, cx-atomR-len, cy, pStub)
            "down"  -> canvas.drawLine(cx, cy+atomR, cx, cy+atomR+len, pStub)
            "up"    -> canvas.drawLine(cx, cy-atomR, cx, cy-atomR-len, pStub)
        }
    }

    private fun drawAtoms(canvas: Canvas, eng: GameEngine) {
        val unsat = eng.unsatisfiedAtomIds()
        for (atom in eng.atoms) {
            val (rF, cF) = animPos(atom)
            drawAtom(canvas, atom, offsetX+(cF+0.5f)*cellSize, offsetY+(rF+0.5f)*cellSize,
                eng.selectedAtomId == atom.id, unsat)
        }
    }

    private fun drawAtom(canvas: Canvas, atom: Atom, cx: Float, cy: Float,
                         selected: Boolean, unsat: Set<Int>) {
        val (bgCol, borderCol, textCol) = atomColor(atom.symbol)
        val r = cellSize * 0.36f

        if (selected) {
            val glow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = borderCol; alpha = 80
                maskFilter = BlurMaskFilter(r * 1.3f, BlurMaskFilter.Blur.NORMAL)
            }
            canvas.drawCircle(cx, cy, r * 1.7f, glow)
        }

        canvas.drawCircle(cx, cy, r, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = bgCol })
        canvas.drawCircle(cx, cy, r, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = borderCol; style = Paint.Style.STROKE; strokeWidth = if (selected) 4f else 2.5f })

        // Draw label — split into symbol and Hₙ
        val sym = atom.symbol
        val hPart = when (atom.hydrogens) {
            0 -> ""; 1 -> "H"; else -> "H${atom.hydrogens.toSubscript()}"
        }
        val fullLabel = sym + hPart

        if (fullLabel.length <= 2) {
            pText.textSize = cellSize * 0.30f; pText.color = textCol
            canvas.drawText(fullLabel, cx, cy - (pText.descent()+pText.ascent())/2f, pText)
        } else {
            // Two-line: symbol on top, H part below
            pText.textSize = cellSize * 0.26f; pText.color = textCol
            canvas.drawText(sym, cx, cy - cellSize*0.06f, pText)
            pSubText.textSize = cellSize * 0.20f; pSubText.color = Color.parseColor("#4fc3f7")
            canvas.drawText(hPart, cx, cy + cellSize*0.14f, pSubText)
        }

        // Unsatisfied red dot
        if (atom.id in unsat && !selected) {
            canvas.drawCircle(cx + r*0.65f, cy - r*0.65f, r*0.18f,
                Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#ff5252") })
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private fun animPos(atom: Atom): Pair<Float, Float> {
        if (!isAnimating || animAtomId != atom.id) return Pair(atom.row.toFloat(), atom.col.toFloat())
        return Pair(
            animFromR + (animToR - animFromR) * animFrac,
            animFromC + (animToC - animFromC) * animFrac
        )
    }

    private fun Int.toSubscript() = when(this) { 2->"₂"; 3->"₃"; 4->"₄"; else->toString() }

    // ── Touch ─────────────────────────────────────────────────────────────────
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val eng = engine ?: return false
        if (isAnimating) return true
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                touchStartX = event.x; touchStartY = event.y
                val col = ((event.x - offsetX) / cellSize).toInt()
                val row = ((event.y - offsetY) / cellSize).toInt()
                eng.selectedAtomId = eng.atomAt(row, col)?.id ?: -1
                invalidate()
            }
            MotionEvent.ACTION_UP -> {
                if (eng.selectedAtomId == -1) return true
                val dx = event.x - touchStartX; val dy = event.y - touchStartY
                if (abs(dx) < cellSize * 0.3f && abs(dy) < cellSize * 0.3f) return true
                val dir = if (abs(dx) > abs(dy)) {
                    if (dx > 0) GameEngine.Direction.RIGHT else GameEngine.Direction.LEFT
                } else { if (dy > 0) GameEngine.Direction.DOWN else GameEngine.Direction.UP }
                doSlide(eng.selectedAtomId, dir)
            }
        }
        return true
    }

    fun slideSelected(dir: GameEngine.Direction) {
        val eng = engine ?: return
        if (eng.selectedAtomId == -1 || isAnimating) return
        doSlide(eng.selectedAtomId, dir)
    }

    private fun doSlide(atomId: Int, dir: GameEngine.Direction) {
        val eng = engine ?: return
        val atom = eng.atoms.find { it.id == atomId } ?: return
        val fr = atom.row.toFloat(); val fc = atom.col.toFloat()
        if (!eng.slide(atomId, dir)) return
        onMoveCountChanged?.invoke(eng.moveCount)

        animAtomId = atomId; animFromR = fr; animFromC = fc
        animToR = atom.row.toFloat(); animToC = atom.col.toFloat()
        isAnimating = true; animFrac = 0f

        ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 180; interpolator = DecelerateInterpolator(1.5f)
            addUpdateListener { animFrac = it.animatedFraction; invalidate() }
            addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(a: android.animation.Animator) {
                    isAnimating = false; animAtomId = -1; invalidate()
                    if (eng.checkWin()) onWin?.invoke()
                }
            })
            start()
        }
    }
}
