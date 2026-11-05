package ru.ithub.support.testing;

import java.time.Clock;
import java.time.Instant;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration
public class TestClockConfig {

  public static final Instant START = Instant.parse("2026-01-15T12:00:00Z");

  @Bean
  @Primary
  Clock testClock() {
    return new MutableClock(START);
  }
}
