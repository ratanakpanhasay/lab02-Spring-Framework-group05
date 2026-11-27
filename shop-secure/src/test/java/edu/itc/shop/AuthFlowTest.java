package edu.itc.shop;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.datasource.url=jdbc:h2:mem:authflow-test",
                "shop.security.jwt-secret=test-secret-key-that-is-at-least-32-characters-long",
                "shop.seed.admin-password=Admin-pass-123",
                "shop.seed.customer-password=Alice-pass-123"
        }
)
class AuthFlowTest {

    @LocalServerPort
    private int port;

    private final HttpClient client = HttpClient.newHttpClient();

    private String baseUrl() {
        return "http://localhost:" + port;
    }

    @Test
    void loginWithCorrectCredentialsReturnsToken() throws Exception {

        String body = """
                {
                    "username": "admin",
                    "password": "Admin-pass-123"
                }
                """;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl() + "/api/auth/login"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = client.send(
                request,
                HttpResponse.BodyHandlers.ofString()
        );

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("\"token\"");
        assertThat(response.body()).contains("\"Bearer\"");
    }

    @Test
    void loginWithWrongPasswordReturns401() throws Exception {

        String body = """
                {
                    "username": "admin",
                    "password": "wrong-password"
                }
                """;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl() + "/api/auth/login"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = client.send(
                request,
                HttpResponse.BodyHandlers.ofString()
        );

        assertThat(response.statusCode()).isEqualTo(401);
    }

    @Test
    void requestWithoutTokenReturns401() throws Exception {

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl() + "/api/orders"))
                .GET()
                .build();

        HttpResponse<String> response = client.send(
                request,
                HttpResponse.BodyHandlers.ofString()
        );

        assertThat(response.statusCode()).isEqualTo(401);
    }

    @Test
    void authenticatedRequestWithTokenSucceeds() throws Exception {

        // Login first
        String loginBody = """
                {
                    "username": "admin",
                    "password": "Admin-pass-123"
                }
                """;

        HttpRequest loginRequest = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl() + "/api/auth/login"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(loginBody))
                .build();

        HttpResponse<String> loginResponse = client.send(
                loginRequest,
                HttpResponse.BodyHandlers.ofString()
        );

        assertThat(loginResponse.statusCode()).isEqualTo(200);

        // Extract the JWT from the JSON response
        String token = loginResponse.body()
                .split("\"token\":\"")[1]
                .split("\"")[0];

        // Use the JWT to access a protected endpoint
        HttpRequest ordersRequest = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl() + "/api/orders"))
                .header("Authorization", "Bearer " + token)
                .GET()
                .build();

        HttpResponse<String> ordersResponse = client.send(
                ordersRequest,
                HttpResponse.BodyHandlers.ofString()
        );

        assertThat(ordersResponse.statusCode()).isEqualTo(200);
    }
}