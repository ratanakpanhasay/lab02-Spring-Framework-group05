
package edu.itc.shop.web;

import edu.itc.shop.service.ProductService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.math.BigDecimal;

import edu.itc.shop.web.dto.ProductDto;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import edu.itc.shop.service.ProductNotFoundException;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import edu.itc.shop.web.dto.CreateProduct;
import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;

@WebMvcTest(ProductController.class)
class ProductControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private ProductService service;

    //Test GET All Products (200 OK)
    @Test
    void listProductsReturns200() throws Exception {

        ProductDto product = new ProductDto(
                1L,
                "Wireless Mouse",
                new BigDecimal("19.99"),
                25
        );

        when(service.search(null))
                .thenReturn(List.of(product));

        mvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Wireless Mouse"))
                .andExpect(jsonPath("$[0].stock").value(25));
    }

    // Test GET Product Not Found (404)
    @Test
    void getProductNotFoundReturns404() throws Exception {

        when(service.findById(999L))
                .thenThrow(new ProductNotFoundException(999L));

        mvc.perform(get("/api/products/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.title").value("Not Found"));
    }

    //Test Validation Error (400 Bad Request)
    
    @Test
    void createInvalidProductReturns400() throws Exception {

        String invalidJson = """
                {
                    "name": "",
                    "price": -5,
                    "stock": -2
                }
                """;

        mvc.perform(post("/api/products")
                .contentType("application/json")
                .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.title").value("Validation Failed"))
                .andExpect(jsonPath("$.errors.length()").value(3));
    }

    //Test POST Success (201 Created)
    
    @Test
    void createValidProductReturns201() throws Exception {

        ProductDto created = new ProductDto(
                6L,
                "Gaming Mouse",
                new BigDecimal("35.99"),
                20
        );

        when(service.create(any(CreateProduct.class)))
                .thenReturn(created);

        String validJson = """
                {
                    "name": "Gaming Mouse",
                    "price": 35.99,
                    "stock": 20
                }
                """;

        mvc.perform(post("/api/products")
                .contentType("application/json")
                .content(validJson))
                .andExpect(status().isCreated())
                .andExpect(header().string(
                        "Location", "/api/products/6"))
                .andExpect(jsonPath("$.id").value(6))
                .andExpect(jsonPath("$.name").value("Gaming Mouse"));
    }




}
