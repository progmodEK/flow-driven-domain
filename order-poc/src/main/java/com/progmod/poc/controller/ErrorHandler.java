package com.progmod.poc.controller;

import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import reactor.core.publisher.Mono;

/**
 * Class defined to manage all controller exceptions, error response and status
 */
@Log4j2
@ControllerAdvice
public class ErrorHandler {

  ////////////////
  // 4xx errors //
  ////////////////
  @ExceptionHandler
  Mono<ResponseEntity<String>> handle(final RuntimeException exception) {
    return Mono.just(ResponseEntity.status(HttpStatusCode.valueOf(406)).body(exception.getMessage()));
  }

}
