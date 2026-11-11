package edu.itc.shop.repository;

import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class ProductSchemaMigrationTest {
    @Test
    void upgradesPopulatedV1WithoutChangingItAndEnforcesConstraints() throws Exception {
        String url = "jdbc:h2:mem:migration_" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1";
        Flyway.configure().dataSource(url, "sa", "").target("1").load().migrate();
        try (var connection = DriverManager.getConnection(url, "sa", "");
             var sql = connection.createStatement()) {
            sql.executeUpdate("""
                    insert into product(id, name, price_amount, price_currency, category)
                    values (1, 'Existing product', 10.00, 'USD', 'OTHER'),
                           (2, 'Another existing product', 20.00, 'USD', 'BOOK')
                    """);
            var result = Flyway.configure().dataSource(url, "sa", "").load().migrate();
            assertThat(result.migrationsExecuted).isEqualTo(1);
            try (var rows = sql.executeQuery("select sku, stock from product order by id")) {
                assertThat(rows.next()).isTrue();
                assertThat(rows.getString("sku")).isEqualTo("LEGACY-1");
                assertThat(rows.getInt("stock")).isZero();
                assertThat(rows.next()).isTrue();
                assertThat(rows.getString("sku")).isEqualTo("LEGACY-2");
                assertThat(rows.next()).isFalse();
            }
            assertThatThrownBy(() -> sql.executeUpdate("update product set stock = -1 where id = 1"))
                    .isInstanceOf(SQLException.class);
            assertThatThrownBy(() -> sql.executeUpdate("update product set sku = 'LEGACY-1' where id = 2"))
                    .isInstanceOf(SQLException.class);
            assertThatThrownBy(() -> sql.executeUpdate("update product set sku = null where id = 1"))
                    .isInstanceOf(SQLException.class);
            assertThat(Flyway.configure().dataSource(url, "sa", "").load().migrate().migrationsExecuted).isZero();
        }
    }
}
