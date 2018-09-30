package club.antigfw.network.basic.surelink

import com.backblaze.erasure.ReedSolomon
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.runBlocking
import org.junit.Assert
import org.junit.BeforeClass
import org.junit.Test
import java.util.*

class DataCutterTest {
    companion object {
        @JvmStatic
        @BeforeClass
        fun init() {
        }
    }

    @Test
    fun testDataCutter() {
        val rand = Random(System.currentTimeMillis())
        val blockSize = rand.nextInt(100) + 1
        println("blockSize is $blockSize")
        val cutter = DataCutter(blockSize)

        val send = launch {
            var sent = 0
            (0..10000).map { rand.nextInt(10000) + 1 }.forEach {
                val bytes = ByteArray(it) { ((sent + it) % 256).toByte() }
                cutter.offer(bytes)
                sent += it
            }
            cutter.close()
        }

        val recv = launch {
            var recvd = 0
            var lastSize = -1
            while (true) {
                val bytes = cutter.poll() ?: break
                if (lastSize > 0) {
                    Assert.assertEquals(blockSize, lastSize)
                }
                (0 until bytes.size).forEach {
                    Assert.assertEquals(bytes[it], ((recvd + it) % 256).toByte())
                }
                recvd += bytes.size
                lastSize = bytes.size
            }
            println("lastBlockSize is $lastSize")
        }

        runBlocking {
            send.join()
            recv.join()
        }
    }

    @Test
    fun testDataCutter2() {
        (0..10).forEach {
            testDataCutter()
        }
    }

}