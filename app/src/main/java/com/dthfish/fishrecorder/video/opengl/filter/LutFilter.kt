package com.dthfish.fishrecorder.video.opengl.filter

import android.content.Context
import android.opengl.GLES20
import androidx.annotation.DrawableRes
import com.dthfish.fishrecorder.utils.BitmapUtil
import com.dthfish.fishrecorder.utils.GLUtil

/**
 * Description 查表滤镜，查找表（Look-Up-Table)
 * Author DthFish
 * Date  2020-01-22.
 */
class LutFilter(private val context: Context, @DrawableRes private val lutResId: Int) : AFilter() {

    private var fragment = """
        precision mediump float;
        varying vec2 vTextureCoord;
        uniform sampler2D uTexture;
        uniform sampler2D uLutTexture;
       
        void main() {
            vec4 textureColor = texture2D(uTexture, vTextureCoord);
            
            float blueColor = textureColor.b * 63.0;
            
            vec2 quad1;
            quad1.y = floor(floor(blueColor) / 8.0);
            quad1.x = floor(blueColor) - (quad1.y * 8.0);
            
            vec2 quad2;
            quad2.y = floor(ceil(blueColor) / 8.0);
            quad2.x = ceil(blueColor) - (quad2.y * 8.0);
            
            vec2 texPos1;
            texPos1.x = (quad1.x * 0.125) + 0.5/512.0 + ((0.125 - 1.0/512.0) * textureColor.r);
            texPos1.y = (quad1.y * 0.125) + 0.5/512.0 + ((0.125 - 1.0/512.0) * textureColor.g);
            
            vec2 texPos2;
            texPos2.x = (quad2.x * 0.125) + 0.5/512.0 + ((0.125 - 1.0/512.0) * textureColor.r);
            texPos2.y = (quad2.y * 0.125) + 0.5/512.0 + ((0.125 - 1.0/512.0) * textureColor.g);
            
            vec4 newColor1 = texture2D(uLutTexture, texPos1);
            vec4 newColor2 = texture2D(uLutTexture, texPos2);
            
            vec4 newColor = mix(newColor1, newColor2, fract(blueColor));
            gl_FragColor = mix(textureColor, vec4(newColor.rgb, textureColor.w), 1.0);
        }
    """.trimIndent()
    private var lutTextureLoc = 0
    private var lutTextureId = 0

    override fun create() {
        fragmentShader = fragment
        super.create()
        lutTextureLoc = GLES20.glGetUniformLocation(program, "uLutTexture")
        val lutBitmap = BitmapUtil.createBitmapForGL(context, lutResId)
        lutTextureId = GLUtil.createBitmapTextureID(lutBitmap)
        lutBitmap.recycle()
    }

    override fun draw() {
        GLES20.glUseProgram(program)
        GLES20.glActiveTexture(GLES20.GL_TEXTURE1)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, lutTextureId)
        GLES20.glUniform1i(lutTextureLoc, 1)
        GLES20.glUseProgram(0)
        super.draw()
    }
}