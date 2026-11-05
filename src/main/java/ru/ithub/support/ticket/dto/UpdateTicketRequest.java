package ru.ithub.support.ticket.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import ru.ithub.support.ticket.Priority;

@Schema(
    description =
        "Полная карточка редактируемых полей. Пустые description и dueAt очищают значения.")
public record UpdateTicketRequest(
    @Size(max = 4000, message = "Описание не длиннее 4000 символов") String description,
    @NotNull(message = "Приоритет обязателен") Priority priority,
    Instant dueAt) {}
