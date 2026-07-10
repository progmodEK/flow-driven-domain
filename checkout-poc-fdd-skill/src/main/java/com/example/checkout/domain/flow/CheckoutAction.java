package com.example.checkout.domain.flow;

import static com.progmod.flow.domain.model.ActionType.SYSTEM;
import static com.progmod.flow.domain.model.ActionType.USER;

import com.progmod.flow.domain.model.ActionType;
import com.progmod.flow.domain.service.parser.definition.FlowAction;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CheckoutAction implements FlowAction {
  START_CHECKOUT(USER),
  SET_SHIPPING(USER),
  SUBMIT_PAYMENT(USER),
  CONFIRM_PAYMENT(SYSTEM),
  CANCEL(USER),
  EXPIRE(SYSTEM);

  private final ActionType type;
}
