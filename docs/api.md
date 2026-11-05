# Описание API

Базовый URL локального запуска: `http://localhost:8080`.

Интерактивная схема: `http://localhost:8080/swagger-ui.html`. Коллекция запросов: `http/support-api.http` и `http/support-api.postman_collection.json`.

Даты передаются в ISO-8601, например `2026-01-20T12:00:00Z`.

## Единый формат ошибки

```json
{
  "timestamp": "2026-01-15T12:00:00Z",
  "status": 409,
  "error": "Conflict",
  "message": "Переход статуса RESOLVED → IN_PROGRESS запрещён",
  "path": "/api/tickets/2f6e2a0e-1c1a-4d4e-9a1b-6d0e1a2b3c4d/status",
  "violations": []
}
```

Для ошибки валидации `message` равен «Ошибка валидации», а `violations` содержит поля и тексты.

## Категории

`GET /api/categories`

Ответ — массив. Идентификаторы фиксированы миграцией:

| code | id | name |
| --- | --- | --- |
| hardware | `11111111-1111-1111-1111-111111111111` | Оборудование |
| software | `22222222-2222-2222-2222-222222222222` | Программное обеспечение |
| network | `33333333-3333-3333-3333-333333333333` | Сеть |
| access | `44444444-4444-4444-4444-444444444444` | Доступ |
| other | `55555555-5555-5555-5555-555555555555` | Прочее |

## Создать заявку

`POST /api/tickets`

```json
{
  "title": "Не печатает принтер",
  "description": "Кабинет 214",
  "categoryId": "11111111-1111-1111-1111-111111111111",
  "priority": "HIGH",
  "dueAt": "2026-01-20T12:00:00Z"
}
```

Ответ `201`:

```json
{
  "id": "2f6e2a0e-1c1a-4d4e-9a1b-6d0e1a2b3c4d",
  "title": "Не печатает принтер",
  "description": "Кабинет 214",
  "category": {
    "id": "11111111-1111-1111-1111-111111111111",
    "code": "hardware",
    "name": "Оборудование"
  },
  "priority": "HIGH",
  "status": "NEW",
  "createdAt": "2026-01-15T12:00:00Z",
  "dueAt": "2026-01-20T12:00:00Z",
  "closedAt": null,
  "overdue": false
}
```

`description` и `dueAt` можно не передавать. Заголовок обрезается по краям. Статус в запросе не принимается.

## Получить заявку

`GET /api/tickets/{id}`

Тело совпадает с ответом создания. Неизвестный id — `404`.

## Изменить описание, приоритет и срок

`PATCH /api/tickets/{id}`

Тело — полная карточка трёх редактируемых полей. Приоритет обязателен. `null` в `description` или `dueAt` очищает значение. Заголовок, категория и статус этим методом не меняются.

```json
{
  "description": "Кабинет 214, бумага замята",
  "priority": "MEDIUM",
  "dueAt": "2026-01-21T09:00:00Z"
}
```

Для заявки в статусе `RESOLVED` или `CANCELLED` ответ `409`.

## Сменить статус

`POST /api/tickets/{id}/status`

```json
{
  "status": "IN_PROGRESS",
  "comment": "Взял в работу"
}
```

`comment` необязателен, не длиннее 500 символов.

Допустимые переходы:

- `NEW` → `IN_PROGRESS` или `CANCELLED`
- `IN_PROGRESS` → `RESOLVED` или `CANCELLED`

Остальные переходы, включая повтор того же статуса, дают `409`. При входе в `RESOLVED` или `CANCELLED` заполняется `closedAt`.

## История

`GET /api/tickets/{id}/history`

```json
[
  {
    "id": "8c1d9b2a-4e5f-6a7b-8c9d-0e1f2a3b4c5d",
    "fromStatus": "NEW",
    "toStatus": "IN_PROGRESS",
    "changedAt": "2026-01-15T12:30:00Z",
    "comment": "Взял в работу"
  }
]
```

## Список

`GET /api/tickets`

Параметры необязательны, кроме значений по умолчанию у страницы:

| Параметр | Значение |
| --- | --- |
| status | `NEW`, `IN_PROGRESS`, `RESOLVED`, `CANCELLED` |
| priority | `LOW`, `MEDIUM`, `HIGH` |
| categoryId | UUID категории |
| overdue | `true` или `false` |
| title | фрагмент заголовка без учёта регистра |
| page | номер с нуля, по умолчанию 0 |
| size | от 1 до 100, по умолчанию 20 |
| sort | `createdAt,desc` по умолчанию. Поля: `createdAt`, `dueAt`, `priority`, `status`, `title`. Направление: `asc` или `desc` |

```json
{
  "content": [],
  "page": 0,
  "size": 20,
  "totalElements": 0,
  "totalPages": 0
}
```

Пример: `GET /api/tickets?status=NEW&overdue=true&page=0&size=20&sort=dueAt,asc`.

## Сводка

`GET /api/reports/summary?from=2026-01-01T00:00:00Z&to=2026-02-01T00:00:00Z`

Период полуоткрытый: заявки с датой создания от `from` включительно до `to` не включая `to`.

```json
{
  "createdCount": 4,
  "byStatus": {
    "NEW": 1,
    "IN_PROGRESS": 1,
    "RESOLVED": 1,
    "CANCELLED": 1
  },
  "byPriority": {
    "LOW": 2,
    "MEDIUM": 1,
    "HIGH": 1
  },
  "overdueOpenCount": 1,
  "averageResolutionSeconds": 5400.0
}
```

`averageResolutionSeconds` равен `null`, если в выборке нет заявок `RESOLVED` с заполненным временем закрытия. Показатель считается как среднее `(closedAt - createdAt)` только по таким заявкам. `overdueOpenCount` использует то же правило просрочки, что и карточка заявки, и тот же текущий момент времени.

Если `from` не раньше `to`, ответ `400` с текстом «Начало периода должно быть раньше конца».

## Проверка работоспособности

`GET /actuator/health` — общий статус. Для оркестратора: `GET /actuator/health/readiness` и `GET /actuator/health/liveness`. Пока база недоступна, readiness равен `DOWN`.
