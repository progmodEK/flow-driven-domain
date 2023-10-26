package com.progmod.poc.domain.flow;

import com.progmod.flow.domain.service.parser.definition.FlowAction;
import com.progmod.flow.domain.service.parser.definition.FlowState;
import com.progmod.flow.domain.service.parser.definition.FlowType;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public enum ProductFlowType implements FlowType {
  DEFAULT("flow/default-workflow.json", ProductAction.class, ProductState.class);

  @Getter
  private final String template;
  @Getter
  private final Class<? extends FlowAction> flowActionType;
  @Getter
  private final Class<? extends FlowState> flowStateType;

}
