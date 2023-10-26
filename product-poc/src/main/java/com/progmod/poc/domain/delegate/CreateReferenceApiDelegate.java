package com.progmod.poc.domain.delegate;

import com.progmod.flow.domain.service.delegate.ActionDelegate;
import com.progmod.poc.domain.Product;
import com.progmod.poc.dto.ApiResponse;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
@Log4j2
public class CreateReferenceApiDelegate implements ActionDelegate<Product, DelegateParams, ApiResponse> {


  @Override
  public Mono<ApiResponse> execute(final Product poc, final Map<String, Object> variables,
                                   final DelegateParams delegateParams) {
    log.info("simulating a reference APi call");
    variables.put("reference", "123");
    return Mono.just(ApiResponse.builder().referenceId("123").description("test").build());
  }

}
