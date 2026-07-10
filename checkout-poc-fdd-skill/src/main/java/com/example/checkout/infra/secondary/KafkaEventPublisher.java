package com.example.checkout.infra.secondary;

import com.progmod.flow.domain.model.Flowable;
import com.progmod.flow.domain.port.EventsPublisher;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;

/** Sample {@link EventsPublisher} — logs instead of publishing to a real broker. */
@Component
@Log4j2
public class KafkaEventPublisher implements EventsPublisher {
  @Override
  public void publishEvents(final Flowable flow) {
    log.info("PUBLISH EVENTS TO KAFKA IF YOU WANT");
  }
}
