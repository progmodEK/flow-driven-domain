package com.progmod.poc.domain.delegate;

import com.progmod.flow.domain.service.delegate.SystemActionDelegate;
import com.progmod.poc.domain.Order;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
@Log4j2
public class debitDelegate extends SystemActionDelegate<Order> {


  @Override
  public Mono<Order> execute(final Order order, final Map<String, Object> variables) {
    log.info("simulating a debit operation");
//    variables.put("reference", "123");
    return Mono.just(order);
  }

}
