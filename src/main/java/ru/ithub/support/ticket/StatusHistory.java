package ru.ithub.support.ticket;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "status_history")
public class StatusHistory {

  @Id private UUID id;

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "ticket_id", nullable = false)
  private Ticket ticket;

  @Enumerated(EnumType.STRING)
  @Column(name = "from_status", nullable = false, length = 32)
  private TicketStatus fromStatus;

  @Enumerated(EnumType.STRING)
  @Column(name = "to_status", nullable = false, length = 32)
  private TicketStatus toStatus;

  @Column(name = "changed_at", nullable = false)
  private Instant changedAt;

  @Column(name = "change_comment", length = 500)
  private String comment;

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public Ticket getTicket() {
    return ticket;
  }

  public void setTicket(Ticket ticket) {
    this.ticket = ticket;
  }

  public TicketStatus getFromStatus() {
    return fromStatus;
  }

  public void setFromStatus(TicketStatus fromStatus) {
    this.fromStatus = fromStatus;
  }

  public TicketStatus getToStatus() {
    return toStatus;
  }

  public void setToStatus(TicketStatus toStatus) {
    this.toStatus = toStatus;
  }

  public Instant getChangedAt() {
    return changedAt;
  }

  public void setChangedAt(Instant changedAt) {
    this.changedAt = changedAt;
  }

  public String getComment() {
    return comment;
  }

  public void setComment(String comment) {
    this.comment = comment;
  }
}
