package ru.ithub.support.report;

import java.time.Instant;
import java.util.List;

public interface TicketReportQuery {

  List<TicketReportRow> findCreatedBetween(Instant from, Instant to);
}
