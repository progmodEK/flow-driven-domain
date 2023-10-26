package com.progmod.poc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication(
    scanBasePackages = {"com.progmod.poc"}
//    exclude = {SystemMetricsAutoConfiguration.class, JvmMetricsAutoConfiguration.class}
)
@EnableScheduling
@EnableTransactionManagement
public class PocApplication {

  public static void main(final String[] args) {
    SpringApplication.run(PocApplication.class, args);
  }
}
