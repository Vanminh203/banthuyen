package com.example.banthuyen

import android.content.Context
import android.content.Intent
import android.graphics.RectF
import android.view.MotionEvent
import androidx.appcompat.app.AppCompatActivity

class GameController(
    private val model: GameModel,
    private val view: GameView,
    private val context: Context
) {
    private var isShooting = false

    fun update() {
        model.update(view.width, view.height, isShooting)
    }

    fun onTouchEvent(event: MotionEvent?): Boolean {
        event?.let {
            val x = event.x
            val y = event.y
            when (it.action) {
                MotionEvent.ACTION_DOWN -> {
                    if (model.showLevelTransition) {
                        if (model.levelManager.hasNextLevel() && view.getNextLevelButtonRect().contains(x, y)) {
                            model.advanceToNextLevel(view.width, view.height)
                            return true
                        } else if (!model.levelManager.hasNextLevel() && view.getHomeButtonRect().contains(x, y)) {
                            val intent = Intent(context, MainActivity::class.java)
                            context.startActivity(intent)
                            (context as AppCompatActivity).finish()
                            return true
                        }
                        return true
                    }

                    if (model.isGameOver) {
                        if (view.getReplayButtonRect().contains(x, y)) {
                            model.resetGame(view.width, view.height)
                            return true
                        } else if (view.getHomeButtonRect().contains(x, y)) {
                            val intent = Intent(context, MainActivity::class.java)
                            context.startActivity(intent)
                            (context as AppCompatActivity).finish()
                            return true
                        } else if (view.getHighScoresButtonRect().contains(x, y)) {
                            val builder = androidx.appcompat.app.AlertDialog.Builder(context)
                            builder.setTitle("High Scores")
                            builder.setMessage("Điểm cao nhất: ${model.highScore}\nĐiểm hiện tại: ${model.score}")
                            builder.setPositiveButton("OK") { _, _ -> }
                            builder.show()
                            return true
                        }
                    }
                    if (view.getMusicRect().contains(x, y)) {
                        model.toggleMusic()
                        return true
                    }
                    if (view.getSoundRect().contains(x, y)) {
                        model.toggleSound()
                        return true
                    }
                    moveSpaceship(x, y)
                }
                MotionEvent.ACTION_MOVE -> {
                    if (!view.getMusicRect().contains(x, y) && !view.getSoundRect().contains(x, y)) {
                        moveSpaceship(x, y)
                    }
                }
                MotionEvent.ACTION_UP -> {
                    isShooting = false
                }
            }
        }
        return true
    }

    private fun moveSpaceship(x: Float, y: Float) {
        model.spaceshipX = x - model.spaceshipBitmap.width / 2
        model.spaceshipY = y - model.spaceshipBitmap.height / 2
        val shipRect = RectF(
            model.spaceshipX,
            model.spaceshipY,
            model.spaceshipX + model.spaceshipBitmap.width,
            model.spaceshipY + model.spaceshipBitmap.height
        )
        isShooting = shipRect.contains(x, y)
        if (model.spaceshipX < 0) {
            model.spaceshipX = 0f
            if (model.isSoundOn) model.playSound(model.soundHitWall)
        }
        if (model.spaceshipX + model.spaceshipBitmap.width > view.width) {
            model.spaceshipX = (view.width - model.spaceshipBitmap.width).toFloat()
            if (model.isSoundOn) model.playSound(model.soundHitWall)
        }
        if (model.spaceshipY < 0) {
            model.spaceshipY = 0f
            if (model.isSoundOn) model.playSound(model.soundHitWall)
        }
        if (model.spaceshipY + model.spaceshipBitmap.height > view.height) {
            model.spaceshipY = (view.height - model.spaceshipBitmap.height).toFloat()
            if (model.isSoundOn) model.playSound(model.soundHitWall)
        }
    }
}
