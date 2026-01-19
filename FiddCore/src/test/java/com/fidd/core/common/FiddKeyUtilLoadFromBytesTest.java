package com.fidd.core.common;

import com.fidd.base.BaseRepositories;
import com.fidd.base.Repository;
import com.fidd.core.fiddkey.FiddKey;
import com.fidd.core.fiddkey.FiddKeySerializer;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class FiddKeyUtilLoadFromBytesTest {

    @Test
    void testSuccessfulDeserializationReturnsFiddKey() {
        BaseRepositories repos = mock(BaseRepositories.class);
        Repository<FiddKeySerializer> repo = mock(Repository.class);

        when(repos.fiddKeyFormatRepo()).thenReturn(repo);
        when(repo.listEntryNames()).thenReturn(List.of("fmt1"));

        FiddKeySerializer serializer = mock(FiddKeySerializer.class);
        FiddKey expected = mock(FiddKey.class);

        when(repo.get("fmt1")).thenReturn(serializer);
        when(serializer.deserialize(any())).thenReturn(expected);

        FiddKey result = FiddKeyUtil.loadFiddKeyFromBytes(repos, "abc".getBytes());

        assertEquals(expected, result);
    }

    @Test
    void testFailedDeserializationReturnsNull() {
        BaseRepositories repos = mock(BaseRepositories.class);
        Repository<FiddKeySerializer> repo = mock(Repository.class);

        when(repos.fiddKeyFormatRepo()).thenReturn(repo);
        when(repo.listEntryNames()).thenReturn(List.of("fmt1", "fmt2"));

        FiddKeySerializer serializer1 = mock(FiddKeySerializer.class);
        FiddKeySerializer serializer2 = mock(FiddKeySerializer.class);

        when(repo.get("fmt1")).thenReturn(serializer1);
        when(repo.get("fmt2")).thenReturn(serializer2);

        when(serializer1.deserialize(any())).thenThrow(new RuntimeException("bad"));
        when(serializer2.deserialize(any())).thenThrow(new RuntimeException("bad"));

        FiddKey result = FiddKeyUtil.loadFiddKeyFromBytes(repos, "abc".getBytes());

        assertNull(result);
    }
}
