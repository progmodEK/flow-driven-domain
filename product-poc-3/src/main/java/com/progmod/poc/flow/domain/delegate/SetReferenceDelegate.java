package com.progmod.poc.flow.domain.delegate;

import com.progmod.flow.domain.model.BaseFlow;
import com.progmod.flow.domain.model.Flowable;
import com.progmod.flow.domain.service.delegate.ActionDelegate;
import com.progmod.poc.product.repository.ProductRepository;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
@Log4j2
public class SetReferenceDelegate implements ActionDelegate<BaseFlow, DelegateParams, Flowable> {

  private final ProductRepository productRepository;


  @Override
  public Mono<Flowable> execute(final BaseFlow productProcess, final Map<String, Object> variables,
                                final DelegateParams delegateParams) {
    log.info("SetReferenceDelegate : set reference by invoking Product Domain");
    return productRepository.getProductById(productProcess.getGlobalVariable("productId", String.class).get())
        .doOnNext(product1 -> product1.setReference(delegateParams.referenceId))
        .flatMap(productRepository::save)
        .thenReturn(productProcess);
  }

}
