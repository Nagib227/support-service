package ru.ithub.support.ticket;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StatusHistoryRepository extends JpaRepository<StatusHistory, UUID> {

  List<StatusHistory> findByTicketIdOrderByChangedAtAsc(UUID ticketId);
}
