package com.fidd.connectors.folder;

import com.fidd.connectors.FiddConnector;
import com.fidd.core.common.SubFileInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.checkNotNull;

public class FolderFiddConnector implements FiddConnector {
    public final static Logger LOGGER = LoggerFactory.getLogger(FolderFiddConnector.class);

    public final static String KEY_SUBFOLDER = "keys";
    public final static String KEY_FILE_NAME = "fidd.key";
    public final static String MESSAGE_FILE_NAME = "fidd.message";

    // Regex: fidd.key.<digits>.sign
    public final static Pattern FIDD_KEY_SIGNATURE_PATTERN = Pattern.compile("fidd\\.key\\.(\\d+)\\.sign");
    // Regex: fidd.message.<digits>.sign
    public final static Pattern FIDD_MESSAGE_SIGNATURE_PATTERN = Pattern.compile("fidd\\.message\\.(\\d+)\\.sign");

    protected static String getFileNameNoExtensions(String fileName) {
        int dot = fileName.indexOf('.');
        return (dot == -1) ? fileName : fileName.substring(0, dot);
    }

    protected static boolean keyFileStartsWith(String full, String fileName) {
        return full.startsWith(getFileNameNoExtensions(fileName));
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
        TreeMap<Long, Long> messages = new TreeMap<>((o1, o2) -> Long.compare(o2, o1));
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
                                                         long earliestMessage, boolean inclusiveEarliest) {
        TreeMap<Long, Long> messages = getMessagesNumberMap(fiddPath);

        SortedMap<Long, Long> tailMap = messages.tailMap(latestMessage, inclusiveLatest);

        List<Long> result = new ArrayList<>();
        for (Long key : tailMap.keySet()) {
            if (key > earliestMessage) {
                result.add(key);
            } else if (key == earliestMessage) {
                if (inclusiveEarliest) {
                    result.add(key);
                }
            } else {
                break;
            }
        }

        return result;
    }

    protected final String fiddFolderPath;
    protected final Path fiddFolder;

    public FolderFiddConnector(String fiddFolderPath) {
        this.fiddFolderPath = fiddFolderPath;
        this.fiddFolder = Paths.get(fiddFolderPath);
    }

    protected Path messageFolderPath(long messageNumber) { return fiddFolder.resolve(Long.toString(messageNumber)); }
    protected Path keyFolderPath(long messageNumber) { return messageFolderPath(messageNumber).resolve(KEY_SUBFOLDER); }
    protected Path messageFilePath(long messageNumber) { return messageFolderPath(messageNumber).resolve(MESSAGE_FILE_NAME); }

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
                                               long earliestMessage, boolean inclusiveEarliest) {
        return getMessageNumbersBetween(fiddFolder, latestMessage, inclusiveLatest, earliestMessage, inclusiveEarliest);
    }

    @Override
    public List<byte[]> getCandidateKeyFiles(long messageNumber, byte[] subscriberId) {
        List<byte[]> result = new ArrayList<>();
        String prefix = new String(subscriberId, StandardCharsets.UTF_8);
        Path keyFolder = keyFolderPath(messageNumber);
        if (!keyFolder.toFile().exists()) {
            return result;
        }

        try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(keyFolder,
                directoryEntry -> Files.isRegularFile(directoryEntry)
                        && (keyFileStartsWith(prefix, directoryEntry.getFileName().toString())))) {
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
                result.add(Files.readAllBytes(file));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return result;
    }

    @Override
    public @Nullable byte[] getUnencryptedKeyFile(long messageNumber) {
        try {
            Path file = messageFolderPath(messageNumber).resolve(KEY_FILE_NAME);
            if (!Files.exists(file) || !Files.isRegularFile(file)) {
                return null;
            }
            return Files.readAllBytes(file);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public InputStream getMessageFile(long messageNumber) {
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
    public InputStream getMessageFileChunk(long messageNumber, long offset, long length) {
        try {
            Path messageFilePath = messageFilePath(messageNumber);
            if (!Files.exists(messageFilePath) || !Files.isRegularFile(messageFilePath)) {
                throw new FileNotFoundException("Message file not found: " + messageNumber);
            }
            return new SubFileInputStream(messageFilePath.toFile(), offset, length);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public int getKeyFileSignatureCount(long messageNumber) {
        return getSignatureCount(messageFolderPath(messageNumber), FIDD_KEY_SIGNATURE_PATTERN, messageNumber);
    }

    @Override
    public byte[] getKeyFileSignature(long messageNumber, int index) {
        try {
            String keyFileSignatureString = String.format(KEY_FILE_NAME + ".%d.sign", index);
            Path keyFileSignatureFile = messageFolderPath(messageNumber).resolve(keyFileSignatureString);
            return Files.readAllBytes(keyFileSignatureFile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public int getMessageFileSignatureCount(long messageNumber) {
        return getSignatureCount(messageFolderPath(messageNumber), FIDD_MESSAGE_SIGNATURE_PATTERN, messageNumber);
    }

    @Override
    public byte[] getMessageFileSignature(long messageNumber, int index) {
        try {
            String messageFileSignatureString = String.format(MESSAGE_FILE_NAME + ".%d.sign", index);
            Path messageFileSignatureFile = messageFolderPath(messageNumber).resolve(messageFileSignatureString);
            return Files.readAllBytes(messageFileSignatureFile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
