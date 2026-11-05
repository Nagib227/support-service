package ru.ithub.support.ticket.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;
import ru.ithub.support.ticket.Priority;

public record CreateTicketRequest(
    @NotBlank(message = "Заголовок обязателен")
        @Size(max = 200, message = "Заголовок не длиннее 200 символов")
        @Schema(example = "Не печатает принтер")
        String title,
    @Size(max = 4000, message = "Описание не длиннее 4000 символов")
        @Schema(example = "Кабинет 214, принтер на втором этаже")
        String description,
    @NotNull(message = "Категория обязательна")
        @Schema(example = "11111111-1111-1111-1111-111111111111")
        UUID categoryId,
    @NotNull(message = "Приоритет обязателен") @Schema(example = "HIGH") Priority priority,
    @Schema(example = "2026-01-20T12:00:00Z") Instant dueAt) {}
