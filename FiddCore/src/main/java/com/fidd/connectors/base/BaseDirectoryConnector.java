package com.fidd.connectors.base;

import com.fidd.connectors.FiddConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

public abstract class BaseDirectoryConnector implements FiddConnector {
    final static Logger LOGGER = LoggerFactory.getLogger(BaseDirectoryConnector.class);

    protected abstract List<String> getSubDirectoryListing(String fiddPath) throws IOException;
    protected abstract String fiddFolderPath();

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
                    Long msgNum = Long.parseLong(Path.of(path).getFileName().toString());
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
}
