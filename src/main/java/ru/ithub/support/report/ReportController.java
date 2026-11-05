package ru.ithub.support.report;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.ithub.support.error.ApiException;

@RestController
@RequestMapping("/api/reports")
@Tag(name = "Отчёты")
public class ReportController {

  private final ReportService reports;

  public ReportController(ReportService reports) {
    this.reports = reports;
  }

  @GetMapping("/summary")
  @Operation(summary = "Сводка по заявкам, созданным в периоде [from, to)")
  public ReportSummary summary(@RequestParam Instant from, @RequestParam Instant to) {
    if (!from.isBefore(to)) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "Начало периода должно быть раньше конца");
    }
    return reports.summary(from, to);
  }
}
