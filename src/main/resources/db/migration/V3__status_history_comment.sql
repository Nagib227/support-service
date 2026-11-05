-- Добавление столбца совместимо с предыдущей версией приложения:
-- старый код его не читает, ограничение NOT NULL не накладывается.
ALTER TABLE status_history
    ADD COLUMN change_comment VARCHAR(500);
