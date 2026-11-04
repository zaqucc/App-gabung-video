package com.projectsore.videomerger

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class CrashActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val trace = intent?.getStringExtra("trace") ?: "(tidak ada detail error)"
        val d = resources.displayMetrics.density

        val text = TextView(this).apply {
            this.text = trace.take(6000)
            setTextColor(Color.WHITE)
            textSize = 11f
            setTextIsSelectable(true)
        }
        val copy = Button(this).apply {
            this.text = "SALIN ERROR"
            setOnClickListener {
                val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                cm.setPrimaryClip(ClipData.newPlainText("crash", trace))
                Toast.makeText(this@CrashActivity, "Disalin", Toast.LENGTH_SHORT).show()
            }
        }
        val title = TextView(this).apply {
            this.text = "Aplikasi crash. Kirim teks di bawah ini:"
            setTextColor(Color.YELLOW)
            textSize = 14f
        }
        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xFF0B1220.toInt())
            val p = (16 * d).toInt()
            setPadding(p, (56 * d).toInt(), p, p)
            addView(title)
            addView(copy)
            addView(text)
        }
        setContentView(ScrollView(this).apply { addView(page) })
    }
}
