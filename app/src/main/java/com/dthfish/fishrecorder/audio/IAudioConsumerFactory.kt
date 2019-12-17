package com.dthfish.fishrecorder.audio

import com.dthfish.fishrecorder.audio.bean.AudioConfig

/**
 * Description 用于创建消费 [AudioRecorder] 产生的 pcm 数据的消费者的工厂类
 * Author DthFish
 * Date  2019-12-05.
 */
interface IAudioConsumerFactory {

    fun createConsumer(config: AudioConfig): IAudioConsumer
}