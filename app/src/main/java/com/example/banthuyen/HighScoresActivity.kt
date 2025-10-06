package com.example.banthuyen

import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat


class HighScoresActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ScrollView để chắc chắn hiển thị hết
        val scrollView = ScrollView(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(ContextCompat.getColor(context, android.R.color.black))
        }

        // Layout chính
        val mainLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(40, 40, 40, 40)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,   // chiếm hết chiều rộng màn hình
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        // Title
        val titleText = TextView(this).apply {
            text = "HIGH SCORES"
            textSize = 28f
            setTextColor(ContextCompat.getColor(context, android.R.color.holo_orange_light))
            gravity = Gravity.CENTER
            setPadding(0, 20, 0, 40)
        }
        mainLayout.addView(titleText)

        // Lấy dữ liệu điểm
        val model = GameModel(this)
        val highScores = model.getHighScores().take(8) // chỉ lấy 8 điểm cao nhất

        if (highScores.isEmpty()) {
            val noScoresText = TextView(this).apply {
                text = "Chưa có điểm số nào"
                textSize = 20f
                setTextColor(ContextCompat.getColor(context, android.R.color.white))
                gravity = Gravity.CENTER
                setPadding(0, 20, 0, 20)
            }
            mainLayout.addView(noScoresText)
        } else {
            // Hiển thị danh sách điểm
            highScores.forEachIndexed { index, score ->
                val scoreText = TextView(this).apply {
                    text = "${index + 1}. ${score} điểm"
                    textSize = 22f
                    setTextColor(
                        when (index) {
                            0 -> ContextCompat.getColor(context, android.R.color.holo_orange_light)
                            1 -> ContextCompat.getColor(context, android.R.color.darker_gray)
                            2 -> ContextCompat.getColor(context, android.R.color.holo_orange_dark)
                            else -> ContextCompat.getColor(context, android.R.color.white)
                        }
                    )
                    gravity = Gravity.CENTER
                    setPadding(0, 15, 0, 15)
                }
                mainLayout.addView(scoreText)
            }
        }

        // Nút đóng
        val closeButton = Button(this).apply {
            text = "Đóng"
            textSize = 18f
            setBackgroundColor(ContextCompat.getColor(context, android.R.color.holo_blue_dark))
            setTextColor(ContextCompat.getColor(context, android.R.color.white))
            setPadding(40, 20, 40, 20)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = 50
                gravity = Gravity.CENTER_HORIZONTAL
            }
            setOnClickListener { finish() }
        }
        mainLayout.addView(closeButton)

        scrollView.addView(mainLayout)
        setContentView(scrollView)
    }
}
