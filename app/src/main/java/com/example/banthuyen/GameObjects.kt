package com.example.banthuyen

import android.graphics.Bitmap
import android.graphics.RectF
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