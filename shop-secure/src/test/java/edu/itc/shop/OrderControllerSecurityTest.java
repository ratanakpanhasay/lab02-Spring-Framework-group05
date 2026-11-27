package edu.itc.shop;

import edu.itc.shop.config.MethodSecurityConfig;
import edu.itc.shop.config.SecurityConfig;
import edu.itc.shop.domain.Order;
import edu.itc.shop.repository.OrderRepository;
import edu.itc.shop.service.OrderService;
import edu.itc.shop.web.ApiExceptionHandler;
import edu.itc.shop.web.OrderController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrderController.class)
@ContextConfiguration(classes = {OrderController.class, ApiExceptionHandler.class})
@Import({SecurityConfig.class, MethodSecurityConfig.class, OrderService.class})
class OrderControllerSecurityTest {

    @Autowired
    private MockMvc mvc;

    // Keep the real, method-secured service in the MVC slice.
    @MockitoBean
    private OrderRepository repository;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private JwtAuthenticationConverter jwtAuthenticationConverter;

    @Test
    void anonymousRequestIsUnauthorized() throws Exception {
        mvc.perform(get("/api/orders"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(repository);
    }

    @Test
    @WithMockUser(username = "alice", roles = "CUSTOMER")
    void customerCannotShipAnOrder() throws Exception {
        mvc.perform(post("/api/orders/1/ship"))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(content().json("""
                        {
                          "title": "Forbidden",
                          "status": 403,
                          "detail": "Access denied",
                          "instance": "/api/orders/1/ship"
                        }
                        """, JsonCompareMode.STRICT));

        verifyNoInteractions(repository);
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void adminCanShipAnOrder() throws Exception {
        var order = new Order("alice", 1, 2);
        when(repository.findById(1L)).thenReturn(Optional.of(order));

        mvc.perform(post("/api/orders/1/ship"))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        assertThat(order.getStatus()).isEqualTo(Order.Status.SHIPPED);
    }
}
