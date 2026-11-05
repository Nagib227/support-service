package ru.ithub.support.ticket;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface TicketRepository
    extends JpaRepository<Ticket, UUID>, JpaSpecificationExecutor<Ticket> {

  List<Ticket> findByCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByCreatedAtAsc(
      Instant from, Instant to);
}
