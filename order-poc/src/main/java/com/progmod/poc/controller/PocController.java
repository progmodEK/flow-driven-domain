package com.progmod.poc.controller;

import com.progmod.flow.domain.port.FlowRepository;
import com.progmod.flow.domain.service.FlowEngine;
import com.progmod.flow.domain.service.parser.definition.FlowDefinition;
import com.progmod.poc.domain.Order;
import com.progmod.poc.domain.delegate.DelegateParams;
import com.progmod.poc.domain.flow.OrderAction;
import com.progmod.poc.domain.flow.OrderFlowType;
import com.progmod.poc.dto.CreateOrderRequest;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.ApplicationContext;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Abstract Checkout rest controller with endpoints.
 * <p>POST /checkout to create a new checkout</p>
 */
@RestController
@Log4j2
@RequestMapping("/pocs")
@RequiredArgsConstructor
public class PocController {

  protected final FlowRepository<Order, UUID> pocRepository;
  protected final FlowEngine<Order, UUID> flowEngine;
  private ApplicationContext applicaioncontext;

  @PostMapping
  public Mono<ResponseEntity<Order>> createPoc(@RequestBody final CreateOrderRequest createProductProcessRequest) {
    return flowEngine.makeFlowable(Order.builder().build(), OrderFlowType.DEFAULT, Map.of()).map(ResponseEntity::ok);
  }


  @GetMapping("/")
  public Flux<Order> getAllOrders() {
    return pocRepository.findAll();
  }

  @GetMapping("/{id}")
  public Mono<ResponseEntity<Order>> getOrder(@PathVariable final String id) {
    return pocRepository.findById(UUID.fromString(id)).map(ResponseEntity::ok)
        .defaultIfEmpty(ResponseEntity.notFound().build());
  }


  @GetMapping("/{id}/flow-definition")
  public Mono<ResponseEntity<FlowDefinition>> getFlow(@PathVariable final String id) {
    return flowEngine.getFlowDefinition(UUID.fromString(id)).map(ResponseEntity::ok);
  }

  @GetMapping("/flow-definition")
  public Mono<ResponseEntity<FlowDefinition>> getFlow() {
    return flowEngine.getFlowDefinitionByType(OrderFlowType.DEFAULT.name()).map(ResponseEntity::ok);
  }


  @PostMapping("/{id}/ship")
  public Mono<ResponseEntity<Order>> ship(@PathVariable final String id) {
    return flowEngine.<DelegateParams, Order>applyAction(UUID.fromString(id), OrderAction.SHIP, DelegateParams.of())
        .map(ResponseEntity::ok).defaultIfEmpty(ResponseEntity.notFound().build());
  }

  @PostMapping("/{id}/deliver")
  public Mono<ResponseEntity<Order>> deliver(@PathVariable final String id,
                                             @RequestBody final Map<String, String> body) {
    return flowEngine.<DelegateParams, Order>applyAction(UUID.fromString(id), OrderAction.DELIVER,
            DelegateParams.of(body.get("referenceId"))).map(ResponseEntity::ok)
        .defaultIfEmpty(ResponseEntity.notFound().build());
  }

}
