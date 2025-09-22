package com.example.banthuyen

import android.animation.ValueAnimator
import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    companion object {
        // Để có thể truy cập từ GameActivity
        var mediaPlayer: MediaPlayer? = null
    }

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

        // 🔹 Tạo MediaPlayer cho nhạc nền (chỉ tạo nếu chưa có)
        if (mediaPlayer == null) {
            mediaPlayer = MediaPlayer.create(this, R.raw.backgroundmusic)
            mediaPlayer?.isLooping = true
            mediaPlayer?.start()
        }

        // 🔹 Nút Start
        startButton.setOnClickListener {
            // Tạm dừng nhạc khi vào game
            mediaPlayer?.pause()
            val intent = Intent(this, GameActivity::class.java)
            startActivity(intent)
        }

        // 🔹 Nút Exit
        exitButton.setOnClickListener {
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        // Khi quay lại MainActivity từ GameActivity, phát nhạc tiếp
        mediaPlayer?.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Giải phóng nhạc nền khi thoát app
        mediaPlayer?.release()
        mediaPlayer = null
    }
}
