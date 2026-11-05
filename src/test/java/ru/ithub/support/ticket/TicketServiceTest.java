package ru.ithub.support.ticket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import ru.ithub.support.category.Category;
import ru.ithub.support.category.CategoryRepository;
import ru.ithub.support.error.ApiException;
import ru.ithub.support.ticket.dto.ChangeStatusRequest;
import ru.ithub.support.ticket.dto.CreateTicketRequest;
import ru.ithub.support.ticket.dto.TicketResponse;
import ru.ithub.support.ticket.dto.UpdateTicketRequest;

@ExtendWith(MockitoExtension.class)
class TicketServiceTest {

  private static final Instant NOW = Instant.parse("2026-01-15T12:00:00Z");
  private static final UUID CATEGORY_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

  @Mock private TicketRepository tickets;

  @Mock private CategoryRepository categories;

  @Mock private StatusHistoryRepository history;

  private TicketService service;

  @BeforeEach
  void setUp() {
    Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    service = new TicketService(tickets, categories, history, clock, new StatusTransitionPolicy());
    lenient()
        .when(tickets.save(any(Ticket.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    lenient()
        .when(history.save(any(StatusHistory.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
  }

  @Test
  @DisplayName("Новая заявка получает статус NEW и время создания из часов")
  void createAssignsNewStatusAndCreationTime() {
    when(categories.findById(CATEGORY_ID)).thenReturn(Optional.of(category()));

    TicketResponse response =
        service.create(
            new CreateTicketRequest(
                "  Не печатает принтер  ",
                "  текст  ",
                CATEGORY_ID,
                Priority.HIGH,
                NOW.plusSeconds(3600)));

    assertThat(response.id()).isNotNull();
    assertThat(response.title()).isEqualTo("Не печатает принтер");
    assertThat(response.description()).isEqualTo("текст");
    assertThat(response.status()).isEqualTo(TicketStatus.NEW);
    assertThat(response.priority()).isEqualTo(Priority.HIGH);
    assertThat(response.createdAt()).isEqualTo(NOW);
    assertThat(response.closedAt()).isNull();
    assertThat(response.overdue()).isFalse();
  }

  @Test
  @DisplayName("Пустой заголовок отклоняется до записи")
  void createRejectsBlankTitle() {
    assertThatThrownBy(
            () ->
                service.create(
                    new CreateTicketRequest("   ", null, CATEGORY_ID, Priority.LOW, null)))
        .isInstanceOf(ApiException.class)
        .extracting(ex -> ((ApiException) ex).getStatus())
        .isEqualTo(HttpStatus.BAD_REQUEST);
    verify(tickets, never()).save(any());
  }

  @Test
  @DisplayName("Неизвестная категория — это ошибка валидации")
  void createRejectsUnknownCategory() {
    when(categories.findById(CATEGORY_ID)).thenReturn(Optional.empty());

    assertThatThrownBy(
            () ->
                service.create(
                    new CreateTicketRequest("Заявка", null, CATEGORY_ID, Priority.LOW, null)))
        .isInstanceOf(ApiException.class)
        .hasMessage("Категория не найдена")
        .extracting(ex -> ((ApiException) ex).getStatus())
        .isEqualTo(HttpStatus.BAD_REQUEST);
    verify(tickets, never()).save(any());
  }

  @Test
  @DisplayName("Срок, совпавший с текущим временем, помечает новую заявку просроченной")
  void createMarksTicketOverdueWhenDueAtEqualsNow() {
    when(categories.findById(CATEGORY_ID)).thenReturn(Optional.of(category()));

    TicketResponse response =
        service.create(new CreateTicketRequest("Срок", null, CATEGORY_ID, Priority.MEDIUM, NOW));

    assertThat(response.overdue()).isTrue();
  }

  @Test
  @DisplayName("Открытую заявку можно изменить, статус при этом не меняется")
  void updateChangesEditableFieldsOnly() {
    Ticket ticket = ticket(TicketStatus.NEW);
    when(tickets.findById(ticket.getId())).thenReturn(Optional.of(ticket));
    Instant due = NOW.plusSeconds(7200);

    TicketResponse response =
        service.update(ticket.getId(), new UpdateTicketRequest("уточнение", Priority.HIGH, due));

    assertThat(response.description()).isEqualTo("уточнение");
    assertThat(response.priority()).isEqualTo(Priority.HIGH);
    assertThat(response.dueAt()).isEqualTo(due);
    assertThat(response.status()).isEqualTo(TicketStatus.NEW);
    assertThat(response.title()).isEqualTo("Исходный заголовок");
    assertThat(response.closedAt()).isNull();
  }

  @Test
  @DisplayName("Закрытую заявку редактировать нельзя, запись не выполняется")
  void updateRejectsResolvedTicket() {
    Ticket ticket = ticket(TicketStatus.RESOLVED);
    when(tickets.findById(ticket.getId())).thenReturn(Optional.of(ticket));

    assertThatThrownBy(
            () ->
                service.update(ticket.getId(), new UpdateTicketRequest("нет", Priority.LOW, null)))
        .isInstanceOf(ApiException.class)
        .hasMessage("Закрытую или отменённую заявку нельзя редактировать")
        .extracting(ex -> ((ApiException) ex).getStatus())
        .isEqualTo(HttpStatus.CONFLICT);
    verify(tickets, never()).save(any());
  }

  @Test
  @DisplayName("Переход в работу не проставляет время закрытия")
  void changeStatusToInProgressDoesNotCloseTicket() {
    Ticket ticket = ticket(TicketStatus.NEW);
    when(tickets.findById(ticket.getId())).thenReturn(Optional.of(ticket));

    TicketResponse response =
        service.changeStatus(
            ticket.getId(), new ChangeStatusRequest(TicketStatus.IN_PROGRESS, null));

    assertThat(response.status()).isEqualTo(TicketStatus.IN_PROGRESS);
    assertThat(response.closedAt()).isNull();
  }

  @Test
  @DisplayName("Завершение ставит время закрытия и пишет историю в том же вызове")
  void changeStatusToResolvedWritesHistoryAndClosedAt() {
    Ticket ticket = ticket(TicketStatus.IN_PROGRESS);
    when(tickets.findById(ticket.getId())).thenReturn(Optional.of(ticket));

    TicketResponse response =
        service.changeStatus(
            ticket.getId(), new ChangeStatusRequest(TicketStatus.RESOLVED, "готово"));

    assertThat(response.status()).isEqualTo(TicketStatus.RESOLVED);
    assertThat(response.closedAt()).isEqualTo(NOW);
    ArgumentCaptor<StatusHistory> captor = ArgumentCaptor.forClass(StatusHistory.class);
    verify(history).save(captor.capture());
    assertThat(captor.getValue().getFromStatus()).isEqualTo(TicketStatus.IN_PROGRESS);
    assertThat(captor.getValue().getToStatus()).isEqualTo(TicketStatus.RESOLVED);
    assertThat(captor.getValue().getChangedAt()).isEqualTo(NOW);
    assertThat(captor.getValue().getComment()).isEqualTo("готово");
    verify(tickets).save(ticket);
  }

  @Test
  @DisplayName("Отмена проставляет время закрытия")
  void cancelFromNewSetsClosedAt() {
    Ticket ticket = ticket(TicketStatus.NEW);
    when(tickets.findById(ticket.getId())).thenReturn(Optional.of(ticket));

    TicketResponse response =
        service.changeStatus(ticket.getId(), new ChangeStatusRequest(TicketStatus.CANCELLED, null));

    assertThat(response.status()).isEqualTo(TicketStatus.CANCELLED);
    assertThat(response.closedAt()).isEqualTo(NOW);
  }

  @Test
  @DisplayName("Запрещённый переход не сохраняет ни заявку, ни историю")
  void forbiddenTransitionDoesNotSave() {
    Ticket ticket = ticket(TicketStatus.RESOLVED);
    ticket.setClosedAt(NOW.minusSeconds(10));
    when(tickets.findById(ticket.getId())).thenReturn(Optional.of(ticket));

    assertThatThrownBy(
            () ->
                service.changeStatus(
                    ticket.getId(), new ChangeStatusRequest(TicketStatus.IN_PROGRESS, null)))
        .isInstanceOf(ApiException.class)
        .extracting(ex -> ((ApiException) ex).getStatus())
        .isEqualTo(HttpStatus.CONFLICT);
    verify(tickets, never()).save(any());
    verify(history, never()).save(any());
    assertThat(ticket.getStatus()).isEqualTo(TicketStatus.RESOLVED);
    assertThat(ticket.getClosedAt()).isEqualTo(NOW.minusSeconds(10));
  }

  private static Category category() {
    Category category = new Category();
    category.setId(CATEGORY_ID);
    category.setCode("software");
    category.setName("Программное обеспечение");
    return category;
  }

  private static Ticket ticket(TicketStatus status) {
    Ticket ticket = new Ticket();
    ticket.setId(UUID.randomUUID());
    ticket.setTitle("Исходный заголовок");
    ticket.setDescription("исходное");
    ticket.setCategory(category());
    ticket.setPriority(Priority.LOW);
    ticket.setStatus(status);
    ticket.setCreatedAt(NOW.minusSeconds(100));
    return ticket;
  }
}
