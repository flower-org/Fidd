package com.fidd.common.streamchain;

import com.fidd.common.streamchain.chain.OutputBufferChain;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BufferChainOutputStreamTest {
    static class TestOutputChain implements OutputBufferChain {
        final List<byte[]> buffers = new ArrayList<>();

        @Override
        public void addBuffer(byte[] buffer) {
            buffers.add(buffer);
        }

        @Override
        public void close() {
            System.out.println("EOF");
        }

        @Override
        public void attachBufferFlusher(BufferFlusher flusher) {
        }
    }

    @Test
    void testSingleByteWrites() throws IOException {
        TestOutputChain chain = new TestOutputChain();
        BufferChainOutputStream out = new BufferChainOutputStream(chain, 4);

        out.write('a');
        out.write('b');
        out.write('c');
        out.write('d'); // buffer full → should flush on next write
        out.write('e');

        out.close();

        assertEquals(2, chain.buffers.size());
        assertArrayEquals("abcd".getBytes(), chain.buffers.get(0));
        assertArrayEquals("e".getBytes(), chain.buffers.get(1));
    }

    @Test
    void testWriteByteArray() throws IOException {
        TestOutputChain chain = new TestOutputChain();
        BufferChainOutputStream out = new BufferChainOutputStream(chain, 5);

        out.write("hello world".getBytes());
        out.close();

        assertEquals(3, chain.buffers.size());
        assertArrayEquals("hello".getBytes(), chain.buffers.get(0));
        assertArrayEquals(" worl".getBytes(), chain.buffers.get(1));
        assertArrayEquals("d".getBytes(), chain.buffers.get(2));
    }

    @Test
    void testFlushEmitsPartialBuffer() throws IOException {
        TestOutputChain chain = new TestOutputChain();
        BufferChainOutputStream out = new BufferChainOutputStream(chain, 10);

        out.write("abc".getBytes());
        out.flush();

        assertEquals(1, chain.buffers.size());
        assertArrayEquals("abc".getBytes(), chain.buffers.get(0));

        out.close();
        assertEquals(1, chain.buffers.size()); // no extra buffer
    }

    @Test
    void testCloseEmitsRemainingBytes() throws IOException {
        TestOutputChain chain = new TestOutputChain();
        BufferChainOutputStream out = new BufferChainOutputStream(chain, 4);

        out.write("xy".getBytes());
        out.close();

        assertEquals(1, chain.buffers.size());
        assertArrayEquals("xy".getBytes(), chain.buffers.get(0));
    }

    @Test
    void testMultipleFlushes() throws IOException {
        TestOutputChain chain = new TestOutputChain();
        BufferChainOutputStream out = new BufferChainOutputStream(chain, 3);

        out.write('a');
        out.flush(); // emits "a"

        out.write('b');
        out.write('c');
        out.flush(); // emits "bc"

        out.close();

        assertEquals(2, chain.buffers.size());
        assertArrayEquals("a".getBytes(), chain.buffers.get(0));
        assertArrayEquals("bc".getBytes(), chain.buffers.get(1));
    }

    @Test
    void testWriteLargeArraySpanningManyBuffers() throws IOException {
        TestOutputChain chain = new TestOutputChain();
        BufferChainOutputStream out = new BufferChainOutputStream(chain, 4);

        out.write("abcdefghijklmnop".getBytes()); // 16 bytes
        out.close();

        assertEquals(4, chain.buffers.size());
        assertArrayEquals("abcd".getBytes(), chain.buffers.get(0));
        assertArrayEquals("efgh".getBytes(), chain.buffers.get(1));
        assertArrayEquals("ijkl".getBytes(), chain.buffers.get(2));
        assertArrayEquals("mnop".getBytes(), chain.buffers.get(3));
    }

    @Test
    void testWriteAfterCloseThrows() throws IOException {
        TestOutputChain chain = new TestOutputChain();
        BufferChainOutputStream out = new BufferChainOutputStream(chain, 4);

        out.close();
        assertThrows(IllegalStateException.class, () -> out.write('x'));
    }
}
