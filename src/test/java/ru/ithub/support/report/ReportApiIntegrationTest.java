package ru.ithub.support.report;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.ithub.support.IntegrationTestBase;
import ru.ithub.support.ticket.Priority;
import ru.ithub.support.ticket.TicketStatus;

class ReportApiIntegrationTest extends IntegrationTestBase {

  private static final Instant FROM = Instant.parse("2026-01-01T00:00:00Z");
  private static final Instant TO = Instant.parse("2026-02-01T00:00:00Z");

  @Test
  @DisplayName("Пустая выборка возвращает нули и null")
  void emptySample() throws Exception {
    ApiResult summary = summary(FROM, TO);

    assertThat(summary.status()).isEqualTo(200);
    assertThat(summary.body().get("createdCount").asInt()).isZero();
    assertThat(summary.body().get("overdueOpenCount").asInt()).isZero();
    assertThat(summary.body().get("byStatus").get("NEW").asInt()).isZero();
    assertThat(summary.body().get("byStatus").get("RESOLVED").asInt()).isZero();
    assertThat(summary.body().get("byPriority").get("LOW").asInt()).isZero();
    assertThat(summary.body().get("averageResolutionSeconds").isNull()).isTrue();
  }

  @Test
  @DisplayName("Если завершённых заявок нет, среднее время равно null")
  void averageIsNullWithoutResolvedTickets() throws Exception {
    setTime(Instant.parse("2026-01-10T00:00:00Z"));
    createTicket("Ещё в очереди", Priority.LOW, Instant.parse("2026-01-11T00:00:00Z"));

    ApiResult summary = summary(FROM, TO);

    assertThat(summary.status()).isEqualTo(200);
    assertThat(summary.body().get("createdCount").asInt()).isEqualTo(1);
    assertThat(summary.body().get("byStatus").get("NEW").asInt()).isEqualTo(1);
    assertThat(summary.body().get("byStatus").get("RESOLVED").asInt()).isZero();
    assertThat(summary.body().get("averageResolutionSeconds").isNull()).isTrue();
  }

  @Test
  @DisplayName("Неверный период отчёта возвращает 400")
  void invalidPeriodReturns400() throws Exception {
    ApiResult same = summary(FROM, FROM);
    assertThat(same.status()).isEqualTo(400);
    assertThat(same.body().get("message").asText())
        .isEqualTo("Начало периода должно быть раньше конца");

    ApiResult reversed = summary(TO, FROM);
    assertThat(reversed.status()).isEqualTo(400);

    ApiResult missing = getJson("/api/reports/summary?from=2026-01-01T00:00:00Z");
    assertThat(missing.status()).isEqualTo(400);
    assertThat(missing.body().get("message").asText()).contains("Некорректный запрос");
  }

  @Test
  @DisplayName("Регрессия D-1: заявка, созданная ровно в конце периода, не входит в сводку")
  void regressionD1_ticketCreatedExactlyAtPeriodEndIsExcluded() throws Exception {
    setTime(FROM);
    createTicket("На нижней границе", Priority.LOW, null);
    setTime(TO.minusSeconds(1));
    createTicket("Внутри периода", Priority.MEDIUM, null);
    setTime(TO);
    UUID atEnd = createTicket("Ровно конец периода", Priority.HIGH, null);
    setTime(FROM.minusSeconds(1));
    createTicket("До периода", Priority.LOW, null);

    ApiResult summary = summary(FROM, TO);

    assertThat(summary.status()).isEqualTo(200);
    assertThat(summary.body().get("createdCount").asInt()).isEqualTo(2);
    assertThat(summary.body().get("byPriority").get("HIGH").asInt()).isZero();
    Integer outside =
        jdbc.queryForObject("select count(*) from tickets where id = ?", Integer.class, atEnd);
    assertThat(outside).isEqualTo(1);
  }

  @Test
  @DisplayName("Среднее время считается только по RESOLVED как closedAt минус createdAt")
  void averageUsesClosedAtOfResolvedTicketsOnly() throws Exception {
    Instant createdAt = Instant.parse("2026-01-10T00:00:00Z");
    setTime(createdAt);
    UUID resolved = createTicket("Решённая", Priority.HIGH, createdAt.plusSeconds(10_000));
    UUID open = createTicket("Открытая", Priority.LOW, createdAt.minusSeconds(10));
    setTime(createdAt.plusSeconds(100));
    assertThat(changeStatus(resolved, TicketStatus.IN_PROGRESS, null).status()).isEqualTo(200);
    setTime(createdAt.plusSeconds(150));
    assertThat(changeStatus(resolved, TicketStatus.RESOLVED, null).status()).isEqualTo(200);
    assertThat(changeStatus(open, TicketStatus.CANCELLED, null).status()).isEqualTo(200);

    ApiResult summary = summary(FROM, TO);

    assertThat(summary.body().get("createdCount").asInt()).isEqualTo(2);
    assertThat(summary.body().get("byStatus").get("RESOLVED").asInt()).isEqualTo(1);
    assertThat(summary.body().get("byStatus").get("CANCELLED").asInt()).isEqualTo(1);
    assertThat(summary.body().get("averageResolutionSeconds").asDouble()).isEqualTo(150.0);
    assertThat(summary.body().get("overdueOpenCount").asInt()).isZero();
  }

  private ApiResult summary(Instant from, Instant to) throws Exception {
    return getJson("/api/reports/summary?from=" + from + "&to=" + to);
  }
}
