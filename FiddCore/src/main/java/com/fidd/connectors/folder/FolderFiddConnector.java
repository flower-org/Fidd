package com.fidd.connectors.folder;

import com.fidd.connectors.FiddConnector;
import com.fidd.core.common.SubFileInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.fidd.connectors.folder.FolderFiddConstants.ENCRYPTED_FIDD_KEY_FILE_EXT;
import static com.fidd.connectors.folder.FolderFiddConstants.FIDD_KEY_FILE_NAME;
import static com.fidd.connectors.folder.FolderFiddConstants.ENCRYPTED_FIDD_KEY_SUBFOLDER;
import static com.fidd.connectors.folder.FolderFiddConstants.FIDD_MESSAGE_FILE_NAME;
import static com.google.common.base.Preconditions.checkNotNull;

public class FolderFiddConnector implements FiddConnector {
    public final static Logger LOGGER = LoggerFactory.getLogger(FolderFiddConnector.class);

    // Regex: fidd.key.<digits>.sign
    public final static Pattern FIDD_KEY_SIGNATURE_PATTERN = Pattern.compile("fidd\\.key\\.(\\d+)\\.sign");
    // Regex: fidd.message.<digits>.sign
    public final static Pattern FIDD_MESSAGE_SIGNATURE_PATTERN = Pattern.compile("fidd\\.message\\.(\\d+)\\.sign");

    protected static String getFileNameNoExtensions(String fileName) {
        int dot = fileName.indexOf('.');
        return (dot == -1) ? fileName : fileName.substring(0, dot);
    }

    protected static boolean footprintStartsWith(String footprint, String fileName) {
        return footprint.startsWith(getFileNameNoExtensions(fileName));
    }

    protected static boolean keyFileStartsWith(String fileName, String footprint) {
        return getFileNameNoExtensions(fileName).startsWith(footprint);
    }

    protected static @Nullable Integer signatureMatch(Pattern pattern, String filename) {
        Matcher matcher = pattern.matcher(filename);
        if (matcher.matches()) {
            return Integer.parseInt(matcher.group(1));
        }
        return null;
    }

    protected static int getSignatureCount(Path messageFolderPath, Pattern pattern, long messageNumber) {
        try {
            if (!Files.exists(messageFolderPath) || !Files.isDirectory(messageFolderPath)) {
                throw new FileNotFoundException("Message folder not found: " + messageNumber);
            }

            int maxIndex = -1;
            try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(messageFolderPath,
                    directoryEntry -> Files.isRegularFile(directoryEntry)
                            && (signatureMatch(pattern, directoryEntry.getFileName().toString()) != null))) {
                for (Path directoryEntry : directoryStream) {
                    try {
                        int signatureIndex = checkNotNull(signatureMatch(pattern, directoryEntry.getFileName().toString()));
                        maxIndex = Math.max(signatureIndex, maxIndex);
                    } catch(Exception e) {
                        LOGGER.debug("Fidd subfolder is not a message / message number parse error", e);
                    }
                }
            } catch (IOException e) {
                System.err.println("Error reading directory: " + e.getMessage());
            }

            return maxIndex + 1;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected static TreeMap<Long, Long> getMessagesNumberMap(Path fiddPath) {
        return getMessagesNumberMap(fiddPath, (o1, o2) -> Long.compare(o2, o1));
    }

    protected static TreeMap<Long, Long> getMessagesNumberMapEarliestFirst(Path fiddPath) {
        return getMessagesNumberMap(fiddPath, Long::compare);
    }

    protected static TreeMap<Long, Long> getMessagesNumberMap(Path fiddPath, Comparator<Long> comparator) {
        TreeMap<Long, Long> messages = new TreeMap<>(comparator);
        try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(fiddPath, Files::isDirectory)) {
            for (Path path : directoryStream) {
                try {
                    Long msgNum = Long.parseLong(path.getFileName().toString());
                    messages.put(msgNum, msgNum);
                } catch(Exception e) {
                    LOGGER.debug("Fidd subfolder is not a message / message number parse error", e);
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading directory: " + e.getMessage());
        }

        return messages;
    }

    protected static List<Long> getMessagesTail(Path fiddPath, @Nullable Long startKey, int count, boolean inclusive) {
        TreeMap<Long, Long> messages = getMessagesNumberMap(fiddPath);

        if (startKey == null) { startKey = messages.firstKey(); }
        SortedMap<Long, Long> tailMap = messages.tailMap(startKey, inclusive);

        List<Long> result = new ArrayList<>();
        int index = 0;
        for (Long key : tailMap.keySet()) {
            if (index < count) {
                result.add(key);
                index++;
            } else {
                break;
            }
        }

        return result;
    }

    protected static List<Long> getMessageNumbersBetween(Path fiddPath, long latestMessage, boolean inclusiveLatest,
                                                         long earliestMessage, boolean inclusiveEarliest, int count, boolean getLatest) {
        List<Long> result = new ArrayList<>();
        int addedCount = 0;

        if (getLatest) {
            TreeMap<Long, Long> messages = getMessagesNumberMap(fiddPath);
            SortedMap<Long, Long> tailMap = messages.tailMap(latestMessage, inclusiveLatest);
            for (Long key : tailMap.keySet()) {
                if (addedCount >= count) {
                    break;
                }
                if (key > earliestMessage) {
                    result.add(key);
                    addedCount++;
                } else if (key == earliestMessage) {
                    if (inclusiveEarliest) {
                        result.add(key);
                        addedCount++;
                    }
                } else {
                    break;
                }
            }
        } else {
            TreeMap<Long, Long> messages = getMessagesNumberMapEarliestFirst(fiddPath);
            SortedMap<Long, Long> tailMap = messages.tailMap(earliestMessage, inclusiveEarliest);
            for (Long key : tailMap.keySet()) {
                if (addedCount >= count) {
                    break;
                }
                if (key < latestMessage) {
                    result.add(0, key);
                    addedCount++;
                } else if (key == latestMessage) {
                    if (inclusiveLatest) {
                        result.add(0, key);
                        addedCount++;
                    }
                } else {
                    break;
                }
            }
        }

        return result;
    }

    protected final String fiddFolderPath;
    protected final Path fiddFolder;

    public FolderFiddConnector(URL fiddFolderUrl) {
        try {
            Path fiddFolder = new File(fiddFolderUrl.toURI()).toPath();
            this.fiddFolder = fiddFolder;
            this.fiddFolderPath = fiddFolder.toAbsolutePath().toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public FolderFiddConnector(String fiddFolderPath) {
        this.fiddFolderPath = fiddFolderPath;
        this.fiddFolder = Paths.get(fiddFolderPath);
    }

    public FolderFiddConnector(Path fiddFolder) {
        this.fiddFolder = fiddFolder;
        this.fiddFolderPath = fiddFolder.toAbsolutePath().toString();
    }

    protected Path messageFolderPath(long messageNumber) { return fiddFolder.resolve(Long.toString(messageNumber)); }
    protected Path keyFolderPath(long messageNumber) { return messageFolderPath(messageNumber).resolve(ENCRYPTED_FIDD_KEY_SUBFOLDER); }
    protected Path messageFilePath(long messageNumber) { return messageFolderPath(messageNumber).resolve(FIDD_MESSAGE_FILE_NAME); }

    @Override
    public List<Long> getMessageNumbersTail(int count) {
        return getMessagesTail(fiddFolder, null, count, true);
    }

    @Override
    public List<Long> getMessageNumbersBefore(long messageNumber, int count, boolean inclusive) {
        return getMessagesTail(fiddFolder, messageNumber, count, false);
    }

    @Override
    public List<Long> getMessageNumbersBetween(long latestMessage, boolean inclusiveLatest,
                                               long earliestMessage, boolean inclusiveEarliest, int count, boolean getLatest) {
        return getMessageNumbersBetween(fiddFolder, latestMessage, inclusiveLatest, earliestMessage, inclusiveEarliest, count, getLatest);
    }

    @Override
    public List<byte[]> getFiddKeyCandidates(long messageNumber, byte[] footprintBytes) {
        List<byte[]> result = new ArrayList<>();
        String footprint = new String(footprintBytes, StandardCharsets.UTF_8);
        Path keyFolder = keyFolderPath(messageNumber);
        if (!keyFolder.toFile().exists()) {
            return result;
        }

        try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(keyFolder,
                directoryEntry -> Files.isRegularFile(directoryEntry)
                        && (
                            (footprintStartsWith(footprint, directoryEntry.getFileName().toString()))
                            ||
                            (keyFileStartsWith(directoryEntry.getFileName().toString(), footprint))
                        )
        )) {
            List<Path> sortedFiles = new ArrayList<>();
            for (Path p : directoryStream) {
                sortedFiles.add(p);
            }
            // Longest prefix first
            sortedFiles.sort((o1, o2) -> {
                String fileName1 = getFileNameNoExtensions(o1.getFileName().toString());
                String fileName2 = getFileNameNoExtensions(o2.getFileName().toString());
                return Integer.compare(fileName2.length(), fileName1.length());
            });
            for (Path file : sortedFiles) {
                result.add(getFileNameNoExtensions(file.getFileName().toString()).getBytes(StandardCharsets.UTF_8));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return result;
    }

    @Override
    public @Nullable byte[] getFiddKey(long messageNumber, byte[] key) {
        try {
            Path keyFolder = keyFolderPath(messageNumber);
            if (!keyFolder.toFile().exists()) {
                return null;
            }

            String keyFileName = new String(key, StandardCharsets.UTF_8);
            Path keyFile = keyFolder.resolve(keyFileName + ENCRYPTED_FIDD_KEY_FILE_EXT);
            if (!keyFile.toFile().exists()) {
                return null;
            }
            return Files.readAllBytes(keyFile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public @Nullable byte[] getUnencryptedFiddKey(long messageNumber) {
        try {
            Path file = messageFolderPath(messageNumber).resolve(FIDD_KEY_FILE_NAME);
            if (!Files.exists(file) || !Files.isRegularFile(file)) {
                return null;
            }
            return Files.readAllBytes(file);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public long getFiddMessageSize(long messageNumber) {
        try {
            Path messageFilePath = messageFilePath(messageNumber);
            if (!Files.exists(messageFilePath) || !Files.isRegularFile(messageFilePath)) {
                throw new FileNotFoundException("Message file not found: " + messageNumber);
            }
            return Files.size(messageFilePath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public InputStream getFiddMessage(long messageNumber) {
        try {
            Path messageFilePath = messageFilePath(messageNumber);
            if (!Files.exists(messageFilePath) || !Files.isRegularFile(messageFilePath)) {
                throw new FileNotFoundException("Message file not found: " + messageNumber);
            }
            return Files.newInputStream(messageFilePath, StandardOpenOption.READ);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public InputStream getFiddMessageChunk(long messageNumber, long offset, long length) {
        try {
            Path messageFilePath = messageFilePath(messageNumber);
            if (!Files.exists(messageFilePath) || !Files.isRegularFile(messageFilePath)) {
                throw new FileNotFoundException("Message file not found: " + messageNumber);
            }
            return SubFileInputStream.of(messageFilePath.toFile(), offset, length);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public int getFiddKeySignatureCount(long messageNumber) {
        return getSignatureCount(messageFolderPath(messageNumber), FIDD_KEY_SIGNATURE_PATTERN, messageNumber);
    }

    @Override
    public byte[] getFiddKeySignature(long messageNumber, int index) {
        try {
            String keyFileSignatureString = String.format(FIDD_KEY_FILE_NAME + ".%d.sign", index);
            Path keyFileSignatureFile = messageFolderPath(messageNumber).resolve(keyFileSignatureString);
            return Files.readAllBytes(keyFileSignatureFile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public int getFiddMessageSignatureCount(long messageNumber) {
        return getSignatureCount(messageFolderPath(messageNumber), FIDD_MESSAGE_SIGNATURE_PATTERN, messageNumber);
    }

    @Override
    public byte[] getFiddMessageSignature(long messageNumber, int index) {
        try {
            String messageFileSignatureString = String.format(FIDD_MESSAGE_FILE_NAME + ".%d.sign", index);
            Path messageFileSignatureFile = messageFolderPath(messageNumber).resolve(messageFileSignatureString);
            return Files.readAllBytes(messageFileSignatureFile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
