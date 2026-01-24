package com.fidd.connectors.ydisk;

import com.fidd.connectors.FiddConnector;
import com.fidd.connectors.base.BaseDirectoryConnector;
import com.yandex.disk.rest.Credentials;
import com.yandex.disk.rest.ResourcesArgs;
import com.yandex.disk.rest.RestClient;
import com.yandex.disk.rest.exceptions.ServerIOException;
import com.yandex.disk.rest.json.Resource;

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
    protected List<String> getListing(String fiddPath, boolean isDirectory) throws IOException {
        ResourcesArgs rootDirSubdirArgs = new ResourcesArgs.Builder().setPath("/").build();
        try {
            List<String> result = new ArrayList<>();
            Resource resources = client.getResources(rootDirSubdirArgs);
            for (Resource resource : resources.getResourceList().getItems()) {
                if (isDirectory && "dir".equals(resource.getType())) {
                    // Add dir
                    result.add(resource.getPath().getPath());
                } else if (!isDirectory && "file".equals(resource.getType())) {
                    //Add file
                    result.add(resource.getPath().getPath());
                }
            }
            return result;
        } catch (ServerIOException e) {
            throw new IOException(e);
        }
    }

    @Override
    protected String fiddFolderPath() { return fiddFolderPath; }

    @Override
    protected boolean pathExists(String path) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected boolean pathIsRegularFile(String path) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected byte[] readAllBytes(String path) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    protected long size(String path) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    protected InputStream getInputStream(String path) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    protected InputStream getSubInpuStream(String path, long offset, long length) throws IOException {
        throw new UnsupportedOperationException();
    }
}
