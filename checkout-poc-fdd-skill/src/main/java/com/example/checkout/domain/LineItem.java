package com.example.checkout.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LineItem {
  private String skuId;
  private String name;
  private int qty;
  private long unitPriceCents;

  public long lineTotalCents() {
    return (long) qty * unitPriceCents;
  }
}
