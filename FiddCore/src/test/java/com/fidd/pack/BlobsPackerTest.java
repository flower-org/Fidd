package com.fidd.pack;

import com.fidd.core.metadata.NotEnoughBytesException;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class BlobsPackerTest {
    @Test
    public void testPackAndUnpackWithZeroBlobs() throws NotEnoughBytesException {
        List<byte[]> blobs = new ArrayList<>();
        byte[] packedData = BlobsPacker.packBlobs(blobs);
        Pair<Long, List<byte[]>> unpackedBlobs = BlobsPacker.unpackBlobs(packedData);
        assertEquals(0, unpackedBlobs.getRight().size(), "Should unpack to zero BLOBs");
    }

    @Test
    public void testPackAndUnpackWithOneBlob() throws NotEnoughBytesException {
        List<byte[]> blobs = List.of("Hello".getBytes());
        byte[] packedData = BlobsPacker.packBlobs(blobs);
        Pair<Long, List<byte[]>> unpackedBlobs = BlobsPacker.unpackBlobs(packedData);
        assertEquals(1, unpackedBlobs.getRight().size(), "Should unpack to one BLOB");
        assertEquals("Hello", new String(unpackedBlobs.getRight().get(0)), "Unpacked BLOB should match the original");
    }

    @Test
    public void testPackAndUnpackWithMultipleBlobs() throws NotEnoughBytesException {
        List<byte[]> blobs = List.of("Hello".getBytes(), "World".getBytes());
        byte[] packedData = BlobsPacker.packBlobs(blobs);
        Pair<Long, List<byte[]>> unpackedBlobs = BlobsPacker.unpackBlobs(packedData);
        assertEquals(2, unpackedBlobs.getRight().size(), "Should unpack to two BLOBs");
        assertEquals("Hello", new String(unpackedBlobs.getRight().get(0)), "First unpacked BLOB should match 'Hello'");
        assertEquals("World", new String(unpackedBlobs.getRight().get(1)), "Second unpacked BLOB should match 'World'");
    }

    @Test
    public void testPackAndUnpackWithLargeBlob() throws NotEnoughBytesException {
        byte[] largeBlob = new byte[1024]; // 1 KB
        List<byte[]> blobs = List.of(largeBlob);
        byte[] packedData = BlobsPacker.packBlobs(blobs);
        Pair<Long, List<byte[]>> unpackedBlobs = BlobsPacker.unpackBlobs(packedData);
        assertEquals(1, unpackedBlobs.getRight().size(), "Should unpack to one BLOB");
        assertArrayEquals(largeBlob, unpackedBlobs.getRight().get(0), "Unpacked BLOB should match the original large BLOB");
    }

    @Test
    public void testInvalidBlobCount() {
        assertThrows(Exception.class, () -> {
            BlobsPacker.unpackBlobs(new byte[]{0, 0, 0, 1}); // 1 BLOB count but no sizes or data
        });
    }
}