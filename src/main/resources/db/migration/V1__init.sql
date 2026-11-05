CREATE TABLE categories (
    id   UUID PRIMARY KEY,
    code VARCHAR(64)  NOT NULL UNIQUE,
    name VARCHAR(200) NOT NULL
);

CREATE TABLE tickets (
    id          UUID PRIMARY KEY,
    title       VARCHAR(200)  NOT NULL,
    description VARCHAR(4000),
    category_id UUID          NOT NULL REFERENCES categories (id),
    priority    VARCHAR(16)   NOT NULL,
    status      VARCHAR(32)   NOT NULL,
    created_at  TIMESTAMPTZ   NOT NULL,
    due_at      TIMESTAMPTZ,
    closed_at   TIMESTAMPTZ,
    CONSTRAINT tickets_priority_chk CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH')),
    CONSTRAINT tickets_status_chk CHECK (status IN ('NEW', 'IN_PROGRESS', 'RESOLVED', 'CANCELLED'))
);

CREATE INDEX tickets_status_idx ON tickets (status);
CREATE INDEX tickets_priority_idx ON tickets (priority);
CREATE INDEX tickets_created_at_idx ON tickets (created_at);
CREATE INDEX tickets_category_idx ON tickets (category_id);
CREATE INDEX tickets_due_at_idx ON tickets (due_at);

CREATE TABLE status_history (
    id          UUID PRIMARY KEY,
    ticket_id   UUID        NOT NULL REFERENCES tickets (id),
    from_status VARCHAR(32) NOT NULL,
    to_status   VARCHAR(32) NOT NULL,
    changed_at  TIMESTAMPTZ NOT NULL
);

CREATE INDEX status_history_ticket_idx ON status_history (ticket_id, changed_at);
