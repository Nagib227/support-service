package ru.ithub.support.ticket;

import java.time.Instant;

public final class OverduePolicy {

  private OverduePolicy() {}

  /**
   * Заявка просрочена, если срок уже наступил (dueAt <= now), а статус ещё открытый. В момент
   * равенства срока и текущего времени заявка уже считается просроченной.
   */
  public static boolean isOverdue(TicketStatus status, Instant dueAt, Instant now) {
    if (dueAt == null || now == null) {
      return false;
    }
    if (status != TicketStatus.NEW && status != TicketStatus.IN_PROGRESS) {
      return false;
    }
    return !dueAt.isAfter(now);
  }
}
