package com.fidd.core.crc;

import com.fidd.core.crc.adler32.Adler32Calculator;
import com.fidd.core.crc.crc32.Crc32Calculator;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class CrcCalculatorTest {
    private static CrcCalculator getCalculator(String algorithm) {
        switch (algorithm) {
            case "Adler32": return new Adler32Calculator();
            case "CRC32":   return new Crc32Calculator();
            default: throw new IllegalArgumentException("Unknown algorithm: " + algorithm);
        }
    }

    private static byte[] hexToBytes(String hex) {
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                    + Character.digit(hex.charAt(i+1), 16));
        }
        return data;
    }

    @ParameterizedTest(name = "{0} checksum of \"{1}\"")
    @CsvSource({
            "Adler32, hello, 062C0215",
            "CRC32,   hello, 3610A686",
            "Adler32, '',    00000001",
            "CRC32,   '',    00000000"
    })
    void testCalculateCrcByteArray(String algorithm, String input, String expectedHex) {
        CrcCalculator calculator = getCalculator(algorithm);
        byte[] checksum = calculator.calculateCrc(input.getBytes(StandardCharsets.UTF_8));
        assertArrayEquals(hexToBytes(expectedHex), checksum);
    }

    @ParameterizedTest(name = "{0} checksum of stream \"{1}\"")
    @CsvSource({
            "Adler32, hello, 062C0215",
            "CRC32,   hello, 3610A686",
            "Adler32, '',    00000001",
            "CRC32,   '',    00000000"
    })
    void testCalculateCrcInputStream(String algorithm, String input, String expectedHex) throws IOException {
        CrcCalculator calculator = getCalculator(algorithm);
        try (ByteArrayInputStream in = new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8))) {
            byte[] checksum = calculator.calculateCrc(in);
            assertArrayEquals(hexToBytes(expectedHex), checksum);
        }
    }
}
