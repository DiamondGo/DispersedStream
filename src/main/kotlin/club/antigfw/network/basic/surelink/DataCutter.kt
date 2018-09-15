package club.antigfw.network.basic.surelink

import kotlinx.coroutines.experimental.channels.Channel
import java.nio.ByteBuffer
import kotlin.math.min


class DataCutter(val blockSize: Int) {
    private val defaultQueueSize = 64
    private val outgoingBuf = Channel<ByteArray>(defaultQueueSize)
    private var buffer: ByteBuffer = ByteBuffer.allocate(blockSize)

    suspend fun offer(bytes : ByteArray) : Boolean {
        var read = 0
        while (read < bytes.size) {
            val len = min(bytes.size - read, buffer.remaining())
            buffer.put(bytes, read, len)
            if (!buffer.hasRemaining()) {
                outgoingBuf.send(buffer.array())
                buffer = ByteBuffer.allocate(blockSize)
            }
            read += len
        }
        return true
    }

    suspend fun close() {
        if (buffer.position() > 0) {
            outgoingBuf.send(buffer.array().sliceArray(0 until  buffer.position()))
        }
        outgoingBuf.close()
    }

    suspend fun poll() : ByteArray? = outgoingBuf.receiveOrNull()

}