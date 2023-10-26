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
public enum OrderAction implements FlowAction {
  CHECK_INV(SYSTEM),
  DEBIT(SYSTEM),
  CANCEL(SYSTEM),
  SHIP(USER),
  DELIVER(USER),
  NOTIFY(SYSTEM);

  private final ActionType type;
}
