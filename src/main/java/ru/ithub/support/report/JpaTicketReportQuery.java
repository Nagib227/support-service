package ru.ithub.support.report;

import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.ithub.support.ticket.TicketRepository;

@Service
public class JpaTicketReportQuery implements TicketReportQuery {

  private final TicketRepository tickets;

  public JpaTicketReportQuery(TicketRepository tickets) {
    this.tickets = tickets;
  }

  @Override
  @Transactional(readOnly = true)
  public List<TicketReportRow> findCreatedBetween(Instant from, Instant to) {
    // Полуинтервал [from, to): заявка, созданная ровно в момент to, в отчёт не входит.
    return tickets
        .findByCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByCreatedAtAsc(from, to)
        .stream()
        .map(
            ticket ->
                new TicketReportRow(
                    ticket.getId(),
                    ticket.getStatus(),
                    ticket.getPriority(),
                    ticket.getCreatedAt(),
                    ticket.getDueAt(),
                    ticket.getClosedAt()))
        .toList();
  }
}
