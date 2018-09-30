package club.antigfw.network.basic.surelink

import club.antigfw.utils.logger
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import java.nio.ByteBuffer
import java.util.*

class ReedSolomonTest {
    val log = logger()
    private var rand: Random = Random(System.currentTimeMillis())
    private var minSize = 0
    private var maxSize = 0
    private var bufSize = 0
    private var buf = byteArrayOf()

    private val BYTES_IN_INT = 4
    private val DATA_SHARDS = 8
    private val PARITY_SHARDS = 2

    @Before
    fun setup() {
        minSize = 40 * 1024
        maxSize = 128 * 1024
        bufSize = rand.nextInt(maxSize - minSize) + minSize
        buf = ByteArray(bufSize)
        rand.nextBytes(buf)
    }

    @Test
    fun testJavaReedSolomon() {
        val storedSize = buf.size + BYTES_IN_INT
        val shardSize = (storedSize + DATA_SHARDS - 1) / DATA_SHARDS
        log.info { "shardSize is $shardSize" }

        val allBytes = ByteArray(shardSize * DATA_SHARDS)
        ByteBuffer.wrap(allBytes).putInt(buf.size).put(buf)

        val shards = Array<ByteArray>(DATA_SHARDS + PARITY_SHARDS) { ByteArray(shardSize) { 0 } }
        (0 until DATA_SHARDS + PARITY_SHARDS).forEach {
            Assert.assertTrue(shards[it].all { it.toInt() == 0 })
        }

        (0 until DATA_SHARDS).forEach {
            System.arraycopy(allBytes, it * shardSize, shards[it], 0, shardSize)
        }

        (DATA_SHARDS until DATA_SHARDS + PARITY_SHARDS).forEach {
            Assert.assertTrue(shards[it].all { it.toInt() == 0 })
        }

        val shardsCopy = Array<ByteArray>(DATA_SHARDS) { shards[it].copyOf() }

        val reedSolomon = com.backblaze.erasure.ReedSolomon.create(DATA_SHARDS, PARITY_SHARDS);
        reedSolomon.encodeParity(shards, 0, shardSize)

        (DATA_SHARDS until DATA_SHARDS + PARITY_SHARDS).forEach {
            Assert.assertTrue(shards[it].any { it.toInt() != 0 })
        }

        (0 until DATA_SHARDS).forEach {
            shards[it].zip(shardsCopy[it]) { a, b -> Assert.assertEquals(a, b) }
        }
    }

    @Test
    fun testReedSolomon() {
        for (i in 1..200) {
            testReedSolomonOnce()
        }
    }

    fun testReedSolomonOnce() {
        val minBlockSize = 100
        val maxBlockSize = 65535
        val blockSize = rand.nextInt(maxBlockSize - minBlockSize) + minBlockSize

        val minDataBlock = 3
        val maxDataBlock = 64
        val minParityBlock = 1
        val dataBlock = rand.nextInt(maxDataBlock - minDataBlock) + minDataBlock
        val parityBlock = rand.nextInt((dataBlock - minParityBlock)/2)+ rand.nextInt((dataBlock - minParityBlock) - (dataBlock - minParityBlock)/2) + minParityBlock
        log.debug { "data block count is $dataBlock, parity block count is $parityBlock" }

        val data = Array<ByteArray>(dataBlock) {
            val buf = ByteArray(blockSize)
            rand.nextBytes(buf)
            buf
        }

        val dataBak = Array<ByteArray>(dataBlock) {
            val buf = ByteArray(blockSize)
            System.arraycopy(data[it], 0, buf, 0, blockSize)
            buf
        }


        val rs = ReedSolomon(dataBlock, parityBlock)
        val parity = rs.encode(data)
        Assert.assertEquals(parityBlock, parity.size)

        val brokenData = Array<ByteArray?>(dataBlock + parityBlock) {
            if (it < dataBlock)
                data[it]
            else
                parity[it - dataBlock]
        }
        for (i in 0 until parityBlock) {
            val broken = rand.nextInt(dataBlock + parityBlock)
            brokenData[broken] = null
        }

        val recoverData = rs.decode(brokenData)

        (0 until dataBlock).forEach {
            recoverData[it].zip(dataBak[it]) {a, b -> Assert.assertEquals(a, b)}
        }
    }
}
