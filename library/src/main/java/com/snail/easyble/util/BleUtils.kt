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
        for (i in 0 until src.size) {
            val hv = Integer.toHexString(src[i].toInt() and 0xFF)
            if (hv.length < 2) {
                stringBuilder.append(0)
            }
            stringBuilder.append(hv)
            if (src.size - 1 != i) {
                stringBuilder.append(" ")
            }
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
            bytes = bytes.copyOf(bytes.size + bs.size)
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

    /**
     * Does raw bytes of scan record contain the uuid
     */
    fun hasUuid(uuid: UUID, advData: ByteArray?): Boolean {
        return hasUuid(listOf(uuid), advData)
    }

    /**
     * Does raw bytes of scan record contains one uuid in the list
     */
    fun hasUuid(uuids: List<UUID>, advData: ByteArray?): Boolean {
        try {
            val buffer = ByteBuffer.wrap(advData).order(ByteOrder.LITTLE_ENDIAN)
            while (buffer.remaining() > 2) {
                var length = buffer.get().toInt()
                if (length == 0) break

                when (buffer.get().toInt()) {
                    0x02, // Partial list of 16-bit UUIDs
                    0x03, // Complete list of 16-bit UUIDs
                    0x14 // List of 16-bit Service Solicitation UUIDs    
                    -> while (length >= 2) {
                        if (uuids.contains(UUID.fromString(String.format(Locale.US, "%08x-0000-1000-8000-00805f9b34fb", buffer.short)))) {
                            return true
                        }
                        length -= 2
                    }
                    0x04,
                    0x05
                    -> while (length >= 4) {
                        if (uuids.contains(UUID.fromString(String.format(Locale.US, "%08x-0000-1000-8000-00805f9b34fb", buffer.int)))) {
                            return true
                        }
                        length -= 4
                    }
                    0x06, // Partial list of 128-bit UUIDs
                    0x07, // Complete list of 128-bit UUIDs
                    0x15 // List of 128-bit Service Solicitation UUIDs
                    -> while (length >= 16) {
                        val lsb = buffer.long
                        val msb = buffer.long
                        if (uuids.contains(UUID(msb, lsb))) {
                            return true
                        }
                        length -= 16
                    }
                    else -> buffer.position(buffer.position() + length - 1)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return false
    }
}
