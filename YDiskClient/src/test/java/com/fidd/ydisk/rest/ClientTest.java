package com.fidd.ydisk.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fidd.ydisk.rest.models.Link;
import com.fidd.ydisk.rest.models.Resource;
import okhttp3.OkHttpClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class ClientTest {

    private HttpServer server;
    private String serverUrl;
    private Client client;
    private Map<String, String> responses = new HashMap<>();
    private Map<String, Integer> responseCodes = new HashMap<>();

    @BeforeEach
    public void setUp() throws Exception {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                String pathAndQuery = exchange.getRequestURI().toString();
                int code = responseCodes.getOrDefault(pathAndQuery, 200);
                String resp = responses.get(pathAndQuery);

                // Allow dynamic matches without exact query strings for simpler tests
                if (resp == null) {
                    for(Map.Entry<String, String> entry : responses.entrySet()) {
                        if (pathAndQuery.startsWith(entry.getKey())) {
                            resp = entry.getValue();
                            code = responseCodes.getOrDefault(entry.getKey(), 200);
                            break;
                        }
                    }
                }

                if (resp != null) {
                    byte[] bytes = resp.getBytes(StandardCharsets.UTF_8);
                    exchange.sendResponseHeaders(code, bytes.length);
                    OutputStream os = exchange.getResponseBody();
                    os.write(bytes);
                    os.close();
                } else if ("PUT".equals(exchange.getRequestMethod())) {
                    exchange.sendResponseHeaders(201, 0); // Mock upload chunk
                    exchange.getResponseBody().close();
                } else {
                    exchange.sendResponseHeaders(404, 0);
                    exchange.getResponseBody().close();
                }
            }
        });
        server.start();
        serverUrl = "http://localhost:" + server.getAddress().getPort();

        // Setup default Client targeting our local HTTP server
        client = new Client("mock-token", new ObjectMapper(), serverUrl, new OkHttpClient());
    }

    @AfterEach
    public void tearDown() {
        server.stop(0);
    }

    @Test
    public void testGetResources() throws Exception {
        String json = "{\n" +
                "  \"name\": \"test_dir\",\n" +
                "  \"type\": \"dir\",\n" +
                "  \"path\": \"disk:/test_dir\",\n" +
                "  \"_embedded\": {\n" +
                "    \"items\": [\n" +
                "      {\n" +
                "        \"name\": \"file.txt\",\n" +
                "        \"type\": \"file\",\n" +
                "        \"path\": \"disk:/test_dir/file.txt\",\n" +
                "        \"size\": 1024\n" +
                "      }\n" +
                "    ]\n" +
                "  }\n" +
                "}";

        responses.put("/resources?path=%2F", json);

        Resource res = client.getResources("/");
        assertNotNull(res);
        assertEquals("dir", res.type());
        assertNotNull(res.embedded());
        assertEquals(1, res.embedded().items().size());
        assertEquals("file.txt", res.embedded().items().get(0).name());
        assertEquals(Long.valueOf(1024), res.embedded().items().get(0).size());
    }

    @Test
    public void testGetDownloadLink() throws Exception {
        String json = "{\n" +
                "  \"href\": \""+ serverUrl + "/download-link\",\n" +
                "  \"method\": \"GET\",\n" +
                "  \"templated\": false\n" +
                "}";

        responses.put("/resources/download?path=%2Ftest.txt", json);

        Link link = client.getDownloadLink("/test.txt");
        assertNotNull(link);
        assertEquals(serverUrl + "/download-link", link.href());
        assertEquals("GET", link.method());
    }

    @Test
    public void testDownloadFile() throws Exception {
        // 1. the download link fetch
        String linkJson = "{\"href\": \"" + serverUrl + "/actual-download\", \"method\": \"GET\", \"templated\": false}";
        responses.put("/resources/download?path=%2Ffile.txt", linkJson);

        // 2. the actual download
        responses.put("/actual-download", "File data here");

        try (InputStream is = client.downloadFile("/file.txt")) {
            assertEquals("File data here", new String(is.readAllBytes(), StandardCharsets.UTF_8));
        }
    }

    @Test
    public void testUploadFile() throws Exception {
        // 1. the upload link fetch
        String linkJson = "{\"href\": \"" + serverUrl + "/actual-upload\", \"method\": \"PUT\", \"templated\": false}";
        responses.put("/resources/upload", linkJson); // using startsWith fallback logic in HttpHandler

        // the PUT request will be naturally handled by the "PUT" default in HttpHandler returning 201

        Path tempFile = Files.createTempFile("yandex_test", ".txt");
        Files.write(tempFile, "Hello Yandex".getBytes(StandardCharsets.UTF_8));

        try {
            client.uploadFile("/dest.txt", tempFile);
            // If it didn't throw an Exception, upload was successful
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    @Test
    public void testApiError() {
        String errJson = "{\n" +
                "  \"message\": \"Not Found\",\n" +
                "  \"description\": \"Resource not found.\",\n" +
                "  \"error\": \"DiskNotFoundError\"\n" +
                "}";

        responses.put("/resources?path=%2Fmissing", errJson);
        responseCodes.put("/resources?path=%2Fmissing", 404);

        try {
            client.getResources("/missing");
            fail("Expected RuntimeException");
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("Yandex Disk Error [404]"));
            assertTrue(e.getMessage().contains("Not Found"));
            assertTrue(e.getMessage().contains("Resource not found"));
        }
    }
}
