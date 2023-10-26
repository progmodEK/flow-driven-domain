package com.progmod.poc.controller;

import com.progmod.flow.domain.model.BaseFlow;
import com.progmod.flow.domain.port.FlowRepository;
import com.progmod.flow.domain.service.FlowEngine;
import com.progmod.poc.domain.ProductAction;
import com.progmod.poc.domain.ProductFlowType;
import com.progmod.poc.domain.delegate.DelegateParams;
import com.progmod.poc.dto.ApiResponse;
import com.progmod.poc.dto.CreateProductProcessRequest;
import com.progmod.poc.dto.Product;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import reactor.core.publisher.Mono;

/**
 * Abstract Checkout rest controller with endpoints.
 * <p>POST /checkout to create a new checkout</p>
 */
@Controller
@Log4j2
@RequestMapping("/pocs")
@RequiredArgsConstructor
public class PocController {

  protected final FlowRepository<BaseFlow, String> pocRepository;
  protected final FlowEngine<BaseFlow, String> flowEngine;

  @PostMapping
  public Mono<ResponseEntity<BaseFlow>> createPoc(
      @RequestBody final CreateProductProcessRequest createProductProcessRequest) {
    return flowEngine.buildBaseFlow(ProductFlowType.DEFAULT,
            Map.of("product", new Product(createProductProcessRequest.getProductId(), null)))
        .map(ResponseEntity::ok);
  }


  @GetMapping("/{id}")
  public Mono<ResponseEntity<BaseFlow>> getProductProcess(@PathVariable final String id) {
    return pocRepository.findById(id)
        .map(ResponseEntity::ok)
        .defaultIfEmpty(ResponseEntity.notFound().build());
  }


  @PostMapping("/{id}/create_external_reference")
  public Mono<ResponseEntity<ApiResponse>> createReference(@PathVariable final String id) {
    return flowEngine.<DelegateParams, ApiResponse>applyAction(id, ProductAction.CREATE_REFERENCE_API,
            DelegateParams.of())
        .map(ResponseEntity::ok)
        .defaultIfEmpty(ResponseEntity.notFound().build());
  }

  @PostMapping("/{id}/set_reference")
  public Mono<ResponseEntity<BaseFlow>> setReference(@PathVariable final String id,
                                                     @RequestBody final Map<String, String> body) {
    return flowEngine.<DelegateParams, BaseFlow>applyAction(id, ProductAction.SET_REFERENCE,
            DelegateParams.of(body.get("referenceId")))
        .map(ResponseEntity::ok)
        .defaultIfEmpty(ResponseEntity.notFound().build());
  }

}
