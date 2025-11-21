package com.fidd.pack;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BlobsPacker {
    public static byte[] packBlobs(byte[]... blobs) {
        List<byte[]> blobList = Arrays.asList(blobs);
        return packBlobs(blobList);
    }

    public static byte[] packBlobs(List<byte[]> blobs) {
        // Calculate total size
        int blobCount = blobs.size();
        long totalSize = 4L + 8L * blobCount; // uint32 count + (uint64 size for each BLOB)

        for (byte[] blob : blobs) {
            totalSize += blob.length; // Add size of each BLOB
        }

        // Create a ByteBuffer with the calculated total size, set to big-endian
        ByteBuffer buffer = ByteBuffer.allocate((int) totalSize).order(ByteOrder.BIG_ENDIAN);

        // Write blob count as uint32
        buffer.putInt(blobCount);

        // Write each blob size as uint64
        for (byte[] blob : blobs) {
            buffer.putLong(blob.length);
        }

        // Write BLOB data
        for (byte[] blob : blobs) {
            buffer.put(blob);
        }

        // Return byte array
        return buffer.array();
    }

    public static List<byte[]> unpackBlobs(byte[] packedData) {
        if (packedData.length < 4) {
            throw new IllegalArgumentException("Can't read uint32 BLOB count. packedData.length " + packedData.length);
        }

        // Create a ByteBuffer to read the packed data, set to big-endian
        ByteBuffer buffer = ByteBuffer.wrap(packedData).order(ByteOrder.BIG_ENDIAN);
        long totalSize = 4L;

        // Read blob count
        // TODO: uint32
        int blobCount = buffer.getInt();
        totalSize += 8L * (long)blobCount;

        // Validate blob count
        if (blobCount < 0) { throw new IllegalArgumentException("Invalid BLOB count."); }
        if (packedData.length < totalSize) {
            throw new IllegalArgumentException("Can't read BLOB sizes. blobCount " + blobCount
                    + "; packedData.length " + packedData.length);
        }

        List<Long> blobLengths = new ArrayList<>(blobCount);
        List<byte[]> blobs = new ArrayList<>(blobCount);

        // Read each blob size and BLOB data
        for (int i = 0; i < blobCount; i++) {
            long size = buffer.getLong();  // Read size of the BLOB
            blobLengths.add(size);
            totalSize += size;
        }

        if (totalSize != packedData.length)  {
            throw new IllegalArgumentException("Size mismatch. totalSize: " + totalSize +
                    "; packedData.length " + packedData.length);
        }

        // Read each blob size and BLOB data
        for (int i = 0; i < blobCount; i++) {
            long size = blobLengths.get(i);  // Read size of the BLOB
            byte[] blob = new byte[(int) size]; // Create a new byte array for the BLOB
            buffer.get(blob); // Read the BLOB data
            blobs.add(blob); // Add to the list
        }

        return blobs;
    }
}
