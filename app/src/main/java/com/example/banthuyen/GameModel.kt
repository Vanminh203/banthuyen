package com.example.banthuyen

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.RectF
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import android.os.Handler
import android.os.Looper
import kotlin.math.min
import kotlin.random.Random

class GameModel(private val context: Context) {
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var soundPool: SoundPool
    private var soundShoot = 0
    var soundHitWall = 0
    private var soundWarning = 0
    private var soundPowerUp = 0
    private var soundShipExplosion = 0
    private var soundCongratulation = 0
    private val soundLoaded = mutableMapOf<Int, Boolean>()
    private lateinit var bgm: MediaPlayer
    var isMusicOn = false
    var isSoundOn = true

    val levelManager = LevelManager(context)
    var isLevelComplete = false
    var showLevelTransition = false

    // Game state
    var isGameOver = false
    var score = 0
    var highScore: Int
    var spaceshipHP = 6
    private var startTime = System.currentTimeMillis()
    var frameCount = 0

    // Spaceship
    var spaceshipX: Float = 0f
    var spaceshipY: Float = 0f
    lateinit var spaceshipBitmap: Bitmap

    // Game objects
    lateinit var targetBitmap: Bitmap
    private lateinit var originalTargetBitmap: Bitmap
    lateinit var bulletsBitmap: Bitmap
    val bullets = mutableListOf<Bullet>()
    val targets = mutableListOf<Target>()
    val powerUps = mutableListOf<PowerUp>()
    val missiles = mutableListOf<Missile>()
    val lasers = mutableListOf<Laser>()
    val enemyBullets = mutableListOf<EnemyBullet>()
    private val enemyBitmapCache = mutableMapOf<Int, Bitmap>()

    lateinit var bulletPowerBitmap: Bitmap
    lateinit var shieldPowerBitmap: Bitmap
    lateinit var missilePowerBitmap: Bitmap
    lateinit var laserPowerBitmap: Bitmap
    lateinit var armorPowerBitmap: Bitmap
    lateinit var hpPowerBitmap: Bitmap
    var bulletBoostActive = false
    var missileActive = false
    var laserActive = false
    var isShieldActive = false
    var armorActive = false
    private var bulletBoostRunnable: Runnable? = null

    lateinit var youLoseBitmap: Bitmap
    lateinit var gameOverBitmap: Bitmap
    lateinit var congratulationsBitmap: Bitmap
    var gameOverImage: Bitmap? = null

    lateinit var musicOnBitmap: Bitmap
    lateinit var musicOffBitmap: Bitmap
    lateinit var soundOnBitmap: Bitmap
    lateinit var soundOffBitmap: Bitmap

    private val prefs: SharedPreferences = context.getSharedPreferences("GamePrefs", Context.MODE_PRIVATE)

    init {
        spaceshipBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.phithuyen)
        originalTargetBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.muctieu)
        targetBitmap = originalTargetBitmap.copy(Bitmap.Config.ARGB_8888, true)
        bulletsBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.dan)
        bulletPowerBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.dan)
        shieldPowerBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.khien)
        missilePowerBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.missilepower)
        laserPowerBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.laserpower)
        armorPowerBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.armorpower)
        hpPowerBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.hppower)
        youLoseBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.youlose)
        gameOverBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.gameover)
        congratulationsBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.congratulations)
        musicOnBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.musicturnon)
        musicOffBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.musicturnoff)
        soundOnBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.soundon)
        soundOffBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.soundoff)

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
        soundCongratulation = soundPool.load(context, R.raw.congratulation, 1)

        bgm = MediaPlayer.create(context, R.raw.backgroundmusic)
        bgm.isLooping = true

        highScore = prefs.getInt("highScore", 0)
    }

    fun applyLevelTargetTint() {
        val level = levelManager.getCurrentLevel()
        targetBitmap = levelManager.createTintedBitmap(originalTargetBitmap, level.targetTint)
    }

    fun getTintedBitmapForEnemy(tintColor: Int): Bitmap {
        if (!enemyBitmapCache.containsKey(tintColor)) {
            val tintedBitmap = levelManager.createTintedBitmap(targetBitmap, tintColor)
            enemyBitmapCache[tintColor] = tintedBitmap
        }
        return enemyBitmapCache[tintColor]!!
    }

    fun update(screenWidth: Int, screenHeight: Int, isShooting: Boolean) {
        if (isGameOver || showLevelTransition) return
        frameCount++

        val currentLevel = levelManager.getCurrentLevel()

        if (score >= currentLevel.scoreToWin && !isLevelComplete) {
            isLevelComplete = true
            showLevelTransition = true
            if (isSoundOn) playSound(soundCongratulation)
            return
        }

        if (!missileActive && !laserActive && isShooting && frameCount % (if (bulletBoostActive) 5 else 15) == 0) {
            bullets.add(Bullet(spaceshipX + spaceshipBitmap.width / 2 - 5, spaceshipY))
            if (isSoundOn) playSound(soundShoot)
        }
        if (missileActive && isShooting && frameCount % 30 == 0) {
            missiles.add(Missile(spaceshipX + spaceshipBitmap.width / 2 - 5, spaceshipY, targets))
            if (isSoundOn) playSound(soundShoot)
        }
        if (laserActive && isShooting && frameCount % 10 == 0) {
            lasers.add(Laser(spaceshipX + spaceshipBitmap.width / 2 - 5, spaceshipY))
            if (isSoundOn) playSound(soundShoot)
        }

        for (i in bullets.size - 1 downTo 0) {
            val b = bullets[i]
            b.update()
            if (b.position.bottom < 0) {
                bullets.removeAt(i)
            }
        }

        for (i in missiles.size - 1 downTo 0) {
            val m = missiles[i]
            m.update()
            if (m.position.bottom < 0) {
                missiles.removeAt(i)
            }
        }

        for (i in lasers.size - 1 downTo 0) {
            val l = lasers[i]
            var hitAny = false
            for (j in targets.size - 1 downTo 0) {
                val t = targets[j]
                if (t.position.bottom <= l.spaceshipY && RectF.intersects(l.position, t.position)) {
                    t.hp -= 1
                    if (t.hp <= 0) {
                        targets.removeAt(j)
                        score += 10
                    }
                    hitAny = true
                }
            }
            if (!hitAny) {
                lasers.removeAt(i)
            }
            handler.postDelayed({ lasers.remove(l) }, 200)
        }

        for (i in targets.size - 1 downTo 0) {
            val t = targets[i]
            t.update(screenHeight, screenWidth, currentLevel.targetSpeed)

            if (t.shouldShoot()) {
                enemyBullets.add(EnemyBullet(
                    t.position.centerX() - 4,
                    t.position.bottom
                ))
            }

            if (t.position.top > screenHeight) {
                targets.removeAt(i)
            } else if (t.position.top > screenHeight / 2 && !t.hasWarned) {
                t.hasWarned = true
                val repeatTimes = (3..6).random()
                playWarning(repeatTimes, 400)
            }
        }

        for (i in enemyBullets.size - 1 downTo 0) {
            val eb = enemyBullets[i]
            eb.update()
            if (eb.position.top > screenHeight) {
                enemyBullets.removeAt(i)
            }
        }

        for (enemyConfig in currentLevel.enemyConfigs) {
            if (Random.nextInt(100) < enemyConfig.spawnRate) {
                val x = Random.nextInt((screenWidth - targetBitmap.width).coerceAtLeast(1)).toFloat()
                val tintedBitmap = getTintedBitmapForEnemy(enemyConfig.tintColor)
                targets.add(Target(
                    x, 0f,
                    tintedBitmap,
                    currentLevel.targetSpeed,
                    enemyConfig.type,
                    enemyConfig.hp
                ))
            }
        }

        if (Random.nextInt(500) < currentLevel.powerUpSpawnRate) {
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

        for (i in bullets.size - 1 downTo 0) {
            val b = bullets[i]
            var hit = false
            for (j in targets.size - 1 downTo 0) {
                val t = targets[j]
                if (RectF.intersects(b.position, t.position)) {
                    bullets.removeAt(i)
                    t.hp -= 1
                    if (t.hp <= 0) {
                        targets.removeAt(j)
                        score += 10
                    }
                    hit = true
                    break
                }
            }
            if (hit) continue
        }

        for (i in missiles.size - 1 downTo 0) {
            val m = missiles[i]
            var hit = false
            for (j in targets.size - 1 downTo 0) {
                val t = targets[j]
                if (RectF.intersects(m.position, t.position)) {
                    missiles.removeAt(i)
                    t.hp -= 1
                    if (t.hp <= 0) {
                        targets.removeAt(j)
                        score += 10
                    }
                    hit = true
                    break
                }
            }
            if (hit) continue
        }

        val shipRect = RectF(
            spaceshipX,
            spaceshipY,
            spaceshipX + spaceshipBitmap.width,
            spaceshipY + spaceshipBitmap.height
        )

        for (i in enemyBullets.size - 1 downTo 0) {
            val eb = enemyBullets[i]
            if (RectF.intersects(eb.position, shipRect)) {
                enemyBullets.removeAt(i)
                if (isShieldActive) {
                    isShieldActive = false
                } else if (armorActive) {
                    spaceshipHP -= 1
                    armorActive = false
                    if (spaceshipHP <= 0) {
                        if (isSoundOn) playSound(soundShipExplosion)
                        isGameOver = true
                        if (score > highScore) {
                            highScore = score
                            prefs.edit().putInt("highScore", highScore).apply()
                        }
                        gameOverImage = if (Random.nextBoolean()) youLoseBitmap else gameOverBitmap
                    }
                } else {
                    spaceshipHP -= 1
                    if (spaceshipHP <= 0) {
                        if (isSoundOn) playSound(soundShipExplosion)
                        isGameOver = true
                        if (score > highScore) {
                            highScore = score
                            prefs.edit().putInt("highScore", highScore).apply()
                        }
                        gameOverImage = if (Random.nextBoolean()) youLoseBitmap else gameOverBitmap
                    }
                }
            }
        }

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
                        if (isSoundOn) playSound(soundShipExplosion)
                        isGameOver = true
                        if (score > highScore) {
                            highScore = score
                            prefs.edit().putInt("highScore", highScore).apply()
                        }
                        gameOverImage = if (Random.nextBoolean()) youLoseBitmap else gameOverBitmap
                    }
                } else {
                    spaceshipHP -= 3
                    if (spaceshipHP <= 0) {
                        if (isSoundOn) playSound(soundShipExplosion)
                        isGameOver = true
                        if (score > highScore) {
                            highScore = score
                            prefs.edit().putInt("highScore", highScore).apply()
                        }
                        gameOverImage = if (Random.nextBoolean()) youLoseBitmap else gameOverBitmap
                    }
                }
                break
            }
        }
    }

    fun advanceToNextLevel(screenWidth: Int, screenHeight: Int) {
        if (levelManager.hasNextLevel()) {
            levelManager.nextLevel()
            isLevelComplete = false
            showLevelTransition = false

            targets.clear()
            bullets.clear()
            missiles.clear()
            lasers.clear()
            powerUps.clear()
            enemyBullets.clear()

            enemyBitmapCache.clear()

            applyLevelTargetTint()

            val targetWidth = screenWidth / 7
            val targetHeight = targetWidth * targetBitmap.height / targetBitmap.width
            targetBitmap = Bitmap.createScaledBitmap(targetBitmap, targetWidth, targetHeight, true)
        } else {
            isGameOver = true
            if (score > highScore) {
                highScore = score
                prefs.edit().putInt("highScore", highScore).apply()
            }
            gameOverImage = congratulationsBitmap
        }
    }

    fun resetGame(screenWidth: Int, screenHeight: Int) {
        levelManager.resetToFirstLevel()
        applyLevelTargetTint()

        enemyBitmapCache.clear()

        isGameOver = false
        isLevelComplete = false
        showLevelTransition = false
        isShieldActive = false
        bulletBoostActive = false
        missileActive = false
        laserActive = false
        armorActive = false
        gameOverImage = null
        spaceshipHP = 6
        bullets.clear()
        missiles.clear()
        lasers.clear()
        targets.clear()
        powerUps.clear()
        enemyBullets.clear()
        score = 0
        startTime = System.currentTimeMillis()
        frameCount = 0
        spaceshipX = (screenWidth - spaceshipBitmap.width) / 2f
        spaceshipY = (screenHeight - spaceshipBitmap.height - 50).toFloat()
    }

    fun toggleMusic() {
        isMusicOn = !isMusicOn
        handler.post {
            if (isMusicOn) {
                if (!bgm.isPlaying) bgm.start()
            } else {
                if (bgm.isPlaying) bgm.pause()
            }
        }
    }

    fun toggleSound() {
        isSoundOn = !isSoundOn
    }

    fun playSound(soundId: Int) {
        if (!isSoundOn) return
        val streamId = soundPool.play(soundId, 1f, 1f, 1, 0, 1f)
        if (streamId == 0) {
            android.util.Log.d("SOUND", "Play failed for soundId=$soundId")
        } else {
            android.util.Log.d("SOUND", "Played soundId=$soundId streamId=$streamId")
        }
    }

    fun playWarning(times: Int, interval: Long) {
        for (i in 0 until times) {
            handler.postDelayed({
                if (isSoundOn) soundPool.play(soundWarning, 1f, 1f, 1, 0, 1f)
            }, i * interval)
        }
    }

    fun getElapsedTime(): Long = (System.currentTimeMillis() - startTime) / 1000

    fun cleanup() {
        if (bgm.isPlaying) bgm.stop()
        bgm.release()
        soundPool.release()
    }
}
