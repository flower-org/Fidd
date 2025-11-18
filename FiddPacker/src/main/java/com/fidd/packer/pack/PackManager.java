package com.fidd.packer.pack;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Random;

import static com.fidd.packer.pack.DirectoryReader.getDirectoryContents;

public class PackManager {
    private static final int BUFFER_SIZE = 8192; // 8 KB buffer size

    public static void packDirectory(File directory, File outputFile) {
        try {
            try (FileChannel outputChannel = FileChannel.open(outputFile.toPath(), StandardOpenOption.CREATE, StandardOpenOption.WRITE)) {
                List<FilePathTuple> files = getDirectoryContents(directory);
                for (FilePathTuple file : files) {
                    encryptAndAppend(file.file().toPath(), outputChannel);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void encryptAndAppend(Path file, FileChannel outputChannel) throws IOException {
        // Prepare ByteBuffer for reading
        ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);

        try (FileChannel inputChannel = FileChannel.open(file, StandardOpenOption.READ)) {
            while (inputChannel.read(buffer) != -1) {
                buffer.flip(); // Switch to read mode

                // Encrypt the data in the buffer
                ByteBuffer encryptedBuffer = encrypt(buffer);

                // Write the encrypted data to the output channel
                while (encryptedBuffer.hasRemaining()) {
                    outputChannel.write(encryptedBuffer);
                }

                buffer.clear(); // Clear the buffer for the next read
            }
        }
    }

    private static void appendRandomBuffer(long totalBufferSize, FileChannel outputChannel) {
        Random random = new Random();
        byte[] tempArray = new byte[BUFFER_SIZE];
        ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);
        long remainingBytes = totalBufferSize;

        // Write data in chunks
        while (remainingBytes > 0) {
            // Determine the size of the current chunk
            int chunkSize = (int) Math.min(BUFFER_SIZE, remainingBytes);

            // Clear the buffer for new data
            buffer.clear(); // Resets position and limit for reuse
            random.nextBytes(tempArray); // Fill the temporary byte array with random data
            buffer.put(tempArray); // Put the random data into the ByteBuffer
            buffer.flip(); // Prepare the buffer for writing

            try {
                // Write the random data to the output channel
                outputChannel.write(buffer);
            } catch (IOException e) {
                e.printStackTrace(); // Handle exception appropriately
            }

            // Decrease the remaining bytes
            remainingBytes -= chunkSize;
        }
    }

    // TODO: Dummy encryption method (simple XOR for demonstration)
    private static ByteBuffer encrypt(ByteBuffer input) {
        // Create a new ByteBuffer for output
        ByteBuffer output = ByteBuffer.allocate(input.remaining());
        byte key = 0x5A; // Example key for XOR

        while (input.hasRemaining()) {
            byte originalByte = input.get();
            output.put((byte) (originalByte ^ key)); // Apply XOR operation
        }

        output.flip(); // Switch to read mode for returning
        return output;
    }
}
