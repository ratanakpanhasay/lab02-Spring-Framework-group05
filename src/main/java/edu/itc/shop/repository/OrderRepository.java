package edu.itc.shop.repository;

import edu.itc.shop.domain.Order;
import edu.itc.shop.domain.enums.OrderStatus;
import edu.itc.shop.web.dto.OrderSummary;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByCustomerEmailAndStatusOrderByPlacedAtDesc(String email, OrderStatus status);

    long countByStatus(OrderStatus status);

    Optional<Order> findFirstByCustomerIdOrderByPlacedAtDesc(Long customerId);

    Page<OrderSummary> findByStatus(OrderStatus status, Pageable pageable);

    Page<OrderSummary> findAllProjectedBy(Pageable pageable);

    @Query("""
            select distinct o from Order o
            join fetch o.customer c
            left join fetch o.lines l
            left join fetch l.product
            where c.email = :email
            order by o.placedAt desc, o.id desc
            """)
    List<Order> findWithLinesByCustomerEmail(@Param("email") String email);

    @Query("select count(l) from Order o join o.lines l where o.status = :status")
    long countLinesByStatus(@Param("status") OrderStatus status);
}
