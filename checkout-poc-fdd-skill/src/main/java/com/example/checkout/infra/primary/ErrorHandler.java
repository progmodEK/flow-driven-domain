package com.example.checkout.infra.primary;

import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@Log4j2
@ControllerAdvice
public class ErrorHandler {
  @ExceptionHandler
  ResponseEntity<String> handle(final RuntimeException ex) {
    log.warn("request failed: {}", ex.getMessage());
    return ResponseEntity.status(HttpStatusCode.valueOf(406)).body(ex.getMessage());
  }
}
