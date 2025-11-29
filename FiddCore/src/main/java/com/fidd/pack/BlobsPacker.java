package com.fidd.pack;

import com.fidd.core.metadata.NotEnoughBytesException;
import org.apache.commons.lang3.tuple.Pair;

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
        return packBlobsPartial(blobs, new int[] {});
    }

    public static byte[] packBlobsPartial(List<byte[]> blobs, int[] additionalBlobLengths) {
        // Calculate total size
        int blobCount = blobs.size();
        long totalSize = 4L + 8L * blobCount; // uint32 count + (uint64 size for each BLOB)
        totalSize += 8L * additionalBlobLengths.length;

        for (byte[] blob : blobs) {
            totalSize += blob.length; // Add size of each BLOB
        }
        for (int blobLength : additionalBlobLengths) {
            totalSize += blobLength; // Add size of each BLOB
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

        for (int blobLength : additionalBlobLengths) {
            totalSize += blobLength; // Add size of each BLOB
        }

        // Return byte array
        return buffer.array();
    }

    public static Pair<Long, List<byte[]>> unpackBlobs(byte[] packedData) throws NotEnoughBytesException {
        if (packedData.length < 4) {
            throw new NotEnoughBytesException("Can't read uint32 BLOB count. packedData.length " + packedData.length);
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
            throw new NotEnoughBytesException("Can't read BLOB sizes. blobCount " + blobCount
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

        if (packedData.length < totalSize)  {
            throw new NotEnoughBytesException("Size mismatch. totalSize: " + totalSize +
                    "; packedData.length " + packedData.length, totalSize);
        }

        // Read each blob size and BLOB data
        for (int i = 0; i < blobCount; i++) {
            long size = blobLengths.get(i);  // Read size of the BLOB
            byte[] blob = new byte[(int) size]; // Create a new byte array for the BLOB
            buffer.get(blob); // Read the BLOB data
            blobs.add(blob); // Add to the list
        }

        return Pair.of(totalSize, blobs);
    }
}
