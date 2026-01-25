package com.fidd.connectors.base;

import com.fidd.connectors.FiddConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.fidd.connectors.folder.FolderFiddConstants.ENCRYPTED_FIDD_KEY_FILE_EXT;
import static com.fidd.connectors.folder.FolderFiddConstants.ENCRYPTED_FIDD_KEY_SUBFOLDER;
import static com.fidd.connectors.folder.FolderFiddConstants.FIDD_KEY_FILE_NAME;
import static com.fidd.connectors.folder.FolderFiddConstants.FIDD_MESSAGE_FILE_NAME;
import static com.google.common.base.Preconditions.checkNotNull;

public abstract class BaseDirectoryConnector implements FiddConnector {
    final static Logger LOGGER = LoggerFactory.getLogger(BaseDirectoryConnector.class);
    final static String PATH_SEPARATOR = "/";

    // Regex: fidd.key.<digits>.sign
    public final static Pattern FIDD_KEY_SIGNATURE_PATTERN = Pattern.compile("fidd\\.key\\.(\\d+)\\.sign");
    // Regex: fidd.message.<digits>.sign
    public final static Pattern FIDD_MESSAGE_SIGNATURE_PATTERN = Pattern.compile("fidd\\.message\\.(\\d+)\\.sign");

    protected abstract List<String> getListing(String fiddPath, boolean isDirectory) throws IOException;
    protected abstract String fiddFolderPath();
    protected abstract boolean pathExists(String path) throws IOException;
    protected abstract boolean pathIsRegularFile(String path) throws IOException;
    protected abstract byte[] readAllBytes(String path) throws IOException;
    protected abstract long size(String path) throws IOException;
    protected abstract InputStream getSubInpuStream(String path, long offset, long length) throws IOException;

    protected List<String> getSubDirectoryListing(String fiddPath) throws IOException {
        return getListing(fiddPath, true);
    }

    protected List<String> getFileListing(String fiddPath) throws IOException {
        return getListing(fiddPath, false);
    }

    protected TreeMap<Long, Long> getMessagesNumberMap(String fiddPath) {
        return getMessagesNumberMap(fiddPath, (o1, o2) -> Long.compare(o2, o1));
    }

    protected TreeMap<Long, Long> getMessagesNumberMapEarliestFirst(String fiddPath) {
        return getMessagesNumberMap(fiddPath, Long::compare);
    }

    protected TreeMap<Long, Long> getMessagesNumberMap(String fiddPath, Comparator<Long> comparator) {
        TreeMap<Long, Long> messages = new TreeMap<>(comparator);
        try {
            List<String> subDirectoryList = getSubDirectoryListing(fiddPath);
            for (String path : subDirectoryList) {
                try {
                    Long msgNum = Long.parseLong(getFileName(path));
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

    public List<Long> getMessagesTail(String fiddPath, @Nullable Long startKey, int count, boolean inclusive) {
        TreeMap<Long, Long> messages = getMessagesNumberMap(fiddPath);
        if (messages.isEmpty()) { return List.of(); }

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

    protected List<Long> getMessageNumbersBetween(String fiddPath, long latestMessage, boolean inclusiveLatest,
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

    @Override
    public List<Long> getMessageNumbersTail(int count) {
        return getMessagesTail(fiddFolderPath(), null, count, true);
    }

    @Override
    public List<Long> getMessageNumbersBefore(long messageNumber, int count, boolean inclusive) {
        return getMessagesTail(fiddFolderPath(), messageNumber, count, false);
    }

    @Override
    public List<Long> getMessageNumbersBetween(long latestMessage, boolean inclusiveLatest,
                                               long earliestMessage, boolean inclusiveEarliest, int count, boolean getLatest) {
        return getMessageNumbersBetween(fiddFolderPath(), latestMessage, inclusiveLatest, earliestMessage, inclusiveEarliest, count, getLatest);
    }

    protected String messageFolderPath(long messageNumber) {
        String fiddFolderPath = fiddFolderPath();
        if (!fiddFolderPath.endsWith(PATH_SEPARATOR)) { fiddFolderPath += PATH_SEPARATOR; }
        return fiddFolderPath + messageNumber;
    }
    protected String keyFolderPath(long messageNumber) { return messageFolderPath(messageNumber) + PATH_SEPARATOR + ENCRYPTED_FIDD_KEY_SUBFOLDER; }
    protected String messageFilePath(long messageNumber) { return messageFolderPath(messageNumber) + PATH_SEPARATOR + FIDD_MESSAGE_FILE_NAME; }

    protected static String getFileName(String path) {
        return Path.of(path).getFileName().toString();
    }

    protected static String getFileNameNoExtensions(String fileName) {
        fileName = getFileName(fileName);
        int dot = fileName.indexOf('.');
        return (dot == -1) ? fileName : fileName.substring(0, dot);
    }

    public static boolean footprintStartsWith(String footprint, String fileName) {
        return footprint.startsWith(getFileNameNoExtensions(fileName));
    }

    public static boolean keyFileStartsWith(String fileName, String footprint) {
        return getFileNameNoExtensions(fileName).startsWith(footprint);
    }

    @Override
    public List<byte[]> getFiddKeyCandidates(long messageNumber, byte[] footprintBytes) throws IOException {
        List<byte[]> result = new ArrayList<>();
        String footprint = new String(footprintBytes, StandardCharsets.UTF_8);
        String keyFolder = keyFolderPath(messageNumber);
        if (!pathExists(keyFolder)) {
            return result;
        }

        List<String> sortedFiles = new ArrayList<>();
        for (String fileName : getFileListing(keyFolder)) {
            if (footprintStartsWith(footprint, fileName) || keyFileStartsWith(fileName, footprint)) {
                sortedFiles.add(fileName);
            }
        }
        // Longest prefix first
        sortedFiles.sort((fn1, fn2) -> {
            String fileName1 = getFileNameNoExtensions(fn1);
            String fileName2 = getFileNameNoExtensions(fn2);
            return Integer.compare(fileName2.length(), fileName1.length());
        });
        for (String fileName : sortedFiles) {
            result.add(getFileNameNoExtensions(fileName).getBytes(StandardCharsets.UTF_8));
        }

        return result;
    }

    @Override
    public @Nullable byte[] getFiddKey(long messageNumber, byte[] key) {
        try {
            String keyFolder = keyFolderPath(messageNumber);
            if (!pathExists(keyFolder)) {
                return null;
            }

            String keyFileName = new String(key, StandardCharsets.UTF_8);
            String keyFile = keyFolder + PATH_SEPARATOR + (keyFileName + ENCRYPTED_FIDD_KEY_FILE_EXT);
            if (!pathExists(keyFile)) {
                return null;
            }
            return readAllBytes(keyFile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public @Nullable byte[] getUnencryptedFiddKey(long messageNumber) {
        try {
            String file = messageFolderPath(messageNumber) + PATH_SEPARATOR + FIDD_KEY_FILE_NAME;
            if (!pathExists(file) || !pathIsRegularFile(file)) {
                return null;
            }
            return readAllBytes(file);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public long getFiddMessageSize(long messageNumber) {
        try {
            String messageFilePath = messageFilePath(messageNumber);
            if (!pathExists(messageFilePath) || !pathIsRegularFile(messageFilePath)) {
                throw new FileNotFoundException("Message file not found: " + messageNumber);
            }
            return size(messageFilePath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static @Nullable Integer signatureMatch(Pattern pattern, String filename) {
        Matcher matcher = pattern.matcher(filename);
        if (matcher.matches()) {
            return Integer.parseInt(matcher.group(1));
        }
        return null;
    }

    public int getSignatureCount(String messageFolderPath, Pattern pattern, long messageNumber) {
        try {
            if (!pathExists(messageFolderPath) || pathIsRegularFile(messageFolderPath)) {
                throw new FileNotFoundException("Message folder not found: " + messageNumber);
            }

            int maxIndex = -1;
            try {
                for (String filePath : getFileListing(messageFolderPath)) {
                    try {
                        int signatureIndex = checkNotNull(signatureMatch(pattern, getFileName(filePath)));
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

    @Override
    public int getFiddKeySignatureCount(long messageNumber) {
        return getSignatureCount(messageFolderPath(messageNumber), FIDD_KEY_SIGNATURE_PATTERN, messageNumber);
    }

    @Override
    public byte[] getFiddKeySignature(long messageNumber, int index) {
        try {
            String keyFileSignatureString = String.format(FIDD_KEY_FILE_NAME + ".%d.sign", index);
            String keyFileSignatureFile = messageFolderPath(messageNumber) + PATH_SEPARATOR + keyFileSignatureString;
            return readAllBytes(keyFileSignatureFile);
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
            String messageFileSignatureFile = messageFolderPath(messageNumber) + PATH_SEPARATOR + messageFileSignatureString;
            return readAllBytes(messageFileSignatureFile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public InputStream getFiddMessageChunk(long messageNumber, long offset, long length) {
        try {
            String messageFilePath = messageFilePath(messageNumber);
            if (!pathExists(messageFilePath) || !pathIsRegularFile(messageFilePath)) {
                throw new FileNotFoundException("Message file not found: " + messageNumber);
            }
            return getSubInpuStream(messageFilePath, offset, length);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
