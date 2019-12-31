package com.dthfish.fishrecorder.video

import com.dthfish.fishrecorder.video.bean.VideoConfig

/**
 * Description
 * Author DthFish
 * Date  2019-12-31.
 */
interface IVideoPackerFactory {

    fun createPacker(config: VideoConfig): IVideoPacker
}