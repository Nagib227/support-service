package ru.ithub.support.report;

import java.time.Instant;
import java.util.UUID;
import ru.ithub.support.ticket.Priority;
import ru.ithub.support.ticket.TicketStatus;

public record TicketReportRow(
    UUID id,
    TicketStatus status,
    Priority priority,
    Instant createdAt,
    Instant dueAt,
    Instant closedAt) {}
