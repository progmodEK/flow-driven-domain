package com.progmod.poc.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Item {
  String skuId;
  String name;
  int qty;
  int qtyPrepared;
  boolean pickedUp;


  /**
   * Update Prepared Qty and invariants.
   */
  public Item updatePreparedQty(final int qtyPrepared) {
    if (qtyPrepared > this.qty) {
      throw new IllegalArgumentException("prepared qty is greater than order qty");
    }
    this.qtyPrepared = qtyPrepared;
    if (qty == qtyPrepared) {
      pickedUp = true;
    }
    return this;
  }
}
