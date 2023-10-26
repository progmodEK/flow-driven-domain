package com.progmod.poc.domain.flow;

import com.progmod.flow.domain.service.parser.definition.FlowState;
import lombok.Getter;

/**
 * Enumeration declaring all available states for the flow.
 */
@Getter
public enum OrderState implements FlowState {
  NEW,
  P_DEBIT,
  P_SHIPMENT,
  P_CANCEL,
  SHIPPED,
  DELIVERED,
  CANCELLED,
  DELIVERY_UNKNOWN
}
