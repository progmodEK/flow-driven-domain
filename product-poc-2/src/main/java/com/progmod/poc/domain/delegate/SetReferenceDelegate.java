package com.progmod.poc.domain.delegate;

import com.progmod.flow.domain.model.BaseFlow;
import com.progmod.flow.domain.service.delegate.ActionDelegate;
import com.progmod.poc.dto.Product;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
@Log4j2
public class SetReferenceDelegate implements ActionDelegate<BaseFlow, DelegateParams, BaseFlow> {


  @Override
  public Mono<BaseFlow> execute(final BaseFlow baseFlow, final Map<String, Object> variables,
                                final DelegateParams delegateParams) {
    log.info("SetReferenceDelegate : set reference on Global Product Variable");
//    productProcess.getProduct().setReference(delegateParams.referenceId);
    final Product product = baseFlow.getFlow().getGlobalVariable("product", Product.class).get();
    product.setReference(
        baseFlow.getFlow().getActionVariable("reference", String.class).orElseGet(() -> delegateParams.referenceId));
    baseFlow.getFlow().getVariables().put("product", product);
    return Mono.just(baseFlow);
  }

}
