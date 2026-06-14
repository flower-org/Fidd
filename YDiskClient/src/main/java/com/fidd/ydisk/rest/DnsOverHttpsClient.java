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
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

public class DnsOverHttpsClient implements Dns {
    private final OkHttpClient client;
    private final ObjectMapper mapper;

    private final Cache<String, List<InetAddress>> lookupCache = Caffeine.newBuilder().maximumSize(1024)
            .expireAfterWrite(java.time.Duration.ofMinutes(45)).build();

    public DnsOverHttpsClient(OkHttpClient client, ObjectMapper mapper) {
        this.client = client;
        this.mapper = mapper;
    }

    @Override
    public List<InetAddress> lookup(String hostname) throws UnknownHostException {
        if (IpAddressUtil.isIPAddress(hostname)) {
            return List.of(IpAddressUtil.fromString(hostname));
        }

        try {
            return lookupCache.get(hostname, host -> lookupRaw(host));
        } catch (RuntimeException e) {
            UnknownHostException uhe = new UnknownHostException("DNS lookup failed for " + hostname);
            uhe.initCause(e);
            throw uhe;
        }
    }

    public List<InetAddress> lookupRaw(String hostname) {
        try {
            HttpUrl url = HttpUrl.get("https://1.1.1.1/dns-query")
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

                okhttp3.ResponseBody responseBody = response.body();
                if (responseBody == null) {
                    throw new RuntimeException("DoH response had no body");
                }

                JsonNode root = mapper.readTree(responseBody.string());
                JsonNode answers = root.path("Answer");
                if (!answers.isArray()) {
                    throw new RuntimeException("DNS lookup failed for " + hostname);
                }

                List<InetAddress> result = new ArrayList<>();
                for (JsonNode answer : answers) {
                    int type = answer.path("type").asInt(-1);
                    String ip = answer.path("data").asText(null);
                    if (type == 1 && ip != null) {
                        if (IpAddressUtil.isIPAddress(ip)) {
                            result.add(InetAddress.getByName(ip));
                        }
                    }
                }

                if (result.isEmpty()) { throw new RuntimeException("DOH DNS lookup failed for " + hostname); }
                return result;
            }
        } catch (Exception e) {
            throw new RuntimeException("DOH DNS lookup failed for " + hostname, e);
        }
    }
}