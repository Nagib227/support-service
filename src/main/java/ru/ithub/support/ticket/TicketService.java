package ru.ithub.support.ticket;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.ithub.support.category.Category;
import ru.ithub.support.category.CategoryRepository;
import ru.ithub.support.error.ApiException;
import ru.ithub.support.ticket.dto.ChangeStatusRequest;
import ru.ithub.support.ticket.dto.CreateTicketRequest;
import ru.ithub.support.ticket.dto.StatusHistoryResponse;
import ru.ithub.support.ticket.dto.TicketPageResponse;
import ru.ithub.support.ticket.dto.TicketResponse;
import ru.ithub.support.ticket.dto.UpdateTicketRequest;

@Service
public class TicketService {

  private static final Logger log = LoggerFactory.getLogger(TicketService.class);
  private static final Set<String> SORT_FIELDS =
      Set.of("createdAt", "dueAt", "priority", "status", "title");

  private final TicketRepository tickets;
  private final CategoryRepository categories;
  private final StatusHistoryRepository history;
  private final Clock clock;
  private final StatusTransitionPolicy transitions;

  public TicketService(
      TicketRepository tickets,
      CategoryRepository categories,
      StatusHistoryRepository history,
      Clock clock,
      StatusTransitionPolicy transitions) {
    this.tickets = tickets;
    this.categories = categories;
    this.history = history;
    this.clock = clock;
    this.transitions = transitions;
  }

  @Transactional
  public TicketResponse create(CreateTicketRequest request) {
    String title = normalizeRequiredTitle(request.title());
    if (request.priority() == null) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "Приоритет обязателен");
    }
    if (request.categoryId() == null) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "Категория обязательна");
    }
    Category category =
        categories
            .findById(request.categoryId())
            .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Категория не найдена"));

    Ticket ticket = new Ticket();
    ticket.setId(UUID.randomUUID());
    ticket.setTitle(title);
    ticket.setDescription(blankToNull(request.description()));
    ticket.setCategory(category);
    ticket.setPriority(request.priority());
    ticket.setStatus(TicketStatus.NEW);
    ticket.setCreatedAt(clock.instant());
    ticket.setDueAt(request.dueAt());
    tickets.save(ticket);
    log.info("Создана заявка {}", ticket.getId());
    return TicketResponse.of(ticket, clock.instant());
  }

  @Transactional(readOnly = true)
  public TicketResponse get(UUID id) {
    return TicketResponse.of(load(id), clock.instant());
  }

  @Transactional
  public TicketResponse update(UUID id, UpdateTicketRequest request) {
    Ticket ticket = load(id);
    if (!transitions.isEditable(ticket.getStatus())) {
      throw new ApiException(
          HttpStatus.CONFLICT, "Закрытую или отменённую заявку нельзя редактировать");
    }
    if (request.priority() == null) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "Приоритет обязателен");
    }
    if (request.description() != null && request.description().length() > 4000) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "Описание не длиннее 4000 символов");
    }
    ticket.setDescription(blankToNull(request.description()));
    ticket.setPriority(request.priority());
    ticket.setDueAt(request.dueAt());
    tickets.save(ticket);
    return TicketResponse.of(ticket, clock.instant());
  }

  @Transactional
  public TicketResponse changeStatus(UUID id, ChangeStatusRequest request) {
    if (request.status() == null) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "Новый статус обязателен");
    }
    if (request.comment() != null && request.comment().length() > 500) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "Комментарий не длиннее 500 символов");
    }
    Ticket ticket = load(id);
    TicketStatus from = ticket.getStatus();
    TicketStatus to = request.status();
    if (!transitions.isAllowed(from, to)) {
      throw new ApiException(
          HttpStatus.CONFLICT, "Переход статуса " + from + " → " + to + " запрещён");
    }

    ticket.setStatus(to);
    if (to == TicketStatus.RESOLVED || to == TicketStatus.CANCELLED) {
      ticket.setClosedAt(clock.instant());
    }
    tickets.save(ticket);

    StatusHistory entry = new StatusHistory();
    entry.setId(UUID.randomUUID());
    entry.setTicket(ticket);
    entry.setFromStatus(from);
    entry.setToStatus(to);
    entry.setChangedAt(clock.instant());
    entry.setComment(blankToNull(request.comment()));
    history.save(entry);

    log.info("Статус заявки {} изменён: {} -> {}", id, from, to);
    return TicketResponse.of(ticket, clock.instant());
  }

  @Transactional(readOnly = true)
  public List<StatusHistoryResponse> history(UUID id) {
    load(id);
    return history.findByTicketIdOrderByChangedAtAsc(id).stream()
        .map(StatusHistoryResponse::of)
        .toList();
  }

  @Transactional(readOnly = true)
  public TicketPageResponse list(TicketFilter filter, int page, int size, String sort) {
    Pageable pageable = pageable(page, size, sort);
    Page<Ticket> result =
        tickets.findAll(TicketSpecifications.from(filter, clock.instant()), pageable);
    Instant now = clock.instant();
    List<TicketResponse> content =
        result.getContent().stream().map(ticket -> TicketResponse.of(ticket, now)).toList();
    return new TicketPageResponse(
        content,
        result.getNumber(),
        result.getSize(),
        result.getTotalElements(),
        result.getTotalPages());
  }

  private Ticket load(UUID id) {
    return tickets
        .findById(id)
        .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Заявка не найдена: " + id));
  }

  private Pageable pageable(int page, int size, String sort) {
    if (page < 0) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "Номер страницы не может быть отрицательным");
    }
    if (size < 1 || size > 100) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "Размер страницы должен быть от 1 до 100");
    }
    if (sort == null || sort.isBlank()) {
      return PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
    }
    String[] parts = sort.split(",");
    if (parts.length > 2) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "Сортировка задаётся как поле,направление");
    }
    String field = parts[0].trim();
    if (!SORT_FIELDS.contains(field)) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "Неизвестное поле сортировки: " + field);
    }
    Sort.Direction direction = Sort.Direction.DESC;
    if (parts.length == 2) {
      String raw = parts[1].trim();
      if (!raw.equalsIgnoreCase("asc") && !raw.equalsIgnoreCase("desc")) {
        throw new ApiException(
            HttpStatus.BAD_REQUEST, "Направление сортировки должно быть asc или desc");
      }
      direction = Sort.Direction.fromString(raw);
    }
    return PageRequest.of(page, size, Sort.by(direction, field));
  }

  private static String normalizeRequiredTitle(String title) {
    if (title == null || title.isBlank()) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "Заголовок обязателен");
    }
    String trimmed = title.trim();
    if (trimmed.length() > 200) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "Заголовок не длиннее 200 символов");
    }
    return trimmed;
  }

  private static String blankToNull(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return value.trim();
  }
}
