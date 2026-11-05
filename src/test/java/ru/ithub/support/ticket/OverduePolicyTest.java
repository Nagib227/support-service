package ru.ithub.support.ticket;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class OverduePolicyTest {

  private static final Instant NOW = Instant.parse("2026-01-15T12:00:00Z");

  @Test
  @DisplayName("В момент наступления срока открытая заявка уже просрочена")
  void exactDueInstantIsOverdue() {
    assertThat(OverduePolicy.isOverdue(TicketStatus.NEW, NOW, NOW)).isTrue();
    assertThat(OverduePolicy.isOverdue(TicketStatus.IN_PROGRESS, NOW, NOW)).isTrue();
  }

  @Test
  @DisplayName("За секунду до срока заявка ещё не просрочена")
  void oneSecondBeforeDueIsNotOverdue() {
    assertThat(OverduePolicy.isOverdue(TicketStatus.NEW, NOW.plusSeconds(1), NOW)).isFalse();
  }

  @Test
  @DisplayName("Истёкший срок у завершённой или отменённой заявки не делает её просроченной")
  void terminalStatusIsNotOverdue() {
    Instant past = NOW.minusSeconds(1);
    assertThat(OverduePolicy.isOverdue(TicketStatus.RESOLVED, past, NOW)).isFalse();
    assertThat(OverduePolicy.isOverdue(TicketStatus.CANCELLED, past, NOW)).isFalse();
  }

  @Test
  @DisplayName("Без срока заявка не просрочена")
  void nullDueIsNotOverdue() {
    assertThat(OverduePolicy.isOverdue(TicketStatus.NEW, null, NOW)).isFalse();
    assertThat(OverduePolicy.isOverdue(TicketStatus.IN_PROGRESS, null, NOW)).isFalse();
  }

  @Test
  @DisplayName("Открытая заявка с истёкшим сроком просрочена")
  void openTicketPastDueIsOverdue() {
    assertThat(OverduePolicy.isOverdue(TicketStatus.IN_PROGRESS, NOW.minusSeconds(1), NOW))
        .isTrue();
  }
}
