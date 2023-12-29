package com.progmod.poc.domain.delegate;

import com.progmod.flow.domain.service.delegate.SystemActionDelegate;
import com.progmod.poc.domain.Order;
import java.util.Map;
import java.util.Random;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
@Log4j2
public class checkInventoryDelegate extends SystemActionDelegate<Order> {

  @Override
  public Mono<Order> execute(final Order order, final Map<String, Object> variables) {
    log.info("simulating an inventory check");
    if ((new Random()).nextBoolean())
      variables.put("transition", "inventoryOK");
    else variables.put("transition", "inventoryKO");
//    variables.put("transition", "inventoryKO");
    return Mono.just(order);
  }

}
