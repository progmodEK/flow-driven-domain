package com.progmod.poc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PocApplication {
  public static void main(final String[] args) {
    SpringApplication.run(PocApplication.class, args);
  }
}
