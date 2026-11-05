-- Демонстрационные заявки. Категории уже загружены миграцией V2.
-- Не входит в автоматические тесты. Загрузка описана в README.

INSERT INTO tickets (id, title, description, category_id, priority, status, created_at, due_at, closed_at) VALUES
    ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa1', 'Не печатает принтер', 'Кабинет 214',
     '11111111-1111-1111-1111-111111111111', 'HIGH', 'NEW',
     TIMESTAMPTZ '2026-01-10 09:00:00+00', TIMESTAMPTZ '2026-01-12 09:00:00+00', NULL),
    ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2', 'Нет доступа к почте', 'Учётная запись ivanov',
     '44444444-4444-4444-4444-444444444444', 'MEDIUM', 'IN_PROGRESS',
     TIMESTAMPTZ '2026-01-11 10:00:00+00', TIMESTAMPTZ '2026-01-18 10:00:00+00', NULL),
    ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa3', 'Обновить офисный пакет', NULL,
     '22222222-2222-2222-2222-222222222222', 'LOW', 'RESOLVED',
     TIMESTAMPTZ '2026-01-08 08:00:00+00', TIMESTAMPTZ '2026-01-15 08:00:00+00',
     TIMESTAMPTZ '2026-01-08 09:30:00+00');

INSERT INTO status_history (id, ticket_id, from_status, to_status, changed_at, change_comment) VALUES
    ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb1', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2',
     'NEW', 'IN_PROGRESS', TIMESTAMPTZ '2026-01-11 11:00:00+00', 'Взял в работу'),
    ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb2', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa3',
     'NEW', 'IN_PROGRESS', TIMESTAMPTZ '2026-01-08 08:20:00+00', NULL),
    ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb3', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa3',
     'IN_PROGRESS', 'RESOLVED', TIMESTAMPTZ '2026-01-08 09:30:00+00', 'Пакет обновлён');
