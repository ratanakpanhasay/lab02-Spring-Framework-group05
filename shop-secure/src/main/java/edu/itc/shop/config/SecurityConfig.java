package edu.itc.shop.config;

import java.io.IOException;
import java.net.URI;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.config.Customizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.core.annotation.Order;
import tools.jackson.databind.json.JsonMapper;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

        @Bean
        @Order(1)
        SecurityFilterChain api(
            HttpSecurity http,
            JsonMapper mapper,
            JwtAuthenticationConverter jwtAuthenticationConverter)
            throws Exception {

        AuthenticationEntryPoint entryPoint = (request, response, ex) -> {
            response.setHeader("WWW-Authenticate", "Bearer");
            writeProblem(request, response, mapper,
                    HttpStatus.UNAUTHORIZED, "Authentication required");
        };
                http.securityMatcher("/api/**");
                http.cors(Customizer.withDefaults());
                // Security headers (Task 5)
                http.headers(headers -> headers
                        .frameOptions(frame -> frame.deny())
                        .httpStrictTransportSecurity(hsts -> hsts
                                .includeSubDomains(true)
                                .maxAgeInSeconds(31_536_000))
                        .contentSecurityPolicy(csp -> csp
                                .policyDirectives(
                                "default-src 'self'; frame-ancestors 'none'; object-src 'none'"))
                        .referrerPolicy(referrer -> referrer
                                .policy(ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                        .contentTypeOptions(Customizer.withDefaults())
                );
                http.authorizeHttpRequests(auth -> auth
                // Public: product catalogue reads & registration & login
                .requestMatchers(HttpMethod.GET, "/api/products/**").permitAll()
                .requestMatchers("/api/register").permitAll()
                .requestMatchers("/api/auth/**").permitAll()

                // Role-restricted writes
                .requestMatchers(HttpMethod.DELETE, "/api/products/**")
                .hasRole("ADMIN")

                // Everything else under /api/** must be authenticated
                .requestMatchers("/api/**").authenticated()

                // Deny anything that didn't match above
                .anyRequest().denyAll()
        );

        // HTTP Basic removed; JWT Bearer authentication enabled
        http.oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt
                        .jwtAuthenticationConverter(jwtAuthenticationConverter)
                )
                .authenticationEntryPoint(entryPoint)
        );

        http.formLogin(form -> form.disable());

        // Bearer-token API: no cookie-based authentication
        http.csrf(csrf -> csrf.disable());

        http.sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
        );

        http.exceptionHandling(errors -> errors
                .authenticationEntryPoint(entryPoint)
                .accessDeniedHandler((request, response, ex) ->
                        writeProblem(request, response, mapper,
                                HttpStatus.FORBIDDEN, "Access denied")
                )
        );

        return http.build();
    }


        @Bean
        @Order(2)
        SecurityFilterChain web(HttpSecurity http) throws Exception {

        http.authorizeHttpRequests(auth -> auth
                .requestMatchers(
                        HttpMethod.GET,
                        "/products/*/reviews.html"
                ).permitAll()
                .anyRequest().denyAll()
        );

        // Keep CSRF protection enabled for web pages
        http.csrf(Customizer.withDefaults());

        http.formLogin(form -> form.disable());

        // Security headers for HTML pages
        http.headers(headers -> headers
                .frameOptions(frame -> frame.deny())
                .httpStrictTransportSecurity(hsts -> hsts
                        .includeSubDomains(true)
                        .maxAgeInSeconds(31_536_000))
                .contentSecurityPolicy(csp -> csp
                        .policyDirectives(
                                "default-src 'self'; frame-ancestors 'none'; object-src 'none'"))
                .referrerPolicy(referrer -> referrer
                        .policy(ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                .contentTypeOptions(Customizer.withDefaults())
        );

        return http.build();
        }

    // ── Helpers ──────────────────────────────────────────────────────────

    private static void writeProblem(
            HttpServletRequest request,
            HttpServletResponse response,
            JsonMapper mapper,
            HttpStatus status,
            String detail) throws IOException {

        ProblemDetail problem = ProblemDetail.forStatus(status);
        problem.setDetail(detail);
        problem.setInstance(URI.create(request.getRequestURI()));

        response.setStatus(status.value());
        response.setContentType("application/problem+json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(mapper.writeValueAsString(problem));
    }
}