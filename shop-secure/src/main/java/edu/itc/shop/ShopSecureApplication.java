
package edu.itc.shop;

import edu.itc.shop.domain.Product;
import edu.itc.shop.repository.ProductRepository;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.math.BigDecimal;

@SpringBootApplication
public class ShopSecureApplication {

    public static void main(String[] args) {
        SpringApplication.run(ShopSecureApplication.class, args);
    }

    @Bean
    CommandLineRunner seedProducts(ProductRepository repository) {
        return args -> {
            if (repository.count() == 0) {
                repository.save(new Product("USB-C cable", new BigDecimal("7.90"), 25));
                repository.save(new Product("Wireless Mouse", new BigDecimal("19.90"), 15));
                repository.save(new Product("Keyboard", new BigDecimal("39.90"), 10));
            }
        };
    }
}
