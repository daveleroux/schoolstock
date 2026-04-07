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
    id              BIGSERIAL    PRIMARY KEY,
    name            VARCHAR(255) NOT NULL,
    description     TEXT,
    stock_quantity  INTEGER      NOT NULL DEFAULT 0 CHECK (stock_quantity >= 0),
    available_stock INTEGER      NOT NULL DEFAULT 0 CHECK (available_stock >= 0),
    provisional     BOOLEAN      NOT NULL DEFAULT FALSE,
    search_vector   TSVECTOR
);

-- Migration helper: set available_stock = stock_quantity for rows that were
-- inserted before this column existed (safe to re-run; only touches rows where
-- they differ and no sub-orders have already reserved stock).
-- UPDATE items SET available_stock = stock_quantity WHERE available_stock = 0 AND stock_quantity > 0;

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

-- orders -------------------------------------------------------
CREATE TABLE IF NOT EXISTS orders (
    id         BIGSERIAL   PRIMARY KEY,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    user_id    BIGINT      NOT NULL REFERENCES users(id)
);

-- sub_orders ---------------------------------------------------
CREATE TABLE IF NOT EXISTS sub_orders (
    id              BIGSERIAL   PRIMARY KEY,
    order_id        BIGINT      NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    state           VARCHAR(50) NOT NULL,
    version         BIGINT      NOT NULL DEFAULT 0,
    sequence_number INTEGER     NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_sub_orders_state ON sub_orders(state);

-- sub_order_items ----------------------------------------------
CREATE TABLE IF NOT EXISTS sub_order_items (
    id           BIGSERIAL PRIMARY KEY,
    sub_order_id BIGINT    NOT NULL REFERENCES sub_orders(id) ON DELETE CASCADE,
    item_id      BIGINT    NOT NULL REFERENCES items(id),
    quantity     INTEGER   NOT NULL CHECK (quantity > 0)
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
