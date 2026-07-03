package com.epn.gravitygame

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
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

    // ---- HUD ----
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

    // ---- Pantalla de inicio / fin ----
    private val overlayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(245, 255, 255, 255)
    }
    private val welcomePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(147, 51, 234) // morado, a juego con la bola
        textSize = 44f
        isFakeBoldText = true
    }
    private val bigTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        textSize = 80f
        isFakeBoldText = true
    }
    private val gameOverTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        textSize = 60f
        isFakeBoldText = true
    }
    private val taglinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(100, 100, 100)
        textSize = 34f
    }
    private val sectionLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(147, 51, 234)
        textSize = 36f
        isFakeBoldText = true
    }
    private val instructionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.DKGRAY
        textSize = 36f
    }
    private val ctaPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 42f
        isFakeBoldText = true
        textAlign = Paint.Align.CENTER
    }
    private val ctaBackgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(147, 51, 234)
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
        val newObstacles = mutableListOf<Obstacle>()
        var attempts = 0

        while (newObstacles.size < 6 && attempts < 100) {
            attempts++
            val x = Random.nextInt(0, (w - 160).coerceAtLeast(1)).toFloat()
            val y = Random.nextInt(200, (h - 200).coerceAtLeast(201)).toFloat()
            val candidate = Obstacle(x, y)

            val overlaps = newObstacles.any { existing ->
                RectF.intersects(candidate.rect(), existing.rect())
            }

            if (!overlaps) newObstacles.add(candidate)
        }

        obstacles.addAll(newObstacles)
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

        // HUD
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
        val cardLeft = 50f
        val cardRight = width - 50f
        val cardTop = centerY - 340f
        val cardBottom = centerY + 340f

        // Tarjeta de fondo
        canvas.drawRoundRect(cardLeft, cardTop, cardRight, cardBottom, 40f, 40f, overlayPaint)

        if (gameOver) {
            canvas.drawText("¡Juego terminado!", cardLeft + 40f, cardTop + 120f, gameOverTitlePaint)
            canvas.drawText("Puntaje final: $score", cardLeft + 40f, cardTop + 210f, taglinePaint)
            canvas.drawText("Perdiste tus 3 vidas. ¡Sigue intentando! 💪", cardLeft + 40f, cardTop + 270f, instructionPaint)

            val btnTop = cardBottom - 140f
            canvas.drawRoundRect(centerX - 240f, btnTop, centerX + 240f, btnTop + 90f, 30f, 30f, ctaBackgroundPaint)
            canvas.drawText("Toca para volver a jugar", centerX, btnTop + 58f, ctaPaint)
        } else {
            // Saludo personal
            canvas.drawText("¡Bienvenido! 👋", cardLeft + 40f, cardTop + 90f, welcomePaint)

            // Título grande del juego
            canvas.drawText("Gravity Ball", cardLeft + 40f, cardTop + 175f, bigTitlePaint)
            canvas.drawText("Kotlin Edition", cardLeft + 40f, cardTop + 220f, taglinePaint)

            // Sección: cómo jugar
            canvas.drawText("🎮  Cómo jugar", cardLeft + 40f, cardTop + 300f, sectionLabelPaint)
            canvas.drawText("• Inclina el celular para mover la bolita.", cardLeft + 40f, cardTop + 350f, instructionPaint)
            canvas.drawText("• Atrapa los objetivos verdes 🟢 para sumar puntos.", cardLeft + 40f, cardTop + 395f, instructionPaint)
            canvas.drawText("• Evita los obstáculos rojos 🔴 y los bordes.", cardLeft + 40f, cardTop + 440f, instructionPaint)
            canvas.drawText("• Tienes 3 vidas. ¡No las pierdas todas!", cardLeft + 40f, cardTop + 485f, instructionPaint)

            val btnTop = cardBottom - 120f
            canvas.drawRoundRect(centerX - 220f, btnTop, centerX + 220f, btnTop + 90f, 30f, 30f, ctaBackgroundPaint)
            canvas.drawText("¡Toca para empezar!", centerX, btnTop + 58f, ctaPaint)
        }
    }
}