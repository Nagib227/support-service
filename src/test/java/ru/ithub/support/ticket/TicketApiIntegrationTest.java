package ru.ithub.support.ticket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.ithub.support.IntegrationTestBase;
import ru.ithub.support.testing.TestClockConfig;
import ru.ithub.support.ticket.dto.UpdateTicketRequest;

class TicketApiIntegrationTest extends IntegrationTestBase {

  @Test
  @DisplayName("Создание и чтение заявки возвращают NEW без времени закрытия")
  void createAndGetReturnsNewTicket() throws Exception {
    Instant due = TestClockConfig.START.plusSeconds(86_400);
    UUID id = createTicket("  Не печатает принтер  ", Priority.HIGH, due);

    ApiResult loaded = getJson("/api/tickets/" + id);

    assertThat(loaded.status()).isEqualTo(200);
    assertThat(loaded.body().get("id").asText()).isEqualTo(id.toString());
    assertThat(loaded.body().get("title").asText()).isEqualTo("Не печатает принтер");
    assertThat(loaded.body().get("status").asText()).isEqualTo("NEW");
    assertThat(loaded.body().get("priority").asText()).isEqualTo("HIGH");
    assertThat(loaded.body().get("category").get("code").asText()).isEqualTo("software");
    assertThat(loaded.body().get("createdAt").asText()).isEqualTo("2026-01-15T12:00:00Z");
    assertThat(loaded.body().get("closedAt").isNull()).isTrue();
    assertThat(loaded.body().get("overdue").asBoolean()).isFalse();
  }

  @Test
  @DisplayName("Неизвестный идентификатор возвращает 404 в едином формате")
  void unknownTicketReturns404() throws Exception {
    UUID missing = UUID.fromString("99999999-9999-9999-9999-999999999999");

    ApiResult response = getJson("/api/tickets/" + missing);

    assertThat(response.status()).isEqualTo(404);
    assertThat(response.body().get("status").asInt()).isEqualTo(404);
    assertThat(response.body().get("error").asText()).isEqualTo("Not Found");
    assertThat(response.body().get("message").asText()).contains("Заявка не найдена");
    assertThat(response.body().get("path").asText()).isEqualTo("/api/tickets/" + missing);
    assertThat(response.body().get("violations").isArray()).isTrue();
    assertThat(response.body().get("timestamp").asText()).isEqualTo("2026-01-15T12:00:00Z");
  }

  @Test
  @DisplayName("Ошибки валидации и неизвестный путь используют единый формат")
  void validationAndBadRequestUseUnifiedBody() throws Exception {
    ApiResult blank =
        postJson(
            "/api/tickets",
            objectMapper.readTree(
                "{\"title\":\"   \",\"description\":\"x\",\"priority\":\"LOW\",\"categoryId\":\""
                    + SOFTWARE_ID
                    + "\"}"));
    assertThat(blank.status()).isEqualTo(400);
    assertThat(blank.body().get("message").asText()).isEqualTo("Ошибка валидации");
    assertThat(blank.body().get("violations").toString()).contains("Заголовок обязателен");

    ApiResult badPriority =
        postJson(
            "/api/tickets",
            objectMapper.readTree(
                "{\"title\":\"Заявка\",\"priority\":\"URGENT\",\"categoryId\":\""
                    + SOFTWARE_ID
                    + "\"}"));
    assertThat(badPriority.status()).isEqualTo(400);
    assertThat(badPriority.body().get("status").asInt()).isEqualTo(400);
    assertThat(badPriority.body().get("message").asText()).contains("Некорректный запрос");

    ApiResult unknownCategory =
        postJson(
            "/api/tickets",
            objectMapper.readTree(
                "{\"title\":\"Заявка\",\"priority\":\"LOW\",\"categoryId\":\"99999999-9999-9999-9999-999999999999\"}"));
    assertThat(unknownCategory.status()).isEqualTo(400);
    assertThat(unknownCategory.body().get("message").asText()).isEqualTo("Категория не найдена");

    ApiResult missingRoute = getJson("/api/no-such-resource");
    assertThat(missingRoute.status()).isEqualTo(404);
    assertThat(missingRoute.body().get("message").asText()).isEqualTo("Ресурс не найден");

    ApiResult badSort = getJson("/api/tickets?sort=password,desc");
    assertThat(badSort.status()).isEqualTo(400);
    assertThat(badSort.body().get("message").asText()).contains("Неизвестное поле сортировки");
  }

  @Test
  @DisplayName("Фильтрация, пагинация и момент наступления срока")
  void filterPaginationAndOverdueBoundary() throws Exception {
    Instant start = TestClockConfig.START;
    setTime(start);
    UUID network = createTicket("Сеть не работает", Priority.HIGH, start);
    setTime(start.plusSeconds(3600));
    UUID software = createTicket("Установка ПО", Priority.LOW, start.plusSeconds(864_000));
    ApiResult moved = changeStatus(software, TicketStatus.IN_PROGRESS, null);
    assertThat(moved.status()).isEqualTo(200);
    setTime(start.plusSeconds(7200));
    UUID vpn = createTicket("Сеть VPN", Priority.MEDIUM, null);
    assertThat(changeStatus(vpn, TicketStatus.CANCELLED, null).status()).isEqualTo(200);

    setTime(start);
    ApiResult exactDue = getJson("/api/tickets/" + network);
    assertThat(exactDue.body().get("overdue").asBoolean()).isTrue();

    setTime(start.plusSeconds(10));
    UUID almostDue = createTicket("Почти срок", Priority.LOW, start.plusSeconds(11));
    ApiResult beforeDue = getJson("/api/tickets/" + almostDue);
    assertThat(beforeDue.body().get("overdue").asBoolean()).isFalse();
    setTime(start.plusSeconds(11));
    ApiResult atDue = getJson("/api/tickets/" + almostDue);
    assertThat(atDue.body().get("overdue").asBoolean()).isTrue();

    ApiResult onlyNew = getJson("/api/tickets?status=NEW");
    assertThat(onlyNew.body().get("totalElements").asInt()).isEqualTo(2);

    ApiResult byTitle = exchange(get("/api/tickets").param("title", "сеть"));
    assertThat(byTitle.body().get("totalElements").asInt()).isEqualTo(2);

    ApiResult overdue = getJson("/api/tickets?overdue=true");
    assertThat(overdue.body().get("totalElements").asInt()).isEqualTo(2);
    assertThat(overdue.body().get("content").toString()).contains(network.toString());
    assertThat(overdue.body().get("content").toString()).contains(almostDue.toString());

    ApiResult notOverdue = getJson("/api/tickets?overdue=false");
    assertThat(notOverdue.body().get("totalElements").asInt()).isEqualTo(2);

    ApiResult low = getJson("/api/tickets?priority=LOW");
    assertThat(low.body().get("totalElements").asInt()).isEqualTo(2);

    ApiResult firstPage = getJson("/api/tickets?page=0&size=1&sort=createdAt,asc");
    ApiResult secondPage = getJson("/api/tickets?page=1&size=1&sort=createdAt,asc");
    assertThat(firstPage.body().get("totalElements").asInt()).isEqualTo(4);
    assertThat(firstPage.body().get("totalPages").asInt()).isEqualTo(4);
    assertThat(firstPage.body().get("content").size()).isEqualTo(1);
    assertThat(firstPage.body().get("content").get(0).get("id").asText())
        .isEqualTo(network.toString());
    assertThat(secondPage.body().get("content").get(0).get("id").asText())
        .isEqualTo(almostDue.toString());

    setTime(start.plusSeconds(8000));
    UUID percent = createTicket("100% готово", Priority.MEDIUM, start.plusSeconds(10_000));
    ApiResult percentSearch = exchange(get("/api/tickets").param("title", "100%"));
    assertThat(percentSearch.body().get("totalElements").asInt()).isEqualTo(1);
    assertThat(percentSearch.body().get("content").get(0).get("id").asText())
        .isEqualTo(percent.toString());
  }

  @Test
  @DisplayName("Смена статуса и запись истории сохраняются вместе")
  void statusChangePersistsHistory() throws Exception {
    UUID id = createTicket("Нужен доступ", Priority.MEDIUM, null);

    ApiResult progress = changeStatus(id, TicketStatus.IN_PROGRESS, "взял в работу");
    assertThat(progress.status()).isEqualTo(200);
    assertThat(progress.body().get("closedAt").isNull()).isTrue();

    setTime(TestClockConfig.START.plusSeconds(90));
    ApiResult resolved = changeStatus(id, TicketStatus.RESOLVED, "доступ выдан");
    assertThat(resolved.status()).isEqualTo(200);
    assertThat(resolved.body().get("status").asText()).isEqualTo("RESOLVED");
    assertThat(resolved.body().get("closedAt").asText()).isEqualTo("2026-01-15T12:01:30Z");

    ApiResult history = getJson("/api/tickets/" + id + "/history");
    assertThat(history.status()).isEqualTo(200);
    assertThat(history.body().size()).isEqualTo(2);
    assertThat(history.body().get(0).get("fromStatus").asText()).isEqualTo("NEW");
    assertThat(history.body().get(0).get("toStatus").asText()).isEqualTo("IN_PROGRESS");
    assertThat(history.body().get(0).get("comment").asText()).isEqualTo("взял в работу");
    assertThat(history.body().get(1).get("fromStatus").asText()).isEqualTo("IN_PROGRESS");
    assertThat(history.body().get(1).get("toStatus").asText()).isEqualTo("RESOLVED");
    assertThat(history.body().get(1).get("comment").asText()).isEqualTo("доступ выдан");

    Integer stored =
        jdbc.queryForObject(
            "select count(*) from status_history where ticket_id = ?", Integer.class, id);
    assertThat(stored).isEqualTo(2);
  }

  @Test
  @DisplayName("Миграции поднимают справочник, историю комментария и health check")
  void migrationsCategoriesAndHealth() throws Exception {
    Integer migrations =
        jdbc.queryForObject(
            "select count(*) from flyway_schema_history where success = true", Integer.class);
    assertThat(migrations).isGreaterThanOrEqualTo(3);

    String commentColumn =
        jdbc.queryForObject(
            "select column_name from information_schema.columns "
                + "where table_name = 'status_history' and column_name = 'change_comment'",
            String.class);
    assertThat(commentColumn).isEqualTo("change_comment");

    ApiResult categories = getJson("/api/categories");
    assertThat(categories.status()).isEqualTo(200);
    assertThat(categories.body().size()).isEqualTo(5);
    assertThat(categories.body().toString()).contains("software");

    ApiResult health = getJson("/actuator/health");
    assertThat(health.status()).isEqualTo(200);
    assertThat(health.body().get("status").asText()).isEqualTo("UP");
  }

  @Test
  @DisplayName("Сквозной сценарий: создание, правка, завершение и отчёт")
  void fullLifecycleCreateEditResolveAndReport() throws Exception {
    Instant createdAt = Instant.parse("2026-01-10T00:00:00Z");
    setTime(createdAt);
    UUID id = createTicket("Почта не открывается", Priority.MEDIUM, createdAt.plusSeconds(86_400));

    ApiResult patched =
        patchJson(
            "/api/tickets/" + id,
            new UpdateTicketRequest(
                "новое описание", Priority.HIGH, Instant.parse("2026-01-20T00:00:00Z")));
    assertThat(patched.status()).isEqualTo(200);
    assertThat(patched.body().get("title").asText()).isEqualTo("Почта не открывается");
    assertThat(patched.body().get("description").asText()).isEqualTo("новое описание");
    assertThat(patched.body().get("priority").asText()).isEqualTo("HIGH");
    assertThat(patched.body().get("status").asText()).isEqualTo("NEW");

    setTime(createdAt.plusSeconds(60));
    assertThat(changeStatus(id, TicketStatus.IN_PROGRESS, null).status()).isEqualTo(200);
    setTime(createdAt.plusSeconds(90));
    ApiResult resolved = changeStatus(id, TicketStatus.RESOLVED, null);
    assertThat(resolved.body().get("status").asText()).isEqualTo("RESOLVED");
    assertThat(resolved.body().get("closedAt").asText()).isEqualTo("2026-01-10T00:01:30Z");

    ApiResult history = getJson("/api/tickets/" + id + "/history");
    assertThat(history.body().size()).isEqualTo(2);

    ApiResult summary =
        getJson("/api/reports/summary?from=2026-01-01T00:00:00Z&to=2026-02-01T00:00:00Z");
    assertThat(summary.status()).isEqualTo(200);
    assertThat(summary.body().get("createdCount").asInt()).isEqualTo(1);
    assertThat(summary.body().get("byStatus").get("RESOLVED").asInt()).isEqualTo(1);
    assertThat(summary.body().get("byPriority").get("HIGH").asInt()).isEqualTo(1);
    assertThat(summary.body().get("averageResolutionSeconds").asDouble()).isEqualTo(90.0);
  }

  @Test
  @DisplayName("Запрещённые операции не меняют заявку и историю")
  void forbiddenOperationDoesNotChangeStoredData() throws Exception {
    UUID skipped = createTicket("Пропуск шага", Priority.LOW, null);
    ApiResult directResolve = changeStatus(skipped, TicketStatus.RESOLVED, null);
    assertThat(directResolve.status()).isEqualTo(409);
    assertThat(directResolve.body().get("message").asText()).contains("NEW → RESOLVED");
    ApiResult skippedState = getJson("/api/tickets/" + skipped);
    assertThat(skippedState.body().get("status").asText()).isEqualTo("NEW");
    assertThat(skippedState.body().get("closedAt").isNull()).isTrue();
    assertThat(getJson("/api/tickets/" + skipped + "/history").body().size()).isZero();

    UUID closed = createTicket("Закрытая заявка", Priority.MEDIUM, null);
    assertThat(changeStatus(closed, TicketStatus.IN_PROGRESS, null).status()).isEqualTo(200);
    setTime(TestClockConfig.START.plusSeconds(30));
    assertThat(changeStatus(closed, TicketStatus.RESOLVED, "сделано").status()).isEqualTo(200);

    ApiResult before = getJson("/api/tickets/" + closed);
    ApiResult historyBefore = getJson("/api/tickets/" + closed + "/history");

    ApiResult reopen = changeStatus(closed, TicketStatus.IN_PROGRESS, "снова");
    assertThat(reopen.status()).isEqualTo(409);
    assertThat(reopen.body().get("message").asText()).contains("запрещён");
    assertThat(reopen.body().get("status").asInt()).isEqualTo(409);

    ApiResult edit =
        patchJson("/api/tickets/" + closed, new UpdateTicketRequest("взлом", Priority.LOW, null));
    assertThat(edit.status()).isEqualTo(409);
    assertThat(edit.body().get("message").asText())
        .isEqualTo("Закрытую или отменённую заявку нельзя редактировать");

    ApiResult after = getJson("/api/tickets/" + closed);
    ApiResult historyAfter = getJson("/api/tickets/" + closed + "/history");
    assertThat(after.body().get("status").asText()).isEqualTo(before.body().get("status").asText());
    assertThat(after.body().get("description").asText())
        .isEqualTo(before.body().get("description").asText());
    assertThat(after.body().get("priority").asText())
        .isEqualTo(before.body().get("priority").asText());
    assertThat(after.body().get("closedAt").asText())
        .isEqualTo(before.body().get("closedAt").asText());
    assertThat(historyAfter.body().size()).isEqualTo(historyBefore.body().size());
    assertThat(historyAfter.body().toString()).isEqualTo(historyBefore.body().toString());

    String storedStatus =
        jdbc.queryForObject("select status from tickets where id = ?", String.class, closed);
    assertThat(storedStatus).isEqualTo("RESOLVED");
  }
}
