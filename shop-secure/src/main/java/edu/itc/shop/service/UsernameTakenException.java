package edu.itc.shop.service;

/**
 * Thrown by {@link AccountService#register} when the requested username
 * is already in use. Maps to HTTP 409 Conflict in {@link edu.itc.shop.web.ApiExceptionHandler}.
 */
public class UsernameTakenException extends RuntimeException {

    private final String username;

    public UsernameTakenException(String username) {
        super("Username already taken: " + username);
        this.username = username;
    }

    public String getUsername() {
        return username;
    }
}
