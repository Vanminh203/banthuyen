package com.example.banthuyen

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter

data class Level(
    val levelNumber: Int,
    val name: String,
    val backgroundColor: Int,
    val targetTint: Int,
    val targetSpawnRate: Double,
    val targetSpeed: Float,
    val powerUpSpawnRate: Double,
    val scoreToWin: Int,
    val description: String
)

class LevelManager(private val context: Context) {
    var currentLevelIndex = 0
    private val levels = listOf(
        Level(
            levelNumber = 1,
            name = "Vùng biển yên bình",
            backgroundColor = Color.rgb(20, 40, 80),
            targetTint = Color.rgb(255, 100, 100),
            targetSpawnRate = 1.5,
            targetSpeed = 4f,
            powerUpSpawnRate = 2.5,
            scoreToWin = 300,
            description = "Cấp độ dễ - Làm quen với trò chơi"
        ),
        Level(
            levelNumber = 2,
            name = "Vùng biển nguy hiểm",
            backgroundColor = Color.rgb(80, 20, 40),
            targetTint = Color.rgb(100, 255, 100),
            targetSpawnRate = 2.5,
            targetSpeed = 6f,
            powerUpSpawnRate = 2.0,
            scoreToWin = 500,
            description = "Cấp độ trung bình - Kẻ thù nhanh hơn"
        ),
        Level(
            levelNumber = 3,
            name = "Vùng biển địa ngục",
            backgroundColor = Color.rgb(40, 20, 60),
            targetTint = Color.rgb(255, 255, 100),
            targetSpawnRate = 3.5,
            targetSpeed = 8f,
            powerUpSpawnRate = 1.5,
            scoreToWin = 700,
            description = "Cấp độ khó - Thử thách cuối cùng"
        )
    )

    fun getCurrentLevel(): Level {
        return levels[currentLevelIndex]
    }

    fun hasNextLevel(): Boolean {
        return currentLevelIndex < levels.size - 1
    }

    fun nextLevel() {
        if (hasNextLevel()) {
            currentLevelIndex++
        }
    }

    fun resetToFirstLevel() {
        currentLevelIndex = 0
    }

    fun getTotalLevels(): Int {
        return levels.size
    }

    fun createTintedBitmap(originalBitmap: Bitmap, tintColor: Int): Bitmap {
        val tintedBitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(tintedBitmap)
        val paint = Paint()
        paint.colorFilter = PorterDuffColorFilter(tintColor, PorterDuff.Mode.SRC_ATOP)
        canvas.drawBitmap(tintedBitmap, 0f, 0f, paint)
        return tintedBitmap
    }
}
