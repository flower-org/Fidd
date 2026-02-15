package com.fidd.view.rest.mapper;

import com.fidd.core.common.FiddSignature;
import com.fidd.core.common.ProgressiveCrc;
import com.fidd.core.logicalfile.ResourceDescriptorType;
import com.fidd.core.fiddkey.FiddKey;
import com.fidd.core.logicalfile.LogicalFileMetadata;
import com.fidd.service.LogicalFileInfo;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class LogicalFileInfoMapperTest {

    @Test
    void mapsSingleLogicalFileInfo() {
        // Arrange nested signatures and external resources
        FiddSignature authorSignature = FiddSignature.of("ed25519", new byte[]{1, 2, 3});
        ProgressiveCrc crc = ProgressiveCrc.of("crc32", new byte[]{9, 8, 7}, 1024L);
        com.fidd.core.logicalfile.LogicalFileMetadata.ExternalResource.FileRegion fileRegion =
                com.fidd.core.logicalfile.ImmutableFileRegion.builder()
                        .offset(64L)
                        .length(128L)
                        .regionFileName("file.part")
                        .resourceDescriptorType(ResourceDescriptorType.URL)
                        .resourceDescriptor("https://example.com/file.part".getBytes())
                        .build();
        com.fidd.core.logicalfile.LogicalFileMetadata.ExternalResource externalResource =
                com.fidd.core.logicalfile.ImmutableExternalResource.builder()
                        .addFileRegions(fileRegion)
                        .build();

        // Arrange source LogicalFileMetadata
        LogicalFileMetadata metadata = com.fidd.core.logicalfile.ImmutableLogicalFileMetadata.builder()
                .updateType(LogicalFileMetadata.FiddUpdateType.CREATE_OVERRIDE)
                .filePath("/path/to/file.txt")
                .createdAt(123L)
                .updatedAt(456L)
                .authorsFileSignatures(List.of(authorSignature))
                .progressiveCrcs(List.of(crc))
                .externalLinks(List.of(externalResource))
                .build();

        // Arrange source Section
        FiddSignature sectionCrc = FiddSignature.of("sha256", new byte[]{4, 5, 6});
        FiddKey.Section section = com.fidd.core.fiddkey.ImmutableSection.builder()
                .sectionOffset(1000L)
                .sectionLength(250L)
                .headerLength(16)
                .encryptionAlgorithm("AES-256-GCM")
                .encryptionKeyData(new byte[]{10, 11, 12})
                .crcs(List.of(sectionCrc))
                .build();

        // Arrange source LogicalFileInfo
        LogicalFileInfo source = com.fidd.service.LogicalFileInfo.of(metadata, section, 42L);

        // Act
        com.fidd.view.rest.model.LogicalFileInfo dto = LogicalFileInfoMapper.toDto(source);

        // Assert
        assertNotNull(dto);
        assertNotNull(dto.getMetadata());
        assertEquals("/path/to/file.txt", dto.getMetadata().getFilePath());
        assertEquals(Long.valueOf(42L), dto.getFileOffset());
        assertNotNull(dto.getSection());
        assertEquals(Long.valueOf(1000L), dto.getSection().getSectionOffset());
        assertEquals(Long.valueOf(250L), dto.getSection().getSectionLength());
        assertEquals(Integer.valueOf(16), dto.getSection().getHeaderLength());
        assertEquals("AES-256-GCM", dto.getSection().getEncryptionAlgorithm());
        assertArrayEquals(new byte[]{10, 11, 12}, dto.getSection().getEncryptionKeyData());
        assertNotNull(dto.getSection().getCrcs());
        assertEquals(1, dto.getSection().getCrcs().size());
        assertEquals("sha256", dto.getSection().getCrcs().get(0).getFormat());
        assertArrayEquals(new byte[]{4, 5, 6}, dto.getSection().getCrcs().get(0).getBytes());

        assertNotNull(dto.getMetadata().getAuthorsFileSignatures());
        assertEquals(1, dto.getMetadata().getAuthorsFileSignatures().size());
        assertEquals("ed25519", dto.getMetadata().getAuthorsFileSignatures().get(0).getFormat());
        assertArrayEquals(new byte[]{1, 2, 3}, dto.getMetadata().getAuthorsFileSignatures().get(0).getBytes());

        assertNotNull(dto.getMetadata().getProgressiveCrcs());
        assertEquals(1, dto.getMetadata().getProgressiveCrcs().size());
        assertEquals("crc32", dto.getMetadata().getProgressiveCrcs().get(0).getFormat());
        assertArrayEquals(new byte[]{9, 8, 7}, dto.getMetadata().getProgressiveCrcs().get(0).getBytes());
        assertEquals(Long.valueOf(1024L), dto.getMetadata().getProgressiveCrcs().get(0).getProgressiveCrcChunkSize());

        assertNotNull(dto.getMetadata().getExternalLinks());
        assertEquals(1, dto.getMetadata().getExternalLinks().size());
        assertNotNull(dto.getMetadata().getExternalLinks().get(0).getFileRegions());
        assertEquals(1, dto.getMetadata().getExternalLinks().get(0).getFileRegions().size());
        assertEquals(Long.valueOf(64L), dto.getMetadata().getExternalLinks().get(0).getFileRegions().get(0).getOffset());
        assertEquals(Long.valueOf(128L), dto.getMetadata().getExternalLinks().get(0).getFileRegions().get(0).getLength());
        assertEquals("file.part", dto.getMetadata().getExternalLinks().get(0).getFileRegions().get(0).getRegionFileName());
        assertEquals(com.fidd.view.rest.model.FileRegion.ResourceDescriptorTypeEnum.URL,
                dto.getMetadata().getExternalLinks().get(0).getFileRegions().get(0).getResourceDescriptorType());
        assertArrayEquals("https://example.com/file.part".getBytes(),
                dto.getMetadata().getExternalLinks().get(0).getFileRegions().get(0).getResourceDescriptor());
    }

    @Test
    void mapsListOfLogicalFileInfo() {
        LogicalFileMetadata metadata1 = com.fidd.core.logicalfile.ImmutableLogicalFileMetadata.builder()
                .updateType(LogicalFileMetadata.FiddUpdateType.CREATE_OVERRIDE)
                .filePath("/a")
                .build();
        FiddKey.Section section1 = com.fidd.core.fiddkey.ImmutableSection.builder()
                .sectionOffset(1L)
                .sectionLength(2L)
                .build();
        LogicalFileInfo s1 = com.fidd.service.LogicalFileInfo.of(metadata1, section1, 10L);

        LogicalFileMetadata metadata2 = com.fidd.core.logicalfile.ImmutableLogicalFileMetadata.builder()
                .updateType(LogicalFileMetadata.FiddUpdateType.DELETE)
                .filePath("/b")
                .build();
        FiddKey.Section section2 = com.fidd.core.fiddkey.ImmutableSection.builder()
                .sectionOffset(3L)
                .sectionLength(4L)
                .build();
        LogicalFileInfo s2 = com.fidd.service.LogicalFileInfo.of(metadata2, section2, 20L);

        List<com.fidd.view.rest.model.LogicalFileInfo> list = LogicalFileInfoMapper.toDtoList(List.of(s1, s2));

        assertEquals(2, list.size());
        assertEquals("/a", list.get(0).getMetadata().getFilePath());
        assertEquals(Long.valueOf(10L), list.get(0).getFileOffset());
        assertEquals("/b", list.get(1).getMetadata().getFilePath());
        assertEquals(Long.valueOf(20L), list.get(1).getFileOffset());
    }

    @Test
    void handlesNullOptionalFields() {
        LogicalFileMetadata metadata = com.fidd.core.logicalfile.ImmutableLogicalFileMetadata.builder()
                .updateType(LogicalFileMetadata.FiddUpdateType.CREATE_OVERRIDE)
                .filePath("/nulls")
                .build();
        FiddKey.Section section = com.fidd.core.fiddkey.ImmutableSection.builder()
                .sectionOffset(0L)
                .sectionLength(0L)
                .build();
        LogicalFileInfo source = com.fidd.service.LogicalFileInfo.of(metadata, section, 0L);

        com.fidd.view.rest.model.LogicalFileInfo dto = LogicalFileInfoMapper.toDto(source);
        assertNull(dto.getMetadata().getAuthorsFileSignatures());
        assertNull(dto.getMetadata().getProgressiveCrcs());
        assertNull(dto.getMetadata().getExternalLinks());
        assertNull(dto.getSection().getEncryptionAlgorithm());
        assertNull(dto.getSection().getEncryptionKeyData());
        assertNull(dto.getSection().getCrcs());
    }

    @Test
    void mapsMultipleNestedEntries() {
        FiddSignature authorSignature1 = FiddSignature.of("ed25519", new byte[]{1, 2, 3});
        FiddSignature authorSignature2 = FiddSignature.of("rsa", new byte[]{7, 8, 9});
        ProgressiveCrc crc1 = ProgressiveCrc.of("crc32", new byte[]{9, 8, 7}, 1024L);
        ProgressiveCrc crc2 = ProgressiveCrc.of("crc64", new byte[]{6, 5, 4}, 2048L);

        com.fidd.core.logicalfile.LogicalFileMetadata.ExternalResource.FileRegion region1 =
                buildRegion(0L, 64L, "file.a", ResourceDescriptorType.URL, "https://example.com/file.a".getBytes());
        com.fidd.core.logicalfile.LogicalFileMetadata.ExternalResource.FileRegion region2 =
                buildRegion(64L, 128L, "file.b", ResourceDescriptorType.URL, "https://example.com/file.b".getBytes());
        com.fidd.core.logicalfile.LogicalFileMetadata.ExternalResource resource1 =
                com.fidd.core.logicalfile.ImmutableExternalResource.builder()
                        .addFileRegions(region1)
                        .addFileRegions(region2)
                        .build();

        com.fidd.core.logicalfile.LogicalFileMetadata.ExternalResource.FileRegion region3 =
                buildRegion(128L, 256L, "file.c", ResourceDescriptorType.URL, "https://example.com/file.c".getBytes());
        com.fidd.core.logicalfile.LogicalFileMetadata.ExternalResource resource2 =
                com.fidd.core.logicalfile.ImmutableExternalResource.builder()
                        .addFileRegions(region3)
                        .build();

        LogicalFileMetadata metadata = com.fidd.core.logicalfile.ImmutableLogicalFileMetadata.builder()
                .updateType(LogicalFileMetadata.FiddUpdateType.CREATE_OVERRIDE)
                .filePath("/path/to/multi.bin")
                .createdAt(1000L)
                .updatedAt(2000L)
                .authorsFileSignatures(List.of(authorSignature1, authorSignature2))
                .progressiveCrcs(List.of(crc1, crc2))
                .externalLinks(List.of(resource1, resource2))
                .build();

        FiddKey.Section section = com.fidd.core.fiddkey.ImmutableSection.builder()
                .sectionOffset(2000L)
                .sectionLength(400L)
                .headerLength(32)
                .encryptionAlgorithm("AES-256-GCM")
                .encryptionKeyData(new byte[]{10, 11, 12})
                .crcs(List.of(FiddSignature.of("sha256", new byte[]{4, 5, 6})))
                .build();

        LogicalFileInfo source = com.fidd.service.LogicalFileInfo.of(metadata, section, 99L);

        com.fidd.view.rest.model.LogicalFileInfo dto = LogicalFileInfoMapper.toDto(source);

        assertNotNull(dto.getMetadata());
        assertEquals(com.fidd.view.rest.model.LogicalFileMetadata.UpdateTypeEnum.CREATE_OVERRIDE,
                dto.getMetadata().getUpdateType());
        assertEquals("/path/to/multi.bin", dto.getMetadata().getFilePath());
        assertEquals(2, dto.getMetadata().getAuthorsFileSignatures().size());
        assertEquals(2, dto.getMetadata().getProgressiveCrcs().size());
        assertEquals(2, dto.getMetadata().getExternalLinks().size());

        assertEquals(2, dto.getMetadata().getExternalLinks().get(0).getFileRegions().size());
        assertEquals("file.a", dto.getMetadata().getExternalLinks().get(0).getFileRegions().get(0).getRegionFileName());
        assertEquals("file.b", dto.getMetadata().getExternalLinks().get(0).getFileRegions().get(1).getRegionFileName());
        assertEquals(com.fidd.view.rest.model.FileRegion.ResourceDescriptorTypeEnum.URL,
                dto.getMetadata().getExternalLinks().get(0).getFileRegions().get(0).getResourceDescriptorType());

        assertEquals(1, dto.getMetadata().getExternalLinks().get(1).getFileRegions().size());
        assertEquals("file.c", dto.getMetadata().getExternalLinks().get(1).getFileRegions().get(0).getRegionFileName());

        assertNotNull(dto.getSection());
        assertEquals(Long.valueOf(2000L), dto.getSection().getSectionOffset());
        assertEquals(Long.valueOf(400L), dto.getSection().getSectionLength());
        assertEquals(Integer.valueOf(32), dto.getSection().getHeaderLength());
    }

    private static com.fidd.core.logicalfile.LogicalFileMetadata.ExternalResource.FileRegion buildRegion(
            long offset,
            long length,
            String regionFileName,
            ResourceDescriptorType type,
            byte[] descriptor) {
        return com.fidd.core.logicalfile.ImmutableFileRegion.builder()
                .offset(offset)
                .length(length)
                .regionFileName(regionFileName)
                .resourceDescriptorType(type)
                .resourceDescriptor(descriptor)
                .build();
    }
}
