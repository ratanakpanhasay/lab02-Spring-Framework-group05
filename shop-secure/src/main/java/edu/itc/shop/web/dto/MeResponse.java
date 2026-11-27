package edu.itc.shop.web.dto;

import java.util.Collection;

/**
 * Response body for {@code GET /api/me} – the currently authenticated user's
 * name and authority strings (e.g. {@code ROLE_CUSTOMER}).
 */
public record MeResponse(String name, Collection<String> roles) {}
