package ru.ithub.support.report

data class ReportSummary(
    val createdCount: Int,
    val byStatus: Map<String, Int>,
    val byPriority: Map<String, Int>,
    val overdueOpenCount: Int,
    val averageResolutionSeconds: Double?,
)
