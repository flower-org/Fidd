package com.fidd.view.rest.mapper;

import com.fidd.core.fiddfile.FiddFileMetadata;
import com.fidd.core.fiddfile.ImmutableFiddFileMetadata;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class FiddFileMetadataMapperTest {

    private com.fidd.view.rest.model.FiddFileMetadata mapSingle(FiddFileMetadata src) {
        return FiddFileMetadataMapper.toDto(src);
    }

    @Test
    void mapsAllFieldsCorrectly() {
        byte[] pubKey = new byte[] {1, 2, 3};
        List<String> fileSigs = List.of("sig1", "sig2");
        List<String> keySigs = List.of("ksig1");

        FiddFileMetadata src = ImmutableFiddFileMetadata.builder()
                .messageNumber(123L)
                .originalMessageNumber(100L)
                .previousMessageNumber(120L)
                .postId("post-abc")
                .versionNumber(2)
                .isNewOrSquash(true)
                .isDelete(false)
                .originalMessageCreationTime(111111111L)
                .messageCreationTime(222222222L)
                .authorsPublicKeyFormat("RSA")
                .authorsPublicKey(pubKey)
                .authorsFiddFileSignatureFormats(fileSigs)
                .authorsFiddKeyFileSignatureFormats(keySigs)
                .build();

        com.fidd.view.rest.model.FiddFileMetadata dto = mapSingle(src);

        assertEquals(123L, dto.getMessageNumber());
        assertEquals(100L, dto.getOriginalMessageNumber());
        assertEquals(120L, dto.getPreviousMessageNumber());
        assertEquals("post-abc", dto.getPostId());
        assertEquals(2, dto.getVersionNumber());
        assertEquals(Boolean.TRUE, dto.getIsNewOrSquash());
        assertEquals(Boolean.FALSE, dto.getIsDelete());
        assertEquals(111111111L, dto.getOriginalMessageCreationTime());
        assertEquals(222222222L, dto.getMessageCreationTime());
        assertEquals("RSA", dto.getAuthorsPublicKeyFormat());
        assertTrue(Arrays.equals(pubKey, dto.getAuthorsPublicKey()));
        assertEquals(fileSigs, dto.getAuthorsFiddFileSignatureFormats());
        assertEquals(keySigs, dto.getAuthorsFiddKeyFileSignatureFormats());
    }

    @Test
    void handlesNullables() {
        FiddFileMetadata src = ImmutableFiddFileMetadata.builder()
                .messageNumber(1L)
                .originalMessageNumber(1L)
                .postId("p")
                .versionNumber(1)
                .isNewOrSquash(false)
                .isDelete(true)
                .build();

        com.fidd.view.rest.model.FiddFileMetadata dto = mapSingle(src);

        assertNull(dto.getPreviousMessageNumber());
        assertNull(dto.getOriginalMessageCreationTime());
        assertNull(dto.getMessageCreationTime());
        assertNull(dto.getAuthorsPublicKeyFormat());
        assertNull(dto.getAuthorsPublicKey());
        assertNull(dto.getAuthorsFiddFileSignatureFormats());
        assertNull(dto.getAuthorsFiddKeyFileSignatureFormats());
    }

    @Test
    void mapsList() {
        FiddFileMetadata a = ImmutableFiddFileMetadata.builder()
                .messageNumber(10L)
                .originalMessageNumber(9L)
                .postId("a")
                .versionNumber(1)
                .isNewOrSquash(false)
                .isDelete(false)
                .build();
        FiddFileMetadata b = ImmutableFiddFileMetadata.builder()
                .messageNumber(20L)
                .originalMessageNumber(19L)
                .postId("b")
                .versionNumber(2)
                .isNewOrSquash(true)
                .isDelete(true)
                .build();

        List<com.fidd.view.rest.model.FiddFileMetadata> list = FiddFileMetadataMapper.toDtoList(List.of(a, b));
        assertEquals(2, list.size());
        assertEquals(10L, list.get(0).getMessageNumber());
        assertEquals(20L, list.get(1).getMessageNumber());
    }
}
