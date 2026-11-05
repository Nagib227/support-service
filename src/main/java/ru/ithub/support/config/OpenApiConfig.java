package ru.ithub.support.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

  @Bean
  public OpenAPI supportOpenApi() {
    return new OpenAPI()
        .info(
            new Info()
                .title("Сервис заявок технической поддержки")
                .version("0.1.0")
                .description(
                    "Регистрация заявок, смена статуса по правилам, история и сводный отчёт за период."));
  }
}
