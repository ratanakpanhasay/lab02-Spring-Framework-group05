
package edu.itc.shop.dto;

import jakarta.validation.constraints.*;

public record ReviewRequest(

        @NotNull
        Long productId,

        @Min(1)
        @Max(5)
        int stars,

        @NotBlank
        @Size(max = 1000)
        String text

) {
}
