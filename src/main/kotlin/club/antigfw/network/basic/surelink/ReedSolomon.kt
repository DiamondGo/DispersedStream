package club.antigfw.network.basic.surelink

import club.antigfw.utils.logger

class ReedSolomon(val dataBlockCount: Int, val parityBlockCount: Int) {
    private val log = logger()
    private val reedSolomon = com.backblaze.erasure.ReedSolomon.create(dataBlockCount, parityBlockCount)!!

    /**
     * Input data ByteArrays, get parity ByteArrays
     */
    fun encode(byteArrays: List<ByteArray>): Array<ByteArray> {
        if (byteArrays.size != dataBlockCount || byteArrays.distinctBy { it.size }.count() > 1) {
            throw InvalidInputException("Data blocks are not the same size.")
        }

        val shardSize = byteArrays.first().size
        val shards = Array(dataBlockCount + parityBlockCount) {
            if (it < dataBlockCount)
                byteArrays[it]
            else
                ByteArray(shardSize)
        }

        reedSolomon.encodeParity(shards, 0, shardSize)

        return Array(parityBlockCount) { shards[it + dataBlockCount] }
    }

    fun encode(byteArrays: Array<ByteArray>): Array<ByteArray> {
        return encode(byteArrays.toList())
    }

    /**
     * Decode
     */
    fun decode(byteArrays: List<ByteArray?>): Array<ByteArray> {
        if (byteArrays.size != dataBlockCount + parityBlockCount
                || byteArrays.count { it != null } < dataBlockCount
                || byteArrays.filter { it != null }.distinctBy { it?.size }.count() > 1) {
            throw InvalidInputException("Input encoded byte arrays are invalid")
        }

        log.debug { "received ${byteArrays.take(dataBlockCount).count { it != null }} data blocks and total ${byteArrays.count { it != null }} blocks" }

        if (byteArrays.take(dataBlockCount).all { it != null }) {
            log.debug { "all $dataBlockCount data blocks are integral." }
            return Array<ByteArray>(dataBlockCount) { byteArrays[it]!! }
        }

        val shardSize = byteArrays.first { it != null }?.size!!
        val shards = Array<ByteArray>(dataBlockCount + parityBlockCount) {
            if (byteArrays[it] == null)
                ByteArray(shardSize)
            else
                byteArrays[it]!!
        }
        val shardPresent = BooleanArray(dataBlockCount + parityBlockCount) {
            byteArrays[it] != null
        }

        reedSolomon.decodeMissing(shards, shardPresent, 0, shardSize)

        return shards.sliceArray((0 .. dataBlockCount))
    }

    fun decode(byteArrays: Array<ByteArray?>): Array<ByteArray> {
        return decode(byteArrays.toList())
    }
}