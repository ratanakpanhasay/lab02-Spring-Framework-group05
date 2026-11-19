
package edu.itc.shop.api.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.PositiveOrZero;

import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAttribute;

import edu.itc.shop.domain.Product;

@XmlRootElement(name = "product")
@XmlAccessorType(XmlAccessType.FIELD)

public class ProductDto {

    @XmlAttribute
    public long id;

    @NotBlank
    @Size(max = 80)
    public String name;

    @NotNull
    @Pattern(regexp = "[A-Z]{2}-\\d{2}")
    public String sku;

    @NotNull
    @DecimalMin("0.01")
    @Digits(integer = 8, fraction = 2)
    public BigDecimal price;

    @PositiveOrZero
    public int stock;

    // Required for XML conversion
    public ProductDto() {
    }

    // Convert Product into ProductDto
    public static ProductDto from(Product product) {
        ProductDto dto = new ProductDto();

        dto.id = product.id();
        dto.name = product.name();
        dto.sku = product.sku();
        dto.price = product.price();
        dto.stock = product.stock();

        return dto;
    }

    // Convert ProductDto into Product
    public Product toProduct(long id, int version) {
        return new Product(
            id,
            name,
            sku,
            price,
            stock,
            version
        );
    }
}

