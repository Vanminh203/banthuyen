package com.example.banthuyen

import android.content.Context
import android.content.Intent
import android.graphics.*
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import android.os.Bundle
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.appcompat.app.AppCompatActivity
import kotlin.random.Random
import android.os.Handler
import android.os.Looper
import kotlin.math.min
import androidx.core.graphics.scale

class GameActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(GameView(this))
    }
}

class GameView(context: Context) : SurfaceView(context), Runnable, SurfaceHolder.Callback {
    private var isPlaying = false
    private lateinit var gameThread: Thread
    private lateinit var canvas: Canvas
    private lateinit var paint: Paint
    private var screenWidth: Int = 0
    private var screenHeight: Int = 0
    private var frameCount = 0
    private var isShooting = false
    private lateinit var soundPool: SoundPool
    private var soundShoot = 0
    private var soundHitWall = 0
    private var soundWarning = 0
    private val handler = Handler(Looper.getMainLooper())
    private var bulletPowerBitmap: Bitmap
    private var shieldPowerBitmap: Bitmap
    private val powerUps = mutableListOf<PowerUp>()
    private var bulletBoostRunnable: Runnable? = null
    private var soundPowerUp = 0
    private var soundShipExplosion = 0
    private val soundLoaded = mutableMapOf<Int, Boolean>()
    private var isGameOver = false
    private var gameOverImage: Bitmap? = null
    private var highScore = 0

    // Bitmap toggle nhạc nền
    private var musicOnBitmap: Bitmap
    private var musicOffBitmap: Bitmap
    private lateinit var musicRect: RectF

    // Bitmap toggle sound effect
    private var soundOnBitmap: Bitmap
    private var soundOffBitmap: Bitmap
    private var isSoundOn = true
    private lateinit var soundRect: RectF

    // MediaPlayer cho nhạc nền
    private lateinit var bgm: MediaPlayer
    private var isMusicOn = false

    // Thêm cơ chế tấn công và phòng thủ mới
    private var bulletBoostActive = false
    private var missileActive = false
    private var laserActive = false
    private var isShieldActive = false
    private var armorActive = false

    // Bitmaps cho power-ups mới
    private var missilePowerBitmap: Bitmap
    private var laserPowerBitmap: Bitmap
    private var armorPowerBitmap: Bitmap
    private var hpPowerBitmap: Bitmap

    // Danh sách cho các loại tấn công đặc biệt
    private val missiles = mutableListOf<Missile>()
    private val lasers = mutableListOf<Laser>()

    // Phi thuyền
    private var spaceshipX: Float = 0f
    private var spaceshipY: Float = 0f
    private var spaceshipBitmap: Bitmap
    private var targetBitmap: Bitmap
    private var bulletsBitmap: Bitmap
    private val bullets = mutableListOf<Bullet>()
    private val targets = mutableListOf<Target>()

    // HP cho phi thuyền
    private var spaceshipHP = 6

    // HUD: Thêm biến cho điểm số và thời gian chơi
    private var score = 0
    private var startTime = System.currentTimeMillis()

    // Game Over: Bitmaps và nút
    private var youLoseBitmap: Bitmap
    private var gameOverBitmap: Bitmap
    private lateinit var replayButtonRect: RectF
    private lateinit var homeButtonRect: RectF
    private lateinit var highScoresButtonRect: RectF

    init {
        holder.addCallback(this)
        spaceshipBitmap = BitmapFactory.decodeResource(resources, R.drawable.phithuyen)
        targetBitmap = BitmapFactory.decodeResource(resources, R.drawable.muctieu)
        bulletsBitmap = BitmapFactory.decodeResource(resources, R.drawable.dan)
        paint = Paint()
        val prefs = context.getSharedPreferences("GamePrefs", Context.MODE_PRIVATE)
        highScore = prefs.getInt("highScore", 0)

        // Tải bitmaps cho Game Over
        youLoseBitmap = BitmapFactory.decodeResource(resources, R.drawable.youlose)
        gameOverBitmap = BitmapFactory.decodeResource(resources, R.drawable.gameover)

        // SoundPool
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(5)
            .setAudioAttributes(audioAttributes)
            .build()

        soundPool.setOnLoadCompleteListener { _, sampleId, status ->
            if (status == 0) {
                soundLoaded[sampleId] = true
                android.util.Log.d("SOUND", "Loaded OK: $sampleId")
            }
        }

        soundShoot = soundPool.load(context, R.raw.shoot, 1)
        soundHitWall = soundPool.load(context, R.raw.hitwall, 1)
        soundWarning = soundPool.load(context, R.raw.warning, 1)
        soundPowerUp = soundPool.load(context, R.raw.powerup, 1)
        soundShipExplosion = soundPool.load(context, R.raw.shipexplosion, 1)

        bulletPowerBitmap = BitmapFactory.decodeResource(resources, R.drawable.dan)
        shieldPowerBitmap = BitmapFactory.decodeResource(resources, R.drawable.khien)
        missilePowerBitmap = BitmapFactory.decodeResource(resources, R.drawable.missilepower)
        laserPowerBitmap = BitmapFactory.decodeResource(resources, R.drawable.laserpower)
        armorPowerBitmap = BitmapFactory.decodeResource(resources, R.drawable.armorpower)
        hpPowerBitmap = BitmapFactory.decodeResource(resources, R.drawable.hppower)
        musicOnBitmap = BitmapFactory.decodeResource(resources, R.drawable.musicturnon)
        musicOffBitmap = BitmapFactory.decodeResource(resources, R.drawable.musicturnoff)
        soundOnBitmap = BitmapFactory.decodeResource(resources, R.drawable.soundon)
        soundOffBitmap = BitmapFactory.decodeResource(resources, R.drawable.soundoff)

        bgm = MediaPlayer.create(context, R.raw.backgroundmusic)
        bgm.isLooping = true
    }

    override fun run() {
        while (isPlaying) {
            update()
            drawGame()
            control()
        }
    }

    private fun update() {
        if (isGameOver) return
        frameCount++
        // Xác định delay bắn dựa trên buff
        val shootDelay = if (bulletBoostActive) 5 else 15
        if (!missileActive && !laserActive && isShooting && frameCount % shootDelay == 0) {
            bullets.add(Bullet(spaceshipX + spaceshipBitmap.width / 2 - 5, spaceshipY))
            if (isSoundOn) soundPool.play(soundShoot, 1f, 1f, 1, 0, 1f)
        }

        if (missileActive && isShooting && frameCount % 30 == 0) {
            missiles.add(Missile(spaceshipX + spaceshipBitmap.width / 2 - 5, spaceshipY, targets))
            if (isSoundOn) soundPool.play(soundShoot, 1f, 1f, 1, 0, 1f)
        }

        if (laserActive && isShooting && frameCount % 10 == 0) {
            lasers.add(Laser(spaceshipX + spaceshipBitmap.width / 2 - 5, spaceshipY))
            if (isSoundOn) soundPool.play(soundShoot, 1f, 1f, 1, 0, 1f)
        }

        // Cập nhật đạn
        for (i in bullets.size - 1 downTo 0) {
            val b = bullets[i]
            b.update()
            if (b.position.bottom < 0) {
                bullets.removeAt(i)
            }
        }

        // Cập nhật tên lửa
        for (i in missiles.size - 1 downTo 0) {
            val m = missiles[i]
            m.update()
            if (m.position.bottom < 0) {
                missiles.removeAt(i)
            }
        }

        // Cập nhật laser
        for (i in lasers.size - 1 downTo 0) {
            val l = lasers[i]
            var hitAny = false
            for (j in targets.size - 1 downTo 0) {
                val t = targets[j]
                if (t.position.bottom <= l.spaceshipY && RectF.intersects(l.position, t.position)) {
                    targets.removeAt(j)
                    hitAny = true
                    score += 10 // Tăng điểm khi tiêu diệt mục tiêu
                }
            }
            if (!hitAny) {
                lasers.removeAt(i)
            }
            handler.postDelayed({ lasers.remove(l) }, 200)
        }

        // Cập nhật mục tiêu
        for (i in targets.size - 1 downTo 0) {
            val t = targets[i]
            t.update(screenHeight, screenWidth)
            if (t.position.top > screenHeight) {
                targets.removeAt(i)
            } else {
                if (t.position.top > screenHeight / 2 && !t.hasWarned) {
                    t.hasWarned = true
                    val repeatTimes = (3..6).random()
                    playWarning(repeatTimes, 400)
                }
            }
        }

        // Spawn mục tiêu mới
        if (Random.nextInt(100) < 1.5) {
            val x = Random.nextInt((screenWidth - targetBitmap.width).coerceAtLeast(1)).toFloat()
            targets.add(Target(x, 0f, targetBitmap))
        }

        // Sinh PowerUp
        if (Random.nextInt(500) < 2.5) {
            val x = Random.nextInt((screenWidth - bulletPowerBitmap.width).coerceAtLeast(1)).toFloat()
            val type = PowerUpType.entries.random()
            val bmp = when (type) {
                PowerUpType.BULLET -> bulletPowerBitmap
                PowerUpType.SHIELD -> shieldPowerBitmap
                PowerUpType.MISSILE -> missilePowerBitmap
                PowerUpType.LASER -> laserPowerBitmap
                PowerUpType.ARMOR -> armorPowerBitmap
                PowerUpType.HP -> hpPowerBitmap
            }
            powerUps.add(PowerUp(x, 0f, bmp, type))
        }

        // Cập nhật PowerUp
        for (i in powerUps.size - 1 downTo 0) {
            val p = powerUps[i]
            p.update(screenHeight)
            if (p.position.top > screenHeight) {
                powerUps.removeAt(i)
            } else {
                val shipRect = RectF(
                    spaceshipX,
                    spaceshipY,
                    spaceshipX + spaceshipBitmap.width,
                    spaceshipY + spaceshipBitmap.height
                )
                if (RectF.intersects(p.position, shipRect)) {
                    when (p.type) {
                        PowerUpType.BULLET -> {
                            bulletBoostActive = true
                            missileActive = false
                            laserActive = false
                            bulletBoostRunnable?.let { handler.removeCallbacks(it) }
                            bulletBoostRunnable = Runnable { bulletBoostActive = false }
                            handler.postDelayed(bulletBoostRunnable!!, 5000)
                        }
                        PowerUpType.MISSILE -> {
                            missileActive = true
                            bulletBoostActive = false
                            laserActive = false
                            handler.postDelayed({ missileActive = false }, 5000)
                        }
                        PowerUpType.LASER -> {
                            laserActive = true
                            missileActive = false
                            bulletBoostActive = false
                            handler.postDelayed({ laserActive = false }, 5000)
                        }
                        PowerUpType.SHIELD -> {
                            isShieldActive = true
                            armorActive = false
                            handler.postDelayed({ isShieldActive = false }, 5000)
                        }
                        PowerUpType.ARMOR -> {
                            armorActive = true
                            isShieldActive = false
                            handler.postDelayed({ armorActive = false }, 5000)
                        }
                        PowerUpType.HP -> {
                            spaceshipHP = min(spaceshipHP + 1, 6)
                        }
                    }
                    if (isSoundOn && soundLoaded[soundPowerUp] == true) playSound(soundPowerUp)
                    powerUps.removeAt(i)
                }
            }
        }

        // Va chạm đạn
        for (i in bullets.size - 1 downTo 0) {
            val b = bullets[i]
            var hit = false
            for (j in targets.size - 1 downTo 0) {
                val t = targets[j]
                if (RectF.intersects(b.position, t.position)) {
                    bullets.removeAt(i)
                    targets.removeAt(j)
                    score += 10 // Tăng điểm khi tiêu diệt mục tiêu
                    hit = true
                    break
                }
            }
            if (hit) continue
        }

        // Va chạm tên lửa
        for (i in missiles.size - 1 downTo 0) {
            val m = missiles[i]
            var hit = false
            for (j in targets.size - 1 downTo 0) {
                val t = targets[j]
                if (RectF.intersects(m.position, t.position)) {
                    missiles.removeAt(i)
                    targets.removeAt(j)
                    score += 10 // Tăng điểm khi tiêu diệt mục tiêu
                    hit = true
                    break
                }
            }
            if (hit) continue
        }

        // Va chạm phi thuyền
        val shipRect = RectF(
            spaceshipX,
            spaceshipY,
            spaceshipX + spaceshipBitmap.width,
            spaceshipY + spaceshipBitmap.height
        )
        for (i in targets.size - 1 downTo 0) {
            val t = targets[i]
            if (RectF.intersects(t.position, shipRect)) {
                targets.removeAt(i)
                if (isShieldActive) {
                    isShieldActive = false
                } else if (armorActive) {
                    spaceshipHP -= 1
                    armorActive = false
                    if (spaceshipHP <= 0) {
                        if (isSoundOn) soundPool.play(soundShipExplosion, 1f, 1f, 1, 0, 1f)
                        isGameOver = true
                        if (score > highScore) {
                            highScore = score
                            val prefs = context.getSharedPreferences("GamePrefs", Context.MODE_PRIVATE)
                            prefs.edit().putInt("highScore", highScore).apply()
                        }
                        gameOverImage = if (Random.nextBoolean()) youLoseBitmap else gameOverBitmap                    }
                } else {
                    spaceshipHP -= 3
                    if (spaceshipHP <= 0) {
                        if (isSoundOn) soundPool.play(soundShipExplosion, 1f, 1f, 1, 0, 1f)
                        isGameOver = true
                        if (score > highScore) {
                            highScore = score
                            val prefs = context.getSharedPreferences("GamePrefs", Context.MODE_PRIVATE)
                            prefs.edit().putInt("highScore", highScore).apply()
                        }
                        gameOverImage = if (Random.nextBoolean()) youLoseBitmap else gameOverBitmap
                    }
                }
                break
            }
        }
    }

    private fun playSound(soundId: Int) {
        if (!isSoundOn) return
        val streamId = soundPool.play(soundId, 1f, 1f, 1, 0, 1f)
        if (streamId == 0) {
            android.util.Log.d("SOUND", "Play failed for soundId=$soundId")
        } else {
            android.util.Log.d("SOUND", "Played soundId=$soundId streamId=$streamId")
        }
    }

    private fun playWarning(times: Int, interval: Long) {
        for (i in 0 until times) {
            handler.postDelayed({
                if (isSoundOn) soundPool.play(soundWarning, 1f, 1f, 1, 0, 1f)
            }, i * interval)
        }
    }

    private fun drawGame() {
        if (holder.surface.isValid) {
            canvas = holder.lockCanvas()
            canvas.drawColor(Color.BLACK)

            if (isGameOver) {
                // Vẽ màn hình Game Over
                if (gameOverImage != null) {
                    val imageWidth = screenWidth / 2
                    val imageHeight = imageWidth * gameOverImage!!.height / gameOverImage!!.width
                    val imageRect = RectF(
                        (screenWidth - imageWidth) / 2f,
                        (screenHeight - imageHeight) / 2f - 100,
                        (screenWidth + imageWidth) / 2f,
                        (screenHeight + imageHeight) / 2f - 100
                    )
                    canvas.drawBitmap(gameOverImage!!, null, imageRect, paint)
                }

                // Vẽ các nút
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

            // Vẽ HUD
            // 1. Thanh HP
            val hpMax = 6f
            val hpCurrent = spaceshipHP.toFloat()
            val barWidth = 200f
            val barHeight = 100f
            val barX = 20f
            val barY = 200f
            // Vẽ nền thanh (đỏ)
            paint.color = Color.RED
            canvas.drawRect(barX, barY, barX + barWidth, barY + barHeight, paint)

            // Vẽ phần HP còn lại (xanh)
            paint.color = Color.GREEN
            val hpFillWidth = (hpCurrent.toFloat() / hpMax) * barWidth
            canvas.drawRect(barX, barY, barX + hpFillWidth, barY + barHeight, paint)

            // Vẽ text HP lên trên thanh (căn giữa)
            paint.color = Color.YELLOW
            paint.textSize = 50f
            paint.textAlign = Paint.Align.LEFT  // mặc định là LEFT
            fun formatNumber(value: Float): String {
                return if (value % 1 == 0f) value.toInt().toString() else value.toString()
            }
            val text = "HP: ${formatNumber(hpCurrent)} / ${formatNumber(hpMax)}"

            // Đo kích thước text
            val textBounds = Rect()
            paint.getTextBounds(text, 0, text.length, textBounds)

            // Tính toạ độ để text nằm giữa thanh
            val textX = barX + (barWidth - textBounds.width()) / 2f
            val textY = barY + (barHeight + textBounds.height()) / 2f

            canvas.drawText(text, textX, textY, paint)

            // 2. Điểm số
            paint.textAlign = Paint.Align.RIGHT
            canvas.drawText("Score: $score", screenWidth - 20f, 200f, paint)

            // 3. Thời gian chơi
            val elapsedSeconds = (System.currentTimeMillis() - startTime) / 1000
            canvas.drawText("Time: $elapsedSeconds s", screenWidth - 20f, 250f, paint)

            // Vẽ các nút điều khiển âm thanh
            val musicBitmap = if (isMusicOn) musicOnBitmap else musicOffBitmap
            canvas.drawBitmap(musicBitmap, null, musicRect, paint)
            val soundBitmap = if (isSoundOn) soundOnBitmap else soundOffBitmap
            canvas.drawBitmap(soundBitmap, null, soundRect, paint)

            // Vẽ phi thuyền
            canvas.drawBitmap(spaceshipBitmap, spaceshipX, spaceshipY, paint)

            // Vẽ đạn
            paint.color = Color.WHITE
            for (bullet in bullets) {
                canvas.drawRect(bullet.position, paint)
            }

            // Vẽ tên lửa
            paint.color = Color.RED
            for (missile in missiles) {
                canvas.drawRect(missile.position, paint)
            }

            // Vẽ laser
            paint.color = Color.BLUE
            paint.strokeWidth = 5f
            for (laser in lasers) {
                canvas.drawLine(
                    laser.x,
                    laser.spaceshipY,
                    laser.x,
                    0f,
                    paint
                )
            }

            // Vẽ mục tiêu
            for (target in targets) {
                canvas.drawBitmap(target.bitmap, target.position.left, target.position.top, paint)
            }

            // Vẽ power-ups
            for (p in powerUps) {
                canvas.drawBitmap(p.bitmap, p.position.left, p.position.top, paint)
            }

            // Vẽ khiên
            if (isShieldActive) {
                paint.style = Paint.Style.STROKE
                paint.color = Color.GREEN
                paint.strokeWidth = 8f
                canvas.drawCircle(
                    spaceshipX + spaceshipBitmap.width / 2,
                    spaceshipY + spaceshipBitmap.height / 2,
                    spaceshipBitmap.width.toFloat(),
                    paint
                )
                paint.style = Paint.Style.FILL
            }

            // Vẽ giáp
            if (armorActive) {
                paint.style = Paint.Style.STROKE
                paint.color = Color.YELLOW
                paint.strokeWidth = 8f
                canvas.drawCircle(
                    spaceshipX + spaceshipBitmap.width / 2,
                    spaceshipY + spaceshipBitmap.height / 2,
                    spaceshipBitmap.width.toFloat(),
                    paint
                )
                paint.style = Paint.Style.FILL
            }

            holder.unlockCanvasAndPost(canvas)
        }
    }

    fun resetGame() {
        // Reset cờ trạng thái
        isGameOver = false
        isShieldActive = false
        bulletBoostActive = false
        missileActive = false
        laserActive = false
        armorActive = false
        gameOverImage = null

        // Reset HP
        spaceshipHP = 6

        // Reset các danh sách đối tượng
        bullets.clear()
        missiles.clear()
        lasers.clear()
        targets.clear()
        powerUps.clear()

        // Reset điểm, thời gian và frameCount
        score = 0
        startTime = System.currentTimeMillis()
        frameCount = 0

        // Đặt lại vị trí phi thuyền về giữa màn hình
        spaceshipX = (screenWidth - spaceshipBitmap.width) / 2f
        spaceshipY = (screenHeight - spaceshipBitmap.height - 50).toFloat()
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        event?.let {
            val x = event.x
            val y = event.y

            when (it.action) {
                MotionEvent.ACTION_DOWN -> {
                    if (isGameOver) {
                        if (replayButtonRect.contains(x, y)) {
                            resetGame()
                            resume()
                            return true
                        } else if (homeButtonRect.contains(x, y)) {
                            val intent = Intent(context, MainActivity::class.java)  // Thay MainActivity bằng tên Activity menu chính của bạn
                            context.startActivity(intent)
                            (context as AppCompatActivity).finish()  // Kết thúc GameActivity
                            return true
                            return true
                        } else if (highScoresButtonRect.contains(x, y)) {
                            val builder = androidx.appcompat.app.AlertDialog.Builder(context)
                            builder.setTitle("High Scores")
                            builder.setMessage("Điểm cao nhất: $highScore\nĐiểm hiện tại: $score")
                            builder.setPositiveButton("OK") { _, _ -> }
                            builder.show()
                            return true
                        }
                    }

                    if (musicRect.contains(x, y)) {
                        toggleMusic()
                        return true
                    }
                    if (soundRect.contains(x, y)) {
                        toggleSound()
                        return true
                    }

                    moveSpaceship(x, y)
                }

                MotionEvent.ACTION_MOVE -> {
                    if (!musicRect.contains(x, y) && !soundRect.contains(x, y)) {
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

    private fun toggleMusic() {
        isMusicOn = !isMusicOn
        handler.post {
            if (isMusicOn) {
                if (!bgm.isPlaying) {
                    bgm.start()
                }
            } else {
                if (bgm.isPlaying) {
                    bgm.pause()
                }
            }
        }
    }

    private fun toggleSound() {
        isSoundOn = !isSoundOn
    }

    private fun moveSpaceship(x: Float, y: Float) {
        spaceshipX = x - spaceshipBitmap.width / 2
        spaceshipY = y - spaceshipBitmap.height / 2
        val shipRect = RectF(
            spaceshipX,
            spaceshipY,
            spaceshipX + spaceshipBitmap.width,
            spaceshipY + spaceshipBitmap.height
        )
        isShooting = shipRect.contains(x, y)

        if (spaceshipX < 0) {
            spaceshipX = 0f
            if (isSoundOn) soundPool.play(soundHitWall, 1f, 1f, 1, 0, 1f)
        }
        if (spaceshipX + spaceshipBitmap.width > screenWidth) {
            spaceshipX = (screenWidth - spaceshipBitmap.width).toFloat()
            if (isSoundOn) soundPool.play(soundHitWall, 1f, 1f, 1, 0, 1f)
        }
        if (spaceshipY < 0) {
            spaceshipY = 0f
            if (isSoundOn) soundPool.play(soundHitWall, 1f, 1f, 1, 0, 1f)
        }
        if (spaceshipY + spaceshipBitmap.height > screenHeight) {
            spaceshipY = (screenHeight - spaceshipBitmap.height).toFloat()
            if (isSoundOn) soundPool.play(soundHitWall, 1f, 1f, 1, 0, 1f)
        }
    }

    private fun control() {
        try {
            Thread.sleep(17) // ~60 FPS
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
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

    override fun surfaceCreated(holder: SurfaceHolder) {
        resume()
        screenWidth = width
        screenHeight = height

        // Scale spaceship
        val shipWidth = screenWidth / 5
        val shipHeight = shipWidth * spaceshipBitmap.height / spaceshipBitmap.width
        spaceshipBitmap = Bitmap.createScaledBitmap(spaceshipBitmap, shipWidth, shipHeight, true)

        // Scale target
        val targetWidth = screenWidth / 7
        val targetHeight = targetWidth * targetBitmap.height / targetBitmap.width
        targetBitmap = Bitmap.createScaledBitmap(targetBitmap, targetWidth, targetHeight, true)

        // Scale power-ups
        val powerWidth = screenWidth / 13
        val powerHeight = powerWidth * bulletPowerBitmap.height / bulletPowerBitmap.width
        bulletPowerBitmap = Bitmap.createScaledBitmap(bulletPowerBitmap, powerWidth, powerHeight, true)
        shieldPowerBitmap = Bitmap.createScaledBitmap(shieldPowerBitmap, powerWidth, powerHeight, true)
        missilePowerBitmap = Bitmap.createScaledBitmap(missilePowerBitmap, powerWidth, powerHeight, true)
        laserPowerBitmap = Bitmap.createScaledBitmap(laserPowerBitmap, powerWidth, powerHeight, true)
        armorPowerBitmap = Bitmap.createScaledBitmap(armorPowerBitmap, powerWidth, powerHeight, true)
        hpPowerBitmap = Bitmap.createScaledBitmap(hpPowerBitmap, powerWidth, powerHeight, true)

        // Scale Game Over images
        val goWidth = screenWidth / 2
        val goHeight = goWidth * youLoseBitmap.height / youLoseBitmap.width
        youLoseBitmap = Bitmap.createScaledBitmap(youLoseBitmap, goWidth, goHeight, true)
        gameOverBitmap = Bitmap.createScaledBitmap(gameOverBitmap, goWidth, goHeight, true)

        // Scale icon music/sound
        val iconWidth = screenWidth / 8
        val iconHeight = iconWidth * musicOnBitmap.height / musicOnBitmap.width
        musicOnBitmap = musicOnBitmap.scale(iconWidth, iconHeight)
        musicOffBitmap = musicOffBitmap.scale(iconWidth, iconHeight)
        soundOnBitmap = soundOnBitmap.scale(iconWidth, iconHeight)
        soundOffBitmap = soundOffBitmap.scale(iconWidth, iconHeight)

        // Music icon
        val musicLeftOffset = 20f
        musicRect = RectF(
            musicLeftOffset,
            (screenHeight / 2f - iconHeight / 2f),
            musicLeftOffset + iconWidth,
            (screenHeight / 2f + iconHeight / 2f)
        )

        // Sound icon
        val soundRightOffset = 20f
        soundRect = RectF(
            (screenWidth - iconWidth - soundRightOffset),
            (screenHeight / 2f - iconHeight / 2f),
            (screenWidth - soundRightOffset).toFloat(),
            (screenHeight / 2f + iconHeight / 2f)
        )

        // Game Over buttons
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

        // Vị trí ban đầu của phi thuyền
        spaceshipX = (screenWidth / 2 - spaceshipBitmap.width / 2).toFloat()
        spaceshipY = (screenHeight - spaceshipBitmap.height - 50).toFloat()
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        pause()
        if (this::bgm.isInitialized && bgm.isPlaying) {
            bgm.stop()
        }
        if (this::bgm.isInitialized) {
            bgm.release()
        }
    }
}

class Bullet(x: Float, y: Float) {
    val position: RectF
    private val speed = 40f

    init {
        position = RectF(x, y, x + 10, y + 30)
    }

    fun update() {
        position.offset(0f, -speed)
    }
}

class Missile(x: Float, y: Float, private val targets: MutableList<Target>) {
    val position: RectF
    private val speed = 30f

    init {
        position = RectF(x, y, x + 15, y + 40)
    }

    fun update() {
        val nearestTarget = targets.minByOrNull {
            val dx = it.position.centerX() - position.centerX()
            val dy = it.position.centerY() - position.centerY()
            (dx * dx + dy * dy)
        }

        if (nearestTarget != null) {
            val dx = nearestTarget.position.centerX() - position.centerX()
            val dy = nearestTarget.position.centerY() - position.centerY()
            val dist = Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()
            if (dist > 0) {
                val vx = (dx / dist) * speed / 2
                val vy = (dy / dist) * speed / 2
                position.offset(vx, vy)
            } else {
                position.offset(0f, -speed)
            }
        } else {
            position.offset(0f, -speed)
        }
    }
}

class Laser(val x: Float, val spaceshipY: Float) {
    val position: RectF
    init {
        position = RectF(x, 0f, x + 5f, spaceshipY)
    }
}

class Target(x: Float, y: Float, val bitmap: Bitmap) {
    val position: RectF
    private val speed = 4f
    var hasWarned: Boolean = false
    init {
        position = RectF(x, y, x + bitmap.width, y + bitmap.height)
    }
    fun update(screenHeight: Int, screenWidth: Int) {
        position.offset(0f, speed)
        if (position.top > screenHeight) {
            position.offsetTo(position.left, -bitmap.height.toFloat())
        }
    }
}

enum class PowerUpType { BULLET, MISSILE, LASER, SHIELD, ARMOR, HP }

class PowerUp(x: Float, y: Float, val bitmap: Bitmap, val type: PowerUpType) {
    val position: RectF
    private val speed = 6f

    init {
        position = RectF(x, y, x + bitmap.width, y + bitmap.height)
    }

    fun update(screenHeight: Int) {
        position.offset(0f, speed)
        if (position.top > screenHeight) {
            position.offsetTo(position.left, -bitmap.height.toFloat())
        }
    }
}