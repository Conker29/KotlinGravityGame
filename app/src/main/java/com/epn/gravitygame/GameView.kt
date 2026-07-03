package com.epn.gravitygame

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.random.Random

class GameView(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {

    private var sensorX = 0f
    private var sensorY = 0f

    private val ball = Ball()
    private val obstacles = mutableListOf<Obstacle>()
    private val targets = mutableListOf<Target>()

    private var score = 0
    private var lives = 3
    private var gameStarted = false
    private var gameOver = false

    private val handler = Handler(Looper.getMainLooper())
    private val frameDelay = 16L

    private val hudBackgroundPaint = Paint().apply { color = Color.WHITE }
    private val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK; textSize = 60f; isFakeBoldText = true
    }
    private val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.DKGRAY; textSize = 40f
    }
    private val coordPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.GRAY; textSize = 36f
    }
    private val overlayPaint = Paint().apply { color = Color.argb(245, 255, 255, 255) }
    private val startTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK; textSize = 70f; isFakeBoldText = true
    }
    private val startBodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.DKGRAY; textSize = 38f
    }

    private val gameLoop = object : Runnable {
        override fun run() {
            if (gameStarted && !gameOver) updateGame()
            invalidate()
            handler.postDelayed(this, frameDelay)
        }
    }

    init {
        handler.post(gameLoop)
    }

    // Llamado desde MainActivity.onSensorChanged()
    fun updateSensorValues(x: Float, y: Float) {
        sensorX = x
        sensorY = y
    }

    private fun updateGame() {
        val w = width
        val h = height
        if (w == 0 || h == 0) return

        if (Collision.hitEdge(ball.position.x, ball.position.y, ball.radius(), w, h)) {
            onEdgeHit()
        }

        ball.update(sensorX, sensorY, w, h)

        for (obstacle in obstacles) {
            if (Collision.circleRect(ball.position.x, ball.position.y, ball.radius(), obstacle.rect())) {
                onObstacleHit()
                break
            }
        }

        for (target in targets) {
            if (!target.collected &&
                Collision.circleCircle(ball.position.x, ball.position.y, ball.radius(), target.position.x, target.position.y, target.radius())
            ) {
                target.collected = true
                score += 10
            }
        }

        if (targets.isNotEmpty() && targets.all { it.collected }) {
            spawnTargets(w, h)
        }
    }

    private var lastEdgeHitTime = 0L
    private fun onEdgeHit() {
        val now = System.currentTimeMillis()
        if (now - lastEdgeHitTime > 800) {
            lastEdgeHitTime = now
            loseLife()
        }
    }

    private var lastObstacleHitTime = 0L
    private fun onObstacleHit() {
        val now = System.currentTimeMillis()
        if (now - lastObstacleHitTime > 800) {
            lastObstacleHitTime = now
            loseLife()
        }
    }

    private fun loseLife() {
        lives--
        if (lives <= 0) gameOver = true
    }

    private fun spawnObstacles(w: Int, h: Int) {
        obstacles.clear()
        repeat(6) {
            val x = Random.nextInt(0, (w - 160).coerceAtLeast(1)).toFloat()
            val y = Random.nextInt(200, (h - 200).coerceAtLeast(201)).toFloat()
            obstacles.add(Obstacle(x, y))
        }
    }

    private fun spawnTargets(w: Int, h: Int) {
        targets.clear()
        repeat(3) {
            val x = Random.nextInt(50, (w - 50).coerceAtLeast(51)).toFloat()
            val y = Random.nextInt(200, (h - 200).coerceAtLeast(201)).toFloat()
            targets.add(Target(Vector2(x, y)))
        }
    }

    private fun resetGame() {
        score = 0
        lives = 3
        gameOver = false
        ball.position = Vector2(width / 2f, height / 2f)
        spawnObstacles(width, height)
        spawnTargets(width, height)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            if (!gameStarted || gameOver) {
                resetGame()
                gameStarted = true
            }
        }
        return true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(Color.rgb(240, 244, 248))

        obstacles.forEach { it.draw(canvas) }
        targets.forEach { it.draw(canvas) }
        ball.draw(canvas)

        canvas.drawRect(0f, 0f, width.toFloat(), 160f, hudBackgroundPaint)
        canvas.drawText("Gravity Ball Kotlin", 30f, 60f, titlePaint)
        canvas.drawText("Puntaje: $score   Vidas: $lives", 30f, 120f, subtitlePaint)
        canvas.drawText(
            "X: ${ball.position.x.toInt()}  Y: ${ball.position.y.toInt()}",
            width - 260f, 60f, coordPaint
        )

        if (!gameStarted || gameOver) drawOverlay(canvas)
    }

    private fun drawOverlay(canvas: Canvas) {
        val centerX = width / 2f
        val centerY = height / 2f

        canvas.drawRect(60f, centerY - 260f, width - 60f, centerY + 260f, overlayPaint)

        if (gameOver) {
            canvas.drawText("¡Juego terminado!", centerX - 260f, centerY - 150f, startTitlePaint)
            canvas.drawText("Puntaje final: $score", centerX - 200f, centerY - 60f, startBodyPaint)
            canvas.drawText("Toca para volver a jugar", centerX - 230f, centerY + 20f, startBodyPaint)
        } else {
            canvas.drawText("Toca para iniciar", centerX - 220f, centerY - 150f, startTitlePaint)
            canvas.drawText("Inclina el celular para mover la bolita.", centerX - 260f, centerY - 60f, startBodyPaint)
            canvas.drawText("Atrapa objetivos verdes y evita obstáculos rojos.", centerX - 300f, centerY, startBodyPaint)
        }
    }
}