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
import ru.ithub.support.category.Category;

@Entity
@Table(name = "tickets")
public class Ticket {

  @Id private UUID id;

  @Column(nullable = false, length = 200)
  private String title;

  @Column(length = 4000)
  private String description;

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "category_id", nullable = false)
  private Category category;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 16)
  private Priority priority;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 32)
  private TicketStatus status;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "due_at")
  private Instant dueAt;

  @Column(name = "closed_at")
  private Instant closedAt;

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public Category getCategory() {
    return category;
  }

  public void setCategory(Category category) {
    this.category = category;
  }

  public Priority getPriority() {
    return priority;
  }

  public void setPriority(Priority priority) {
    this.priority = priority;
  }

  public TicketStatus getStatus() {
    return status;
  }

  public void setStatus(TicketStatus status) {
    this.status = status;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }

  public Instant getDueAt() {
    return dueAt;
  }

  public void setDueAt(Instant dueAt) {
    this.dueAt = dueAt;
  }

  public Instant getClosedAt() {
    return closedAt;
  }

  public void setClosedAt(Instant closedAt) {
    this.closedAt = closedAt;
  }
}
