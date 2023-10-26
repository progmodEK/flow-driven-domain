package com.progmod.poc.domain.delegate;

import com.progmod.flow.domain.service.delegate.SystemActionDelegate;
import com.progmod.poc.domain.Product;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
@Log4j2
public class TimeoutDelegate extends SystemActionDelegate<Product> {


  @Override
  public Mono<Product> execute(final Product productProcess, final Map<String, Object> variables) {
    log.info("timeout invoked");
    return Mono.just(productProcess);
  }

}
