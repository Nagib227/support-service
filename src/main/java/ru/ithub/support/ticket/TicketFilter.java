package ru.ithub.support.ticket;

import java.util.UUID;

public record TicketFilter(
    TicketStatus status, Priority priority, UUID categoryId, Boolean overdue, String title) {}
