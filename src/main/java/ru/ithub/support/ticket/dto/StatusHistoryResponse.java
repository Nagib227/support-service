package ru.ithub.support.ticket.dto;

import java.time.Instant;
import java.util.UUID;
import ru.ithub.support.ticket.StatusHistory;
import ru.ithub.support.ticket.TicketStatus;

public record StatusHistoryResponse(
    UUID id, TicketStatus fromStatus, TicketStatus toStatus, Instant changedAt, String comment) {

  public static StatusHistoryResponse of(StatusHistory history) {
    return new StatusHistoryResponse(
        history.getId(),
        history.getFromStatus(),
        history.getToStatus(),
        history.getChangedAt(),
        history.getComment());
  }
}
