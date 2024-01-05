package com.progmod.poc.domain.delegate;

import static com.progmod.flow.utils.FlowUtils.ACTION_TRANSITION_VARIABLE;

import com.progmod.flow.domain.service.delegate.ActionDelegate;
import com.progmod.poc.domain.OrderPreparation;
import com.progmod.poc.dto.PickItemsRequest;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
@Log4j2
public class PickItemsDelegate implements ActionDelegate<OrderPreparation, PickItemsRequest, OrderPreparation> {

  @Override
  public Mono<OrderPreparation> execute(final OrderPreparation orderPreparation,
                                        final Map<String, Object> actionContext,
                                        final PickItemsRequest pickItemsRequest) {
    orderPreparation.updatePreparation(pickItemsRequest);
    actionContext.put(ACTION_TRANSITION_VARIABLE, orderPreparation.isFullyPrepared() ? "full" : "partial");
    return Mono.just(orderPreparation);
  }

}
