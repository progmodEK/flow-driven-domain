package com.progmod.poc.domain.flow;

import com.progmod.flow.domain.service.parser.definition.FlowAction;
import com.progmod.flow.domain.service.parser.definition.FlowState;
import com.progmod.flow.domain.service.parser.definition.FlowType;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public enum OrderPreparationFlowType implements FlowType {
  DEFAULT("flow/in-store-workflow.json", OrderPreparationAction.class, OrderPreparationState.class);

  @Getter
  private final String template;
  @Getter
  private final Class<? extends FlowAction> flowActionType;
  @Getter
  private final Class<? extends FlowState> flowStateType;

}
