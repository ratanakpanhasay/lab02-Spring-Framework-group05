CREATE SEQUENCE customer_seq
    START WITH 1 INCREMENT BY 50;

CREATE SEQUENCE product_seq
    START WITH 1 INCREMENT BY 50;

CREATE SEQUENCE tag_seq
    START WITH 1 INCREMENT BY 50;

CREATE SEQUENCE order_seq
    START WITH 1 INCREMENT BY 50;

CREATE SEQUENCE order_line_seq
    START WITH 1 INCREMENT BY 50;

    CREATE TABLE customer (
    id BIGINT PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    street VARCHAR(255),
    city VARCHAR(100),
    country VARCHAR(100),
    postal_code VARCHAR(30)
);

CREATE TABLE product (
    id BIGINT PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    price_amount DECIMAL(19,2) NOT NULL,
    price_currency VARCHAR(3) NOT NULL,
    category VARCHAR(50) NOT NULL
);

CREATE TABLE tag (
    id   BIGINT       PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE orders (
    id BIGINT PRIMARY KEY,
    status VARCHAR(20) NOT NULL,
    placed_at TIMESTAMP NOT NULL,
    version BIGINT NOT NULL,
    customer_id BIGINT NOT NULL REFERENCES customer(id)
);

CREATE TABLE order_line (
    id BIGINT PRIMARY KEY,
    order_id BIGINT NOT NULL REFERENCES orders(id),
    product_id BIGINT NOT NULL REFERENCES product(id),
    quantity INT NOT NULL CHECK (quantity > 0),
    unit_price_amount DECIMAL(19,2) NOT NULL,
    unit_price_currency VARCHAR(3) NOT NULL
);

CREATE TABLE product_tag (
    product_id BIGINT NOT NULL,
    tag_id     BIGINT NOT NULL,

    PRIMARY KEY (product_id, tag_id),

    FOREIGN KEY (product_id) REFERENCES product(id),
    FOREIGN KEY (tag_id)     REFERENCES tag(id)
);
