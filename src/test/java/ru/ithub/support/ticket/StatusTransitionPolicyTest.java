package ru.ithub.support.ticket;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class StatusTransitionPolicyTest {

  private StatusTransitionPolicy policy;

  @BeforeEach
  void setUp() {
    policy = new StatusTransitionPolicy();
  }

  @Test
  @DisplayName("NEW можно перевести в работу")
  void allowsNewToInProgress() {
    assertThat(policy.isAllowed(TicketStatus.NEW, TicketStatus.IN_PROGRESS)).isTrue();
  }

  @Test
  @DisplayName("IN_PROGRESS можно завершить")
  void allowsInProgressToResolved() {
    assertThat(policy.isAllowed(TicketStatus.IN_PROGRESS, TicketStatus.RESOLVED)).isTrue();
  }

  @Test
  @DisplayName("Отмена разрешена из NEW")
  void allowsCancelFromNew() {
    assertThat(policy.isAllowed(TicketStatus.NEW, TicketStatus.CANCELLED)).isTrue();
  }

  @Test
  @DisplayName("Отмена разрешена из IN_PROGRESS")
  void allowsCancelFromInProgress() {
    assertThat(policy.isAllowed(TicketStatus.IN_PROGRESS, TicketStatus.CANCELLED)).isTrue();
  }

  @Test
  @DisplayName("Нельзя закрыть заявку, минуя работу")
  void rejectsNewToResolved() {
    assertThat(policy.isAllowed(TicketStatus.NEW, TicketStatus.RESOLVED)).isFalse();
  }

  @Test
  @DisplayName("Закрытую заявку нельзя вернуть в работу")
  void rejectsResolvedToInProgress() {
    assertThat(policy.isAllowed(TicketStatus.RESOLVED, TicketStatus.IN_PROGRESS)).isFalse();
  }

  @Test
  @DisplayName("Отменённую заявку нельзя открыть заново")
  void rejectsCancelledToNew() {
    assertThat(policy.isAllowed(TicketStatus.CANCELLED, TicketStatus.NEW)).isFalse();
  }

  @Test
  @DisplayName("Повтор того же статуса запрещён")
  void rejectsSameStatus() {
    assertThat(policy.isAllowed(TicketStatus.NEW, TicketStatus.NEW)).isFalse();
    assertThat(policy.isAllowed(TicketStatus.IN_PROGRESS, TicketStatus.IN_PROGRESS)).isFalse();
  }

  @Test
  @DisplayName("Редактировать можно только открытую заявку")
  void editabilityFollowsOpenStatuses() {
    assertThat(policy.isEditable(TicketStatus.NEW)).isTrue();
    assertThat(policy.isEditable(TicketStatus.IN_PROGRESS)).isTrue();
    assertThat(policy.isEditable(TicketStatus.RESOLVED)).isFalse();
    assertThat(policy.isEditable(TicketStatus.CANCELLED)).isFalse();
  }
}
