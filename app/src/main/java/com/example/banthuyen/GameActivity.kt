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


    // Thêm cơ chế
    private var bulletBoostActive = false
    private var isShieldActive = false

    // Phi thuyền
    private var spaceshipX: Float = 0f
    private var spaceshipY: Float = 0f
    private var spaceshipBitmap: Bitmap
    private var targetBitmap: Bitmap
    private var bulletsBitmap: Bitmap
    private val bullets = mutableListOf<Bullet>()
    private val targets = mutableListOf<Target>()

    val shootDelay = if (bulletBoostActive) 5 else 15

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
        // Bắn khi đang chạm vào phi thuyền
        if (isShooting && frameCount % shootDelay == 0) {
            bullets.add(Bullet(spaceshipX + spaceshipBitmap.width / 2 - 5, spaceshipY))
            if (isSoundOn) soundPool.play(soundShoot, 1f, 1f, 1, 0, 1f)
        }

        // === Cập nhật tất cả đạn, xóa đạn ra khỏi màn hình ===
        for (i in bullets.size - 1 downTo 0) {
            val b = bullets[i]
            b.update()
            if (b.position.bottom < 0) {
                bullets.removeAt(i)
            }
        }

        // === Cập nhật tất cả mục tiêu ===
        for (i in targets.size - 1 downTo 0) {
            val t = targets[i]
            // Nếu Target.update cần tham số màn hình, truyền vào tương ứng:
            // t.update(screenWidth, screenHeight)
            t.update(screenHeight,screenWidth)
            // ví dụ nếu bạn muốn xóa khi rơi qua đáy:
            if (t.position.top > screenHeight) {
                targets.removeAt(i)
            } else {
                // nếu muốn cảnh báo khi vượt 1/2 màn hình
                if (t.position.top > screenHeight / 2 && !t.hasWarned) {
                    // gán flag trong Target để không warn lặp vô hạn
                    t.hasWarned = true
                    val repeatTimes = (3..6).random()
                    playWarning(repeatTimes, 400)
                }
            }
        }

        // === Spa mục tiêu mới ===
        if (Random.nextInt(100) < 2) {
            val x = Random.nextInt((screenWidth - targetBitmap.width).coerceAtLeast(1)).toFloat()
            targets.add(Target(x, 0f, targetBitmap))
        }

        // === Sinh PowerUp ngẫu nhiên (xác suất nhỏ) ===
        if (Random.nextInt(500) < 2) { // tỉ lệ thấp hơn target
            val x = Random.nextInt((screenWidth - bulletPowerBitmap.width).coerceAtLeast(1)).toFloat()
            val type = if (Random.nextBoolean()) PowerUpType.BULLET else PowerUpType.SHIELD
            val bmp = if (type == PowerUpType.BULLET) bulletPowerBitmap else shieldPowerBitmap
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
                val shipRect = RectF(spaceshipX, spaceshipY, spaceshipX + spaceshipBitmap.width, spaceshipY + spaceshipBitmap.height)
                if (RectF.intersects(p.position, shipRect)) {
                    if (p.type == PowerUpType.BULLET) {
                        bulletBoostActive = true
                        // Hủy bỏ timer cũ nếu có
                        bulletBoostRunnable?.let { handler.removeCallbacks(it) }
                        // Tạo timer mới
                        bulletBoostRunnable = Runnable { bulletBoostActive = false }
                        handler.postDelayed({ bulletBoostActive = false }, 5000) // hiệu lực 5s
                    } else if (p.type == PowerUpType.SHIELD) {
                        isShieldActive = true
                        handler.postDelayed({ isShieldActive = false }, 5000)
                    }
                    if (isSoundOn && soundLoaded[soundPowerUp] == true ) playSound(soundPowerUp)
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
                    // Xử lý va chạm: xóa đúng viên đạn và đúng target
                    bullets.removeAt(i)
                    targets.removeAt(j)

                    // TODO: hiệu ứng nổ / tăng điểm / sound
                    // score += 10
                    //soundPool.play(soundExplosion, 1f, 1f, 1, 0, 1f)
                    hit = true
                    break
                }
            }
            if (hit) continue
        }
        // Kiểm tra phi thuyền va chạm mục tiêu
        val shipRect = RectF(spaceshipX, spaceshipY, spaceshipX + spaceshipBitmap.width, spaceshipY + spaceshipBitmap.height)
        for (i in targets.size - 1 downTo 0) {
            val t = targets[i]
            if (RectF.intersects(t.position, shipRect)) {
                if (isShieldActive) {
                    isShieldActive = false
                    targets.removeAt(i)
                } else {
                    if (isSoundOn) {
                        soundPool.play(soundShipExplosion, 1f, 1f, 1, 0, 1f)
                    }
                    isGameOver = true   // đánh dấu game kết thúc
                    handler.postDelayed({ gameOver() }, 800)  // gọi gameOver một lần
                    break
                }
            }
        }
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
                val musicBitmap = if (isMusicOn) musicOffBitmap else musicOnBitmap
                canvas.drawBitmap(musicBitmap, null, musicRect, paint)
            }

            if (this::soundRect.isInitialized) {
                val soundBitmap = if (isSoundOn) soundOffBitmap else soundOnBitmap
                canvas.drawBitmap(soundBitmap, null, soundRect, paint)
            }

            // Vẽ phi thuyền
            canvas.drawBitmap(spaceshipBitmap, spaceshipX, spaceshipY, paint)

            // Vẽ đạn
            paint.color = Color.WHITE
            for (bullet in bullets) {
                canvas.drawRect(bullet.position, paint)
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
                paint.color = Color.CYAN
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

        // Reset các danh sách đối tượng
        bullets.clear()
        targets.clear()
        powerUps.clear()

        // Reset điểm, frameCount
        frameCount = 0

        // Đặt lại vị trí phi thuyền về giữa màn hình
        spaceshipX = (screenWidth - spaceshipBitmap.width) / 2f
        spaceshipY = (screenHeight - spaceshipBitmap.height - 50).toFloat()

        // Nếu cần spawn mục tiêu ban đầu thì làm ở đây
        // targets.add(Target(...))

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
enum class PowerUpType { BULLET, SHIELD }

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
