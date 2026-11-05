# Сервис заявок технической поддержки

Один сервис на Java и Kotlin: регистрация заявок, статусы, история, поиск и сводка за период. База — PostgreSQL, схема — Flyway, API описан в OpenAPI.

Документы практики: [спецификация](docs/specification.md), [backlog](docs/backlog.md), [API](docs/api.md), [тест-план](docs/test-plan.md), [дефекты](docs/defects.md), [отчёт](docs/report.md).

## Зафиксированные версии

| Инструмент | Версия |
| --- | --- |
| Java | 21, bytecode release 21. Локальная сборка допускается на более новом JDK |
| Kotlin | 2.2.21 |
| Spring Boot | 3.5.16 |
| Gradle | 8.14.4, Wrapper в репозитории |
| PostgreSQL | 16 |
| Flyway | 11.7.2 из BOM Spring Boot |
| JUnit Jupiter | 5.12.2 из BOM |
| Testcontainers | 1.21.4 из BOM |
| springdoc-openapi | 2.8.16 |

Секретов в репозитории нет. Образец переменных — `.env.example`.

## Что нужно заранее

- JDK 21 или новее
- Docker Desktop с работающим Linux-движком (для Compose и Testcontainers)
- Git

Проверка движка: `docker version` показывает и Client, и Server. Если Server отвечает ошибкой 500, откройте Docker Desktop и дождитесь статуса Engine running. На этой машине для Linux-контейнеров нужен WSL2: `wsl --status` не должен сообщать об отсутствии подсистемы.

## Запуск

Из корня репозитория:

```powershell
Copy-Item .env.example .env
docker compose up -d --build --wait
```

Сервис: `http://localhost:8080`. Swagger: `http://localhost:8080/swagger-ui.html`. Проверка сценария после старта:

```powershell
.\scripts\smoke.ps1
```

Миграции применяются сами при старте приложения. Логи приложения и базы видны через `docker compose logs -f app` и пишутся в stdout контейнера.

Остановка без удаления данных: `docker compose down`. Том `pgdata` при этом сохраняется. Команда `docker compose down -v` удаляет данные.

Демонстрационные заявки, не участвующие в автотестах:

```powershell
docker compose cp .\scripts\demo-data.sql postgres:/tmp/demo-data.sql
docker compose exec -T postgres psql -U support -d support -f /tmp/demo-data.sql
```

Повторный запуск этого файла упрётся в уже занятые идентификаторы.

Запуск без Docker, если PostgreSQL уже слушает `localhost:5432` и есть база `support` с пользователем `support`:

```powershell
.\gradlew.bat bootRun
```

Адрес и пароль базы переопределяются переменными `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`.

## Тесты

```powershell
.\gradlew.bat test
.\gradlew.bat jacocoTestReport
```

HTML-покрытие: `build/reports/jacoco/test/html/index.html`. Интеграционные тесты сами поднимают PostgreSQL через Testcontainers и не зависят от базы из Compose.

## Сохранность данных и резервная копия

1. Создайте заявку через Swagger или `http/support-api.http`.
2. `docker compose restart`.
3. Повторите `GET /api/tickets/{id}`: заявка на месте, потому что данные лежат в томе `pgdata`.

Копия рабочей базы:

```powershell
.\scripts\backup.ps1
```

Файл появится в `backups/`. Этот каталог не коммитится.

Восстановление идёт в отдельную базу `support_restore_test` и не перезаписывает рабочую `support`:

```powershell
.\scripts\restore.ps1 .\backups\support-YYYYMMDD-HHMMSS.sql
```

Проверка числа заявок в копии выполняется скриптом в конце. На Linux те же действия делают `scripts/backup.sh` и `scripts/restore.sh`.

## Откат к предыдущему образу

Образы помечаются полным SHA коммита.

```powershell
docker build -t support-service:<старый-sha> .
.\scripts\rollback.ps1 -Tag <старый-sha>
```

Скрипт поднимает Compose с `APP_IMAGE=support-service:<sha>` без пересборки и снова выполняет smoke-проверку. В GitHub то же действие запускается вручную workflow `Rollback`: он скачивает образ `ghcr.io/<owner>/support-service:<sha>`, проверяет его и при заданных секретах стенда выкладывает этот тег.

Миграции совместимы с предыдущим приложением: они не удаляют и не переименовывают столбцы. `V3` только добавляет необязательный `change_comment`.

## Диагностика

### База недоступна

Признак: приложение не проходит readiness, в `docker compose logs app` есть `Connection refused` или `UnknownHost`. `GET /actuator/health` показывает компонент `db` в состоянии `DOWN`.

Что сделать:

1. `docker compose ps` — у `postgres` статус не `healthy`.
2. `docker compose logs postgres` — смотрите пароль, инициализацию тома и место на диске.
3. Сверьте `SPRING_DATASOURCE_URL` с именем сервиса `postgres`, базой и пользователем из Compose.
4. После исправления: `docker compose up -d --wait`. Данные в томе сохраняются.

### Приложение не проходит health check

Признак: контейнер `app` в статусе `unhealthy` или `restarting`, `curl http://localhost:8080/actuator/health/readiness` не возвращает `UP`.

Что сделать:

1. `docker compose logs app --tail 100` — исключение на старте или ответ базы `DOWN`.
2. Если база `healthy`, а приложение нет, проверьте, что в образ попал актуальный jar и порт 8080 не занят другим процессом на хосте.
3. `docker compose up -d --wait --no-build` после смены образа. Liveness падает при зависании процесса, readiness — когда сервис ещё не готов обслуживать запросы или не видит базу.

### Миграция завершается ошибкой

Признак: в логе `FlywayException` / `Migration ... failed`, приложение не стартует, readiness не становится `UP`.

Что сделать:

1. Найдите версию в логе и текст SQL в `src/main/resources/db/migration`.
2. `docker compose exec postgres psql -U support -d support -c "select version, success from flyway_schema_history order by installed_rank"`.
3. Неудачная миграция PostgreSQL у Flyway откатывается. Исправьте скрипт. Если Flyway уже записал неуспешную строку, удалите только её после исправления SQL, не переписывая применённые версии.
4. Не удаляйте том, если в нём уже есть заявки. Новая миграция должна добавлять объекты, а не менять старые так, чтобы предыдущий образ перестал читать схему.

## CI/CD

Файл `.github/workflows/ci.yml`.

- Pull request: получение кода, JDK 21, проверка форматирования Spotless, тесты, выгрузка отчётов JUnit и JaCoCo. Публикации и развёртывания нет.
- Слияние в `main`: те же проверки, затем сборка образа. Образ запускается через Compose, smoke-скрипт создаёт заявку, завершает её и читает сводку. Только после этого образ публикуется в GHCR с тегом SHA коммита.
- Если тесты или smoke падают, публикация не выполняется.
- Секреты стенда не хранятся в репозитории: `DEPLOY_HOST`, `DEPLOY_USER`, `DEPLOY_SSH_KEY`, `DEPLOY_PATH`. Пока их нет, внешний SSH-шаг пишет, что стенд не задан, и не подменяет собой уже выполненную проверку запуска образа.
- Пароль GHCR — встроенный `GITHUB_TOKEN`.

Локальная имитация поставки на эту машину: `.\scripts\deploy-local.ps1`.

Ветки: задача ведётся в отдельной ветке, в `main` попадает через pull request. Для первого снимка репозитория история может состоять из одного коммита; дальнейшие изменения — через ветку и PR, чтобы сработал путь без развёртывания.

## Сценарий защиты

1. Показать критерий про HTTP 409 в спецификации, `TicketService.changeStatus` и тест `forbiddenOperationDoesNotChangeStoredData`.
2. Создать заявку, перевести в `IN_PROGRESS`, затем в `RESOLVED`, открыть `GET /api/reports/summary`.
3. Объяснить вызов: Java `ReportController` → Kotlin `ReportService` → Java `TicketReportQuery`.
4. Повторить переход закрытой заявки в работу и показать, что статус и история не изменились.
5. Открыть успешный прогон https://github.com/Nagib227/support-service/actions/runs/41992262995 и назвать образ `ghcr.io/nagib227/support-service:3713ad291af47c51fc73118471d814055cab5093`. Для отката запустить workflow `Rollback` с этим тегом.
6. Разобрать один из трёх сбоев выше.

## Состав репозитория

| Путь | Содержание |
| --- | --- |
| `src/main/java` | Заявки, категории, ошибки, Java-граница отчёта |
| `src/main/kotlin` | Модель и расчёт сводки |
| `src/main/resources/db/migration` | Схема, категории, комментарий истории |
| `src/test` | Модульные, интеграционные и сквозные тесты |
| `http` | Запросы для ручной проверки |
| `scripts` | Smoke, копия, восстановление, локальный деплой и откат |
| `.github/workflows` | CI и откат образа |
