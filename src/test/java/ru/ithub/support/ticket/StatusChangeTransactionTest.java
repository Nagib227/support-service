package ru.ithub.support.ticket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import ru.ithub.support.IntegrationTestBase;

class StatusChangeTransactionTest extends IntegrationTestBase {

  @MockitoBean private StatusHistoryRepository history;

  @Test
  @DisplayName("Сбой записи истории откатывает изменение статуса")
  void historyWriteFailureRollsBackStatusChange() throws Exception {
    when(history.save(any(StatusHistory.class)))
        .thenThrow(new IllegalStateException("сбой записи истории"));
    UUID id = createTicket("Транзакция", Priority.MEDIUM, null);

    ApiResult failed = changeStatus(id, TicketStatus.IN_PROGRESS, null);

    assertThat(failed.status()).isEqualTo(500);
    assertThat(failed.body().get("status").asInt()).isEqualTo(500);
    assertThat(failed.body().get("message").asText()).isEqualTo("Внутренняя ошибка сервера");
    assertThat(failed.body().get("path").asText()).isEqualTo("/api/tickets/" + id + "/status");

    String stored =
        jdbc.queryForObject("select status from tickets where id = ?", String.class, id);
    Integer historyRows =
        jdbc.queryForObject(
            "select count(*) from status_history where ticket_id = ?", Integer.class, id);
    assertThat(stored).isEqualTo("NEW");
    assertThat(historyRows).isZero();
  }
}
