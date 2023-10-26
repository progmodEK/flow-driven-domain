package com.progmod.poc.flow.domain;

import com.progmod.flow.domain.service.parser.definition.FlowState;
import lombok.Getter;

/**
 * Enumeration declaring all available states for the flow.
 */
@Getter
public enum ProductState implements FlowState {
  PENDING_CREATE_REFERENCE_API,
  PENDING_SET_REFERENCE,
  PENDING_SEND_POC,
  EXPIRED,
  COMPLETED
}
