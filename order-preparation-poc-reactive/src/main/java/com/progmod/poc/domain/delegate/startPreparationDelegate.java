package com.progmod.poc.domain.delegate;

import com.progmod.flow.domain.service.delegate.ActionDelegate;
import com.progmod.poc.domain.OrderPreparation;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
@Log4j2
public class startPreparationDelegate
    implements ActionDelegate<OrderPreparation, Map<String, String>, OrderPreparation> {

  @Override
  public Mono<OrderPreparation> execute(final OrderPreparation orderPreparation,
                                        final Map<String, Object> actionContext,
                                        final Map<String, String> inputParams) {
    log.info("startPreparation invoked");
    return Mono.just(orderPreparation);
  }
}
