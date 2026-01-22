package com.fidd.packer.pack;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PackManagerTest {

    private File testDirectory;

    @BeforeEach
    public void setUp() throws IOException {
        // Create a temporary directory for testing
        testDirectory = Files.createTempDirectory("testDir").toFile();
        // Create test files and directories
        createTestStructure(testDirectory);
    }

    private void createTestStructure(File root) throws IOException {
        // Create test files
        File file1 = new File(root, "file1.txt");
        Files.writeString(file1.toPath(), "Sample content 1");

        File file2 = new File(root, "file2.txt");
        Files.writeString(file2.toPath(), "Sample content 2");

        // Create a subdirectory
        File subDir = new File(root, "subDir");
        subDir.mkdir();

        // Create a file in the subdirectory
        File file3 = new File(subDir, "file3.txt");
        Files.writeString(file3.toPath(), "Sample content 3");
    }

    @Test
    public void testProcessDirectory() {
        List<FilePathTuple> result = DirectoryReader.getDirectoryContents(testDirectory);

        assertNotNull(result);
        assertEquals(3, result.size()); // We expect 3 files

        Set<String> paths = result.stream().map(FilePathTuple::relativePath).collect(Collectors.toSet());

        // Check relative paths
        assertEquals(3, paths.size());
        assertTrue(paths.contains("file1.txt"));
        assertTrue(paths.contains("file2.txt"));
        assertTrue(paths.contains("subDir" + File.separator + "file3.txt"));

        // Additional assertions can be made on file sizes, etc.
        assertTrue(result.stream().anyMatch(tuple -> tuple.file().getName().equals("file1.txt")));
    }

    @Test
    public void testEmptyDirectory() throws IOException {
        // Create an empty directory for testing
        File emptyDir = Files.createTempDirectory("emptyDir").toFile();

        List<FilePathTuple> result = DirectoryReader.getDirectoryContents(emptyDir);

        assertNotNull(result);
        assertTrue(result.isEmpty()); // Expecting an empty list
    }

    @AfterEach
    public void tearDown() throws IOException {
        // Delete the test directory and its contents
        for (File file : testDirectory.listFiles()) {
            if (file.isDirectory()) {
                deleteDirectory(file);
            } else {
                file.delete();
            }
        }
        testDirectory.delete();
    }

    private void deleteDirectory(File directory) throws IOException {
        for (File file : directory.listFiles()) {
            if (file.isDirectory()) {
                deleteDirectory(file);
            }
            file.delete();
        }
        directory.delete();
    }
}
