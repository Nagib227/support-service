# Покрытие

Отчёт JaCoCo строится командой `.\gradlew.bat test jacocoTestReport`.

HTML: `build/reports/jacoco/test/html/index.html`. В CI тот же каталог публикуется артефактом `test-and-coverage` вместе с отчётом JUnit.

В бизнес-логику входят пакеты `ru.ithub.support.ticket`, `ru.ithub.support.report` и `ru.ithub.support.category`. Класс запуска и конфигурация OpenAPI на оценку правил не влияют.

Прогон 43 тестов, включая PostgreSQL через Testcontainers, 5 ноября 2026:

| Область | Инструкции | Ветки |
| --- | --- | --- |
| `ru.ithub.support.ticket` | 90,3% (884 из 979) | 76,2% (77 из 101) |
| `ru.ithub.support.report` | 100% (246 из 246) | 100% (12 из 12) |
| `ru.ithub.support.category` | 100% (60 из 60) | ветвлений нет |
| `ru.ithub.support.error` | 92,8% (141 из 152) | ветвлений нет |
| весь проект | 93,1% (1507 из 1618) | 78,8% (89 из 113) |

Непокрытые ветки в `ticket` — это защитные проверки длины полей и сортировки, которые на HTTP-границе отсекает валидация до входа в сервис. Расчёт сводки на Kotlin покрыт полностью.
