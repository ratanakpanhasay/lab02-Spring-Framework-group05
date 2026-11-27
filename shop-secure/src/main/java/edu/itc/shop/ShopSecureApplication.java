package edu.itc.shop;

import edu.itc.shop.domain.Product;
import edu.itc.shop.domain.Role;
import edu.itc.shop.repository.ProductRepository;
import edu.itc.shop.repository.UserAccountRepository;
import edu.itc.shop.service.AccountService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Bean;

import java.math.BigDecimal;
import java.util.Set;

@SpringBootApplication
@ConfigurationPropertiesScan
public class ShopSecureApplication {

    public static void main(String[] args) {
        SpringApplication.run(ShopSecureApplication.class, args);
    }

    // ── Product seeding (Task 1) ──────────────────────────────────────────────

    @Bean
    CommandLineRunner seedProducts(ProductRepository repository) {
        return args -> {
            if (repository.count() == 0) {
                repository.save(new Product("USB-C cable",    new BigDecimal("7.90"),  25));
                repository.save(new Product("Wireless Mouse", new BigDecimal("19.90"), 15));
                repository.save(new Product("Keyboard",       new BigDecimal("39.90"), 10));
            }
        };
    }

    // ── User seeding (Task 2) ─────────────────────────────────────────────────
    //
    // Seed passwords come from environment-aware properties:
    //   shop.seed.admin-password    (env: SHOP_SEED_ADMIN_PASSWORD)
    //   shop.seed.customer-password (env: SHOP_SEED_CUSTOMER_PASSWORD)
    //
    // The CommandLineRunner is idempotent: it checks existsByUsername before saving.

    @Bean
    CommandLineRunner seedUsers(
            UserAccountRepository userRepository,
            AccountService accountService,
            SeedProperties seedProps) {

        return args -> {
            if (!userRepository.existsByUsername("admin")) {
                accountService.register(
                        "admin",
                        seedProps.adminPassword(),
                        Set.of(Role.ADMIN, Role.CUSTOMER)
                );
            }
            if (!userRepository.existsByUsername("alice")) {
                accountService.register(
                        "alice",
                        seedProps.customerPassword(),
                        Set.of(Role.CUSTOMER)
                );
            }
        };
    }
}
