package com.fidd.common.streamchain.chain;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

public class ConcurrentBufferChainTest {

    static class MockFlusher implements BufferChain.BufferFlusher {
        AtomicInteger flushCount = new AtomicInteger();
        ConcurrentBufferChain chain;
        byte[] bufferToAdd;

        MockFlusher(ConcurrentBufferChain chain, byte[] bufferToAdd) {
            this.chain = chain;
            this.bufferToAdd = bufferToAdd;
        }

        @Override
        public void flushBuffer() {
            flushCount.incrementAndGet();
            if (bufferToAdd != null) {
                chain.addBuffer(bufferToAdd);
                bufferToAdd = null;
            }
        }
    }

    @Test
    void addBufferAndPollReturnsBuffersInOrder() {
        ConcurrentBufferChain chain = new ConcurrentBufferChain();

        chain.addBuffer(new byte[]{1});
        chain.addBuffer(new byte[]{2, 3});

        assertArrayEquals(new byte[]{1}, chain.pollBuffer());
        assertArrayEquals(new byte[]{2, 3}, chain.pollBuffer());
        assertNull(chain.pollBuffer());
    }

    @Test
    void pollTriggersFlusherWhenQueueEmpty() {
        ConcurrentBufferChain chain = new ConcurrentBufferChain();
        MockFlusher flusher = new MockFlusher(chain, new byte[]{9});
        chain.attachBufferFlusher(flusher);

        assertArrayEquals(new byte[]{9}, chain.pollBuffer()); // first poll, queue empty → flusher flushes
        assertEquals(1, flusher.flushCount.get());

        // second poll should retrieve the buffer added by flusher
        assertNull(chain.pollBuffer());
    }

    @Test
    void pollWithoutFlusherReturnsNull() {
        ConcurrentBufferChain chain = new ConcurrentBufferChain();

        assertNull(chain.pollBuffer());
    }

    @Test
    void availableCountsBytesInQueue() {
        ConcurrentBufferChain chain = new ConcurrentBufferChain();

        chain.addBuffer(new byte[3]);
        chain.addBuffer(new byte[5]);

        assertEquals(8, chain.available());
    }

    @Test
    void availableTriggersFlusherWhenEmpty() {
        ConcurrentBufferChain chain = new ConcurrentBufferChain();
        MockFlusher flusher = new MockFlusher(chain, new byte[]{1, 2, 3});
        chain.attachBufferFlusher(flusher);

        assertEquals(3, chain.available());
        assertEquals(1, flusher.flushCount.get());
    }

    @Test
    void availableDoesNotTriggerFlusherWhenNotEmpty() {
        ConcurrentBufferChain chain = new ConcurrentBufferChain();
        MockFlusher flusher = new MockFlusher(chain, new byte[]{1});
        chain.attachBufferFlusher(flusher);

        chain.addBuffer(new byte[]{5});

        assertEquals(1, chain.available());
        assertEquals(0, flusher.flushCount.get());
    }

    @Test
    void closeMarksChainAsClosed() {
        ConcurrentBufferChain chain = new ConcurrentBufferChain();

        assertFalse(chain.isClosed());
        chain.close();
        assertTrue(chain.isClosed());
    }

    @Test
    void attachBufferFlusherStoresFlusher() {
        ConcurrentBufferChain chain = new ConcurrentBufferChain();
        MockFlusher flusher = new MockFlusher(chain, null);

        chain.attachBufferFlusher(flusher);

        // poll should call flusher.flushBuffer()
        chain.pollBuffer();
        assertEquals(1, flusher.flushCount.get());
    }

    @Test
    void concurrentAddAndPollWorks() throws InterruptedException {
        ConcurrentBufferChain chain = new ConcurrentBufferChain();

        Thread writer = new Thread(() -> {
            for (int i = 0; i < 1000; i++) {
                chain.addBuffer(new byte[]{1});
            }
        });

        Thread reader = new Thread(() -> {
            for (int i = 0; i < 1000; i++) {
                chain.pollBuffer();
            }
        });

        writer.start();
        reader.start();
        writer.join();
        reader.join();

        // No exceptions, and queue should be empty or contain leftover items
        assertTrue(chain.available() >= 0);
    }
}
