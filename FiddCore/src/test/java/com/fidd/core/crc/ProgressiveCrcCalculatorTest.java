package com.fidd.core.crc;
import com.fidd.core.crc.adler32.Adler32Calculator;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

public class ProgressiveCrcCalculatorTest {
    private final Adler32Calculator adlerCalc = new Adler32Calculator();

    private byte[] expectedCrc(byte[] chunk) throws IOException {
        return adlerCalc.calculateCrc(new ByteArrayInputStream(chunk));
    }

    private byte[] concat(byte[] a, byte[] b) {
        byte[] out = new byte[a.length + b.length];
        System.arraycopy(a, 0, out, 0, a.length);
        System.arraycopy(b, 0, out, a.length, b.length);
        return out;
    }

    @Test
    void testExactMultipleChunks() throws IOException {
        byte[] data = "abcdefghij".getBytes(); // 10 bytes
        ByteArrayInputStream in = new ByteArrayInputStream(data);

        byte[] result = ProgressiveCrcCalculator.calculateProgressiveCrc(in, 5, adlerCalc);

        // Expected CRCs for "abcde" and "fghij"
        byte[] expected = concat(
                expectedCrc("abcde".getBytes()),
                expectedCrc("fghij".getBytes())
        );

        assertArrayEquals(expected, result);
    }

    @Test
    void testPartialLastChunk() throws IOException {
        byte[] data = "abcdefg".getBytes(); // 7 bytes
        ByteArrayInputStream in = new ByteArrayInputStream(data);

        byte[] result = ProgressiveCrcCalculator.calculateProgressiveCrc(in, 5, adlerCalc);

        // Expected CRCs for "abcde" and "fg"
        byte[] expected = concat(
                expectedCrc("abcde".getBytes()),
                expectedCrc("fg".getBytes())
        );

        assertArrayEquals(expected, result);
    }

    @Test
    void testSingleSmallChunk() throws IOException {
        byte[] data = "abc".getBytes(); // 3 bytes
        ByteArrayInputStream in = new ByteArrayInputStream(data);

        byte[] result = ProgressiveCrcCalculator.calculateProgressiveCrc(in, 5, adlerCalc);

        // Expected CRC for "abc"
        byte[] expected = expectedCrc("abc".getBytes());

        assertArrayEquals(expected, result);
    }

    @Test
    void testEmptyStream() throws IOException {
        byte[] data = new byte[0];
        ByteArrayInputStream in = new ByteArrayInputStream(data);

        byte[] result = ProgressiveCrcCalculator.calculateProgressiveCrc(in, 5, new Adler32Calculator());

        // No chunks at all
        assertEquals(0, result.length);
    }

    @Test
    void testIllegalChunkSize() {
        ByteArrayInputStream in = new ByteArrayInputStream("abc".getBytes());

        assertThrows(IllegalArgumentException.class,
                () -> ProgressiveCrcCalculator.calculateProgressiveCrc(in, 0, new Adler32Calculator()));
    }

    @Test
    void testLargeChunks() throws IOException {
        final int chunkSize = 1024*1024;
        final int iterations = 16;
        Random random = new Random();
        CrcCalculator crcCalc = new Adler32Calculator();

        byte[] data = new byte[chunkSize];
        byte[] fullData = new byte[]{};
        byte[] fullCrc = new byte[]{};

        for (int i = 0; i < iterations; i++) {
            random.nextBytes(data);
            fullData = concat(fullData, data);
            fullCrc = concat(fullCrc, crcCalc.calculateCrc(data));
        }

        InputStream dataStream = new ByteArrayInputStream(fullData);
        byte[] progressiveCrc = ProgressiveCrcCalculator.calculateProgressiveCrc(dataStream, chunkSize, crcCalc);

        assertArrayEquals(fullCrc, progressiveCrc);
    }
}
