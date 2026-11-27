package edu.itc.shop.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Request body for {@code POST /api/register}.
 *
 * <p>Validation rules:</p>
 * <ul>
 *   <li>Username: 3–40 lowercase letters, digits, underscores or dots.</li>
 *   <li>Password: 12–72 chars, must contain at least one uppercase letter,
 *       one lowercase letter and one digit.</li>
 * </ul>
 */
public record RegisterRequest(

        @NotBlank
        @Size(min = 3, max = 40)
        @Pattern(
                regexp  = "[a-z0-9_.]+",
                message = "lower-case letters, digits, _ and . only"
        )
        String username,

        @NotBlank
        @Size(min = 12, max = 72)
        @Pattern(
                regexp  = "(?=.*\\d)(?=.*[a-z])(?=.*[A-Z]).*",
                message = "must contain an upper-case letter, a lower-case letter and a digit"
        )
        String password

) {}
