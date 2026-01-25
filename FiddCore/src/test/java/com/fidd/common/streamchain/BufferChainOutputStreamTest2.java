package com.fidd.common.streamchain;

import com.fidd.common.streamchain.chain.OutputBufferChain;
import org.junit.jupiter.api.*;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BufferChainOutputStreamTest2 {

    static class MockChain implements OutputBufferChain {

        List<byte[]> buffers = new ArrayList<>();
        boolean closed = false;
        BufferFlusher flusher;

        @Override
        public void addBuffer(byte[] buffer) {
            buffers.add(buffer);
        }

        @Override
        public void close() {
            closed = true;
        }

        @Override
        public void attachBufferFlusher(BufferFlusher flusher) {
            this.flusher = flusher;
        }
    }

    @Test
    void constructorRejectsInvalidBufferSize() {
        MockChain chain = new MockChain();
        assertThrows(IllegalArgumentException.class,
                () -> new BufferChainOutputStream(chain, 0));
    }

    @Test
    void writeSingleBytesFlushesWhenFull() {
        MockChain chain = new MockChain();
        BufferChainOutputStream out = new BufferChainOutputStream(chain, 3);

        out.write(1);
        out.write(2);
        out.write(3); // buffer full → flush next write
        assertEquals(0, chain.buffers.size());

        out.write(4); // triggers flush of [1,2,3]
        assertEquals(1, chain.buffers.size());
        assertArrayEquals(new byte[]{1,2,3}, chain.buffers.get(0));
    }

    @Test
    void writeArrayFlushesAsNeeded() {
        MockChain chain = new MockChain();
        BufferChainOutputStream out = new BufferChainOutputStream(chain, 4);

        out.write(new byte[]{10,20,30,40,50}, 0, 5);
        out.flushBuffer();

        assertEquals(2, chain.buffers.size());
        assertArrayEquals(new byte[]{10,20,30,40}, chain.buffers.get(0));
        assertArrayEquals(new byte[]{50}, chain.buffers.get(1));
    }

    @Test
    void writeArrayFlushesAsNeeded2() {
        MockChain chain = new MockChain();
        BufferChainOutputStream out = new BufferChainOutputStream(chain, 4);

        out.write(new byte[]{10,20,30,40,50,60,70,80}, 0, 8);

        assertEquals(2, chain.buffers.size());
        assertArrayEquals(new byte[]{10,20,30,40}, chain.buffers.get(0));
        assertArrayEquals(new byte[]{50,60,70,80}, chain.buffers.get(1));
    }

    @Test
    void writeArrayFlushesAsNeeded3() {
        MockChain chain = new MockChain();
        BufferChainOutputStream out = new BufferChainOutputStream(chain, 4);

        out.write(new byte[]{10,20,30,40,50,60,70,80,90,100}, 0, 8);

        assertEquals(2, chain.buffers.size());
        assertArrayEquals(new byte[]{10,20,30,40}, chain.buffers.get(0));
        assertArrayEquals(new byte[]{50,60,70,80}, chain.buffers.get(1));
    }

    @Test
    void writeArrayRejectsNull() {
        MockChain chain = new MockChain();
        BufferChainOutputStream out = new BufferChainOutputStream(chain, 4);

        assertThrows(NullPointerException.class,
                () -> out.write(null, 0, 1));
    }

    @Test
    void writeArrayRejectsInvalidBounds() {
        MockChain chain = new MockChain();
        BufferChainOutputStream out = new BufferChainOutputStream(chain, 4);

        byte[] data = new byte[5];

        assertThrows(IndexOutOfBoundsException.class,
                () -> out.write(data, -1, 3));

        assertThrows(IndexOutOfBoundsException.class,
                () -> out.write(data, 0, 10));

        assertThrows(IndexOutOfBoundsException.class,
                () -> out.write(data, 3, 3));
    }

    @Test
    void flushWritesRemainingData() {
        MockChain chain = new MockChain();
        BufferChainOutputStream out = new BufferChainOutputStream(chain, 5);

        out.write(7);
        out.flush();

        assertEquals(1, chain.buffers.size());
        assertArrayEquals(new byte[]{7}, chain.buffers.get(0));
    }

    @Test
    void closeFlushesAndClosesChain() {
        MockChain chain = new MockChain();
        BufferChainOutputStream out = new BufferChainOutputStream(chain, 5);

        out.write(9);
        out.close();

        assertEquals(1, chain.buffers.size());
        assertTrue(chain.closed);
    }

    @Test
    void writingAfterCloseThrows() {
        MockChain chain = new MockChain();
        BufferChainOutputStream out = new BufferChainOutputStream(chain, 5);

        out.close();
        assertThrows(IllegalStateException.class, () -> out.write(1));
        assertThrows(IllegalStateException.class, () -> out.flush());
    }

    @Test
    void dataLimitStopsWritingAndAutoCloses() {
        MockChain chain = new MockChain();
        BufferChainOutputStream out = new BufferChainOutputStream(chain, 10, 3L);

        out.write(new byte[]{1,2,3,4,5}, 0, 5);

        // Only 3 bytes allowed
        assertEquals(1, chain.buffers.size());
        assertArrayEquals(new byte[]{1,2,3}, chain.buffers.get(0));

        // Auto-closed
        assertTrue(chain.closed);
        assertThrows(IllegalStateException.class, () -> out.write(6));
    }

    @Test
    void dataLimitTriggersFinalFlushOnExactBoundary() {
        MockChain chain = new MockChain();
        BufferChainOutputStream out = new BufferChainOutputStream(chain, 5, 2L);

        out.write(10);
        out.write(20); // limit reached → flush + close

        assertEquals(1, chain.buffers.size());
        assertArrayEquals(new byte[]{10,20}, chain.buffers.get(0));
        assertTrue(chain.closed);
    }

    @Test
    void flushBufferDoesNothingWhenEmpty() {
        MockChain chain = new MockChain();
        BufferChainOutputStream out = new BufferChainOutputStream(chain, 5);

        out.flushBuffer();
        assertEquals(0, chain.buffers.size());
    }
}
