package com.prime.llamachat.helpers

class Helpers {
    fun floatArrayToByteArray(array: FloatArray): ByteArray {
        val buffer = java.nio.ByteBuffer.allocate(4 * array.size)
        array.forEach { buffer.putFloat(it) }
        return buffer.array()
    }

    fun byteArrayToFloatArry(bytes: ByteArray): FloatArray {
        val buffer = java.nio.ByteBuffer.wrap(bytes)
        val floats = FloatArray(bytes.size / 4)
        for (i in floats.indices) {
            floats[i] = buffer.float
        }
        return floats
    }
}