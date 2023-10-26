package com.progmod.poc.infra;

import com.progmod.flow.domain.model.Flowable;
import com.progmod.flow.domain.port.EventsPublisher;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@Log4j2
public class KafkaEventPublisher implements EventsPublisher {
  @Override
  public Mono<Void> publishEvents(final Flowable flowable) {
    log.info("PUBLISH EVENTS TO KAFKA IF I WANT");
    return Mono.empty();
  }
}
