# Тест-план

Проверки привязаны к историям и критериям из [specification.md](specification.md). Время в интеграционных тестах задаёт `MutableClock` со стартом `2026-01-15T12:00:00Z`. Перед каждым тестом заявки и история удаляются, категории справочника остаются. Порядок тестов на результат не влияет.

Запуск: `.\gradlew.bat test`. Отчёт о покрытии: `.\gradlew.bat jacocoTestReport`, каталог `build/reports/jacoco/test/html`. Краткий итог после прогона — [coverage.md](coverage.md).

## Модульные тесты

| Тест | История | Что доказывает |
| --- | --- | --- |
| `StatusTransitionPolicyTest` — 9 методов | US-4 | Разрешённые и запрещённые переходы, запрет правки закрытых и отменённых |
| `OverduePolicyTest` — 5 методов | US-6 | Просрочка в момент срока, за секунду до срока, для закрытых и без срока |
| `TicketServiceTest` — 10 методов | US-1, US-3, US-4 | Создание `NEW`, пустой заголовок, неизвестная категория, просрочка при создании, правка, запрет сохранения при 409, время закрытия и запись истории |
| `ReportServiceTest` — 5 методов | US-8 | Пустая выборка, `null` вместо нуля, одна выборка на все показатели, безопасный `closedAt`, вызов Java-политики статусов |

Java-тесты сервиса используют Mockito. Kotlin-тест сводки подменяет Java-интерфейс `TicketReportQuery` и отдельно вызывает Java-класс `StatusTransitionPolicy`.

## Интеграционные тесты

PostgreSQL 16 поднимается через Testcontainers. Миграции Flyway применяются к этой базе.

| Тест | История | Что доказывает |
| --- | --- | --- |
| `createAndGetReturnsNewTicket` | US-1, US-2 | REST, статус `NEW`, категория из миграции |
| `unknownTicketReturns404` | US-2 | HTTP 404 и единый формат |
| `validationAndBadRequestUseUnifiedBody` | US-1, US-9 | HTTP 400, неизвестный путь, неверная сортировка |
| `filterPaginationAndOverdueBoundary` | US-6 | Фильтры, страницы, `%` в заголовке, момент наступления срока |
| `statusChangePersistsHistory` | US-4, US-5 | История из двух записей сохранена в PostgreSQL |
| `migrationsCategoriesAndHealth` | US-7 | Три миграции, столбец комментария, health `UP` |
| `emptySample` | US-8 | Пустая сводка: нули и `null` |
| `averageIsNullWithoutResolvedTickets` | US-8 | Нет завершённых — среднее `null` |
| `invalidPeriodReturns400` | US-8 | Равный, перевёрнутый и неполный период |
| `averageUsesClosedAtOfResolvedTicketsOnly` | US-8 | Среднее только по `RESOLVED` |
| `historyWriteFailureRollsBackStatusChange` | US-4 | Сбой записи истории откатывает статус, HTTP 500 в том же формате |

## Сквозные сценарии

| Тест | История | Что доказывает |
| --- | --- | --- |
| `fullLifecycleCreateEditResolveAndReport` | US-1…US-5, US-8 | Создание, правка, `IN_PROGRESS`, `RESOLVED`, история и сводка со средним 90 секунд |
| `forbiddenOperationDoesNotChangeStoredData` | US-3, US-4 | `NEW → RESOLVED` и повторное открытие закрытой заявки дают 409, данные и история не меняются |

## Регрессия

`ReportApiIntegrationTest.regressionD1_ticketCreatedExactlyAtPeriodEndIsExcluded` закрывает дефект D-1: заявка, созданная ровно в `to`, не входит в сводку. Описание — в [defects.md](defects.md).

## Граничные случаи из задания

| Случай | Тест |
| --- | --- |
| Пустая выборка отчёта | `emptySample`, `ReportServiceTest.emptySampleReturnsZerosAndNullAverage` |
| Нет завершённых заявок | `averageIsNullWithoutResolvedTickets` |
| Момент наступления срока | `OverduePolicyTest.exactDueInstantIsOverdue`, `filterPaginationAndOverdueBoundary` |
| Неверный период | `invalidPeriodReturns400` |
| Неизвестный идентификатор | `unknownTicketReturns404` |
