package ru.ithub.support.report

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import ru.ithub.support.ticket.Priority
import ru.ithub.support.ticket.StatusTransitionPolicy
import ru.ithub.support.ticket.TicketStatus
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID

class ReportServiceTest {
    private val now: Instant = Instant.parse("2026-01-15T12:00:00Z")
    private val clock: Clock = Clock.fixed(now, ZoneOffset.UTC)

    @Test
    @DisplayName("Пустая выборка даёт нули и null вместо среднего")
    fun emptySampleReturnsZerosAndNullAverage() {
        val summary = service(emptyList()).summary(now.minusSeconds(10), now)

        assertEquals(0, summary.createdCount)
        assertEquals(0, summary.overdueOpenCount)
        assertEquals(0, summary.byStatus.getValue("NEW"))
        assertEquals(0, summary.byStatus.getValue("IN_PROGRESS"))
        assertEquals(0, summary.byStatus.getValue("RESOLVED"))
        assertEquals(0, summary.byStatus.getValue("CANCELLED"))
        assertEquals(0, summary.byPriority.getValue("LOW"))
        assertEquals(0, summary.byPriority.getValue("MEDIUM"))
        assertEquals(0, summary.byPriority.getValue("HIGH"))
        assertNull(summary.averageResolutionSeconds)
    }

    @Test
    @DisplayName("Без завершённых заявок среднее время null, а не ноль")
    fun averageIsNullWhenNothingIsResolved() {
        val sample =
            listOf(
                row(TicketStatus.NEW, Priority.LOW),
                row(TicketStatus.CANCELLED, Priority.HIGH, closedAt = now),
            )

        val summary = service(sample).summary(now.minusSeconds(1000), now.plusSeconds(1))

        assertEquals(2, summary.createdCount)
        assertEquals(1, summary.byStatus.getValue("NEW"))
        assertEquals(1, summary.byStatus.getValue("CANCELLED"))
        assertNull(summary.averageResolutionSeconds)
    }

    @Test
    @DisplayName("Сводка считается по одной выборке: статусы, приоритеты, просрочка и среднее")
    fun aggregatesSingleSample() {
        val sample =
            listOf(
                row(TicketStatus.NEW, Priority.HIGH, dueAt = now),
                row(TicketStatus.IN_PROGRESS, Priority.LOW, dueAt = now.plusSeconds(1)),
                row(
                    TicketStatus.RESOLVED,
                    Priority.MEDIUM,
                    createdAt = now.minusSeconds(100),
                    closedAt = now,
                ),
                row(
                    TicketStatus.RESOLVED,
                    Priority.MEDIUM,
                    createdAt = now.minusSeconds(40),
                    closedAt = now,
                ),
                row(TicketStatus.CANCELLED, Priority.LOW, dueAt = now.minusSeconds(50), closedAt = now),
            )
        val query = RecordingQuery(sample)
        val from = now.minusSeconds(1000)
        val to = now.plusSeconds(1)

        val summary = ReportService(query, clock).summary(from, to)

        assertEquals(1, query.calls)
        assertEquals(from, query.capturedFrom)
        assertEquals(to, query.capturedTo)
        assertEquals(5, summary.createdCount)
        assertEquals(1, summary.byStatus.getValue("NEW"))
        assertEquals(1, summary.byStatus.getValue("IN_PROGRESS"))
        assertEquals(2, summary.byStatus.getValue("RESOLVED"))
        assertEquals(1, summary.byStatus.getValue("CANCELLED"))
        assertEquals(2, summary.byPriority.getValue("LOW"))
        assertEquals(2, summary.byPriority.getValue("MEDIUM"))
        assertEquals(1, summary.byPriority.getValue("HIGH"))
        assertEquals(1, summary.overdueOpenCount)
        assertEquals(70.0, summary.averageResolutionSeconds!!, 0.001)
    }

    @Test
    @DisplayName("RESOLVED без времени закрытия не ломает расчёт и не входит в среднее")
    fun resolvedWithoutClosedAtDoesNotFail() {
        val summary =
            service(listOf(row(TicketStatus.RESOLVED, Priority.LOW, closedAt = null)))
                .summary(now.minusSeconds(10), now.plusSeconds(10))

        assertEquals(1, summary.createdCount)
        assertEquals(1, summary.byStatus.getValue("RESOLVED"))
        assertNull(summary.averageResolutionSeconds)
    }

    @Test
    @DisplayName("Java-правило запрещает повторно открыть закрытую или отменённую заявку")
    fun javaStatusPolicyRejectsReopenOfTerminalTickets() {
        val policy = StatusTransitionPolicy()

        assertFalse(policy.isAllowed(TicketStatus.RESOLVED, TicketStatus.IN_PROGRESS))
        assertFalse(policy.isAllowed(TicketStatus.CANCELLED, TicketStatus.NEW))
        assertFalse(policy.isEditable(TicketStatus.RESOLVED))
        assertFalse(policy.isEditable(TicketStatus.CANCELLED))
        assertTrue(policy.isAllowed(TicketStatus.NEW, TicketStatus.IN_PROGRESS))
        assertTrue(policy.isEditable(TicketStatus.NEW))
    }

    private fun service(rows: List<TicketReportRow>): ReportService = ReportService(RecordingQuery(rows), clock)

    private fun row(
        status: TicketStatus,
        priority: Priority,
        createdAt: Instant = now.minusSeconds(100),
        dueAt: Instant? = null,
        closedAt: Instant? = null,
    ): TicketReportRow = TicketReportRow(UUID.randomUUID(), status, priority, createdAt, dueAt, closedAt)

    private class RecordingQuery(
        private val rows: List<TicketReportRow>,
    ) : TicketReportQuery {
        var calls: Int = 0
        var capturedFrom: Instant? = null
        var capturedTo: Instant? = null

        override fun findCreatedBetween(
            from: Instant,
            to: Instant,
        ): List<TicketReportRow> {
            calls += 1
            capturedFrom = from
            capturedTo = to
            return rows
        }
    }
}
