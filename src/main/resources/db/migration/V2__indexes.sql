-- Task 5: Performance indexes

CREATE INDEX idx_orders_status_placed
ON orders(status, placed_at DESC);

CREATE INDEX idx_orders_customer_placed
ON orders(customer_id, placed_at DESC);

CREATE INDEX idx_order_line_order_product
ON order_line(order_id, product_id);