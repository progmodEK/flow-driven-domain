package com.example.checkout.domain.flow;

import com.progmod.flow.domain.service.parser.definition.FlowState;
import lombok.Getter;

@Getter
public enum CheckoutState implements FlowState {
  CART,
  AWAITING_SHIPPING,
  AWAITING_PAYMENT,
  PROCESSING_PAYMENT,
  RETRY_PAYMENT,
  CONFIRMED,
  PAYMENT_FAILED,
  CANCELLED,
  EXPIRED
}
