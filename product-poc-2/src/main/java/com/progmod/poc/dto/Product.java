package com.progmod.poc.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class Product {

  String id;
  String reference;

  public Product(final String id, final String reference) {
    this.id = id;
    this.reference = reference;
  }
}
