package com.fidd.connector.controller;

import com.fidd.connector.service.FiddConnectorRestService;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.file.NoSuchFileException;
import java.util.List;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MessagesController implements MessagesApi {

  private final FiddConnectorRestService fiddConnectorRestService;

  public MessagesController(FiddConnectorRestService fiddConnectorRestService) {
    this.fiddConnectorRestService = fiddConnectorRestService;
  }

  // GET /messages/tail?count=...
  @Override
  public ResponseEntity<List<Long>> getMessageNumbersTail(Integer count) {
    List<Long> messageNumbers = fiddConnectorRestService.getMessageNumbersTail(count);
    return ResponseEntity.ok(messageNumbers);
  }

  // GET /messages/{messageNumber}/before?count=...&inclusive=...
  @Override
  public ResponseEntity<List<Long>> getMessageNumbersBefore(
      Long messageNumber, Integer count, Boolean inclusive) {
    List<Long> messageNumbers =
        fiddConnectorRestService.getMessageNumbersBefore(messageNumber, count, inclusive);
    return ResponseEntity.ok(messageNumbers);
  }

  // GET
  // /messages/range?latestMessage=...&inclusiveLatest=...&earliestMessage=...&inclusiveEarliest=...&count=...&getLatest=...
  @Override
  public ResponseEntity<List<Long>> getMessageNumbersBetween(
      Long latestMessage,
      Boolean inclusiveLatest,
      Long earliestMessage,
      Boolean inclusiveEarliest,
      Integer count,
      Boolean getLatest) {
    List<Long> messageNumbers =
        fiddConnectorRestService.getMessageNumbersBetween(
            latestMessage, inclusiveLatest, earliestMessage, inclusiveEarliest, count, getLatest);
    return ResponseEntity.ok(messageNumbers);
  }

  // GET /messages/{messageNumber}/content/size
  @Override
  public ResponseEntity<Long> getFiddMessageSize(Long messageNumber) {
    try {
      long messageSize = fiddConnectorRestService.getFiddMessageSize(messageNumber);
      return ResponseEntity.ok(messageSize);
    } catch (RuntimeException e) {
      if (e.getCause() instanceof FileNotFoundException
          || e.getCause() instanceof NoSuchFileException) {
        return ResponseEntity.notFound().build();
      }
      throw e;
    }
  }

  // GET /messages/{messageNumber}/content?offset=...&length=...
  @Override
  public ResponseEntity<Resource> getFiddMessageChunk(
      Long messageNumber, Long offset, Long length) {
    try {
      if (offset < 0 || length < 0) {
        return ResponseEntity.badRequest().build();
      }

      long messageSize = fiddConnectorRestService.getFiddMessageSize(messageNumber);
      if (offset > messageSize || (offset == messageSize && length > 0)) {
        return ResponseEntity.status(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE).build();
      }

      long actualLength = Math.min(length, messageSize - offset);
      InputStream chunk =
          fiddConnectorRestService.getFiddMessageChunk(messageNumber, offset, actualLength);
      return ResponseEntity.ok()
          .contentType(MediaType.APPLICATION_OCTET_STREAM)
          .contentLength(actualLength)
          .body(new InputStreamResource(chunk));
    } catch (RuntimeException e) {
      if (e.getCause() instanceof FileNotFoundException
          || e.getCause() instanceof NoSuchFileException) {
        return ResponseEntity.notFound().build();
      }
      throw e;
    }
  }
}
