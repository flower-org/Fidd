package com.fidd.core.common;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

public class LimitedInputStreamTest {

    @Test
    void testReadExactChunkSize() throws IOException {
        byte[] data = "abcdefghij".getBytes(); // 10 bytes
        LimitedInputStream lis = new LimitedInputStream(new ByteArrayInputStream(data), 10);

        byte[] buffer = new byte[10];
        int read = lis.read(buffer);

        assertEquals(10, read);
        assertArrayEquals(data, buffer);
        assertEquals(10, lis.getBytesRead());
        assertEquals(-1, lis.read()); // EOF after limit
    }

    @Test
    void testReadLessThanChunkSize() throws IOException {
        byte[] data = "abc".getBytes(); // 3 bytes
        LimitedInputStream lis = new LimitedInputStream(new ByteArrayInputStream(data), 10);

        byte[] buffer = new byte[10];
        int read = lis.read(buffer);

        assertEquals(3, read);
        assertArrayEquals("abc".getBytes(), copyOf(buffer, 3));
        assertEquals(3, lis.getBytesRead());
        assertEquals(-1, lis.read()); // EOF
    }

    @Test
    void testMultipleReadsWithinLimit() throws IOException {
        byte[] data = "abcdefghij".getBytes(); // 10 bytes
        LimitedInputStream lis = new LimitedInputStream(new ByteArrayInputStream(data), 10);

        assertEquals('a', lis.read());
        assertEquals('b', lis.read());
        assertEquals(2, lis.getBytesRead());

        byte[] buffer = new byte[5];
        int read = lis.read(buffer);
        assertEquals(5, read);
        assertArrayEquals("cdefg".getBytes(), buffer);
        assertEquals(7, lis.getBytesRead());
    }

    @Test
    void testSkipRespectsLimit() throws IOException {
        byte[] data = "abcdefghij".getBytes();
        LimitedInputStream lis = new LimitedInputStream(new ByteArrayInputStream(data), 5);

        long skipped = lis.skip(10);
        assertEquals(5, skipped);
        assertEquals(5, lis.getBytesRead());
        assertEquals(-1, lis.read()); // limit reached

        // To check correctness, compare skipped portion with expected substring
        String skippedString = new String(data, 0, (int) skipped);
        assertEquals("abcde", skippedString);
    }

    @Test
    void testAvailableRespectsLimit() throws IOException {
        byte[] data = "abcdefghij".getBytes();
        LimitedInputStream lis = new LimitedInputStream(new ByteArrayInputStream(data), 5);

        assertTrue(lis.available() <= 5);
        lis.read();
        assertEquals(1, lis.getBytesRead());
        assertTrue(lis.available() <= 4);
    }

    // Helper for trimming arrays
    private static byte[] copyOf(byte[] src, int length) {
        byte[] copy = new byte[length];
        System.arraycopy(src, 0, copy, 0, length);
        return copy;
    }
}
