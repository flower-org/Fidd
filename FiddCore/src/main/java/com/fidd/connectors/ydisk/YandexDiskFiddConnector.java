package com.fidd.connectors.ydisk;

import com.fidd.connectors.FiddConnector;
import com.fidd.connectors.base.BaseDirectoryConnector;
import com.yandex.disk.rest.Credentials;
import com.yandex.disk.rest.ResourcesArgs;
import com.yandex.disk.rest.RestClient;
import com.yandex.disk.rest.exceptions.ServerIOException;
import com.yandex.disk.rest.json.Resource;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class YandexDiskFiddConnector extends BaseDirectoryConnector implements FiddConnector {
    public static void main(String[] args) throws IOException {
        URL url = new URL("https://user:token@ydisk/");
        FiddConnector connector = new YandexDiskFiddConnector(url);

        connector.getMessageNumbersTail(10);

/*        RestClient client = new RestClient(creds);

        DiskInfo diskInfo = client.getDiskInfo();
        System.out.println(diskInfo);

        ResourcesArgs rootDirSubdirArgs = new ResourcesArgs.Builder().setPath("/").setMediaType("dir").build();
        Resource resources = client.getResources(rootDirSubdirArgs);

        System.out.println(resources);*/
    }

    final String user;
    final String token;
    final RestClient client;
    final String fiddFolderPath;

    public YandexDiskFiddConnector(URL fiddFolderUrl) {
        try {
            String userInfo = fiddFolderUrl.getUserInfo();
            String[] userInfoParts = userInfo.split(":");
            user = userInfoParts[0];
            token = userInfoParts[1];
            fiddFolderPath = fiddFolderUrl.getPath();
            client = new RestClient(new Credentials(user, token));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected List<String> getSubDirectoryListing(String fiddPath) throws IOException {
        ResourcesArgs rootDirSubdirArgs = new ResourcesArgs.Builder().setPath("/").setMediaType("dir").build();
        try {
            List<String> result = new ArrayList<>();
            Resource resources = client.getResources(rootDirSubdirArgs);
            for (Resource resource : resources.getResourceList().getItems()) {
                result.add(resource.getPath().getPath());
            }
            return result;
        } catch (ServerIOException e) {
            throw new IOException(e);
        }
    }

    @Override
    public List<Long> getMessageNumbersTail(int count) {
        return getMessagesTail(fiddFolderPath, null, count, true);
    }

    @Override
    public List<Long> getMessageNumbersBefore(long messageNumber, int count, boolean inclusive) {
        return getMessagesTail(fiddFolderPath, messageNumber, count, false);
    }

    @Override
    public List<Long> getMessageNumbersBetween(long latestMessage, boolean inclusiveLatest,
                                               long earliestMessage, boolean inclusiveEarliest, int count, boolean getLatest) {
        return getMessageNumbersBetween(fiddFolderPath, latestMessage, inclusiveLatest, earliestMessage, inclusiveEarliest, count, getLatest);
    }

    @Override
    public List<byte[]> getFiddKeyCandidates(long messageNumber, byte[] footprint) {
        throw new UnsupportedOperationException();
    }

    @Override
    public @Nullable byte[] getFiddKey(long messageNumber, byte[] key) {
        throw new UnsupportedOperationException();
    }

    @Override
    public @Nullable byte[] getUnencryptedFiddKey(long messageNumber) {
        throw new UnsupportedOperationException();
    }

    @Override
    public long getFiddMessageSize(long messageNumber) {
        throw new UnsupportedOperationException();
    }

    @Override
    public InputStream getFiddMessage(long messageNumber) {
        throw new UnsupportedOperationException();
    }

    @Override
    public InputStream getFiddMessageChunk(long messageNumber, long offset, long length) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getFiddKeySignatureCount(long messageNumber) {
        throw new UnsupportedOperationException();
    }

    @Override
    public byte[] getFiddKeySignature(long messageNumber, int index) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getFiddMessageSignatureCount(long messageNumber) {
        throw new UnsupportedOperationException();
    }

    @Override
    public byte[] getFiddMessageSignature(long messageNumber, int index) {
        throw new UnsupportedOperationException();
    }
}
