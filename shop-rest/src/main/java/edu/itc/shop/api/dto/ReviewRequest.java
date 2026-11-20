
package edu.itc.shop.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class ReviewRequest {

    @NotNull
    @Min(1)
    @Max(5)
    public Integer stars;

    @NotBlank
    public String author;

    @Size(max = 500)
    public String text;

    public ReviewRequest() {
    }
}
