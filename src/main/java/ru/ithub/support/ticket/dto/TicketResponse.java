package ru.ithub.support.ticket.dto;

import java.time.Instant;
import java.util.UUID;
import ru.ithub.support.category.CategoryResponse;
import ru.ithub.support.ticket.OverduePolicy;
import ru.ithub.support.ticket.Priority;
import ru.ithub.support.ticket.Ticket;
import ru.ithub.support.ticket.TicketStatus;

public record TicketResponse(
    UUID id,
    String title,
    String description,
    CategoryResponse category,
    Priority priority,
    TicketStatus status,
    Instant createdAt,
    Instant dueAt,
    Instant closedAt,
    boolean overdue) {

  public static TicketResponse of(Ticket ticket, Instant now) {
    return new TicketResponse(
        ticket.getId(),
        ticket.getTitle(),
        ticket.getDescription(),
        CategoryResponse.of(ticket.getCategory()),
        ticket.getPriority(),
        ticket.getStatus(),
        ticket.getCreatedAt(),
        ticket.getDueAt(),
        ticket.getClosedAt(),
        OverduePolicy.isOverdue(ticket.getStatus(), ticket.getDueAt(), now));
  }
}
