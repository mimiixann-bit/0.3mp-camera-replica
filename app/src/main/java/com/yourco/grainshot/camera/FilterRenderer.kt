package com.yourco.grainshot.camera

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi

// Clean-room CMOS artifact simulation shader.
// Derives from published literature on CFA aliasing, sensor noise models,
// and blooming. No code from the original app was used or referenced.
private const val CMOS_SHADER_SRC = """
uniform shader  inputShader;
uniform float2  iResolution;
uniform float   uZoom;
uniform float   uTime;
uniform int     uFlash;

float hash(float2 p) {
    p = fract(p * float2(443.897, 441.423));
    p += dot(p, p.yx + 19.19);
    return fract((p.x + p.y) * p.x);
}

half4 bayerSample(shader src, float2 uv, float2 res) {
    float2 px   = uv * res;
    float2 cell = floor(px / 2.0) * 2.0;
    float2 snappedUv = cell / res;
    half4  raw  = src.eval(snappedUv);
    int cx = int(mod(px.x, 2.0));
    int cy = int(mod(px.y, 2.0));
    half4 out = half4(0.0);
    if (cx == 0 && cy == 0)      out = half4(raw.r, 0.0, 0.0, 1.0);
    else if (cx == 1 && cy == 1) out = half4(0.0, 0.0, raw.b, 1.0);
    else                         out = half4(0.0, raw.g, 0.0, 1.0);
    half4 avg = (src.eval(cell / res)
               + src.eval((cell + float2(1,0)) / res)
               + src.eval((cell + float2(0,1)) / res)
               + src.eval((cell + float2(1,1)) / res)) * 0.25;
    return mix(avg, out, 0.35);
}

half3 darkNoise(half3 col, float2 uv, float zoom) {
    if (uFlash == 1) return col;
    float lum      = dot(col.rgb, half3(0.299, 0.587, 0.114));
    float noiseMag = (1.0 - lum) * 0.07 * clamp(zoom * 0.6, 1.0, 2.5);
    float n1 = hash(uv + uTime) * 2.0 - 1.0;
    float n2 = hash(uv + uTime + 7.3) * 2.0 - 1.0;
    float gaussian = (n1 + n2) * 0.5;
    return clamp(col + half3(gaussian * noiseMag), 0.0, 1.0);
}

half3 banding(half3 col, float2 uv) {
    float band = sin(uv.y * iResolution.y * 0.5 + uTime * 3.0) * 0.012
               + sin(uv.y * iResolution.y * 0.13) * 0.008;
    return clamp(col + half3(band), 0.0, 1.0);
}

half3 blooming(shader src, half3 col, float2 uv, float2 res) {
    half4 wide = (src.eval(uv + float2( 2.0 / res.x, 0.0))
                + src.eval(uv + float2(-2.0 / res.x, 0.0))
                + src.eval(uv + float2(0.0,  2.0 / res.y))
                + src.eval(uv + float2(0.0, -2.0 / res.y))) * 0.25;
    float highlights = max(wide.r, max(wide.g, wide.b));
    float bloom = smoothstep(0.78, 1.0, highlights) * 0.18;
    return clamp(col + col * bloom, 0.0, 1.2);
}

half3 colourGrade(half3 col) {
    col = col * 0.88 + 0.045;
    col.r *= 1.04; col.g *= 1.08; col.b *= 0.92;
    col = col / (col + 0.3) * 1.3;
    return clamp(col, 0.0, 1.0);
}

float2 zoomUv(float2 uv, float zoom) {
    float2 center  = float2(0.5, 0.5);
    float2 delta   = uv - center;
    float2 zoomed  = center + delta / zoom;
    float  step    = zoom / min(iResolution.x, iResolution.y);
    zoomed = floor(zoomed / step) * step;
    return zoomed;
}

half4 main(float2 fragCoord) {
    float2 uv     = fragCoord / iResolution;
    float2 sampUv = (uZoom > 1.001) ? zoomUv(uv, uZoom) : uv;
    half4  col4   = bayerSample(inputShader, sampUv, iResolution);
    half3  col    = col4.rgb;
    col = darkNoise(col, sampUv, uZoom);
    col = banding(col, sampUv);
    col = blooming(inputShader, col, sampUv, iResolution);
    col = colourGrade(col);
    return half4(col, 1.0);
}
"""

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
object FilterRenderer {
    fun buildRenderEffect(
        width: Int, height: Int,
        zoom: Float, flashOn: Boolean, timeMs: Long,
    ): RenderEffect {
        val shader = RuntimeShader(CMOS_SHADER_SRC)
        shader.setFloatUniform("iResolution", width.toFloat(), height.toFloat())
        shader.setFloatUniform("uZoom", zoom.coerceAtLeast(1f))
        shader.setIntUniform("uFlash", if (flashOn) 1 else 0)
        shader.setFloatUniform("uTime", (timeMs % 10_000L) / 1000f)
        return RenderEffect.createRuntimeShaderEffect(shader, "inputShader")
    }
}
