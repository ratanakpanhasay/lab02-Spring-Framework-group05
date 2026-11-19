package edu.itc.shop.api.filters;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.glassfish.jersey.test.JerseyTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import edu.itc.shop.Main;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Application;

class ApiKeysPropertyTest extends JerseyTest {

    @Override
    protected Application configure() {
        System.setProperty(ApiKeyFilter.KEYS_PROPERTY, "alpha-1, beta-2");
        return Main.config();
    }

    @AfterAll
    static void clearKeys() {
        System.clearProperty(ApiKeyFilter.KEYS_PROPERTY);
    }

    private int create(String key, String sku) {
        return target("products").request()
                .header(ApiKeyFilter.HEADER, key)
                .post(Entity.json("{\"name\":\"Cup\",\"sku\":\"" + sku + "\",\"price\":3.00,\"stock\":1}"))
                .getStatus();
    }

    @Test
    void everyConfiguredKeyIsAccepted() {
        assertEquals(201, create("alpha-1", "CP-01"));
        assertEquals(201, create("beta-2", "CP-02"));
    }

    @Test
    void defaultKeyIsRejectedWhenPropertyIsSet() {
        assertEquals(401, create("demo-123", "CP-03"));
    }
}
