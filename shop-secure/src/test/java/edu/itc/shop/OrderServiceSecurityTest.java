package edu.itc.shop;

import edu.itc.shop.domain.Order;
import edu.itc.shop.repository.OrderRepository;
import edu.itc.shop.service.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@SpringBootTest
class OrderServiceSecurityTest {

    @Autowired
    private OrderService service;

    @MockitoBean
    private OrderRepository repository;

    @Test
    @WithMockUser(username = "alice", roles = "CUSTOMER")
    void aliceCanReadHerHistory() {
        var order = new Order("alice", 1, 2);
        when(repository.findByCustomer("alice")).thenReturn(List.of(order));

        assertThat(service.history("alice")).containsExactly(order);
    }

    @Test
    @WithMockUser(username = "alice", roles = "CUSTOMER")
    void aliceCannotReadBobsHistory() {
        assertThatThrownBy(() -> service.history("bob")).isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(repository);
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void adminCanReadBobsHistory() {
        var order = new Order("bob", 1, 2);
        when(repository.findByCustomer("bob")).thenReturn(List.of(order));

        assertThat(service.history("bob")).containsExactly(order);
    }

    @Test
    @WithMockUser(username = "alice", roles = "CUSTOMER")
    void aliceCannotShipAnOrder() {
        assertThatThrownBy(() -> service.ship(1)).isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(repository);
    }

    @Test
    void historyRequiresAuthentication() {
        assertThatThrownBy(() -> service.history("alice"))
                .isInstanceOf(AuthenticationCredentialsNotFoundException.class);
        verifyNoInteractions(repository);
    }
}
