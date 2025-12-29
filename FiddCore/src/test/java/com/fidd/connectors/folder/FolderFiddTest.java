package com.fidd.connectors.folder;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class FolderFiddTest {

    @TempDir
    Path temp;

    private Path createMessageFolder(long num) throws IOException {
        Path msg = temp.resolve(Long.toString(num));
        Files.createDirectories(msg);
        return msg;
    }

    private void write(Path p, String content) throws IOException {
        Files.write(p, content.getBytes(), StandardOpenOption.CREATE);
    }

    // ------------------------------------------------------------
    // signatureMatch
    // ------------------------------------------------------------
    @Test
    void testSignatureMatch() {
        assertEquals(5, FolderFidd.signatureMatch(FolderFidd.FIDD_KEY_SIGNATURE_PATTERN, "fidd.key.5.sign"));
        assertNull(FolderFidd.signatureMatch(FolderFidd.FIDD_KEY_SIGNATURE_PATTERN, "not-a-match"));
    }

    // ------------------------------------------------------------
    // getSignatureCount
    // ------------------------------------------------------------
    @Test
    void testGetSignatureCount() throws IOException {
        Path msg = createMessageFolder(1);

        write(msg.resolve("fidd.key.0.sign"), "a");
        write(msg.resolve("fidd.key.1.sign"), "b");
        write(msg.resolve("fidd.key.3.sign"), "c");

        int count = FolderFidd.getSignatureCount(msg, FolderFidd.FIDD_KEY_SIGNATURE_PATTERN, 1);

        assertEquals(4, count); // max index = 3 → count = 4
    }

    // ------------------------------------------------------------
    // getMessagesTail
    // ------------------------------------------------------------
    @Test
    void testGetMessagesTail() throws IOException {
        createMessageFolder(10);
        createMessageFolder(11);
        createMessageFolder(12);

        List<Long> tail = FolderFidd.getMessagesTail(temp, null, 2, true);
        assertEquals(List.of(12L, 11L), tail);
    }

    // ------------------------------------------------------------
    // getMessageNumbersTail
    // ------------------------------------------------------------
    @Test
    void testGetMessageNumbersTail() throws IOException {
        createMessageFolder(1);
        createMessageFolder(2);
        createMessageFolder(3);

        FolderFidd fidd = new FolderFidd(temp.toString());
        assertEquals(List.of(3L, 2L), fidd.getMessageNumbersTail(2));
    }

    // ------------------------------------------------------------
    // getMessageNumbersBeforeExclusive
    // ------------------------------------------------------------
    @Test
    void testGetMessageNumbersBeforeExclusive() throws IOException {
        createMessageFolder(5);
        createMessageFolder(6);
        createMessageFolder(7);

        FolderFidd fidd = new FolderFidd(temp.toString());
        assertEquals(List.of(6L, 5L), fidd.getMessageNumbersBefore(7, 2, false));
    }

    // ------------------------------------------------------------
    // getMessageNumbersBetween
    // ------------------------------------------------------------
    @Test
    void testGetMessageNumbersBetween_exclusiveBoth() throws IOException {
        // Messages: 100, 90, 80, 70, 60
        createMessageFolder(100);
        createMessageFolder(90);
        createMessageFolder(80);
        createMessageFolder(70);
        createMessageFolder(60);

        FolderFidd fidd = new FolderFidd(temp.toString());

        // latest=100 (exclusive), earliest=70 (exclusive)
        List<Long> result = fidd.getMessageNumbersBetween(100, false, 70, false);

        // Should include: 90, 80
        assertEquals(List.of(90L, 80L), result);
    }

    @Test
    void testGetMessageNumbersBetween_inclusiveLatest_exclusiveEarliest() throws IOException {
        createMessageFolder(100);
        createMessageFolder(90);
        createMessageFolder(80);
        createMessageFolder(70);

        FolderFidd fidd = new FolderFidd(temp.toString());

        // latest=90 (inclusive), earliest=70 (exclusive)
        List<Long> result = fidd.getMessageNumbersBetween(90, true, 70, false);

        assertEquals(List.of(90L, 80L), result);
    }

    @Test
    void testGetMessageNumbersBetween_exclusiveLatest_inclusiveEarliest() throws IOException {
        createMessageFolder(100);
        createMessageFolder(90);
        createMessageFolder(80);
        createMessageFolder(70);

        FolderFidd fidd = new FolderFidd(temp.toString());

        // latest=100 (exclusive), earliest=80 (inclusive)
        List<Long> result = fidd.getMessageNumbersBetween(100, false, 80, true);

        assertEquals(List.of(90L, 80L), result);
    }

    @Test
    void testGetMessageNumbersBetween_inclusiveBoth() throws IOException {
        createMessageFolder(100);
        createMessageFolder(90);
        createMessageFolder(80);
        createMessageFolder(70);

        FolderFidd fidd = new FolderFidd(temp.toString());

        // latest=90 (inclusive), earliest=80 (inclusive)
        List<Long> result = fidd.getMessageNumbersBetween(90, true, 80, true);

        assertEquals(List.of(90L, 80L), result);
    }

    @Test
    void testGetMessageNumbersBetween_stopsWhenBelowEarliest() throws IOException {
        createMessageFolder(100);
        createMessageFolder(90);
        createMessageFolder(80);
        createMessageFolder(70);
        createMessageFolder(60); // should not be reached

        FolderFidd fidd = new FolderFidd(temp.toString());

        List<Long> result = fidd.getMessageNumbersBetween(100, true, 70, true);

        // Should include 100, 90, 80, 70 — but NOT 60
        assertEquals(List.of(100L, 90L, 80L, 70L), result);
    }

    @Test
    void testGetMessageNumbersBetween_unmatchingNumbers() throws IOException {
        createMessageFolder(100);
        createMessageFolder(90);
        createMessageFolder(80);
        createMessageFolder(70);
        createMessageFolder(60); // should not be reached

        FolderFidd fidd = new FolderFidd(temp.toString());

        List<Long> result = fidd.getMessageNumbersBetween(105, false, 65, false);

        // Should include 100, 90, 80, 70 — but NOT 60
        assertEquals(List.of(100L, 90L, 80L, 70L), result);
    }

    @Test
    void testGetMessageNumbersBetween_ignoresNonNumericFolders() throws IOException {
        createMessageFolder(100);
        createMessageFolder(90);
        createMessageFolder(80);

        // Non-numeric folder
        Files.createDirectories(temp.resolve("not-a-number"));

        FolderFidd fidd = new FolderFidd(temp.toString());

        List<Long> result = fidd.getMessageNumbersBetween(100, true, 80, true);

        assertEquals(List.of(100L, 90L, 80L), result);
    }

    // ------------------------------------------------------------
    // getKeyFile
    // ------------------------------------------------------------
    @Test
    void testGetKeyFile() throws IOException {
        Path msg = createMessageFolder(1);
        Path keys = msg.resolve("keys");
        Files.createDirectories(keys);

        Path keyFile = keys.resolve("abc");
        write(keyFile, "hello");

        FolderFidd fidd = new FolderFidd(temp.toString());
        byte[] data = fidd.getKeyFile(1, "abc".getBytes());

        assertEquals("hello", new String(data));
    }

    @Test
    void testGetKeyFileMissing() {
        FolderFidd fidd = new FolderFidd(temp.toString());
        assertThrows(RuntimeException.class,
                () -> fidd.getKeyFile(1, "missing".getBytes()));
    }

    // ------------------------------------------------------------
    // getMessageFile
    // ------------------------------------------------------------
    @Test
    void testGetMessageFile() throws IOException {
        Path msg = createMessageFolder(1);
        Path file = msg.resolve("fidd.message");
        write(file, "xyz");

        FolderFidd fidd = new FolderFidd(temp.toString());
        try (InputStream in = fidd.getMessageFile(1)) {
            assertEquals("xyz", new String(in.readAllBytes()));
        }
    }

    // ------------------------------------------------------------
    // getMessageFileChunk
    // ------------------------------------------------------------
    @Test
    void testGetMessageFileChunk() throws IOException {
        Path msg = createMessageFolder(1);
        Path file = msg.resolve("fidd.message");
        write(file, "abcdef");

        FolderFidd fidd = new FolderFidd(temp.toString());
        try (InputStream in = fidd.getMessageFileChunk(1, 2, 3)) {
            assertEquals("cde", new String(in.readAllBytes()));
        }
    }

    // ------------------------------------------------------------
    // Signature file retrieval
    // ------------------------------------------------------------
    @Test
    void testGetKeyFileSignatureCount() throws IOException {
        Path msg = createMessageFolder(1);

        // Valid signature files
        write(msg.resolve("fidd.key.0.sign"), "a");
        write(msg.resolve("fidd.key.2.sign"), "b");
        write(msg.resolve("fidd.key.5.sign"), "c");

        // Should be ignored
        write(msg.resolve("fidd.key.X.sign"), "bad");
        write(msg.resolve("fidd.key.10.sig"), "bad");
        write(msg.resolve("not-a-signature"), "bad");

        FolderFidd fidd = new FolderFidd(temp.toString());

        // max index = 5 → count = 6
        assertEquals(6, fidd.getKeyFileSignatureCount(1));
    }

    @Test
    void testGetKeyFileSignature() throws IOException {
        Path msg = createMessageFolder(1);
        write(msg.resolve("fidd.key.0.sign"), "sig0");

        FolderFidd fidd = new FolderFidd(temp.toString());
        assertEquals("sig0", new String(fidd.getKeyFileSignature(1, 0)));
    }

    @Test
    void testGetMessageFileSignatureCount() throws IOException {
        Path msg = createMessageFolder(2);

        // Valid signature files
        write(msg.resolve("fidd.message.1.sign"), "x");
        write(msg.resolve("fidd.message.4.sign"), "y");

        // Should be ignored
        write(msg.resolve("fidd.message.A.sign"), "bad");
        write(msg.resolve("fidd.message.3.sig"), "bad");
        write(msg.resolve("random-file"), "bad");

        FolderFidd fidd = new FolderFidd(temp.toString());

        // max index = 4 → count = 5
        assertEquals(5, fidd.getMessageFileSignatureCount(2));
    }

    @Test
    void testGetMessageFileSignature() throws IOException {
        Path msg = createMessageFolder(1);
        write(msg.resolve("fidd.message.1.sign"), "sig1");

        FolderFidd fidd = new FolderFidd(temp.toString());
        assertEquals("sig1", new String(fidd.getMessageFileSignature(1, 1)));
    }
}
