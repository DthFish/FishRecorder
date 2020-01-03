package com.dthfish.fishrecorder.video.opengl.filter

/**
 * Description
 * Author DthFish
 * Date  2020-01-02.
 */
class GaryFilter : AFilter() {

    private var vertex = """

        attribute vec4 aPosition;
        attribute vec2 aTextureCoord;
        uniform mat4 uMatrix;
        varying vec2 vTextureCoord;
        void main(){
            gl_Position = uMatrix*aPosition;
            vTextureCoord = aTextureCoord;
        }

    """.trimIndent()

    private var fragment = """
        precision mediump float;
        varying vec2 vTextureCoord;
        uniform sampler2D uTexture;
        void main() {
            vec4 color=texture2D( uTexture, vTextureCoord);
            float rgb=color.g;
            vec4 c=vec4(rgb,rgb,rgb,color.a);
            gl_FragColor = c;
        }
    """.trimIndent()

    override fun create() {
        vertexShader = vertex
        fragmentShader = fragment
        super.create()
    }


}