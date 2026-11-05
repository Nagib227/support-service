package ru.ithub.support.ticket.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import ru.ithub.support.ticket.TicketStatus;

public record ChangeStatusRequest(
    @NotNull(message = "Новый статус обязателен") TicketStatus status,
    @Size(max = 500, message = "Комментарий не длиннее 500 символов") String comment) {}
