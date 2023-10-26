package com.progmod.poc.domain.delegate;

import com.progmod.flow.domain.service.delegate.ActionDelegate;
import com.progmod.poc.domain.Product;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
@Log4j2
public class SetReferenceDelegate implements ActionDelegate<Product, DelegateParams, Product> {


  @Override
  public Mono<Product> execute(final Product product, final Map<String, Object> variables,
                               final DelegateParams delegateParams) {
    log.info("SetReferenceDelegate : set reference on Product");
    product.setReference(
        product.getFlow().getActionVariable("reference", String.class).orElseGet(() -> delegateParams.referenceId));
    return Mono.just(product);
  }

}
