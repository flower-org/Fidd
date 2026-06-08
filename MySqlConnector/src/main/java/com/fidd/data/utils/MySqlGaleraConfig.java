package com.fidd.data.utils;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import java.util.List;

@Value.Immutable
@JsonSerialize(as = ImmutableMySqlGaleraConfig.class)
@JsonDeserialize(as = ImmutableMySqlGaleraConfig.class)
public interface MySqlGaleraConfig {
    List<String> hosts();
    int port();
    String database();
    String user();
    String password();

    @Value.Default
    default int maxPoolSize() {
        return 10;
    }
}