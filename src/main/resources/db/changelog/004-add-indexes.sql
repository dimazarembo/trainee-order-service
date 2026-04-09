--liquibase formatted sql

--changeset dzarembo:004
CREATE INDEX idx_items_name ON items (name);
CREATE INDEX idx_orders_user_id ON orders (user_id);
CREATE INDEX idx_orders_status ON orders (status);
CREATE INDEX idx_order_items_order_id ON order_items (order_id);
CREATE INDEX idx_order_items_item_id ON order_items (item_id);

--rollback DROP INDEX IF EXISTS idx_order_items_item_id;
--rollback DROP INDEX IF EXISTS idx_order_items_order_id;
--rollback DROP INDEX IF EXISTS idx_orders_status;
--rollback DROP INDEX IF EXISTS idx_orders_user_id;
--rollback DROP INDEX IF EXISTS idx_items_name;
