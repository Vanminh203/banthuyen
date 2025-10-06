package com.example.banthuyen

import android.graphics.Bitmap
import android.graphics.RectF
import kotlin.math.sin
import kotlin.math.sqrt

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
            val dist = sqrt((dx * dx + dy * dy).toDouble()).toFloat()
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

enum class EnemyType {
    BASIC,      // Di chuyển thẳng (Level 1)
    ZIGZAG,     // Di chuyển zigzag (Level 2)
    SHOOTER,    // Bắn lại người chơi (Level 2)
    BOSS        // Boss mạnh (Level 3)
}

class Target(
    x: Float,
    y: Float,
    val bitmap: Bitmap,
    private val speed: Float = 4f,
    val type: EnemyType = EnemyType.BASIC,
    var hp: Int = 1
) {
    val position: RectF
    var hasWarned: Boolean = false
    private var frameCount = 0
    private var zigzagDirection = 1f

    init {
        position = RectF(x, y, x + bitmap.width, y + bitmap.height)
    }

    fun update(screenHeight: Int, screenWidth: Int, levelSpeed: Float = speed) {
        frameCount++

        when (type) {
            EnemyType.BASIC -> {
                // Di chuyển thẳng xuống
                position.offset(0f, levelSpeed)
            }
            EnemyType.ZIGZAG -> {
                // Di chuyển zigzag
                position.offset(0f, levelSpeed)
                val zigzagSpeed = 3f
                position.offset(zigzagSpeed * zigzagDirection, 0f)

                // Đổi hướng khi chạm biên
                if (position.left <= 0 || position.right >= screenWidth) {
                    zigzagDirection *= -1
                }
            }
            EnemyType.SHOOTER, EnemyType.BOSS -> {
                // Di chuyển chậm hơn
                position.offset(0f, levelSpeed * 0.5f)
            }
        }

        if (position.top > screenHeight) {
            position.offsetTo(position.left, -bitmap.height.toFloat())
        }
    }

    fun shouldShoot(): Boolean {
        // Shooter và Boss bắn định kỳ
        return (type == EnemyType.SHOOTER || type == EnemyType.BOSS) && frameCount % 60 == 0
    }
}

class EnemyBullet(x: Float, y: Float) {
    val position: RectF
    private val speed = 15f

    init {
        position = RectF(x, y, x + 8, y + 20)
    }

    fun update() {
        position.offset(0f, speed)
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
