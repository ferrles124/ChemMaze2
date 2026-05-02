package com.chemmaze.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.chemmaze.R
import com.chemmaze.databinding.ActivityMainBinding
import com.chemmaze.engine.LevelLoader
import com.chemmaze.model.Level

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val levels = LevelLoader.listLevelFiles(this).map { LevelLoader.load(this, it) }
        binding.rvLevels.layoutManager = GridLayoutManager(this, 3)
        binding.rvLevels.adapter = LevelAdapter(levels) {
            startActivity(Intent(this, GameActivity::class.java).putExtra("LEVEL_INDEX", it.id))
        }
    }
}

class LevelAdapter(private val levels: List<Level>, private val onClick: (Level) -> Unit)
    : RecyclerView.Adapter<LevelAdapter.VH>() {
    inner class VH(v: View) : RecyclerView.ViewHolder(v) {
        val tvNum: TextView = v.findViewById(R.id.tvLevelNumber)
        val tvFormula: TextView = v.findViewById(R.id.tvLevelFormula)
    }
    override fun onCreateViewHolder(p: ViewGroup, t: Int) =
        VH(LayoutInflater.from(p.context).inflate(R.layout.item_level, p, false))
    override fun onBindViewHolder(h: VH, pos: Int) {
        val lv = levels[pos]
        h.tvNum.text = "${lv.id}"; h.tvFormula.text = lv.formula
        h.itemView.setOnClickListener { onClick(lv) }
    }
    override fun getItemCount() = levels.size
    }
    
