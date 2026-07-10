package com.example.checkout.domain;

import com.example.checkout.dto.PaymentRequest;
import com.example.checkout.dto.ShippingRequest;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.progmod.flow.domain.model.Flow;
import com.progmod.flow.domain.model.Flowable;
import java.beans.Transient;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonPropertyOrder({"id", "cartRef", "items", "shippingAddress", "paymentRef", "state", "flow"})
public class Checkout implements Flowable<UUID> {
  @Builder.Default
  private UUID id = UUID.randomUUID();
  private String cartRef;
  private List<LineItem> items;
  private String shippingAddress;
  private String paymentRef;

  // ---- flow fields required by Flowable; persisted WITH the aggregate ----
  private String state;
  private Flow flow;

  /** Cart must hold at least one line, each with a positive quantity. */
  public void validateCart() {
    if (items == null || items.isEmpty()) {
      throw new IllegalArgumentException("checkout has no items");
    }
    final boolean allPositive = items.stream().allMatch(i -> i.getQty() > 0);
    if (!allPositive) {
      throw new IllegalArgumentException("every line item must have a positive quantity");
    }
  }

  public void applyShipping(final ShippingRequest request) {
    if (request.address() == null || request.address().isBlank()) {
      throw new IllegalArgumentException("shipping address is required");
    }
    this.shippingAddress = request.address();
  }

  /** The submitted payment amount must match the computed order total to the cent. */
  public void applyPayment(final PaymentRequest request) {
    if (request.amountCents() != totalCents()) {
      throw new IllegalArgumentException(
          "payment amount " + request.amountCents() + " does not match order total " + totalCents());
    }
    this.paymentRef = request.paymentRef();
  }

  @Transient
  public long totalCents() {
    return items == null ? 0L : items.stream().mapToLong(LineItem::lineTotalCents).sum();
  }
}
