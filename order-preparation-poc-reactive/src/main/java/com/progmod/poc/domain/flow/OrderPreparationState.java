package com.progmod.poc.domain.flow;

import com.progmod.flow.domain.service.parser.definition.FlowState;
import lombok.Getter;

/**
 * Enumeration declaring all available states for the flow.
 */
@Getter
public enum OrderPreparationState implements FlowState {
  TO_PREPARE,
  IN_PREPARATION,
  PENDING_PICKUP,
  DELIVERED,
  RETRY_NOTIFICATION,
  COMPLETED,
  ERROR
}
