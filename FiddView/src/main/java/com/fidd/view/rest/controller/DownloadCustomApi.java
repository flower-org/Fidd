package com.fidd.view.rest.controller;

import com.fidd.base.BaseRepositories;
import com.fidd.core.encryption.EncryptionAlgorithm;
import com.fidd.core.encryption.RandomAccessEncryptionAlgorithm;
import com.fidd.service.FiddContentService;
import com.fidd.service.FiddContentServiceManager;
import com.fidd.service.LogicalFileInfo;
import com.fidd.view.common.PlaylistSort;
import com.fidd.view.rest.invoker.ApiResponse;
import io.vertx.core.Future;
import io.vertx.ext.web.handler.HttpException;
import org.apache.commons.lang3.StringUtils;

import javax.activation.MimetypesFileTypeMap;
import javax.annotation.Nullable;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static io.netty.handler.codec.http.HttpResponseStatus.REQUESTED_RANGE_NOT_SATISFIABLE;

public class DownloadCustomApi implements DownloadApi {
    protected final FiddContentServiceManager fiddContentServiceManager;
    protected final BaseRepositories baseRepositories;

    public DownloadCustomApi(FiddContentServiceManager fiddContentServiceManager, BaseRepositories baseRepositories) {
        this.fiddContentServiceManager = fiddContentServiceManager;
        this.baseRepositories = baseRepositories;
    }

    public static boolean match(String filename, String filter) {
        // TODO: m.b. LRU cache of patterns?
        // Escape regex special characters except for the wildcard '*'
        String regex = filter.replace(".", "\\.")  // Escape dot
                .replace("?", ".")    // Replace '?' with regex for any single character
                .replace("*", ".*");   // Replace '*' with regex for zero or more characters

        // Compile the regex pattern to match the filename
        Pattern pattern = Pattern.compile(regex);

        // Return true if the filename matches the pattern, otherwise false
        return pattern.matcher(filename).matches();
    }

    public static @Nullable String getContentType(String filePath) {
        int dot_pos = filePath.lastIndexOf(".");
        if (dot_pos >= 0) {
            String fileExt = filePath.substring(dot_pos + 1);
            if (fileExt.equals("mp4")) {
                return "video/mp4";
            } else if (fileExt.equals("log")) {
                return "text/plain";
            } else if (fileExt.equals("svg")) {
                return "image/svg+xml";
            }
        }

        MimetypesFileTypeMap mimeTypesMap = new MimetypesFileTypeMap();
        return mimeTypesMap.getContentType(filePath);
    }

    @Override
    public Future<ApiResponse<FileInfo>> readLogicalFile(String fiddId, Long messageNumber, String logicalFilePath, @Nullable String range,
                                                            @Nullable String _list, @Nullable List<String> filterIn, @Nullable List<String> filterOut,
                                                            @Nullable String sortStr, @Nullable Boolean includeSubfolders) {
        FiddContentService fiddService = fiddContentServiceManager.getService(fiddId);
        if (fiddService == null) {
            return Future.failedFuture(new HttpException(404));
        }

        if (StringUtils.isBlank(_list)) {
            // -----------------------------------------------
            // File download/stream logic
            // -----------------------------------------------
            LogicalFileInfo logicalFileInfo = null;
            List<LogicalFileInfo> logicalFileInfos = fiddService.getLogicalFileInfos(messageNumber);
            if (logicalFileInfos == null) {
                return Future.failedFuture(new HttpException(404));
            }
            // TODO: cache / optimize? MB not on this level
            for (LogicalFileInfo candidate : logicalFileInfos) {
                if (candidate.metadata().filePath().equals(logicalFilePath)) {
                    logicalFileInfo = candidate;
                    break;
                }
            }
            if (logicalFileInfo == null) {
                return Future.failedFuture(new HttpException(404));
            }

            long fileLengthEncrypted = logicalFileInfo.section().sectionLength() - logicalFileInfo.fileOffset();
            EncryptionAlgorithm encryptionAlgorithm = baseRepositories.encryptionAlgorithmRepo().get(logicalFileInfo.section().encryptionAlgorithm());
            if (encryptionAlgorithm == null) {
                return Future.failedFuture(new HttpException(501));
            }
            long fileLength = checkNotNull(encryptionAlgorithm).plaintextLengthToCiphertextLength(fileLengthEncrypted);

            boolean headersGotRange = !StringUtils.isBlank(range);
            if (!headersGotRange) {
                // Get full file content

                InputStream responseFileStream = fiddService.readLogicalFile(messageNumber, logicalFileInfo);
                if (responseFileStream == null) {
                    return Future.failedFuture(new HttpException(404));
                }

                FileInfo responseFileInfo = new FileInfo(responseFileStream);
                responseFileInfo.setContentLength(Long.toString(fileLength));
                String contentType = getContentType(logicalFilePath);
                if (contentType != null) {
                    responseFileInfo.setContentType(contentType);
                }
                return Future.succeededFuture(new ApiResponse<>(200, responseFileInfo));
            } else {
                // Partial content

                //TODO: the standard also describes multiple ranges in the request, like "Range: 0-99,200-299"
                // ideally we want to support that, but practically speaking video streaming (ffplay, VLC, browsers)
                // work just fine without it, utilizing only single range requests.
                if (!(encryptionAlgorithm instanceof RandomAccessEncryptionAlgorithm)) {
                    return Future.failedFuture(new HttpException(REQUESTED_RANGE_NOT_SATISFIABLE.code()));
                }

                range = checkNotNull(range).replace("bytes=", "");
                String[] rangeParts = range.split("-");
                long startPos = 0;
                long endPos = fileLength - 1;
                if (!StringUtils.isBlank(rangeParts[0])) {
                    startPos = Long.parseLong(rangeParts[0]);
                }
                if (rangeParts.length > 1 && !StringUtils.isBlank(rangeParts[1])) {
                    endPos = Long.parseLong(rangeParts[1]);
                }

                if (startPos >= fileLength || endPos >= fileLength) {
                    FileInfo responseFileInfo = new FileInfo(null);
                    responseFileInfo.setContentRange("bytes */" + fileLength);
                    return Future.succeededFuture(new ApiResponse<>(416, responseFileInfo));
                }

                long contentLength = endPos - startPos + 1;
                InputStream responseFileStream = fiddService.readLogicalFileChunk(messageNumber, logicalFileInfo, startPos, contentLength);
                if (responseFileStream == null) {
                    return Future.failedFuture(new HttpException(404));
                }

                FileInfo responseFileInfo = new FileInfo(responseFileStream);
                responseFileInfo.setContentRange("bytes " + startPos + "-" + endPos + "/" + fileLength);
                responseFileInfo.setAcceptRanges("bytes");
                responseFileInfo.setContentLength(Long.toString(contentLength));

                String contentType = getContentType(logicalFilePath);
                if (contentType != null) {
                    responseFileInfo.setContentType(contentType);
                }
                return Future.succeededFuture(new ApiResponse<>(206, responseFileInfo));
            }
        } else {
            // -----------------------------------------------
            // Playlist generation logic
            // -----------------------------------------------
            if (!"m3u".equalsIgnoreCase(_list)) {
                return Future.failedFuture(new HttpException(BAD_REQUEST.code()));
            }

            // TODO: cache / optimize? MB not on this level
            List<LogicalFileInfo> logicalFileInfos = fiddService.getLogicalFileInfos(messageNumber);
            if (logicalFileInfos == null) {
                return Future.failedFuture(new HttpException(NOT_FOUND.code()));
            }

            List<String> filteredLogicalFileNames = new ArrayList<>();
            for (LogicalFileInfo candidateLogicalFileInfo : logicalFileInfos) {
                // 1. The file should be under our folder
                if (candidateLogicalFileInfo.metadata().filePath().startsWith(logicalFilePath)) {
                    String remainderPath = candidateLogicalFileInfo.metadata().filePath().substring(logicalFilePath.length());

                    // 2. Include subfolder content only if `includeSubfolders` flag is set
                    includeSubfolders = (includeSubfolders == null) ? false : includeSubfolders;
                    if (!includeSubfolders && remainderPath.contains("/")) {
                        continue;
                    }

                    File file = new File(remainderPath);
                    String filename = file.getName();
                    // 3. FilterIn - allow matching files only
                    if (filterIn != null && !filterIn.isEmpty()) {
                        boolean match = false;
                        for (String filter : filterIn) {
                            if (match(filename, filter)) {
                                match = true;
                                break;
                            }
                        }
                        if (!match) {
                            continue;
                        }
                    }

                    // 4. FilterOut - disallow all matching files
                    if (filterOut != null && !filterOut.isEmpty()) {
                        boolean match = false;
                        for (String filter : filterOut) {
                            if (match(filename, filter)) {
                                match = true;
                                break;
                            }
                        }
                        if (match) {
                            continue;
                        }
                    }

                    // 5. Now we can add file to the result
                    // This works with and without URLEncode in Celluloid and VLC, but URLEncode messes up the filenames
                    filteredLogicalFileNames.add(remainderPath);
                }
            }

            // 6. If custom sorting is requested, sort
            Comparator<String> comparator = null;
            PlaylistSort sort = StringUtils.isBlank(sortStr) ? PlaylistSort.NUMERICAL_ASC : PlaylistSort.valueOf(sortStr);
            switch (sort) {
                case ALPHABETICAL_DESC: { comparator = new AlphabeticalComparator(false); break; }
                case ALPHABETICAL_ASC: { comparator = new AlphabeticalComparator(true); break; }
                case NUMERICAL_DESC: { comparator = new NumericPrefixComparator(false); break; }
                case NUMERICAL_ASC: { comparator = new NumericPrefixComparator(true); break; }
            }

            // TODO: this sorting doesn't play with subfolders too well, we might want files and subfolders in separate groups
            if (comparator != null) {
                filteredLogicalFileNames.sort(comparator);
            }

            String m3uPlaylist = M3uFileCreator.createM3UPlaylist(filteredLogicalFileNames);
            ByteArrayInputStream m3uListStream = new ByteArrayInputStream(m3uPlaylist.getBytes(StandardCharsets.UTF_8));

            FileInfo responseFileInfo = new FileInfo(m3uListStream);
            responseFileInfo.setContentLength(Long.toString(m3uListStream.available()));
            responseFileInfo.setContentType("text/plain");
            String m3uFilename = (fiddId + "/" + messageNumber + "/" + logicalFilePath).replace('/', '_') + ".m3u";
            responseFileInfo.setContentDisposition("attachment; filename=\"" + m3uFilename + "\"");
            return Future.succeededFuture(new ApiResponse<>(200, responseFileInfo));
        }
    }
}
