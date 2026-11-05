package ru.ithub.support.report

import org.springframework.stereotype.Service
import ru.ithub.support.ticket.OverduePolicy
import ru.ithub.support.ticket.Priority
import ru.ithub.support.ticket.TicketStatus
import java.time.Clock
import java.time.Duration
import java.time.Instant

@Service
class ReportService(
    private val tickets: TicketReportQuery,
    private val clock: Clock,
) {
    fun summary(
        from: Instant,
        to: Instant,
    ): ReportSummary {
        val sample = tickets.findCreatedBetween(from, to)
        val byStatus =
            TicketStatus.values().associate { status ->
                status.name to sample.count { it.status == status }
            }
        val byPriority =
            Priority.values().associate { priority ->
                priority.name to sample.count { it.priority == priority }
            }
        val now = clock.instant()
        val overdueOpenCount =
            sample.count { row ->
                OverduePolicy.isOverdue(row.status, row.dueAt, now)
            }
        val resolutionSeconds =
            sample
                .filter { it.status == TicketStatus.RESOLVED }
                .mapNotNull { row ->
                    row.closedAt?.let { closed ->
                        Duration.between(row.createdAt, closed).toMillis() / 1000.0
                    }
                }
        val average: Double? = if (resolutionSeconds.isEmpty()) null else resolutionSeconds.average()
        return ReportSummary(
            createdCount = sample.size,
            byStatus = byStatus,
            byPriority = byPriority,
            overdueOpenCount = overdueOpenCount,
            averageResolutionSeconds = average,
        )
    }
}
