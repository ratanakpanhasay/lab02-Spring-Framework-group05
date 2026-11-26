package edu.itc.shop.config;

import java.io.IOException;
import java.net.URI;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;

import tools.jackson.databind.json.JsonMapper;

/**
 * Task-2 security configuration.
 *
 * <p>The temporary {@code InMemoryUserDetailsManager} from Task 1 has been removed.
 * Authentication is now delegated to {@link edu.itc.shop.security.JpaUserDetailsService},
 * which is picked up automatically by Spring Security's
 * {@link org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration}.</p>
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain api(HttpSecurity http, JsonMapper mapper) throws Exception {

        AuthenticationEntryPoint entryPoint = (request, response, ex) -> {
            response.setHeader("WWW-Authenticate", "Basic realm=\"Realm\"");
            writeProblem(request, response, mapper,
                    HttpStatus.UNAUTHORIZED, "Authentication required");
        };

        http.authorizeHttpRequests(auth -> auth
                // Public: product catalogue reads & registration
                .requestMatchers(HttpMethod.GET, "/api/products/**").permitAll()
                .requestMatchers("/api/register").permitAll()
                // Role-restricted writes
                .requestMatchers(HttpMethod.DELETE, "/api/products/**").hasRole("ADMIN")
                // Everything else under /api/** must be authenticated
                .requestMatchers("/api/**").authenticated()
                // Deny anything that didn't match above (defence in depth)
                .anyRequest().denyAll()
        );

        http.httpBasic(basic -> basic.authenticationEntryPoint(entryPoint));

        http.formLogin(form -> form.disable());

        // CSRF disabled: this chain accepts only Basic / Bearer credentials,
        // never cookie-based session credentials, so CSRF is not applicable.
        http.csrf(csrf -> csrf.disable());

        http.sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
        );

        http.exceptionHandling(errors -> errors
                .authenticationEntryPoint(entryPoint)
                .accessDeniedHandler((request, response, ex) ->
                        writeProblem(request, response, mapper,
                                HttpStatus.FORBIDDEN, "Access denied"))
        );

        return http.build();
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

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
