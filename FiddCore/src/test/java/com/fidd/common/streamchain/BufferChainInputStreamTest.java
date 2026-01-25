package com.fidd.common.streamchain;

import com.fidd.common.streamchain.chain.InputBufferChain;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Queue;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class BufferChainInputStreamTest {
    static class TestChain implements InputBufferChain {
        private final Queue<byte[]> buffers = new ArrayDeque<>();
        protected long available = 0;
        boolean eof = false;

        TestChain add(byte[] data) {
            buffers.add(data);
            available += data.length;
            return this;
        }

        @Override
        public byte[] getNextBuffer() {
            byte[] buffer = buffers.poll();
            if (buffer == null) { return null; }
            available -= buffer.length;
            return buffer;
        }

        @Override
        public long available() {
            return available;
        }

        public void close() {
            eof = true;
        }

        public boolean isClosed() {
            return eof;
        }
    }

    @Test
    void testSingleBufferRead() throws IOException {
        TestChain chain = new TestChain()
                .add("hello".getBytes());
        chain.close();

        try (BufferChainInputStream in = new BufferChainInputStream(chain)) {
            assertEquals('h', in.read());
            assertEquals('e', in.read());
            assertEquals('l', in.read());
            assertEquals('l', in.read());
            assertEquals('o', in.read());
            assertEquals(-1, in.read());
        }
    }

    @Test
    void testMultipleBuffersSequentialRead() throws IOException {
        TestChain chain = new TestChain()
                .add("abc".getBytes())
                .add("def".getBytes())
                .add("ghi".getBytes());
        chain.close();

        try (BufferChainInputStream in = new BufferChainInputStream(chain)) {
            byte[] out = in.readAllBytes();
            assertArrayEquals("abcdefghi".getBytes(), out);
        }
    }

    @Test
    void testBufferSizeRespected() throws IOException {
        byte[] buf = new byte[] { 'x', 'y', 'z' };
        byte[] buf2 = new byte[] { 'q', 'w', 'e' };
        TestChain chain = new TestChain().add(buf).add(buf2); // only "xyz" should be visible
        chain.close();

        try (BufferChainInputStream in = new BufferChainInputStream(chain)) {
            byte[] out = in.readAllBytes();
            assertArrayEquals("xyzqwe".getBytes(), out);
        }
    }

    @Test
    void testReadIntoByteArray() throws IOException {
        TestChain chain = new TestChain().add("hello".getBytes());
        chain.close();

        try (BufferChainInputStream in = new BufferChainInputStream(chain)) {
            byte[] target = new byte[10];
            int n = in.read(target, 2, 5);

            assertEquals(5, n);
            assertEquals('h', target[2]);
            assertEquals('e', target[3]);
            assertEquals('l', target[4]);
            assertEquals('l', target[5]);
            assertEquals('o', target[6]);
        }
    }

    @Test
    void testSkip() throws IOException {
        TestChain chain = new TestChain()
                .add("12345".getBytes())
                .add("67890".getBytes());
        chain.close();

        try (BufferChainInputStream in = new BufferChainInputStream(chain)) {
            long skipped = in.skip(7); // skip "1234567"
            assertEquals(7, skipped);

            assertEquals('8', in.read());
            assertEquals('9', in.read());
            assertEquals('0', in.read());
            assertEquals(-1, in.read());
        }
    }

    @Test
    void testEofOnlyAfterBufferConsumed() throws IOException {
        TestChain chain = new TestChain().add("hi".getBytes());
        chain.close();

        try (BufferChainInputStream in = new BufferChainInputStream(chain)) {
            assertEquals('h', in.read());
            assertEquals('i', in.read());
            assertEquals(-1, in.read());
        }
    }

    @Test
    void testEmptyEofBuffer() throws IOException {
        TestChain chain = new TestChain().add(new byte[0]);
        chain.close();

        try (BufferChainInputStream in = new BufferChainInputStream(chain)) {
            assertEquals(-1, in.read());
        }
    }

    @Test
    void testAvailable() throws IOException {
        TestChain chain = new TestChain()
                .add("abc".getBytes())
                .add("defg".getBytes())
                .add("hijkl".getBytes());
        chain.close();

        byte[] check = "abcdefghijkl".getBytes();

        try (BufferChainInputStream in = new BufferChainInputStream(chain)) {
            assertEquals(12, in.available());
            for (int i = 11; i >= 0; i--) {
                int b = in.read();
                assertEquals(check[check.length - 1 - i], b);
                assertEquals(i, in.available());
            }
            assertEquals(-1, in.read());
        }
    }
}
