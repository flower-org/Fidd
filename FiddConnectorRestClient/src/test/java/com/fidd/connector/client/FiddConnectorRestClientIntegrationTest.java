package com.fidd.connector.client;

import static com.fidd.connectors.folder.FolderFiddConstants.ENCRYPTED_FIDD_KEY_FILE_EXT;
import static com.fidd.connectors.folder.FolderFiddConstants.ENCRYPTED_FIDD_KEY_SUBFOLDER;
import static com.fidd.connectors.folder.FolderFiddConstants.FIDD_KEY_FILE_NAME;
import static com.fidd.connectors.folder.FolderFiddConstants.FIDD_MESSAGE_FILE_NAME;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fidd.connector.FiddConnectorRestServerApplication;
import com.fidd.connector.client.invoker.ApiException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(
    classes = FiddConnectorRestServerApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class FiddConnectorRestClientIntegrationTest {

  private static final String FIDD_FOLDER_PROPERTY = "fidd.folder-path";
  private static final String TEMP_DIRECTORY_PREFIX = "fidd-rest-client-test-";
  private static final String LOCALHOST_BASE_URL_FORMAT = "http://localhost:%d";

  private static final long FIRST_MESSAGE_NUMBER = 1L;
  private static final long SECOND_MESSAGE_NUMBER = 2L;
  private static final long THIRD_MESSAGE_NUMBER = 3L;
  private static final long MISSING_MESSAGE_NUMBER = 404L;
  private static final int HTTP_NOT_FOUND = 404;

  private static final String FIRST_MESSAGE_CONTENT = "first-message-content";
  private static final String SECOND_MESSAGE_CONTENT = "second-message-content";
  private static final String THIRD_MESSAGE_CONTENT = "third-message-content";
  private static final String MESSAGE_CHUNK = "message";
  private static final String MESSAGE_REMAINDER_FROM_CHUNK_OFFSET = "message-content";
  private static final long MESSAGE_CHUNK_OFFSET = 7L;
  private static final long MESSAGE_CHUNK_LENGTH = 7L;
  private static final long OVERSIZED_MESSAGE_CHUNK_LENGTH = 1_000L;

  private static final String PLAIN_KEY_PREFIX = "plain-key-";
  private static final String SUBSCRIBER_FOOTPRINT = "subscriber";
  private static final String SUBSCRIBER_ALPHA_KEY_NAME = "subscriberAlpha";
  private static final String SHORT_SUBSCRIBER_KEY_NAME = "sub";
  private static final String MISSING_KEY_NAME = "missing";
  private static final String ENCRYPTED_ALPHA_KEY = "encrypted-alpha-key";
  private static final String ENCRYPTED_SHORT_KEY = "encrypted-short-key";

  private static final String FIDD_KEY_SIGNATURE_FILE_FORMAT = "fidd.key.%d.sign";
  private static final String FIDD_MESSAGE_SIGNATURE_FILE_FORMAT = "fidd.message.%d.sign";
  private static final String KEY_SIGNATURE_PREFIX = "key-signature-";
  private static final String MESSAGE_SIGNATURE_PREFIX = "message-signature-";
  private static final int FIRST_SIGNATURE_INDEX = 0;
  private static final int SECOND_SIGNATURE_INDEX = 1;
  private static final int MISSING_SIGNATURE_INDEX = 10;
  private static final int SIGNATURE_COUNT = 2;

  private static final Path FIDD_FOLDER = createTempDirectory();

  @LocalServerPort private int port;

  private FiddConnectorRestClient client;

  @DynamicPropertySource
  static void registerProperties(DynamicPropertyRegistry registry) {
    registry.add(FIDD_FOLDER_PROPERTY, FIDD_FOLDER::toString);
  }

  @BeforeEach
  void setUp() throws IOException {
    recreateFiddFolder();
    createMessage(FIRST_MESSAGE_NUMBER, FIRST_MESSAGE_CONTENT);
    createMessage(SECOND_MESSAGE_NUMBER, SECOND_MESSAGE_CONTENT);
    createMessage(THIRD_MESSAGE_NUMBER, THIRD_MESSAGE_CONTENT);
    client = new FiddConnectorRestClient(LOCALHOST_BASE_URL_FORMAT.formatted(port));
  }

  @AfterAll
  static void tearDown() throws IOException {
    deleteRecursively(FIDD_FOLDER);
  }

  @Test
  void listsMessageNumbersThroughRestTransport() {
    assertEquals(
        List.of(THIRD_MESSAGE_NUMBER, SECOND_MESSAGE_NUMBER), client.getMessageNumbersTail(2));
    assertEquals(
        List.of(SECOND_MESSAGE_NUMBER, FIRST_MESSAGE_NUMBER),
        client.getMessageNumbersBefore(THIRD_MESSAGE_NUMBER, 2, false));
    assertEquals(
        List.of(THIRD_MESSAGE_NUMBER, SECOND_MESSAGE_NUMBER, FIRST_MESSAGE_NUMBER),
        client.getMessageNumbersBetween(
            THIRD_MESSAGE_NUMBER, true, FIRST_MESSAGE_NUMBER, true, 10, true));
  }

  @Test
  void readsMessageContentThroughRestTransport() throws IOException {
    byte[] message = SECOND_MESSAGE_CONTENT.getBytes(UTF_8);
    assertEquals(message.length, client.getFiddMessageSize(SECOND_MESSAGE_NUMBER));

    try (InputStream chunk =
        client.getFiddMessageChunk(
            SECOND_MESSAGE_NUMBER, MESSAGE_CHUNK_OFFSET, MESSAGE_CHUNK_LENGTH)) {
      assertArrayEquals(MESSAGE_CHUNK.getBytes(UTF_8), chunk.readAllBytes());
    }

    try (InputStream chunk =
        client.getFiddMessageChunk(
            SECOND_MESSAGE_NUMBER, MESSAGE_CHUNK_OFFSET, OVERSIZED_MESSAGE_CHUNK_LENGTH)) {
      assertArrayEquals(MESSAGE_REMAINDER_FROM_CHUNK_OFFSET.getBytes(UTF_8), chunk.readAllBytes());
    }
  }

  @Test
  void readsKeysThroughRestTransport() throws IOException {
    byte[] subscriberFootprint = SUBSCRIBER_FOOTPRINT.getBytes(UTF_8);
    List<byte[]> candidates =
        client.getFiddKeyCandidates(SECOND_MESSAGE_NUMBER, subscriberFootprint);

    assertEquals(2, candidates.size());
    assertArrayEquals(SUBSCRIBER_ALPHA_KEY_NAME.getBytes(UTF_8), candidates.get(0));
    assertArrayEquals(SHORT_SUBSCRIBER_KEY_NAME.getBytes(UTF_8), candidates.get(1));
    assertArrayEquals(
        ENCRYPTED_ALPHA_KEY.getBytes(UTF_8),
        client.getFiddKey(SECOND_MESSAGE_NUMBER, candidates.get(0)));
    assertNull(client.getFiddKey(SECOND_MESSAGE_NUMBER, MISSING_KEY_NAME.getBytes(UTF_8)));
    assertArrayEquals(
        (PLAIN_KEY_PREFIX + SECOND_MESSAGE_NUMBER).getBytes(UTF_8),
        client.getUnencryptedFiddKey(SECOND_MESSAGE_NUMBER));
  }

  @Test
  void readsSignaturesThroughRestTransport() {
    assertEquals(SIGNATURE_COUNT, client.getFiddKeySignatureCount(SECOND_MESSAGE_NUMBER));
    assertArrayEquals(
        (KEY_SIGNATURE_PREFIX + SECOND_SIGNATURE_INDEX).getBytes(UTF_8),
        client.getFiddKeySignature(SECOND_MESSAGE_NUMBER, SECOND_SIGNATURE_INDEX));

    assertEquals(SIGNATURE_COUNT, client.getFiddMessageSignatureCount(SECOND_MESSAGE_NUMBER));
    assertArrayEquals(
        (MESSAGE_SIGNATURE_PREFIX + SECOND_SIGNATURE_INDEX).getBytes(UTF_8),
        client.getFiddMessageSignature(SECOND_MESSAGE_NUMBER, SECOND_SIGNATURE_INDEX));
  }

  @Test
  void returnsNotFoundForMissingSignature() {
    RuntimeException exception =
        assertThrows(
            RuntimeException.class,
            () -> client.getFiddMessageSignature(SECOND_MESSAGE_NUMBER, MISSING_SIGNATURE_INDEX));

    ApiException apiException = (ApiException) exception.getCause();
    assertEquals(HTTP_NOT_FOUND, apiException.getCode());
  }

  @Test
  void returnsNotFoundForMissingMessageContent() {
    RuntimeException exception =
        assertThrows(
            RuntimeException.class, () -> client.getFiddMessageSize(MISSING_MESSAGE_NUMBER));

    ApiException apiException = (ApiException) exception.getCause();
    assertEquals(HTTP_NOT_FOUND, apiException.getCode());
  }

  private static Path createTempDirectory() {
    try {
      return Files.createTempDirectory(TEMP_DIRECTORY_PREFIX);
    } catch (IOException e) {
      throw new ExceptionInInitializerError(e);
    }
  }

  private static void recreateFiddFolder() throws IOException {
    deleteRecursively(FIDD_FOLDER);
    Files.createDirectories(FIDD_FOLDER);
  }

  private static void createMessage(long messageNumber, String messageContent) throws IOException {
    Path messageFolder = FIDD_FOLDER.resolve(Long.toString(messageNumber));
    Files.createDirectories(messageFolder);
    Files.writeString(messageFolder.resolve(FIDD_MESSAGE_FILE_NAME), messageContent, UTF_8);
    Files.writeString(
        messageFolder.resolve(FIDD_KEY_FILE_NAME), PLAIN_KEY_PREFIX + messageNumber, UTF_8);
    writeSignatureFiles(messageFolder);

    Path encryptedKeysFolder = messageFolder.resolve(ENCRYPTED_FIDD_KEY_SUBFOLDER);
    Files.createDirectories(encryptedKeysFolder);
    Files.writeString(
        encryptedKeysFolder.resolve(SUBSCRIBER_ALPHA_KEY_NAME + ENCRYPTED_FIDD_KEY_FILE_EXT),
        ENCRYPTED_ALPHA_KEY,
        UTF_8);
    Files.writeString(
        encryptedKeysFolder.resolve(SHORT_SUBSCRIBER_KEY_NAME + ENCRYPTED_FIDD_KEY_FILE_EXT),
        ENCRYPTED_SHORT_KEY,
        UTF_8);
  }

  private static void writeSignatureFiles(Path messageFolder) throws IOException {
    Files.writeString(
        messageFolder.resolve(FIDD_KEY_SIGNATURE_FILE_FORMAT.formatted(FIRST_SIGNATURE_INDEX)),
        KEY_SIGNATURE_PREFIX + FIRST_SIGNATURE_INDEX,
        UTF_8);
    Files.writeString(
        messageFolder.resolve(FIDD_KEY_SIGNATURE_FILE_FORMAT.formatted(SECOND_SIGNATURE_INDEX)),
        KEY_SIGNATURE_PREFIX + SECOND_SIGNATURE_INDEX,
        UTF_8);
    Files.writeString(
        messageFolder.resolve(FIDD_MESSAGE_SIGNATURE_FILE_FORMAT.formatted(FIRST_SIGNATURE_INDEX)),
        MESSAGE_SIGNATURE_PREFIX + FIRST_SIGNATURE_INDEX,
        UTF_8);
    Files.writeString(
        messageFolder.resolve(FIDD_MESSAGE_SIGNATURE_FILE_FORMAT.formatted(SECOND_SIGNATURE_INDEX)),
        MESSAGE_SIGNATURE_PREFIX + SECOND_SIGNATURE_INDEX,
        UTF_8);
  }

  private static void deleteRecursively(Path root) throws IOException {
    if (!Files.exists(root)) {
      return;
    }
    try (Stream<Path> paths = Files.walk(root)) {
      paths
          .sorted(Comparator.reverseOrder())
          .forEach(
              path -> {
                try {
                  Files.deleteIfExists(path);
                } catch (IOException e) {
                  throw new UncheckedIOException(e);
                }
              });
    } catch (UncheckedIOException e) {
      throw e.getCause();
    }
  }
}
