--liquibase formatted sql

--changeset dzarembo:003
CREATE TABLE order_items (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL,
    item_id BIGINT NOT NULL,
    quantity INTEGER NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_order_items_orders FOREIGN KEY (order_id) REFERENCES orders (id),
    CONSTRAINT fk_order_items_items FOREIGN KEY (item_id) REFERENCES items (id)
);

--rollback DROP TABLE IF EXISTS order_items;
