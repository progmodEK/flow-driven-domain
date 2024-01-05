package com.progmod.poc.domain.delegate;

import com.progmod.flow.domain.service.DelegateException;
import com.progmod.flow.domain.service.delegate.SystemActionDelegate;
import com.progmod.poc.domain.OrderPreparation;
import java.util.Map;
import java.util.Random;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
@Log4j2
public class notifyDelegate extends SystemActionDelegate<OrderPreparation> {


  @Override
  public Mono<OrderPreparation> execute(final OrderPreparation orderPreparation, final Map<String, Object> variables) {
    log.info("notification invoked");
    // simulate a 50% error to test the flow
    if ((new Random()).nextBoolean()) {
      return Mono.error(new DelegateException(1, "notification error"));
    } else {
      return Mono.just(orderPreparation);
    }
  }

}
