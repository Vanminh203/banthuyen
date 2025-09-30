package com.example.banthuyen

import android.content.Context
import android.graphics.*
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.core.graphics.scale

class GameView(context: Context, private val model: GameModel) :
    SurfaceView(context), SurfaceHolder.Callback, Runnable {

    private var controller: GameController? = null

    private var isPlaying = false
    private lateinit var gameThread: Thread
    private lateinit var canvas: Canvas
    private val paint = Paint()
    private var screenWidth: Int = 0
    private var screenHeight: Int = 0
    private lateinit var musicRect: RectF
    private lateinit var soundRect: RectF
    private lateinit var replayButtonRect: RectF
    private lateinit var homeButtonRect: RectF
    private lateinit var highScoresButtonRect: RectF

    init {
        holder.addCallback(this)
    }

    fun setController(controller: GameController) {
        this.controller = controller
    }

    override fun run() {
        while (isPlaying) {
            controller?.update()
            drawGame()
            control()
        }
    }

    private fun drawGame() {
        if (holder.surface.isValid) {
            canvas = holder.lockCanvas()
            canvas.drawColor(Color.BLACK)

            if (model.isGameOver) {
                // Draw Game Over screen
                model.gameOverImage?.let {
                    val imageWidth = screenWidth / 2
                    val imageHeight = imageWidth * it.height / it.width
                    val imageRect = RectF(
                        (screenWidth - imageWidth) / 2f,
                        (screenHeight - imageHeight) / 2f - 100,
                        (screenWidth + imageWidth) / 2f,
                        (screenHeight + imageHeight) / 2f - 100
                    )
                    canvas.drawBitmap(it, null, imageRect, paint)
                }

                // Draw buttons
                paint.color = Color.GRAY
                paint.style = Paint.Style.FILL
                canvas.drawRect(replayButtonRect, paint)
                canvas.drawRect(homeButtonRect, paint)
                canvas.drawRect(highScoresButtonRect, paint)

                paint.color = Color.WHITE
                paint.textSize = 50f
                paint.textAlign = Paint.Align.CENTER
                canvas.drawText("Replay", replayButtonRect.centerX(), replayButtonRect.centerY() + 15, paint)
                canvas.drawText("Home", homeButtonRect.centerX(), homeButtonRect.centerY() + 15, paint)
                canvas.drawText("High Scores", highScoresButtonRect.centerX(), highScoresButtonRect.centerY() + 15, paint)

                holder.unlockCanvasAndPost(canvas)
                return
            }

            // Draw HUD
            // HP Bar
            val hpMax = 6f
            val hpCurrent = model.spaceshipHP.toFloat()
            val barWidth = 200f
            val barHeight = 100f
            val barX = 20f
            val barY = 200f
            paint.color = Color.RED
            canvas.drawRect(barX, barY, barX + barWidth, barY + barHeight, paint)
            paint.color = Color.GREEN
            val hpFillWidth = (hpCurrent / hpMax) * barWidth
            canvas.drawRect(barX, barY, barX + hpFillWidth, barY + barHeight, paint)
            paint.color = Color.YELLOW
            paint.textSize = 50f
            paint.textAlign = Paint.Align.LEFT
            val text = "HP: ${formatNumber(hpCurrent)} / ${formatNumber(hpMax)}"
            val textBounds = Rect()
            paint.getTextBounds(text, 0, text.length, textBounds)
            val textX = barX + (barWidth - textBounds.width()) / 2f
            val textY = barY + (barHeight + textBounds.height()) / 2f
            canvas.drawText(text, textX, textY, paint)

            // Score
            paint.textAlign = Paint.Align.RIGHT
            canvas.drawText("Score: ${model.score}", screenWidth - 20f, 200f, paint)

            // Time
            canvas.drawText("Time: ${model.getElapsedTime()} s", screenWidth - 20f, 250f, paint)

            // Draw audio controls
            val musicBitmap = if (model.isMusicOn) model.musicOnBitmap else model.musicOffBitmap
            canvas.drawBitmap(musicBitmap, null, musicRect, paint)
            val soundBitmap = if (model.isSoundOn) model.soundOnBitmap else model.soundOffBitmap
            canvas.drawBitmap(soundBitmap, null, soundRect, paint)

            // Draw spaceship
            canvas.drawBitmap(model.spaceshipBitmap, model.spaceshipX, model.spaceshipY, paint)

            // Draw bullets
            paint.color = Color.WHITE
            for (bullet in model.bullets) {
                canvas.drawRect(bullet.position, paint)
            }

            // Draw missiles
            paint.color = Color.RED
            for (missile in model.missiles) {
                canvas.drawRect(missile.position, paint)
            }

            // Draw lasers
            paint.color = Color.BLUE
            paint.strokeWidth = 5f
            for (laser in model.lasers) {
                canvas.drawLine(laser.x, laser.spaceshipY, laser.x, 0f, paint)
            }

            // Draw targets
            for (target in model.targets) {
                canvas.drawBitmap(target.bitmap, target.position.left, target.position.top, paint)
            }

            // Draw power-ups
            for (p in model.powerUps) {
                canvas.drawBitmap(p.bitmap, p.position.left, p.position.top, paint)
            }

            // Draw shield
            if (model.isShieldActive) {
                paint.style = Paint.Style.STROKE
                paint.color = Color.GREEN
                paint.strokeWidth = 8f
                canvas.drawCircle(
                    model.spaceshipX + model.spaceshipBitmap.width / 2,
                    model.spaceshipY + model.spaceshipBitmap.height / 2,
                    model.spaceshipBitmap.width.toFloat(),
                    paint
                )
                paint.style = Paint.Style.FILL
            }

            // Draw armor
            if (model.armorActive) {
                paint.style = Paint.Style.STROKE
                paint.color = Color.YELLOW
                paint.strokeWidth = 8f
                canvas.drawCircle(
                    model.spaceshipX + model.spaceshipBitmap.width / 2,
                    model.spaceshipY + model.spaceshipBitmap.height / 2,
                    model.spaceshipBitmap.width.toFloat(),
                    paint
                )
                paint.style = Paint.Style.FILL
            }

            holder.unlockCanvasAndPost(canvas)
        }
    }

    private fun formatNumber(value: Float): String {
        return if (value % 1 == 0f) value.toInt().toString() else value.toString()
    }

    private fun control() {
        try {
            Thread.sleep(17) // ~60 FPS
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        screenWidth = width
        screenHeight = height

        // Scale bitmaps
        val shipWidth = screenWidth / 5
        val shipHeight = shipWidth * model.spaceshipBitmap.height / model.spaceshipBitmap.width
        model.spaceshipBitmap = Bitmap.createScaledBitmap(model.spaceshipBitmap, shipWidth, shipHeight, true)

        val targetWidth = screenWidth / 7
        val targetHeight = targetWidth * model.targetBitmap.height / model.targetBitmap.width
        model.targetBitmap = Bitmap.createScaledBitmap(model.targetBitmap, targetWidth, targetHeight, true)

        val powerWidth = screenWidth / 13
        val powerHeight = powerWidth * model.bulletPowerBitmap.height / model.bulletPowerBitmap.width
        model.bulletPowerBitmap = Bitmap.createScaledBitmap(model.bulletPowerBitmap, powerWidth, powerHeight, true)
        model.shieldPowerBitmap = Bitmap.createScaledBitmap(model.shieldPowerBitmap, powerWidth, powerHeight, true)
        model.missilePowerBitmap = Bitmap.createScaledBitmap(model.missilePowerBitmap, powerWidth, powerHeight, true)
        model.laserPowerBitmap = Bitmap.createScaledBitmap(model.laserPowerBitmap, powerWidth, powerHeight, true)
        model.armorPowerBitmap = Bitmap.createScaledBitmap(model.armorPowerBitmap, powerWidth, powerHeight, true)
        model.hpPowerBitmap = Bitmap.createScaledBitmap(model.hpPowerBitmap, powerWidth, powerHeight, true)

        val goWidth = screenWidth / 2
        val goHeight = goWidth * model.youLoseBitmap.height / model.youLoseBitmap.width
        model.youLoseBitmap = Bitmap.createScaledBitmap(model.youLoseBitmap, goWidth, goHeight, true)
        model.gameOverBitmap = Bitmap.createScaledBitmap(model.gameOverBitmap, goWidth, goHeight, true)

        val iconWidth = screenWidth / 8
        val iconHeight = iconWidth * model.musicOnBitmap.height / model.musicOnBitmap.width
        model.musicOnBitmap = model.musicOnBitmap.scale(iconWidth, iconHeight)
        model.musicOffBitmap = model.musicOffBitmap.scale(iconWidth, iconHeight)
        model.soundOnBitmap = model.soundOnBitmap.scale(iconWidth, iconHeight)
        model.soundOffBitmap = model.soundOffBitmap.scale(iconWidth, iconHeight)

        // Initialize UI elements
        val musicLeftOffset = 20f
        musicRect = RectF(
            musicLeftOffset,
            (screenHeight / 2f - iconHeight / 2f),
            musicLeftOffset + iconWidth,
            (screenHeight / 2f + iconHeight / 2f)
        )
        val soundRightOffset = 20f
        soundRect = RectF(
            (screenWidth - iconWidth - soundRightOffset),
            (screenHeight / 2f - iconHeight / 2f),
            (screenWidth - soundRightOffset).toFloat(),
            (screenHeight / 2f + iconHeight / 2f)
        )
        val buttonWidth = screenWidth / 4f
        val buttonHeight = 60f
        val buttonSpacing = 20f
        val startY = screenHeight / 2f + 100
        replayButtonRect = RectF(
            (screenWidth - buttonWidth) / 2,
            startY,
            (screenWidth + buttonWidth) / 2,
            startY + buttonHeight
        )
        homeButtonRect = RectF(
            (screenWidth - buttonWidth) / 2,
            startY + buttonHeight + buttonSpacing,
            (screenWidth + buttonWidth) / 2,
            startY + 2 * buttonHeight + buttonSpacing
        )
        highScoresButtonRect = RectF(
            (screenWidth - buttonWidth) / 2,
            startY + 2 * buttonHeight + 2 * buttonSpacing,
            (screenWidth + buttonWidth) / 2,
            startY + 3 * buttonHeight + 2 * buttonSpacing
        )

        // Set initial spaceship position
        model.spaceshipX = (screenWidth / 2 - model.spaceshipBitmap.width / 2).toFloat()
        model.spaceshipY = (screenHeight - model.spaceshipBitmap.height - 50).toFloat()

        // Start game loop
        resume()
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        pause()
        model.cleanup()
    }

    private fun pause() {
        isPlaying = false
        try {
            gameThread.join()
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }

    private fun resume() {
        isPlaying = true
        gameThread = Thread(this)
        gameThread.start()
    }
    override fun onTouchEvent(event: android.view.MotionEvent?): Boolean {
        return controller?.onTouchEvent(event) ?: false
    }

    fun getMusicRect(): RectF = musicRect
    fun getSoundRect(): RectF = soundRect
    fun getReplayButtonRect(): RectF = replayButtonRect
    fun getHomeButtonRect(): RectF = homeButtonRect
    fun getHighScoresButtonRect(): RectF = highScoresButtonRect
}
