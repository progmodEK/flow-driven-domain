package com.progmod.poc.product.domain;

import lombok.Builder;
import lombok.Data;
import lombok.extern.log4j.Log4j2;
import reactor.core.publisher.Mono;

@Data
@Builder
@Log4j2
public class Product {

  String id;
  String reference;

  public Product(final String id, final String reference) {
    this.id = id;
    this.reference = reference;
  }

  public Mono<Product> setReference(final String reference) {
    log.info("Product domain invoked");
    this.reference = reference;
    return Mono.just(this);
  }
}
