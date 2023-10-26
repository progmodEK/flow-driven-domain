package com.progmod.poc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.autoconfigure.metrics.JvmMetricsAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.SystemMetricsAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(
    scanBasePackages = {"com.progmod.poc"},
    exclude = {SystemMetricsAutoConfiguration.class, JvmMetricsAutoConfiguration.class}
)
@EnableScheduling
public class PocApplication {
  public static void main(final String[] args) {
    SpringApplication.run(PocApplication.class, args);
  }
}
