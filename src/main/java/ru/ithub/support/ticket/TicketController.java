package ru.ithub.support.ticket;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.ithub.support.ticket.dto.ChangeStatusRequest;
import ru.ithub.support.ticket.dto.CreateTicketRequest;
import ru.ithub.support.ticket.dto.StatusHistoryResponse;
import ru.ithub.support.ticket.dto.TicketPageResponse;
import ru.ithub.support.ticket.dto.TicketResponse;
import ru.ithub.support.ticket.dto.UpdateTicketRequest;

@RestController
@RequestMapping("/api/tickets")
@Tag(name = "Заявки")
public class TicketController {

  private final TicketService tickets;

  public TicketController(TicketService tickets) {
    this.tickets = tickets;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Зарегистрировать заявку")
  public TicketResponse create(@Valid @RequestBody CreateTicketRequest request) {
    return tickets.create(request);
  }

  @GetMapping("/{id}")
  @Operation(summary = "Получить заявку по идентификатору")
  public TicketResponse get(@PathVariable UUID id) {
    return tickets.get(id);
  }

  @PatchMapping("/{id}")
  @Operation(summary = "Изменить описание, приоритет и срок")
  public TicketResponse update(
      @PathVariable UUID id, @Valid @RequestBody UpdateTicketRequest request) {
    return tickets.update(id, request);
  }

  @PostMapping("/{id}/status")
  @Operation(summary = "Изменить статус")
  public TicketResponse changeStatus(
      @PathVariable UUID id, @Valid @RequestBody ChangeStatusRequest request) {
    return tickets.changeStatus(id, request);
  }

  @GetMapping("/{id}/history")
  @Operation(summary = "История изменения статусов")
  public List<StatusHistoryResponse> history(@PathVariable UUID id) {
    return tickets.history(id);
  }

  @GetMapping
  @Operation(summary = "Список заявок с фильтрацией и пагинацией")
  public TicketPageResponse list(
      @RequestParam(required = false) TicketStatus status,
      @RequestParam(required = false) Priority priority,
      @RequestParam(required = false) UUID categoryId,
      @RequestParam(required = false) Boolean overdue,
      @RequestParam(required = false) String title,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      @RequestParam(defaultValue = "createdAt,desc") String sort) {
    return tickets.list(
        new TicketFilter(status, priority, categoryId, overdue, title), page, size, sort);
  }
}
