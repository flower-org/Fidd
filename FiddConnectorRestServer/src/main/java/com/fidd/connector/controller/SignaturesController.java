package com.fidd.connector.controller;

import com.fidd.connector.service.FiddConnectorRestService;
import java.io.FileNotFoundException;
import java.nio.file.NoSuchFileException;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SignaturesController implements SignaturesApi {

  private final FiddConnectorRestService fiddConnectorRestService;

  public SignaturesController(FiddConnectorRestService fiddConnectorRestService) {
    this.fiddConnectorRestService = fiddConnectorRestService;
  }

  // GET /messages/{messageNumber}/signatures/key/{index}
  @Override
  public ResponseEntity<Resource> getFiddKeySignature(Long messageNumber, Integer index) {
    return notFoundOnMissingFile(
        () -> binaryResponse(fiddConnectorRestService.getFiddKeySignature(messageNumber, index)));
  }

  // GET /messages/{messageNumber}/signatures/key/count
  @Override
  public ResponseEntity<Integer> getFiddKeySignatureCount(Long messageNumber) {
    return notFoundOnMissingFile(
        () -> ResponseEntity.ok(fiddConnectorRestService.getFiddKeySignatureCount(messageNumber)));
  }

  // GET /messages/{messageNumber}/signatures/message/{index}
  @Override
  public ResponseEntity<Resource> getFiddMessageSignature(Long messageNumber, Integer index) {
    return notFoundOnMissingFile(
        () ->
            binaryResponse(fiddConnectorRestService.getFiddMessageSignature(messageNumber, index)));
  }

  // GET /messages/{messageNumber}/signatures/message/count
  @Override
  public ResponseEntity<Integer> getFiddMessageSignatureCount(Long messageNumber) {
    return notFoundOnMissingFile(
        () ->
            ResponseEntity.ok(
                fiddConnectorRestService.getFiddMessageSignatureCount(messageNumber)));
  }

  private ResponseEntity<Resource> binaryResponse(byte[] content) {
    return ResponseEntity.ok()
        .contentType(MediaType.APPLICATION_OCTET_STREAM)
        .contentLength(content.length)
        .body(new ByteArrayResource(content));
  }

  private <T> ResponseEntity<T> notFoundOnMissingFile(ResponseSupplier<T> responseSupplier) {
    try {
      return responseSupplier.get();
    } catch (RuntimeException e) {
      if (e.getCause() instanceof FileNotFoundException
          || e.getCause() instanceof NoSuchFileException) {
        return ResponseEntity.notFound().build();
      }
      throw e;
    }
  }

  @FunctionalInterface
  private interface ResponseSupplier<T> {
    ResponseEntity<T> get();
  }
}
