package com.progmod.poc.domain.flow;

import static com.progmod.flow.domain.model.ActionType.SYSTEM;
import static com.progmod.flow.domain.model.ActionType.USER;

import com.progmod.flow.domain.model.ActionType;
import com.progmod.flow.domain.service.parser.definition.FlowAction;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Enumeration declaring all available actions name inside a flow.
 */
@Getter
@RequiredArgsConstructor
public enum ProductAction implements FlowAction {
  CREATE_REFERENCE_API(USER),
  SET_REFERENCE(USER),
  SEND_POC(SYSTEM),
  TIMEOUT(SYSTEM);

  private final ActionType type;


}
