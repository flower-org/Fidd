package com.fidd.connectors.folder;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;

import static com.fidd.connectors.folder.FolderFiddConstants.FIDD_KEY_FILE_NAME;
import static com.fidd.connectors.folder.FolderFiddConstants.ENCRYPTED_FIDD_KEY_SUBFOLDER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class FolderFiddConnectorTest {

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

    private void delete(Path p) throws IOException {
        Files.delete(p);
    }

    // ------------------------------------------------------------
    // footprintStartsWith
    // ------------------------------------------------------------

    @Test
    void testFootprintStartsWith_exactMatch() {
        assertTrue(FolderFiddConnector.footprintStartsWith("abc123", "abc123"));
    }

    @Test
    void testFootprintStartsWith_prefixMatch() {
        assertTrue(FolderFiddConnector.footprintStartsWith("abc123", "abc"));
    }

    @Test
    void testFootprintStartsWith_prefixWithExtension() {
        assertTrue(FolderFiddConnector.footprintStartsWith("abc123", "abc.txt"));
    }

    @Test
    void testFootprintStartsWith_noMatch() {
        assertFalse(FolderFiddConnector.footprintStartsWith("xyz123", "abc"));
    }

    @Test
    void testFootprintStartsWith_extensionOnly() {
        assertTrue(FolderFiddConnector.footprintStartsWith("abc123", ".txt"));
    }

    @Test
    void testFootprintNameWithoutDot() {
        assertTrue(FolderFiddConnector.footprintStartsWith("helloWorld", "hello"));
    }

    // ------------------------------------------------------------
    // keyFileStartsWith
    // ------------------------------------------------------------

    @Test
    void testKeyFileStartsWith_exactMatch() {
        assertTrue(FolderFiddConnector.keyFileStartsWith("abc123", "abc123"));
    }

    @Test
    void testKeyFileStartsWith_prefixMatch() {
        assertTrue(FolderFiddConnector.keyFileStartsWith("abc123", "abc"));
    }

    @Test
    void testKeyFileStartsWith_prefixWithExtension() {
        assertTrue(FolderFiddConnector.keyFileStartsWith("abc123.txt", "abc"));
    }

    @Test
    void testKeyFileStartsWith_noMatch() {
        assertFalse(FolderFiddConnector.keyFileStartsWith("xyz123", "abc"));
    }

    @Test
    void testKeyFileStartsWith_extensionOnly() {
        assertTrue(FolderFiddConnector.keyFileStartsWith("abc123", ""));
    }

    @Test
    void testKeyfileNameWithoutDot() {
        assertTrue(FolderFiddConnector.keyFileStartsWith("helloWorld", "hello"));
    }

    // ------------------------------------------------------------
    // signatureMatch
    // ------------------------------------------------------------
    @Test
    void testSignatureMatch() {
        assertEquals(5, FolderFiddConnector.signatureMatch(FolderFiddConnector.FIDD_KEY_SIGNATURE_PATTERN, "fidd.key.5.sign"));
        assertNull(FolderFiddConnector.signatureMatch(FolderFiddConnector.FIDD_KEY_SIGNATURE_PATTERN, "not-a-match"));
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

        int count = FolderFiddConnector.getSignatureCount(msg, FolderFiddConnector.FIDD_KEY_SIGNATURE_PATTERN, 1);

        assertEquals(4, count); // max index = 3 → count = 4
    }

    // ------------------------------------------------------------
    // getMessagesTail
    // ------------------------------------------------------------
    @Test
    void testGetFiddMessagesTail() throws IOException {
        createMessageFolder(10);
        createMessageFolder(11);
        createMessageFolder(12);

        List<Long> tail = FolderFiddConnector.getMessagesTail(temp, null, 2, true);
        assertEquals(List.of(12L, 11L), tail);
    }

    // ------------------------------------------------------------
    // getMessageNumbersTail
    // ------------------------------------------------------------
    @Test
    void testGetFiddMessageNumbersTail() throws IOException {
        createMessageFolder(1);
        createMessageFolder(2);
        createMessageFolder(3);

        FolderFiddConnector fidd = new FolderFiddConnector(temp.toString());
        assertEquals(List.of(3L, 2L), fidd.getMessageNumbersTail(2));
    }

    // ------------------------------------------------------------
    // getMessageNumbersBeforeExclusive
    // ------------------------------------------------------------
    @Test
    void testGetFiddMessageNumbersBeforeExclusive() throws IOException {
        createMessageFolder(5);
        createMessageFolder(6);
        createMessageFolder(7);

        FolderFiddConnector fidd = new FolderFiddConnector(temp.toString());
        assertEquals(List.of(6L, 5L), fidd.getMessageNumbersBefore(7, 2, false));
    }

    // ------------------------------------------------------------
    // getMessageNumbersBetween
    // ------------------------------------------------------------
    @Test
    void testGetFiddMessageNumbersBetween_exclusiveBoth() throws IOException {
        // Messages: 100, 90, 80, 70, 60
        createMessageFolder(100);
        createMessageFolder(90);
        createMessageFolder(80);
        createMessageFolder(70);
        createMessageFolder(60);

        FolderFiddConnector fidd = new FolderFiddConnector(temp.toString());

        // latest=100 (exclusive), earliest=70 (exclusive)
        List<Long> result = fidd.getMessageNumbersBetween(100, false, 70, false, 10, true);

        // Should include: 90, 80
        assertEquals(List.of(90L, 80L), result);

        result = fidd.getMessageNumbersBetween(100, false, 70, false, 1, true);
        assertEquals(List.of(90L), result);

        result = fidd.getMessageNumbersBetween(100, false, 70, false, 1, false);
        assertEquals(List.of(80L), result);

        result = fidd.getMessageNumbersBetween(100, false, 70, false, 2, false);
        assertEquals(List.of(90L, 80L), result);
    }

    @Test
    void testGetFiddMessageNumbersBetween_inclusiveLatest_exclusiveEarliest() throws IOException {
        createMessageFolder(100);
        createMessageFolder(90);
        createMessageFolder(80);
        createMessageFolder(70);

        FolderFiddConnector fidd = new FolderFiddConnector(temp.toString());

        // latest=90 (inclusive), earliest=70 (exclusive)
        List<Long> result = fidd.getMessageNumbersBetween(90, true, 70, false, 10, true);

        assertEquals(List.of(90L, 80L), result);

        result = fidd.getMessageNumbersBetween(90, true, 70, false, 1, true);
        assertEquals(List.of(90L), result);

        result = fidd.getMessageNumbersBetween(90, true, 70, false, 1, false);
        assertEquals(List.of(80L), result);

        result = fidd.getMessageNumbersBetween(90, true, 70, false, 10, false);
        assertEquals(List.of(90L, 80L), result);
    }

    @Test
    void testGetFiddMessageNumbersBetween_exclusiveLatest_inclusiveEarliest() throws IOException {
        createMessageFolder(100);
        createMessageFolder(90);
        createMessageFolder(80);
        createMessageFolder(70);

        FolderFiddConnector fidd = new FolderFiddConnector(temp.toString());

        // latest=100 (exclusive), earliest=80 (inclusive)
        List<Long> result = fidd.getMessageNumbersBetween(100, false, 80, true, 10, true);

        assertEquals(List.of(90L, 80L), result);

        result = fidd.getMessageNumbersBetween(100, false, 80, true, 1, true);
        assertEquals(List.of(90L), result);

        result = fidd.getMessageNumbersBetween(100, false, 80, true, 1, false);
        assertEquals(List.of(80L), result);

        result = fidd.getMessageNumbersBetween(100, false, 80, true, 10, false);
        assertEquals(List.of(90L, 80L), result);
    }

    @Test
    void testGetFiddMessageNumbersBetween_inclusiveBoth() throws IOException {
        createMessageFolder(100);
        createMessageFolder(90);
        createMessageFolder(80);
        createMessageFolder(70);

        FolderFiddConnector fidd = new FolderFiddConnector(temp.toString());

        // latest=90 (inclusive), earliest=80 (inclusive)
        List<Long> result = fidd.getMessageNumbersBetween(90, true, 80, true, 10, true);

        assertEquals(List.of(90L, 80L), result);

        result = fidd.getMessageNumbersBetween(90, true, 80, true, 1, true);
        assertEquals(List.of(90L), result);

        result = fidd.getMessageNumbersBetween(90, true, 80, true, 1, false);
        assertEquals(List.of(80L), result);

        result = fidd.getMessageNumbersBetween(90, true, 80, true, 10, false);
        assertEquals(List.of(90L, 80L), result);
    }

    @Test
    void testGetFiddMessageNumbersBetween_stopsWhenBelowEarliest() throws IOException {
        createMessageFolder(100);
        createMessageFolder(90);
        createMessageFolder(80);
        createMessageFolder(70);
        createMessageFolder(60); // should not be reached

        FolderFiddConnector fidd = new FolderFiddConnector(temp.toString());

        List<Long> result = fidd.getMessageNumbersBetween(100, true, 70, true, 10, true);

        // Should include 100, 90, 80, 70 — but NOT 60
        assertEquals(List.of(100L, 90L, 80L, 70L), result);

        result = fidd.getMessageNumbersBetween(100, true, 70, true, 2, true);
        assertEquals(List.of(100L, 90L), result);

        result = fidd.getMessageNumbersBetween(100, true, 70, true, 3, false);
        assertEquals(List.of(90L, 80L, 70L), result);

        result = fidd.getMessageNumbersBetween(100, true, 70, true, 10, false);
        assertEquals(List.of(100L, 90L, 80L, 70L), result);
    }

    @Test
    void testGetFiddMessageNumbersBetween_unmatchingNumbers() throws IOException {
        createMessageFolder(100);
        createMessageFolder(90);
        createMessageFolder(80);
        createMessageFolder(70);
        createMessageFolder(60); // should not be reached

        FolderFiddConnector fidd = new FolderFiddConnector(temp.toString());

        List<Long> result = fidd.getMessageNumbersBetween(105, false, 65, false, 10, true);

        // Should include 100, 90, 80, 70 — but NOT 60
        assertEquals(List.of(100L, 90L, 80L, 70L), result);

        result = fidd.getMessageNumbersBetween(105, false, 65, false, 3, true);
        assertEquals(List.of(100L, 90L, 80L), result);

        result = fidd.getMessageNumbersBetween(105, false, 65, false, 2, false);
        assertEquals(List.of(80L, 70L), result);

        result = fidd.getMessageNumbersBetween(105, false, 65, false, 10, false);
        assertEquals(List.of(100L, 90L, 80L, 70L), result);
    }

    @Test
    void testGetFiddMessageNumbersBetween_ignoresNonNumericFolders() throws IOException {
        createMessageFolder(100);
        createMessageFolder(90);
        createMessageFolder(80);

        // Non-numeric folder
        Files.createDirectories(temp.resolve("not-a-number"));

        FolderFiddConnector fidd = new FolderFiddConnector(temp.toString());

        List<Long> result = fidd.getMessageNumbersBetween(100, true, 80, true, 10, true);

        assertEquals(List.of(100L, 90L, 80L), result);

        result = fidd.getMessageNumbersBetween(100, true, 80, true, 1, true);
        assertEquals(List.of(100L), result);

        result = fidd.getMessageNumbersBetween(100, true, 80, true, 2, false);
        assertEquals(List.of(90L, 80L), result);

        result = fidd.getMessageNumbersBetween(100, true, 80, true, 10, true);
        assertEquals(List.of(100L, 90L, 80L), result);
    }

    // ------------------------------------------------------------
    // getKeyFile
    // ------------------------------------------------------------
    @Test
    void testGetKeyFile_readsMatchingFiles() throws IOException {
        long messageNumber = 42L;
        Path msg = createMessageFolder(messageNumber);
        Path keys = msg.resolve(ENCRYPTED_FIDD_KEY_SUBFOLDER);
        Files.createDirectories(keys);

        // Mock keyFolderPath(messageNumber)
        FolderFiddConnector fidd = new FolderFiddConnector(temp.toString());

        // Create files
        Path f1 = keys.resolve("sub1.fidd.key.crypt");
        Path f2 = keys.resolve("su.fidd.key.crypt");
        Path f3 = keys.resolve("sub2.fidd.key.crypt"); // should NOT match
        Path f4 = keys.resolve("sub1QWERTY.fidd.key.crypt");

        Files.write(f1, "AAA".getBytes());
        Files.write(f2, "BBB".getBytes());
        Files.write(f3, "CCC".getBytes());
        Files.write(f4, "DDD".getBytes());

        byte[] subscriberId = "sub1".getBytes(StandardCharsets.UTF_8);

        List<byte[]> candidates = fidd.getFiddKeyCandidates(messageNumber, subscriberId);
        List<byte[]> result = candidates.stream().map(
                key -> fidd.getFiddKey(messageNumber, key)
        ).toList();

        assertEquals(3, result.size());
        assertEquals("DDD", new String(result.get(0)));
        assertEquals("AAA", new String(result.get(1)));
        assertEquals("BBB", new String(result.get(2)));
    }

    @Test
    void testGetKeyFile_returnsEmptyListWhenNoMatches() {
        FolderFiddConnector fidd = new FolderFiddConnector(temp.toString());

        byte[] subscriberId = "nomatch".getBytes(StandardCharsets.UTF_8);

        List<byte[]> result = fidd.getFiddKeyCandidates(1L, subscriberId);

        assertTrue(result.isEmpty());
    }

    @Test
    void testGetKeyFile_returnsNullIfFileDisappears() throws IOException {
        long messageNumber = 43L;
        Path msg = createMessageFolder(messageNumber);
        Path keys = msg.resolve(ENCRYPTED_FIDD_KEY_SUBFOLDER);
        Files.createDirectories(keys);

        FolderFiddConnector fidd = new FolderFiddConnector(temp.toString());

        Path f1 = keys.resolve("sub.key");
        Files.write(f1, "AAA".getBytes());

        // Delete file after creation to trigger the null return
        Files.delete(f1);

        byte[] subscriberId = "sub".getBytes(StandardCharsets.UTF_8);

        List<byte[]> result = fidd.getFiddKeyCandidates(messageNumber, subscriberId);

        assertTrue(result.isEmpty());
    }

    @Test
    void testGetKeyFile_ignoresDirectories() throws IOException {
        long messageNumber = 44L;
        Path msg = createMessageFolder(messageNumber);
        Path keys = msg.resolve(ENCRYPTED_FIDD_KEY_SUBFOLDER);
        Files.createDirectories(keys);

        FolderFiddConnector fidd = new FolderFiddConnector(temp.toString());

        // Directory with matching prefix — should be ignored
        Files.createDirectory(keys.resolve("sub1"));

        byte[] subscriberId = "sub1".getBytes(StandardCharsets.UTF_8);

        List<byte[]> result = fidd.getFiddKeyCandidates(messageNumber, subscriberId);

        assertTrue(result.isEmpty());
    }

    @Test
    void testGetUnencryptedFiddKey() throws IOException {
        Path msg = createMessageFolder(1);
        Path keyFile = msg.resolve(FIDD_KEY_FILE_NAME);
        write(keyFile, "hello");

        FolderFiddConnector fidd = new FolderFiddConnector(temp.toString());
        byte[] data = fidd.getUnencryptedFiddKey(1);

        assertEquals("hello", new String(data));

        delete(keyFile);
    }

    @Test
    void testGetUnencryptedFiddKeyMissing() {
        FolderFiddConnector fidd = new FolderFiddConnector(temp.toString());
        assertNull(fidd.getUnencryptedFiddKey(1));
    }

    // ------------------------------------------------------------
    // getMessageFileSize
    // ------------------------------------------------------------
    @Test
    void testGetFiddMessageSize() throws IOException {
        Path msg = createMessageFolder(1);
        Path file = msg.resolve("fidd.message");
        write(file, "xyz");

        FolderFiddConnector fidd = new FolderFiddConnector(temp.toString());
        long size = fidd.getFiddMessageSize(1);
        assertEquals(3, size);
    }

    // ------------------------------------------------------------
    // getMessageFile
    // ------------------------------------------------------------
    @Test
    void testGetFiddMessage() throws IOException {
        Path msg = createMessageFolder(1);
        Path file = msg.resolve("fidd.message");
        write(file, "xyz");

        FolderFiddConnector fidd = new FolderFiddConnector(temp.toString());
        try (InputStream in = fidd.getFiddMessage(1)) {
            assertEquals("xyz", new String(in.readAllBytes()));
        }
    }

    // ------------------------------------------------------------
    // getMessageFileChunk
    // ------------------------------------------------------------
    @Test
    void testGetFiddMessageChunk() throws IOException {
        Path msg = createMessageFolder(1);
        Path file = msg.resolve("fidd.message");
        write(file, "abcdef");

        FolderFiddConnector fidd = new FolderFiddConnector(temp.toString());
        try (InputStream in = fidd.getFiddMessageChunk(1, 2, 3)) {
            assertEquals("cde", new String(in.readAllBytes()));
        }
    }

    // ------------------------------------------------------------
    // Signature file retrieval
    // ------------------------------------------------------------
    @Test
    void testGetFiddKeySignatureCount() throws IOException {
        Path msg = createMessageFolder(1);

        // Valid signature files
        write(msg.resolve("fidd.key.0.sign"), "a");
        write(msg.resolve("fidd.key.2.sign"), "b");
        write(msg.resolve("fidd.key.5.sign"), "c");

        // Should be ignored
        write(msg.resolve("fidd.key.X.sign"), "bad");
        write(msg.resolve("fidd.key.10.sig"), "bad");
        write(msg.resolve("not-a-signature"), "bad");

        FolderFiddConnector fidd = new FolderFiddConnector(temp.toString());

        // max index = 5 → count = 6
        assertEquals(6, fidd.getFiddKeySignatureCount(1));
    }

    @Test
    void testGetFiddKeySignature() throws IOException {
        Path msg = createMessageFolder(1);
        write(msg.resolve("fidd.key.0.sign"), "sig0");

        FolderFiddConnector fidd = new FolderFiddConnector(temp.toString());
        assertEquals("sig0", new String(fidd.getFiddKeySignature(1, 0)));
    }

    @Test
    void testGetFiddMessageSignatureCount() throws IOException {
        Path msg = createMessageFolder(2);

        // Valid signature files
        write(msg.resolve("fidd.message.1.sign"), "x");
        write(msg.resolve("fidd.message.4.sign"), "y");

        // Should be ignored
        write(msg.resolve("fidd.message.A.sign"), "bad");
        write(msg.resolve("fidd.message.3.sig"), "bad");
        write(msg.resolve("random-file"), "bad");

        FolderFiddConnector fidd = new FolderFiddConnector(temp.toString());

        // max index = 4 → count = 5
        assertEquals(5, fidd.getFiddMessageSignatureCount(2));
    }

    @Test
    void testGetFiddMessageSignature() throws IOException {
        Path msg = createMessageFolder(1);
        write(msg.resolve("fidd.message.1.sign"), "sig1");

        FolderFiddConnector fidd = new FolderFiddConnector(temp.toString());
        assertEquals("sig1", new String(fidd.getFiddMessageSignature(1, 1)));
    }
}
