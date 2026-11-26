package edu.itc.shop.web.dto;

/**
 * Response body returned after a successful registration.
 * Intentionally contains only the username – never the password.
 */
public record UsernameResponse(String username) {}
