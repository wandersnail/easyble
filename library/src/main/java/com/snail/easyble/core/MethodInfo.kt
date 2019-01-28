package com.snail.easyble.core

/**
 *
 *
 * date: 2019/1/28 18:27
 * author: zengfansheng
 */
data class MethodInfo(val name: String, val valueTypePairs: Array<ValueTypePair>?) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MethodInfo) return false

        if (name != other.name) return false
        if (valueTypePairs != null) {
            if (other.valueTypePairs == null) return false
            if (!valueTypePairs.contentEquals(other.valueTypePairs)) return false
        } else if (other.valueTypePairs != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + (valueTypePairs?.contentHashCode() ?: 0)
        return result
    }
}