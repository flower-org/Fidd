package com.fidd.core.connection;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import java.util.List;

@Value.Immutable
@JsonSerialize(as = ImmutableFiddConnectionList.class)
@JsonDeserialize(as = ImmutableFiddConnectionList.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
public interface FiddConnectionList {

    List<FiddConnection> fiddConnectionList();

    static FiddConnectionList of(FiddConnection... fiddConnections) {
        return ImmutableFiddConnectionList.builder()
                .addFiddConnectionList(fiddConnections)
                .build();
    }
}
