package com.fidd.connectors.folder;

import com.fidd.connectors.FiddConnector;
import com.fidd.connectors.base.BaseDirectoryConnector;
import com.fidd.core.common.SubFileInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

public class FolderFiddConnector extends BaseDirectoryConnector implements FiddConnector {
    public final static Logger LOGGER = LoggerFactory.getLogger(FolderFiddConnector.class);

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

    @Override
    protected List<String> getListing(String fiddPath, boolean isDirectory) throws IOException {
        try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(
                Path.of(fiddPath), f -> isDirectory ? Files.isDirectory(f) :  Files.isRegularFile(f))) {
            List<String> result = new ArrayList<>();
            for (Path path : directoryStream) {
                try {
                    result.add(path.toString());
                } catch(Exception e) {
                    LOGGER.debug("Fidd subfolder is not a message / message number parse error", e);
                }
            }

            return result;
        } catch (IOException e) {
            System.err.println("Error reading directory: " + e.getMessage());
            throw e;
        }
    }

    @Override
    protected String fiddFolderPath() { return fiddFolder.toString(); }

    @Override
    protected boolean pathExists(String path) {
        return Files.exists(Path.of(path));
    }

    @Override
    protected boolean pathIsRegularFile(String path) {
        return Files.isRegularFile(Path.of(path));
    }

    @Override
    protected byte[] readAllBytes(String path) throws IOException {
        return Files.readAllBytes(Path.of(path));
    }

    @Override
    protected long size(String path) throws IOException {
        return Files.size(Path.of(path));
    }

    @Override
    protected InputStream getSubInpuStream(String path, long offset, long length) throws IOException {
        return SubFileInputStream.of(Path.of(path).toFile(), offset, length);
    }
}
