package com.yourco.grainshot.camera

import android.graphics.Bitmap
import kotlin.math.*
import kotlin.random.Random

object BitmapFilter {

    fun apply(
        src: Bitmap,
        zoomLevel: Float = 1f,
        flashOn: Boolean = false,
        seed: Long = 0L,
    ): Bitmap {
        val w = src.width; val h = src.height
        val px = IntArray(w * h)
        src.getPixels(px, 0, w, 0, 0, w, h)
        val rng = Random(seed)
        bayerPass(px, w, h)
        if (!flashOn) darkNoisePass(px, w, h, zoomLevel, rng)
        bandingPass(px, w, h, rng)
        bloomingPass(px, w, h)
        colourGradePass(px)
        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        out.setPixels(px, 0, w, 0, 0, w, h)
        return out
    }

    private fun bayerPass(px: IntArray, w: Int, h: Int) {
        for (y in 0 until h step 2) for (x in 0 until w step 2) {
            val c00 = px[idx(x, y, w)]
            val c10 = if (x+1 < w) px[idx(x+1, y, w)] else c00
            val c01 = if (y+1 < h) px[idx(x, y+1, w)] else c00
            val c11 = if (x+1 < w && y+1 < h) px[idx(x+1, y+1, w)] else c00
            val aR = (c00.r+c10.r+c01.r+c11.r)/4
            val aG = (c00.g+c10.g+c01.g+c11.g)/4
            val aB = (c00.b+c10.b+c01.b+c11.b)/4
            fun blend(ch: Int, avg: Int) = ((avg*65+ch*35)/100).coerceIn(0,255)
            px[idx(x,y,w)] = argb(blend(c00.r,aR),blend(0,aG),blend(0,aB))
            if (x+1<w) px[idx(x+1,y,w)] = argb(blend(0,aR),blend(c10.g,aG),blend(0,aB))
            if (y+1<h) px[idx(x,y+1,w)] = argb(blend(0,aR),blend(c01.g,aG),blend(0,aB))
            if (x+1<w&&y+1<h) px[idx(x+1,y+1,w)] = argb(blend(0,aR),blend(0,aG),blend(c11.b,aB))
        }
    }

    private fun darkNoisePass(px: IntArray, w: Int, h: Int, zoom: Float, rng: Random) {
        val amp = (zoom*0.6f).coerceIn(1f,2.5f)
        for (i in px.indices) {
            val lum = (px[i].r*0.299+px[i].g*0.587+px[i].b*0.114)/255.0
            val mag = (1.0-lum)*0.07*amp
            val n   = ((rng.nextFloat()+rng.nextFloat())/2f-0.5f)*2f
            val d   = (n*mag*255).toInt()
            px[i] = argb((px[i].r+d).coerceIn(0,255),(px[i].g+d).coerceIn(0,255),(px[i].b+d).coerceIn(0,255))
        }
    }

    private fun bandingPass(px: IntArray, w: Int, h: Int, rng: Random) {
        val phase = rng.nextFloat() * PI.toFloat()
        for (y in 0 until h) {
            val d = ((sin(y*0.5f+phase)*0.012f+sin(y*0.13f)*0.008f)*255).toInt()
            for (x in 0 until w) {
                val i = idx(x,y,w)
                px[i] = argb((px[i].r+d).coerceIn(0,255),(px[i].g+d).coerceIn(0,255),(px[i].b+d).coerceIn(0,255))
            }
        }
    }

    private fun bloomingPass(px: IntArray, w: Int, h: Int) {
        val out = px.copyOf()
        for (y in 0 until h) for (x in 0 until w) {
            val nb = listOf(safeGet(px,x+2,y,w,h),safeGet(px,x-2,y,w,h),safeGet(px,x,y+2,w,h),safeGet(px,x,y-2,w,h))
            val aR=nb.map{it.r}.average(); val aG=nb.map{it.g}.average(); val aB=nb.map{it.b}.average()
            val hi = maxOf(aR,aG,aB)/255.0
            val t  = ((hi-0.78)/(1.0-0.78)).coerceIn(0.0,1.0)
            val bl = t*t*(3-2*t)*0.18
            val i  = idx(x,y,w)
            out[i] = argb((px[i].r+px[i].r*bl).toInt().coerceIn(0,255),(px[i].g+px[i].g*bl).toInt().coerceIn(0,255),(px[i].b+px[i].b*bl).toInt().coerceIn(0,255))
        }
        out.copyInto(px)
    }

    private fun colourGradePass(px: IntArray) {
        for (i in px.indices) {
            var r=px[i].r/255f; var g=px[i].g/255f; var b=px[i].b/255f
            r=r*0.88f+0.045f; g=g*0.88f+0.045f; b=b*0.88f+0.045f
            r*=1.04f; g*=1.08f; b*=0.92f
            r=r/(r+0.3f)*1.3f; g=g/(g+0.3f)*1.3f; b=b/(b+0.3f)*1.3f
            px[i]=argb((r*255).toInt().coerceIn(0,255),(g*255).toInt().coerceIn(0,255),(b*255).toInt().coerceIn(0,255))
        }
    }

    private fun idx(x: Int, y: Int, w: Int) = y*w+x
    private fun safeGet(px: IntArray, x: Int, y: Int, w: Int, h: Int) =
        if (x in 0 until w && y in 0 until h) px[idx(x,y,w)] else 0
    private fun argb(r: Int, g: Int, b: Int) = (0xFF shl 24)or(r shl 16)or(g shl 8)or b
    private val Int.r get() = (this shr 16) and 0xFF
    private val Int.g get() = (this shr 8)  and 0xFF
    private val Int.b get() = this           and 0xFF
}
