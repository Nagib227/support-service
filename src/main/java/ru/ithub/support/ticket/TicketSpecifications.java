package ru.ithub.support.ticket;

import jakarta.persistence.criteria.Predicate;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.data.jpa.domain.Specification;

public final class TicketSpecifications {

  private TicketSpecifications() {}

  public static Specification<Ticket> from(TicketFilter filter, Instant now) {
    return (root, query, cb) -> {
      List<Predicate> predicates = new ArrayList<>();
      if (filter.status() != null) {
        predicates.add(cb.equal(root.get("status"), filter.status()));
      }
      if (filter.priority() != null) {
        predicates.add(cb.equal(root.get("priority"), filter.priority()));
      }
      if (filter.categoryId() != null) {
        predicates.add(cb.equal(root.get("category").get("id"), filter.categoryId()));
      }
      if (filter.title() != null && !filter.title().isBlank()) {
        predicates.add(cb.like(cb.lower(root.get("title")), likePattern(filter.title()), '\\'));
      }
      if (filter.overdue() != null) {
        Predicate overdue =
            cb.and(
                root.get("status").in(TicketStatus.NEW, TicketStatus.IN_PROGRESS),
                cb.isNotNull(root.get("dueAt")),
                cb.lessThanOrEqualTo(root.get("dueAt"), now));
        predicates.add(filter.overdue() ? overdue : cb.not(overdue));
      }
      if (predicates.isEmpty()) {
        return cb.conjunction();
      }
      return cb.and(predicates.toArray(Predicate[]::new));
    };
  }

  private static String likePattern(String raw) {
    String escaped =
        raw.toLowerCase(Locale.ROOT).replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    return "%" + escaped + "%";
  }
}
