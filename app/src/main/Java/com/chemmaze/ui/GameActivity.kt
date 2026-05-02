package com.chemmaze.ui

import android.os.Bundle
import android.view.KeyEvent
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.chemmaze.databinding.ActivityGameBinding
import com.chemmaze.engine.GameEngine
import com.chemmaze.engine.LevelLoader

class GameActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGameBinding
    private lateinit var engine: GameEngine
    private var levelIndex = 1
    private var totalLevels = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGameBinding.inflate(layoutInflater)
        setContentView(binding.root)
        levelIndex = intent.getIntExtra("LEVEL_INDEX", 1)
        totalLevels = LevelLoader.listLevelFiles(this).size
        loadLevel()
        setupButtons()
    }

    private fun loadLevel() {
        val level = LevelLoader.loadByIndex(this, levelIndex)
        engine = GameEngine(level)
        binding.gameView.engine = engine
        binding.gameView.onWin = { runOnUiThread { showWinDialog() } }
        binding.gameView.onMoveCountChanged = { runOnUiThread { binding.tvMoves.text = "Hamle: $it" } }
        binding.tvLevelTitle.text = level.title
        binding.tvFormula.text = level.formula
        binding.tvMoleculeName.text = level.moleculeName
        binding.tvMoves.text = "Hamle: 0"
    }

    private fun setupButtons() {
        binding.btnUp.setOnClickListener    { binding.gameView.slideSelected(GameEngine.Direction.UP) }
        binding.btnDown.setOnClickListener  { binding.gameView.slideSelected(GameEngine.Direction.DOWN) }
        binding.btnLeft.setOnClickListener  { binding.gameView.slideSelected(GameEngine.Direction.LEFT) }
        binding.btnRight.setOnClickListener { binding.gameView.slideSelected(GameEngine.Direction.RIGHT) }
        binding.btnUndo.setOnClickListener {
            if (engine.undo()) { binding.tvMoves.text = "Hamle: ${engine.moveCount}"; binding.gameView.invalidate() }
        }
        binding.btnReset.setOnClickListener {
            engine.reset(); binding.gameView.invalidate(); binding.tvMoves.text = "Hamle: 0"
        }
        binding.btnBack.setOnClickListener { finish() }
    }

    private fun showWinDialog() {
        AlertDialog.Builder(this)
            .setTitle("🧪 Molekül Tamamlandı!")
            .setMessage("${engine.moveCount} hamlede çözdün!\n\n${
                if (levelIndex < totalLevels) "Sonraki bölüme geç?" else "Tebrikler, hepsini bitirdin!"
            }")
            .apply {
                if (levelIndex < totalLevels)
                    setPositiveButton("Sonraki →") { _, _ -> levelIndex++; loadLevel() }
                setNegativeButton("Ana Menü") { _, _ -> finish() }
                setNeutralButton("Tekrar") { _, _ ->
                    engine.reset(); binding.gameView.invalidate(); binding.tvMoves.text = "Hamle: 0"
                }
            }
            .setCancelable(false).show()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        val dir = when (keyCode) {
            KeyEvent.KEYCODE_DPAD_UP    -> GameEngine.Direction.UP
            KeyEvent.KEYCODE_DPAD_DOWN  -> GameEngine.Direction.DOWN
            KeyEvent.KEYCODE_DPAD_LEFT  -> GameEngine.Direction.LEFT
            KeyEvent.KEYCODE_DPAD_RIGHT -> GameEngine.Direction.RIGHT
            else -> return super.onKeyDown(keyCode, event)
        }
        binding.gameView.slideSelected(dir)
        return true
    }
}
