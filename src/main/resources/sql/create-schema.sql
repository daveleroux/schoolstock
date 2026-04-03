-- =============================================================
--  Schoolstock schema
--  Run once against an empty database before starting the app.
-- =============================================================

-- users -------------------------------------------------------
CREATE TABLE IF NOT EXISTS users (
    id       BIGSERIAL    PRIMARY KEY,
    username VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL
);

CREATE TABLE IF NOT EXISTS user_roles (
    user_id BIGINT      NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role    VARCHAR(50) NOT NULL,
    PRIMARY KEY (user_id, role)
);

-- items -------------------------------------------------------
CREATE TABLE IF NOT EXISTS items (
    id            BIGSERIAL    PRIMARY KEY,
    name           VARCHAR(255) NOT NULL,
    description    TEXT,
    stock_quantity INTEGER      NOT NULL DEFAULT 0 CHECK (stock_quantity >= 0),
    provisional    BOOLEAN      NOT NULL DEFAULT FALSE,
    search_vector  TSVECTOR
);

-- orderer_approvers -------------------------------------------
CREATE TABLE IF NOT EXISTS orderer_approvers (
    orderer_id  BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    approver_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    PRIMARY KEY (orderer_id, approver_id)
);

-- cart_items --------------------------------------------------
CREATE TABLE IF NOT EXISTS cart_items (
    id       BIGSERIAL PRIMARY KEY,
    user_id  BIGINT    NOT NULL REFERENCES users(id)  ON DELETE CASCADE,
    item_id  BIGINT    NOT NULL REFERENCES items(id)  ON DELETE CASCADE,
    quantity INTEGER   NOT NULL DEFAULT 1 CHECK (quantity > 0),
    UNIQUE (user_id, item_id)
);

-- GIN index for full-text search on items
CREATE INDEX IF NOT EXISTS idx_items_search_vector
    ON items USING GIN(search_vector);

-- Trigger: keep search_vector in sync with name + description
CREATE OR REPLACE FUNCTION items_search_vector_update()
RETURNS trigger AS $$
BEGIN
    NEW.search_vector := to_tsvector(
        'english',
        coalesce(NEW.name, '') || ' ' || coalesce(NEW.description, '')
    );
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_items_search_vector ON items;
CREATE TRIGGER trg_items_search_vector
    BEFORE INSERT OR UPDATE ON items
    FOR EACH ROW EXECUTE FUNCTION items_search_vector_update();
