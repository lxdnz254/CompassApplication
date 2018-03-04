package com.lxdnz.nz.compassapplication

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

/**
 * Created by alex on 4/03/18.
 */
class CompassView : View {
    private var wdt = 0
    private var ht = 0
    private var matrx: Matrix? = null // to manage rotation of the compass view
    private var bitmap: Bitmap? = null
    private var bearing: Float = 0.toFloat() // rotation angle to North

    constructor(context: Context) : super(context) {
        initialize()
    }

    constructor(context: Context, attr: AttributeSet) : super(context, attr) {
        initialize()
    }

    private fun initialize() {
        matrx = Matrix()
        // create bitmap for compass icon
        bitmap = BitmapFactory.decodeResource(resources,
                R.drawable.compass_icon)
    }

    fun setBearing(b: Float) {
        bearing = b
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        wdt = View.MeasureSpec.getSize(widthMeasureSpec)
        ht = View.MeasureSpec.getSize(heightMeasureSpec)
        setMeasuredDimension(wdt, ht)
    }

    override fun onDraw(canvas: Canvas) {
        val bitmapWidth = bitmap!!.width
        val bitmapHeight = bitmap!!.height
        val canvasWidth = canvas.width
        val canvasHeight = canvas.height

        if (bitmapWidth > canvasWidth || bitmapHeight > canvasHeight) {
            // resize bitmap to fit in canvas
            bitmap = Bitmap.createScaledBitmap(bitmap!!,
                    (bitmapWidth * 0.85).toInt(), (bitmapHeight * 0.85).toInt(), true)
        }

        // center
        val bitmapX = bitmap!!.width / 2
        val bitmapY = bitmap!!.height / 2
        val parentX = wdt / 2
        val parentY = ht / 2
        val centerX = parentX - bitmapX
        val centerY = parentY - bitmapY

        // calculate rotation angle
        val rotation = (360 - bearing).toInt()

        // reset matrix
        matrx!!.reset()
        matrx!!.setRotate(rotation.toFloat(), bitmapX.toFloat(), bitmapY.toFloat())
        // center bitmap on canvas
        matrx!!.postTranslate(centerX.toFloat(), centerY.toFloat())
        // draw bitmap
        canvas.drawBitmap(bitmap!!, matrx!!, paint)
    }

    companion object {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    }
}
