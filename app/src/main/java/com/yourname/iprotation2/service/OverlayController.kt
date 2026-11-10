package com.yourname.iprotation2.service

import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import com.yourname.iprotation2.engine.RotationState
import kotlin.math.abs

/**
 * Draggable floating bubble drawn over other apps.
 * Tap = expand/collapse. Drag = move. "Stop" button stops the rotation.
 */
class OverlayController(
    private val context: Context,
    private val onStop: () -> Unit
) {
    private val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val slop = ViewConfiguration.get(context).scaledTouchSlop

    private var root: LinearLayout? = null
    private var timerTv: TextView? = null
    private var ipTv: TextView? = null
    private var detailTv: TextView? = null
    private var stopTv: TextView? = null
    private var expanded = true
    private var lastState = RotationState()

    private val params = WindowManager.LayoutParams(
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.WRAP_CONTENT,
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else
            @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
        PixelFormat.TRANSLUCENT
    ).apply {
        gravity = Gravity.TOP or Gravity.START
        x = dp(16)
        y = dp(120)
    }

    private fun dp(v: Int): Int = (v * context.resources.displayMetrics.density).toInt()

    val isShown: Boolean get() = root != null

    fun show() {
        if (root != null) return
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(10), dp(14), dp(10))
            background = GradientDrawable().apply {
                cornerRadius = dp(16).toFloat()
                setColor(Color.parseColor("#EE0B1F4B"))
                setStroke(dp(1), Color.parseColor("#7FE3F5"))
            }
        }
        val timer = TextView(context).apply {
            setTextColor(Color.parseColor("#7FE3F5"))
            textSize = 18f
            setTypeface(typeface, Typeface.BOLD)
        }
        val ip = TextView(context).apply {
            setTextColor(Color.WHITE)
            textSize = 14f
            typeface = Typeface.MONOSPACE
        }
        val detail = TextView(context).apply {
            setTextColor(Color.parseColor("#B8C4E0"))
            textSize = 12f
        }
        val stop = TextView(context).apply {
            text = "■  Stop"
            setTextColor(Color.WHITE)
            textSize = 13f
            gravity = Gravity.CENTER
            setPadding(dp(12), dp(6), dp(12), dp(6))
            background = GradientDrawable().apply {
                cornerRadius = dp(12).toFloat()
                setColor(Color.parseColor("#E53935"))
            }
            setOnClickListener { onStop() }
        }
        container.addView(timer)
        container.addView(ip)
        container.addView(detail)
        container.addView(
            stop,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(8) }
        )

        container.setOnTouchListener(DragListener())

        root = container
        timerTv = timer
        ipTv = ip
        detailTv = detail
        stopTv = stop

        try {
            wm.addView(container, params)
        } catch (e: Exception) {
            root = null
            return
        }
        update(lastState)
    }

    fun hide() {
        val r = root ?: return
        try {
            wm.removeView(r)
        } catch (_: Exception) {
        }
        root = null
        timerTv = null
        ipTv = null
        detailTv = null
        stopTv = null
    }

    fun update(state: RotationState) {
        lastState = state
        if (root == null) return
        timerTv?.text = if (state.running) "⏱ Next: ${state.countdown}s" else "Stopped"
        ipTv?.text = if (state.ip.isBlank()) state.status else state.ip
        val parts = mutableListOf<String>()
        if (state.country.isNotBlank()) parts += "${state.flag} ${state.country}".trim()
        if (state.pingMs >= 0) parts += "${state.pingMs} ms"
        if (state.previousIp.isNotBlank()) parts += "prev: ${state.previousIp}"
        detailTv?.text = parts.joinToString("\n")
        detailTv?.visibility = if (expanded && parts.isNotEmpty()) View.VISIBLE else View.GONE
        stopTv?.visibility = if (expanded) View.VISIBLE else View.GONE
    }

    private inner class DragListener : View.OnTouchListener {
        private var startX = 0
        private var startY = 0
        private var touchX = 0f
        private var touchY = 0f
        private var moved = false

        override fun onTouch(v: View, e: MotionEvent): Boolean {
            when (e.action) {
                MotionEvent.ACTION_DOWN -> {
                    startX = params.x
                    startY = params.y
                    touchX = e.rawX
                    touchY = e.rawY
                    moved = false
                    return true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (e.rawX - touchX).toInt()
                    val dy = (e.rawY - touchY).toInt()
                    if (abs(dx) > slop || abs(dy) > slop) moved = true
                    if (moved) {
                        params.x = startX + dx
                        params.y = startY + dy
                        try {
                            wm.updateViewLayout(v, params)
                        } catch (_: Exception) {
                        }
                    }
                    return true
                }
                MotionEvent.ACTION_UP -> {
                    if (!moved) {
                        expanded = !expanded
                        update(lastState)
                    }
                    return true
                }
            }
            return false
        }
    }
}
