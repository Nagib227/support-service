package ru.ithub.support;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.testcontainers.containers.PostgreSQLContainer;
import ru.ithub.support.testing.MutableClock;
import ru.ithub.support.testing.TestClockConfig;
import ru.ithub.support.ticket.dto.ChangeStatusRequest;
import ru.ithub.support.ticket.dto.CreateTicketRequest;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestClockConfig.class)
public abstract class IntegrationTestBase {

  protected static final UUID SOFTWARE_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

  @SuppressWarnings("resource")
  protected static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16");

  static {
    POSTGRES.start();
  }

  @DynamicPropertySource
  static void datasource(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    registry.add("spring.datasource.username", POSTGRES::getUsername);
    registry.add("spring.datasource.password", POSTGRES::getPassword);
  }

  @Autowired protected MockMvc mockMvc;

  @Autowired protected ObjectMapper objectMapper;

  @Autowired protected Clock clock;

  @Autowired protected JdbcTemplate jdbc;

  @BeforeEach
  void resetDatabaseAndClock() {
    ((MutableClock) clock).setInstant(TestClockConfig.START);
    jdbc.update("DELETE FROM status_history");
    jdbc.update("DELETE FROM tickets");
  }

  protected void setTime(Instant instant) {
    ((MutableClock) clock).setInstant(instant);
  }

  protected String json(Object value) throws Exception {
    return objectMapper.writeValueAsString(value);
  }

  protected ApiResult postJson(String path, Object body) throws Exception {
    return exchange(post(path).contentType(MediaType.APPLICATION_JSON).content(json(body)));
  }

  protected ApiResult patchJson(String path, Object body) throws Exception {
    return exchange(patch(path).contentType(MediaType.APPLICATION_JSON).content(json(body)));
  }

  protected ApiResult getJson(String path) throws Exception {
    return exchange(get(path).accept(MediaType.APPLICATION_JSON));
  }

  protected ApiResult exchange(MockHttpServletRequestBuilder request) throws Exception {
    MvcResult result = mockMvc.perform(request).andReturn();
    String raw = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
    JsonNode body =
        raw == null || raw.isBlank() ? objectMapper.createObjectNode() : objectMapper.readTree(raw);
    return new ApiResult(result.getResponse().getStatus(), body);
  }

  protected UUID createTicket(
      String title, ru.ithub.support.ticket.Priority priority, Instant dueAt) throws Exception {
    ApiResult created =
        postJson(
            "/api/tickets",
            new CreateTicketRequest(title, "описание", SOFTWARE_ID, priority, dueAt));
    if (created.status() != 201 || !created.body().hasNonNull("id")) {
      throw new IllegalStateException(
          "Заявка не создана: " + created.status() + " " + created.body());
    }
    return UUID.fromString(created.body().get("id").asText());
  }

  protected ApiResult changeStatus(
      UUID id, ru.ithub.support.ticket.TicketStatus status, String comment) throws Exception {
    return postJson("/api/tickets/" + id + "/status", new ChangeStatusRequest(status, comment));
  }

  protected record ApiResult(int status, JsonNode body) {}
}
