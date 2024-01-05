package com.progmod.poc.infra.secondary;

import com.progmod.flow.domain.model.Flowable;
import com.progmod.flow.domain.port.EventsPublisher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class KafkaEventPublisher implements EventsPublisher {
  @Override
  public void publishEvents(final Flowable flow) {
    log.info("PUBLISH EVENTS TO KAFKA IF I WANT");
  }
}
