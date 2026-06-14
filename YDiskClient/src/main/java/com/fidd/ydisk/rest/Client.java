package com.fidd.ydisk.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fidd.ydisk.rest.models.Link;
import com.fidd.ydisk.rest.models.Resource;
import com.fidd.ydisk.rest.models.YandexApiError;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

import javax.annotation.Nullable;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.file.Path;
import java.time.Duration;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class Client {
    protected static final String DEFAULT_API_BASE = "https://cloud-api.yandex.net/v1/disk";

    public static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss.SSS").withZone(ZoneId.systemDefault());

    protected final String apiBase;
    protected final OkHttpClient httpClient;
    protected final String oauthToken;
    protected final ObjectMapper mapper;

    public Client(String oauthToken, ObjectMapper mapper) {
        this(
                oauthToken,
                mapper,
                DEFAULT_API_BASE,
                new OkHttpClient.Builder()
                        .dns(new DnsOverHttpsClient())
                        .connectTimeout(Duration.ofSeconds(10))
                        .followRedirects(false) // matches your current behavior
                        .followSslRedirects(false)
                        .build()
        );
    }

    protected Client(String oauthToken, ObjectMapper mapper, String apiBase, OkHttpClient httpClient) {
        this.oauthToken = oauthToken;
        this.mapper = mapper;
        this.apiBase = apiBase;
        this.httpClient = httpClient;
    }

    protected Request.Builder baseRequest(String endpoint) {
        return new Request.Builder()
                .url(apiBase + endpoint)
                .header("Authorization", "OAuth " + oauthToken)
                .header("Accept", "application/json");
    }

    protected <T> T send(Request request, Class<T> responseClass) throws Exception {
        try (Response response = httpClient.newCall(request).execute()) {
            ResponseBody responseBody = response.body();
            String body = responseBody != null ? responseBody.string() : "";

            if (!response.isSuccessful()) {
                try {
                    YandexApiError apiError =
                            mapper.readValue(body, YandexApiError.class);

                    throw new RuntimeException(
                            "Yandex Disk Error [" + response.code() + "]: "
                                    + apiError.message()
                                    + " - "
                                    + apiError.description()
                    );
                } catch (Exception e) {
                    if (e instanceof RuntimeException) {
                        throw e;
                    }

                    throw new RuntimeException(
                            "HTTP Error [" + response.code() + "]: " + body
                    );
                }
            }

            return mapper.readValue(body, responseClass);
        }
    }

    public Resource getResources(String path) throws Exception {
        String endpoint =
                "/resources?path="
                        + URLEncoder.encode(path, StandardCharsets.UTF_8)
                        + "&limit=200";

        Request request = baseRequest(endpoint)
                .get()
                .build();

        return send(request, Resource.class);
    }

    public InputStream downloadFile(String remotePath) throws Exception {
        return downloadFileWithRange(remotePath, 0, null);
    }

    public InputStream downloadFileWithRange(String remotePath, long offset, @Nullable Long limit) throws Exception {
        Link downloadLink = getDownloadLink(remotePath);
        URI uri = URI.create(downloadLink.href());

        return downloadOrRedirect(uri, offset, limit, 0);
    }

    protected InputStream downloadOrRedirect(URI uri, long offset, @Nullable Long limit, int redirectCount) throws Exception {
        if (redirectCount > 5) {
            throw new RuntimeException("Too many redirects");
        }

        Request.Builder builder = new Request.Builder()
                .url(uri.toString())
                .get();

        if (offset > 0 || limit != null) {
            String rangeHeader = "bytes=" + offset + "-";
            if (limit != null && limit > 0) {
                rangeHeader += (offset + limit - 1);
            }
            builder.header("Range", rangeHeader);
        }

        Response response = httpClient.newCall(builder.build()).execute();

        int code = response.code();

        if (code >= 300 && code < 400) {

            String location = response.header("Location");

            response.close();

            if (location == null) {
                throw new RuntimeException("Redirect without Location");
            }

            return downloadOrRedirect(
                    uri.resolve(location),
                    offset,
                    limit,
                    redirectCount + 1
            );
        }

        if (code >= 400 && code != 416) {
            response.close();
            throw new RuntimeException(
                    "Download failed with status: " + code
            );
        }

        okhttp3.ResponseBody responseBody = response.body();
        if (responseBody == null) {
            response.close();
            throw new RuntimeException("Download response had no body");
        }
        return responseBody.byteStream();
    }

    public Link getUploadLink(String remotePath, boolean overwrite) throws Exception {
        String endpoint = "/resources/upload?path=" + URLEncoder.encode(remotePath, StandardCharsets.UTF_8) + "&overwrite=" + overwrite;
        Request request = baseRequest(endpoint).get().build();
        return send(request, Link.class);
    }

    public void uploadFile(String remotePath, Path localFile) throws Exception {
        Link uploadLink = getUploadLink(remotePath, true);

        RequestBody body = RequestBody.create(localFile.toFile(), null);

        Request request = new Request.Builder()
                .url(uploadLink.href())
                .method(uploadLink.method(), body)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (response.code() != 201 && response.code() != 202) {
                throw new RuntimeException("Upload failed with status: " + response.code());
            }
        }
    }

    public Link getDownloadLink(String remotePath) throws Exception {
        String endpoint = "/resources/download?path=" + URLEncoder.encode( remotePath, StandardCharsets.UTF_8);
        Request request = baseRequest(endpoint).get().build();
        return send(request, Link.class);
    }
}
