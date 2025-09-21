package com.example.banthuyen

import android.animation.ValueAnimator
import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    private var mediaPlayer: MediaPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val startButton: Button = findViewById(R.id.startButton)
        val exitButton: Button = findViewById(R.id.exitButton)
        val scrollingBackground: ScrollingBackgroundView = findViewById(R.id.scrollingBackground)

        // 🔹 Animate nền
        val animator = ValueAnimator.ofFloat(0f, 1000f)
        animator.duration = 10000L
        animator.repeatCount = ValueAnimator.INFINITE
        animator.addUpdateListener { valueAnimator ->
            val x = valueAnimator.animatedValue as Float
            scrollingBackground.setOffset(x)
        }
        animator.start()

        // 🔹 Tạo MediaPlayer cho nhạc nền
        mediaPlayer = MediaPlayer.create(this, R.raw.backgroundmusic)
        mediaPlayer?.isLooping = true // Lặp lại nhạc
        mediaPlayer?.start()

        // 🔹 Nút Start
        startButton.setOnClickListener {
            val intent = Intent(this, GameActivity::class.java)
            startActivity(intent)
        }

        // 🔹 Nút Exit
        exitButton.setOnClickListener {
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        mediaPlayer = null
    }
}
