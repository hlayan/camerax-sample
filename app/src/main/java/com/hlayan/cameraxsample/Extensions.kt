package com.hlayan.cameraxsample

import android.annotation.SuppressLint
import android.graphics.*
import android.media.Image
import android.util.Rational
import androidx.camera.core.ImageProxy
import androidx.camera.core.internal.utils.ImageUtil
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import kotlin.math.roundToInt

fun Image.toBitmap(): Bitmap {
    val yBuffer = planes[0].buffer // Y
    val vuBuffer = planes[2].buffer // VU

    val ySize = yBuffer.remaining()
    val vuSize = vuBuffer.remaining()

    val nv21 = ByteArray(ySize + vuSize)

    yBuffer.get(nv21, 0, ySize)
    vuBuffer.get(nv21, ySize, vuSize)

    val yuvImage = YuvImage(nv21, ImageFormat.NV21, width, height, null)
    val out = ByteArrayOutputStream()
    yuvImage.compressToJpeg(Rect(0, 0, yuvImage.width, yuvImage.height), 50, out)
    val imageBytes = out.toByteArray()
    return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
}

fun Image.toBitmap1(): Bitmap {
    val planes: Array<Image.Plane> = planes

    val yBuffer: ByteBuffer = planes[0].buffer
    val uBuffer: ByteBuffer = planes[1].buffer
    val vBuffer: ByteBuffer = planes[2].buffer

    val ySize: Int = yBuffer.remaining()
    val uSize: Int = uBuffer.remaining()
    val vSize: Int = vBuffer.remaining()

    val nv21 = ByteArray(ySize + uSize + vSize)

    //U and V are swapped
    yBuffer.get(nv21, 0, ySize)
    vBuffer.get(nv21, ySize, vSize)
    uBuffer.get(nv21, ySize + vSize, uSize)

    val yuvImage = YuvImage(nv21, ImageFormat.NV21, width, height, null)
    val out = ByteArrayOutputStream()
    yuvImage.compressToJpeg(Rect(0, 0, yuvImage.width, yuvImage.height), 100, out)
    val imageBytes: ByteArray = out.toByteArray()
    return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
}

@SuppressLint("RestrictedApi")
fun ImageProxy.decodeBitmap(): Bitmap? {
//    val buffer = planes[0].buffer
//    val data = ByteArray(buffer.capacity())
//    buffer.rewind()
//    buffer[data]

    val data = ImageUtil.jpegImageToJpegByteArray(this)

    val srcBmp = BitmapFactory.decodeByteArray(data, 0, data.size)

//    val newHeight = (193f / 295f) * srcBmp.width
    val newHeight = (srcBmp.width / 295f * 193).roundToInt()

    val cropTop = (srcBmp.height - newHeight) / 2

    return Bitmap.createBitmap(srcBmp, 0, cropTop, srcBmp.width, newHeight)
}

fun Bitmap.cropCenter(aspectRatio: Rational): Bitmap? {

//    val newHeight = ((aspectRatio.denominator.toFloat() / aspectRatio.numerator) * width).roundToInt()
//    var cropTop = (height - newHeight) / 2

    val srcRatio: Float = width / height.toFloat()

    return if (aspectRatio.toFloat() > srcRatio) {
        val outputHeight =
            (width / aspectRatio.numerator.toFloat() * aspectRatio.denominator).roundToInt()
        val cropTop = (height - outputHeight) / 2
        Bitmap.createBitmap(this, 0, cropTop, width, outputHeight)
    } else {
        val outputWidth =
            (height / aspectRatio.denominator.toFloat() * aspectRatio.numerator).roundToInt()
        val cropLeft = (width - outputWidth) / 2
        Bitmap.createBitmap(this, cropLeft, 0, outputWidth, height)
    }
}

fun ImageProxy.getRect(aspectRatio: Rational): Rect {

    val srcRatio: Float = width / height.toFloat()

    return if (aspectRatio.toFloat() > srcRatio) {
        val outputHeight =
            (width / aspectRatio.numerator.toFloat() * aspectRatio.denominator).roundToInt()
        val cropTop = (height - outputHeight) / 2
        Rect(0, cropTop, width, outputHeight)
    } else {
        val outputWidth =
            (height / aspectRatio.denominator.toFloat() * aspectRatio.numerator).roundToInt()
        val cropLeft = (width - outputWidth) / 2
        Rect(cropLeft, 0, outputWidth, height)
    }
}

fun ImageProxy.toByteArray(): ByteArray {
    val buffer = planes[0].buffer
    val data = ByteArray(buffer.capacity())
    buffer.rewind()
    buffer[data]
    return data
}

fun ImageProxy.toBitmap(): Bitmap {
    val byteArray = toByteArray()
    return BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
}
