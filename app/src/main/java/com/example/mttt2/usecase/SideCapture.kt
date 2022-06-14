package com.example.mttt2.usecase

import android.media.Image
import com.example.mttt2.Capture
import com.example.mttt2.ICameraCapture

class SideCapture : ICameraCapture {
    override fun setConfig(config: Map<String, Any>): Boolean {
        return true
    }

    override suspend fun takeCapture(meta: Map<String, Any>): Array<Capture> {
        val outputArray = arrayOf<Capture>() ?: return emptyArray()
        /*
        * TODO:
        * */
        return outputArray
    }
}

data class Capture(
    val image: Image,
    val meta: Map<String, Any>,
)