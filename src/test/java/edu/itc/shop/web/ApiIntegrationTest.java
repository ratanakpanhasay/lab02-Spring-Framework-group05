package edu.itc.shop.web;

import edu.itc.shop.repository.*;
import edu.itc.shop.service.DataSeeder;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.datasource.url=jdbc:h2:mem:task3_api;DB_CLOSE_DELAY=-1")
@ActiveProfiles("dev")
class ApiIntegrationTest {
    @Value("${local.server.port}") int port;
    @Autowired DataSeeder seeder;
    @Autowired CustomerRepository customers;
    @Autowired ProductRepository products;
    @Autowired OrderRepository orders;
    @Autowired TagRepository tags;

    private final HttpClient http = HttpClient.newHttpClient();
    private final JsonMapper json = JsonMapper.builder().build();

    private HttpResponse<String> request(String path) throws Exception {
        return http.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).GET().build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private JsonNode get(String path) throws Exception {
        var response = request(path);
        assertThat(response.statusCode()).isEqualTo(200);
        return json.readTree(response.body());
    }

    @Test
    void developmentSeedIsRepeatableWithThirtyOrdersAndTwoToFourLines() throws Exception {
        seeder.run(new DefaultApplicationArguments());
        assertThat(customers.count()).isEqualTo(3);
        assertThat(products.count()).isEqualTo(12);
        assertThat(orders.count()).isEqualTo(30);
        assertThat(tags.count()).isEqualTo(2);
        for (String email : new String[]{"dara@example.com", "sophea@example.com", "sokha@example.com"}) {
            JsonNode result = get("/api/customers/" + email + "/orders");
            assertThat(result.size()).isEqualTo(10);
            result.forEach(order -> assertThat(order.get("lines").size()).isBetween(2, 4));
        }
        assertThat(products.findByTagsName("dev-featured")).hasSize(12);
    }

    @Test
    void orderApiSupportsDefaultPagingStatusAndExplicitSortWithoutEntityFields() throws Exception {
        JsonNode defaults = get("/api/orders");
        assertThat(defaults.get("size").asInt()).isEqualTo(20);
        assertThat(defaults.get("totalElements").asInt()).isEqualTo(30);
        assertThat(defaults.get("content").size()).isEqualTo(20);
        Instant previous = Instant.MAX;
        for (JsonNode order : defaults.get("content")) {
            Instant timestamp = Instant.parse(order.get("placedAt").asText());
            assertThat(timestamp).isBeforeOrEqualTo(previous);
            previous = timestamp;
            assertThat(order.has("lines")).isFalse();
            assertThat(order.has("version")).isFalse();
            assertThat(order.get("customer").size()).isEqualTo(1);
            assertThat(order.get("customer").has("name")).isTrue();
        }
        JsonNode paid = get("/api/orders?status=PAID&page=0&size=2&sort=placedAt,asc");
        assertThat(paid.get("totalElements").asInt()).isEqualTo(5);
        assertThat(paid.get("totalPages").asInt()).isEqualTo(3);
        assertThat(paid.get("content").size()).isEqualTo(2);
        paid.get("content").forEach(order -> assertThat(order.get("status").asText()).isEqualTo("PAID"));
        assertThat(Instant.parse(paid.get("content").get(0).get("placedAt").asText()))
                .isBefore(Instant.parse(paid.get("content").get(1).get("placedAt").asText()));
        assertThat(get("/api/orders?status=PAID&page=2&size=2").get("content").size()).isEqualTo(1);
        assertThat(request("/api/orders?status=INVALID").statusCode()).isEqualTo(400);
    }

    @Test
    void productApiCombinesFiltersAndSerializesOnlyRecordFields() throws Exception {
        JsonNode result = get("/api/products?q=KEY&category=HARDWARE&maxPrice=60&inStock=true&page=0&size=1");
        assertThat(result.get("totalElements").asInt()).isEqualTo(2);
        assertThat(result.get("totalPages").asInt()).isEqualTo(2);
        JsonNode product = result.get("content").get(0);
        assertThat(product.size()).isEqualTo(5);
        assertThat(product.get("sku").asText()).isEqualTo("DEV-001");
        assertThat(product.get("amount").decimalValue()).isEqualByComparingTo("55.00");
        assertThat(product.get("stock").asInt()).isPositive();
        assertThat(get("/api/products").get("totalElements").asInt()).isEqualTo(12);
        assertThat(get("/api/products?inStock=false").get("totalElements").asInt()).isEqualTo(3);
        assertThat(get("/api/products?q=absent").get("content").size()).isZero();
        assertThat(get("/api/products?q=key&category=HARDWARE&maxPrice=60&inStock=true&page=1&size=1")
                .get("content").get(0).get("sku").asText()).isEqualTo("DEV-002");
        assertThat(request("/api/products?category=INVALID").statusCode()).isEqualTo(400);
    }

    @Test
    void customerOrdersIncludeLineSnapshotsAndTotalsWithOpenInViewDisabled() throws Exception {
        JsonNode result = get("/api/customers/dara@example.com/orders");
        assertThat(result.size()).isEqualTo(10);
        for (JsonNode order : result) {
            assertThat(order.get("customerEmail").asText()).isEqualTo("dara@example.com");
            java.math.BigDecimal total = java.math.BigDecimal.ZERO;
            for (JsonNode line : order.get("lines")) {
                assertThat(line.get("sku").asText()).startsWith("DEV-");
                assertThat(line.get("productName").asText()).isNotBlank();
                assertThat(line.has("order")).isFalse();
                total = total.add(line.get("unitPrice").get("amount").decimalValue()
                        .multiply(java.math.BigDecimal.valueOf(line.get("quantity").asInt())));
            }
            assertThat(order.get("total").get("amount").decimalValue()).isEqualByComparingTo(total);
        }
        assertThat(get("/api/customers/missing@example.com/orders").size()).isZero();
    }
}
