package ru.ithub.support.ticket;

import org.springframework.stereotype.Component;

@Component
public class StatusTransitionPolicy {

  public boolean isAllowed(TicketStatus from, TicketStatus to) {
    if (from == null || to == null || from == to) {
      return false;
    }
    return switch (from) {
      case NEW -> to == TicketStatus.IN_PROGRESS || to == TicketStatus.CANCELLED;
      case IN_PROGRESS -> to == TicketStatus.RESOLVED || to == TicketStatus.CANCELLED;
      case RESOLVED, CANCELLED -> false;
    };
  }

  public boolean isEditable(TicketStatus status) {
    return status == TicketStatus.NEW || status == TicketStatus.IN_PROGRESS;
  }
}
