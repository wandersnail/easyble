package com.snail.easyble.util

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.*

/**
 * date: 2018/4/24 15:27
 * author: zengfansheng
 */
object BleUtils {
    fun bytesToHexString(src: ByteArray?): String {
        val stringBuilder = StringBuilder()
        if (src == null || src.isEmpty()) {
            return ""
        }
        for (aSrc in src) {
            val v = aSrc.toInt() and 0xFF
            val hv = Integer.toHexString(v)
            if (hv.length < 2) {
                stringBuilder.append(0)
            }
            stringBuilder.append(hv)
            stringBuilder.append(" ")
        }
        return stringBuilder.toString().toUpperCase(Locale.ENGLISH)
    }

    /**
     * 将整数转字节数组
     *
     * @param bigEndian ture表示高位在前，false表示低位在前
     * @param value     整数，short、int、long
     * @param len 结果取几个字节，如是高位在前，从数组后端向前计数；如是低位在前，从数组前端向后计数
     */
    fun numberToBytes(bigEndian: Boolean, value: Long, len: Int): ByteArray {
        val bytes = ByteArray(8)
        for (i in 0..7) {
            val j = if (bigEndian) 7 - i else i
            bytes[i] = (value shr 8 * j and 0xFF).toByte()
        }
        return if (len > 8) bytes else Arrays.copyOfRange(bytes, if (bigEndian) 8 - len else 0, if (bigEndian) 8 else len)
    }

    /**
     * 将字节数组转long数值
     *
     * @param bigEndian ture表示高位在前，false表示低位在前
     * @param src       待转字节数组
     */
    fun bytesToLong(bigEndian: Boolean, vararg src: Byte): Long {
        val len = Math.min(8, src.size)
        val bs = ByteArray(8)
        System.arraycopy(src, 0, bs, if (bigEndian) 8 - len else 0, len)
        var value: Long = 0
        // 循环读取每个字节通过移位运算完成long的8个字节拼装
        for (i in 0..7) {
            val shift = (if (bigEndian) 7 - i else i) shl 3
            value = value or (0xff.toLong() shl shift and (bs[i].toLong() shl shift))
        }
        return when {
            src.size == 1 -> value.toByte().toLong()
            src.size == 2 -> value.toShort().toLong()
            src.size <= 4 -> value.toInt().toLong()
            else -> value
        }
    }

    /**
     * 分包
     * @param src 源
     * @param size 包大小，字节
     * @return 分好的包的集合
     */
    fun splitPackage(src: ByteArray, size: Int): List<ByteArray> {
        val list = ArrayList<ByteArray>()
        val loopCount = src.size / size + if (src.size % size == 0) 0 else 1
        for (j in 0 until loopCount) {
            val from = j * size
            val to = Math.min(src.size, from + size)
            list.add(Arrays.copyOfRange(src, j * size, to))
        }
        return list
    }

    /**
     * 合包
     * @param src 源
     * @return 合好的字节数组
     */
    fun joinPackage(vararg src: ByteArray): ByteArray {
        var bytes = ByteArray(0)
        for (bs in src) {
            bytes = Arrays.copyOf(bytes, bytes.size + bs.size)
            System.arraycopy(bs, 0, bytes, bytes.size - bs.size, bs.size)
        }
        return bytes
    }

    /**
     * 16-bit Service Class UUIDs或32-bit Service Class UUIDs
     */
    fun generateFromBaseUuid(paramLong: Long): UUID {
        return UUID(4096L + (paramLong shl 32), -9223371485494954757L)
    }

    /**
     * 128-bit Service Class UUIDs
     */
    fun generateBluetoothUuid(bytes: ByteArray): UUID {
        if (bytes.size != 8) {
            throw IllegalArgumentException("ByteArray's size must be 8")
        }
        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        val lsb = buffer.long
        val msb = buffer.long
        return UUID(msb, lsb)
    } 
}
