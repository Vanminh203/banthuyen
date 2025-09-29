package com.example.banthuyen

import android.content.Context
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
import kotlin.math.pow

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

    // HP cho phi thuyền (để hỗ trợ cơ chế armor)
    private var spaceshipHP = 6

    init {
        holder.addCallback(this)
        spaceshipBitmap = BitmapFactory.decodeResource(resources, R.drawable.phithuyen)
        targetBitmap = BitmapFactory.decodeResource(resources, R.drawable.muctieu)
        bulletsBitmap = BitmapFactory.decodeResource(resources, R.drawable.dan)
        paint = Paint()

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
        val shootDelay = if (bulletBoostActive) 5 else 15  // bắn nhanh hơn khi có buff
        // Bắn đạn thường chỉ khi không có missile hoặc laser active
        if (!missileActive && !laserActive && isShooting && frameCount % shootDelay == 0) {
            bullets.add(Bullet(spaceshipX + spaceshipBitmap.width / 2 - 5, spaceshipY))
            if (isSoundOn) soundPool.play(soundShoot, 1f, 1f, 1, 0, 1f)
        }

        // Thêm bắn tên lửa nếu active (cơ chế tấn công 2: bắn tên lửa)
        if (missileActive && isShooting && frameCount % 30 == 0) {
            missiles.add(Missile(spaceshipX + spaceshipBitmap.width / 2 - 5, spaceshipY, targets))
            if (isSoundOn) soundPool.play(soundShoot, 1f, 1f, 1, 0, 1f)
        }

        // Thêm phun laser nếu active (cơ chế tấn công 3: phun laser liên tục)
        if (laserActive && isShooting && frameCount % 10 == 0) {
            lasers.add(Laser(spaceshipX + spaceshipBitmap.width / 2 - 5, spaceshipY))
            if (isSoundOn) soundPool.play(soundShoot, 1f, 1f, 1, 0, 1f)  // Có thể dùng sound laser
        }

        // === Cập nhật tất cả đạn, xóa đạn ra khỏi màn hình ===
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

        // Thêm phun laser nếu active
        if (laserActive && isShooting && frameCount % 10 == 0) { // Giảm tần suất để cân bằng
            lasers.clear() // Xóa laser cũ
            lasers.add(Laser(spaceshipX + spaceshipBitmap.width / 2 - 2.5f, spaceshipY))
            if (isSoundOn) soundPool.play(soundShoot, 1f, 1f, 1, 0, 1f)
        }

        // Va chạm laser (tiêu diệt tất cả mục tiêu nằm trên đường laser)
        // Va chạm laser (laser có thể xuyên qua nhiều mục tiêu, nhưng chỉ phía trên)
        for (i in lasers.size - 1 downTo 0) {
            val l = lasers[i]
            var hitAny = false  // Track nếu hit ít nhất 1 target
            for (j in targets.size - 1 downTo 0) {
                val t = targets[j]
                // Chỉ hit nếu target ở phía trên hoặc tại vị trí bắn (t.bottom <= l.spaceshipY)
                if (t.position.bottom <= l.spaceshipY && RectF.intersects(l.position, t.position)) {
                    targets.removeAt(j)
                    hitAny = true
                }
            }
            // Nếu không hit gì, xóa laser ngay
            if (!hitAny) {
                lasers.removeAt(i)
            }
            // Optional: Nếu muốn xóa laser sau 0.5s dù hit hay không (để không persist mãi), thêm:
            handler.postDelayed({ lasers.remove(l) }, 200)
        }

        // === Cập nhật tất cả mục tiêu ===
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

        // === Spawn mục tiêu mới ===
        if (Random.nextInt(100) < 1.5) {
            val x = Random.nextInt((screenWidth - targetBitmap.width).coerceAtLeast(1)).toFloat()
            targets.add(Target(x, 0f, targetBitmap))
        }

        // === Sinh PowerUp ngẫu nhiên (xác suất nhỏ) ===
        if (Random.nextInt(500) < 2.5) { // tỉ lệ thấp hơn target
            val x =
                Random.nextInt((screenWidth - bulletPowerBitmap.width).coerceAtLeast(1)).toFloat()
            val type = PowerUpType.entries.random()  // Chọn ngẫu nhiên từ enum mở rộng
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

        // === Cập nhật PowerUp ===
        for (i in powerUps.size - 1 downTo 0) {
            val p = powerUps[i]
            p.update(screenHeight)
            if (p.position.top > screenHeight) {
                powerUps.removeAt(i)
            } else {
                // Kiểm tra va chạm với phi thuyền
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
                            armorActive = false // Vô hiệu hóa các phòng thủ khác
                            handler.postDelayed({ isShieldActive = false }, 5000)
                        }

                        PowerUpType.ARMOR -> {
                            armorActive = true
                            isShieldActive = false // Vô hiệu hóa các phòng thủ khác
                            handler.postDelayed({ armorActive = false }, 5000)
                        }
                        PowerUpType.HP -> {
                            spaceshipHP = minOf(spaceshipHP + 1, 6)  // +1 HP, cap max
                        }
                    }
                    if (isSoundOn && soundLoaded[soundPowerUp] == true) playSound(soundPowerUp)
                    powerUps.removeAt(i)
                }
            }
        }

// === Va chạm: mỗi viên đạn chỉ tiêu diệt 1 mục tiêu ===
        for (i in bullets.size - 1 downTo 0) {
            val b = bullets[i]
            var hit = false
            for (j in targets.size - 1 downTo 0) {
                val t = targets[j]
                if (RectF.intersects(b.position, t.position)) {
                    bullets.removeAt(i)
                    targets.removeAt(j)
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
                    hit = true
                    break
                }
            }
            if (hit) continue
        }

// Va chạm laser (laser có thể xuyên qua nhiều mục tiêu)
        for (i in lasers.size - 1 downTo 0) {
            val l = lasers[i]
            for (j in targets.size - 1 downTo 0) {
                val t = targets[j]
                if (RectF.intersects(l.position, t.position)) {
                    targets.removeAt(j)
                }
            }
        }

// Kiểm tra phi thuyền va chạm mục tiêu
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
                    isShieldActive = false // Vô hiệu hóa shield sau một lần sử dụng
                } else if (armorActive) {
                    spaceshipHP -= 1 // Giảm HP
                    armorActive = false // Vô hiệu hóa armor sau một lần sử dụng
                    if (spaceshipHP <= 0) {
                        if (isSoundOn) soundPool.play(soundShipExplosion, 1f, 1f, 1, 0, 1f)
                        isGameOver = true
                        handler.postDelayed({ gameOver() }, 800)
                    }
                }  else {
                    spaceshipHP -= 3 // Giảm HP
                    if (spaceshipHP <= 0) {
                        if (isSoundOn) soundPool.play(soundShipExplosion, 1f, 1f, 1, 0, 1f)
                        isGameOver = true
                        handler.postDelayed({ gameOver() }, 800)
                    }
                }
                break
            }
        }
    }

    private fun findNearestTarget(): Target? {
        if (targets.isEmpty()) return null
        return targets.minByOrNull { (it.position.top - spaceshipY).pow(2) + (it.position.left - spaceshipX).pow(2) }
    }

    private fun playSound(soundId: Int) {
        if (!isSoundOn) return
        val streamId = soundPool.play(soundId, 1f, 1f, 1, 0, 1f)
        if (streamId == 0) {
            android.util.Log.d("SOUND", "Play failed for soundId=$soundId (chưa load xong?)")
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
            if (this::musicRect.isInitialized) {
                val musicBitmap = if (isMusicOn) musicOnBitmap else musicOffBitmap
                canvas.drawBitmap(musicBitmap, null, musicRect, paint)
            }

            if (this::soundRect.isInitialized) {
                val soundBitmap = if (isSoundOn) soundOnBitmap else soundOffBitmap
                canvas.drawBitmap(soundBitmap, null, soundRect, paint)
            }
            // Vẽ thanh máu (HP bar)
            val hpMax = 6f  // HP tối đa
            val hpCurrent = spaceshipHP.toFloat()
            val barWidth = 200f  // Chiều rộng thanh
            val barHeight = 100f  // Chiều cao thanh
            val barX = 20f  // Vị trí X
            val barY = 200f  // Vị trí Y (góc trên trái)

            // Vẽ nền thanh (đỏ)
            paint.color = Color.RED
            canvas.drawRect(barX, barY, barX + barWidth, barY + barHeight, paint)

            // Vẽ phần HP còn lại (xanh)
            paint.color = Color.GREEN
            val hpFillWidth = (hpCurrent.toFloat() / hpMax) * barWidth
            canvas.drawRect(barX, barY, barX + hpFillWidth, barY + barHeight, paint)

            // Vẽ text HP lên trên thanh (căn giữa)
            paint.color = Color.BLACK
            paint.textSize = 40f
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

            // Vẽ phi thuyền
            canvas.drawBitmap(spaceshipBitmap, spaceshipX, spaceshipY, paint)

            // Vẽ đạn
            paint.color = Color.WHITE
            for (bullet in bullets) {
                canvas.drawRect(bullet.position, paint)
            }

            // Vẽ tên lửa (giả sử vẽ hình chữ nhật đỏ cho missile)
            paint.color = Color.RED
            for (missile in missiles) {
                canvas.drawRect(missile.position, paint)
            }

            // Vẽ laser (đường xanh thẳng từ phi thuyền đến y=0)
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

            // Nếu có khiên, vẽ vòng tròn xanh quanh phi thuyền
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

            // Vẽ giáp (nếu active, vẽ vòng vàng)
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

            // Vẽ HP (text đơn giản)
            paint.color = Color.WHITE
            paint.textSize = 40f

            canvas.drawText(text, 20f, 60f, paint)
            holder.unlockCanvasAndPost(canvas)
        }
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        event?.let {
            val x = event.x
            val y = event.y

            when (it.action) {
                MotionEvent.ACTION_DOWN -> {
                    // === ƯU TIÊN KIỂM TRA ICON ===
                    if (musicRect.contains(x, y)) {
                        toggleMusic()
                        return true
                    }
                    if (soundRect.contains(x, y)) {
                        toggleSound()
                        return true
                    }

                    // === NẾU KHÔNG CLICK ICON => ĐIỀU KHIỂN PHI THUYỀN ===
                    moveSpaceship(x, y)
                }

                MotionEvent.ACTION_MOVE -> {
                    // Chỉ di chuyển phi thuyền, không đụng icon
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
        // Cập nhật vị trí phi thuyền
        spaceshipX = x - spaceshipBitmap.width / 2
        spaceshipY = y - spaceshipBitmap.height / 2

        // Kiểm tra chạm vào phi thuyền => bắn
        val shipRect = RectF(
            spaceshipX,
            spaceshipY,
            spaceshipX + spaceshipBitmap.width,
            spaceshipY + spaceshipBitmap.height
        )
        isShooting = shipRect.contains(x, y)

        // Giới hạn biên + âm thanh va chạm
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

    private fun gameOver() {
        isPlaying = false
        if (isSoundOn) {
            // Đợi 1 giây cho âm thanh chạy rồi mới show dialog
            handler.postDelayed({ showGameOverDialog() }, 1000)
        } else {
            showGameOverDialog()
        }
    }

    private fun showGameOverDialog() {
        handler.post {
            val builder = androidx.appcompat.app.AlertDialog.Builder(context)
            builder.setTitle("Game Over")
            builder.setMessage("Bạn đã thua! Muốn chơi lại không?")
            builder.setCancelable(false)
            builder.setPositiveButton("Restart") { _, _ ->
                resetGame()
                resume()
            }
            builder.setNegativeButton("Exit") { _, _ ->
                (context as AppCompatActivity).finish()
            }
            builder.show()
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

        // Reset HP
        spaceshipHP = 6

        // Reset các danh sách đối tượng
        bullets.clear()
        missiles.clear()
        lasers.clear()
        targets.clear()
        powerUps.clear()

        // Reset điểm, frameCount
        frameCount = 0

        // Đặt lại vị trí phi thuyền về giữa màn hình
        spaceshipX = (screenWidth - spaceshipBitmap.width) / 2f
        spaceshipY = (screenHeight - spaceshipBitmap.height - 50).toFloat()
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

        // ================= Scale spaceship =================
        val shipWidth = screenWidth / 5
        val shipHeight = shipWidth * spaceshipBitmap.height / spaceshipBitmap.width
        spaceshipBitmap = Bitmap.createScaledBitmap(spaceshipBitmap, shipWidth, shipHeight, true)

        // ================= Scale target =================
        val targetWidth = screenWidth / 7
        val targetHeight = targetWidth * targetBitmap.height / targetBitmap.width
        targetBitmap = Bitmap.createScaledBitmap(targetBitmap, targetWidth, targetHeight, true)

        // ================= Scale power-ups =================
        val powerWidth = screenWidth / 13   // chiếm ~1/13 chiều rộng màn hình
        val powerHeight = powerWidth * bulletPowerBitmap.height / bulletPowerBitmap.width
        bulletPowerBitmap = Bitmap.createScaledBitmap(bulletPowerBitmap, powerWidth, powerHeight, true)
        shieldPowerBitmap = Bitmap.createScaledBitmap(shieldPowerBitmap, powerWidth, powerHeight, true)
        missilePowerBitmap = Bitmap.createScaledBitmap(missilePowerBitmap, powerWidth, powerHeight, true)
        laserPowerBitmap = Bitmap.createScaledBitmap(laserPowerBitmap, powerWidth, powerHeight, true)
        armorPowerBitmap = Bitmap.createScaledBitmap(armorPowerBitmap, powerWidth, powerHeight, true)
        hpPowerBitmap = Bitmap.createScaledBitmap(hpPowerBitmap, powerWidth, powerHeight, true)

        // ================= Scale icon music/sound =================
        val iconWidth = screenWidth / 8
        val iconHeight = iconWidth * musicOnBitmap.height / musicOnBitmap.width
        musicOnBitmap = Bitmap.createScaledBitmap(musicOnBitmap, iconWidth, iconHeight, true)
        musicOffBitmap = Bitmap.createScaledBitmap(musicOffBitmap, iconWidth, iconHeight, true)
        soundOnBitmap = Bitmap.createScaledBitmap(soundOnBitmap, iconWidth, iconHeight, true)
        soundOffBitmap = Bitmap.createScaledBitmap(soundOffBitmap, iconWidth, iconHeight, true)

        // Music icon ở chính giữa cạnh trái
        val musicLeftOffset = 20f
        musicRect = RectF(
            musicLeftOffset,
            (screenHeight / 2f - iconHeight / 2f),
            musicLeftOffset + iconWidth,
            (screenHeight / 2f + iconHeight / 2f)
        )

        // Sound icon ở chính giữa cạnh phải
        val soundRightOffset = 20f
        soundRect = RectF(
            (screenWidth - iconWidth - soundRightOffset),
            (screenHeight / 2f - iconHeight / 2f),
            (screenWidth - soundRightOffset).toFloat(),
            (screenHeight / 2f + iconHeight / 2f)
        )

        // ================= Vị trí ban đầu của phi thuyền =================
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

// =================== CLASS BULLET ===================
class Bullet(x: Float, y: Float) {
    val position: RectF
    private val speed = 40f

    init {
        position = RectF(x, y, x + 10, y + 30) // viên đạn hình chữ nhật
    }

    fun update() {
        position.offset(0f, -speed) // bay lên trên
    }
}

// =================== CLASS MISSILE (Cơ chế tấn công 2: Tên lửa tự dẫn) ===================
class Missile(x: Float, y: Float, private val targets: MutableList<Target>) {
    val position: RectF
    private val speed = 30f

    init {
        position = RectF(x, y, x + 15, y + 40) // lớn hơn đạn thường
    }

    fun update() {
        // Tìm mục tiêu gần nhất dựa trên khoảng cách Euclidean
        val nearestTarget = targets.minByOrNull {
            val dx = it.position.centerX() - position.centerX()
            val dy = it.position.centerY() - position.centerY()
            (dx * dx + dy * dy) // Bình phương khoảng cách
        }

        if (nearestTarget != null) {
            val dx = nearestTarget.position.centerX() - position.centerX()
            val dy = nearestTarget.position.centerY() - position.centerY()
            val dist = Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()
            if (dist > 0) {
                val vx = (dx / dist) * speed / 2 // Tốc độ homing
                val vy = (dy / dist) * speed / 2 // Không ưu tiên bay lên
                position.offset(vx, vy)
            } else {
                position.offset(0f, -speed) // Bay thẳng nếu mục tiêu trùng vị trí
            }
        } else {
            position.offset(0f, -speed) // Bay thẳng nếu không có mục tiêu
        }
    }
}

// =================== CLASS LASER (Cơ chế tấn công 3: Laser liên tục) ===================
class Laser(val x: Float, val spaceshipY: Float) {
    val position: RectF
    init {
        position = RectF(x, 0f, x + 5f, spaceshipY) // Đường từ phi thuyền đến y=0
    }
}

// =================== CLASS TARGET ===================
class Target(x: Float, y: Float, val bitmap: Bitmap) {
    val position: RectF
    private val speed = 4f
    // Flag để chỉ cảnh báo 1 lần khi vượt nửa màn hình
    var hasWarned: Boolean = false
    init {
        position = RectF(x, y, x + bitmap.width, y + bitmap.height)
    }
    fun update(screenHeight: Int, screenWidth: Int ) {
        position.offset(0f, speed) // rơi xuống dưới
        // Nếu đi quá dưới → xuất hiện lại trên
        if (position.top > screenHeight) {
            position.offsetTo(position.left, -bitmap.height.toFloat())
        }
    }
}

// =================== CLASS POWERUP ===================
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