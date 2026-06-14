package com.fidd.data.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.vertx.mysqlclient.MySQLBuilder;
import io.vertx.mysqlclient.MySQLConnectOptions;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.PoolOptions;

import io.vertx.sqlclient.SqlConnectOptions;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

public class DBPoolFactory {

    private static final ObjectMapper MAPPER = new ObjectMapper(new YAMLFactory())
            .registerModule(new GuavaModule())
            .registerModule(new JavaTimeModule());

    public static MySqlGaleraConfig loadConfig(File configFile) throws IOException {
        return MAPPER.readValue(configFile, MySqlGaleraConfig.class);
    }

    public static Pool createGaleraPool(@javax.annotation.Nullable io.vertx.core.Vertx vertx, MySqlGaleraConfig config) {
        // Map each host in the config into its own connection option for round-robin balancing
        List<SqlConnectOptions> connectOptionsList = config.hosts().stream()
                .map(host -> (SqlConnectOptions) new MySQLConnectOptions()
                        .setHost(host)
                        .setPort(config.port())
                        .setDatabase(config.database())
                        .setUser(config.user())
                        .setPassword(config.password()))
                .collect(Collectors.toList());

        PoolOptions poolOptions = new PoolOptions()
                .setMaxSize(config.maxPoolSize());

        io.vertx.sqlclient.ClientBuilder<Pool> builder = MySQLBuilder.pool();
        if (vertx != null) {
            builder.using(vertx);
        }

        // Spawn a single connection pool across the Galera cluster
        return builder
                .connectingTo(connectOptionsList)
                .with(poolOptions)
                .build();
    }
}