package com.example.mttt2

import android.media.Image

interface ICameraCapture {
    fun setConfig(config:Map<String,Any>):Boolean
    suspend fun takeCapture(meta:Map<String,Any>):Array<Capture>
}

data class Capture(var image: Image, var meta: Map<String, Any>)

