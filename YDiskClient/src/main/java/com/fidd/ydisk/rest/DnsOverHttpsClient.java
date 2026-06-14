package com.fidd.ydisk.rest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.Dns;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

public class DnsOverHttpsClient implements Dns {
    private final OkHttpClient client = new OkHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    public final Cache<String, List<InetAddress>> lookupCache = Caffeine.newBuilder().maximumSize(1024).build();

    @Override
    public List<InetAddress> lookup(String hostname) {
            return lookupCache.get(hostname,  host -> lookupRaw(host));
    }

    public List<InetAddress> lookupRaw(String hostname) {
        try {
            HttpUrl url = HttpUrl.parse("https://1.1.1.1/dns-query")
                    .newBuilder()
                    .addQueryParameter("name", hostname)
                    .addQueryParameter("type", "A")
                    .build();

            Request request = new Request.Builder()
                    .url(url)
                    .header("accept", "application/dns-json")
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    throw new RuntimeException("DoH failed: " + response.code());
                }

                JsonNode root = mapper.readTree(response.body().string());

                List<InetAddress> result = new ArrayList<>();

                for (JsonNode answer : root.get("Answer")) {
                    int type = answer.path("type").asInt(-1);
                    String data = answer.path("data").asText(null);
                    if (type == 1 && data != null) {
                        String ip = answer.get("data").asText();

                        if (IpAddressUtil.isIPAddress(ip)) {
                            result.add(InetAddress.getByName(ip));
                        }
                    }
                }

                if (result.isEmpty()) { throw new RuntimeException("DNS lookup failed for " + hostname); }

                return result;
            }
        } catch (Exception e) {
            throw new RuntimeException("DNS lookup failed for " + hostname, e);
        }
    }
}