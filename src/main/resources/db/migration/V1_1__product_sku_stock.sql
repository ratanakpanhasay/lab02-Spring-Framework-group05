-- Run after V1 and before Task 5's reserved V2__indexes.sql.
ALTER TABLE product ADD COLUMN sku VARCHAR(64);
ALTER TABLE product ADD COLUMN stock INT DEFAULT 0 NOT NULL;

-- Existing product IDs are unique, so this also upgrades populated V1 databases.
UPDATE product SET sku = 'LEGACY-' || CAST(id AS VARCHAR(20));
ALTER TABLE product ALTER COLUMN sku SET NOT NULL;
ALTER TABLE product ADD CONSTRAINT uk_product_sku UNIQUE (sku);
ALTER TABLE product ADD CONSTRAINT ck_product_stock CHECK (stock >= 0);
