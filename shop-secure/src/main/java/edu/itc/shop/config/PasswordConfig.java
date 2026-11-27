package edu.itc.shop.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Exposes the {@link PasswordEncoder} bean used throughout the application.
 * <p>
 * {@link PasswordEncoderFactories#createDelegatingPasswordEncoder()} uses
 * bcrypt as the default encoding scheme, prefixing each hash with {@code {bcrypt}}.
 * This means hashes stored in the database are automatically upgraded if the
 * default algorithm changes in a future Spring Security release.
 */
@Configuration
public class PasswordConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }
}
