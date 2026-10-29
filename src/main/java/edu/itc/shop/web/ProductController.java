
package edu.itc.shop.web;

import edu.itc.shop.service.ProductService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
import edu.itc.shop.web.dto.ProductDto;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;
import edu.itc.shop.web.dto.CreateProduct;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.http.ResponseEntity;
import java.net.URI;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.DeleteMapping;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService service;

    public ProductController(ProductService service) {
        this.service = service;
    }

    
    @GetMapping
    public List<ProductDto> list(
            @RequestParam(required = false) String q) {
        return service.search(q);
    }

    //GET
    @GetMapping("/{id}")
    public ProductDto getById(@PathVariable long id) {
        return service.findById(id);
    }

    //POST
    @PostMapping
    public ResponseEntity<ProductDto> create(
            @Valid @RequestBody CreateProduct request) { 

        ProductDto created = service.create(request);

        return ResponseEntity
                .created(URI.create("/api/products/" + created.id()))
                .body(created);
    }

    //PATCH  
    
    @PatchMapping("/{id}/stock")
    public ProductDto updateStock(
            @PathVariable long id,
            @RequestBody StockUpdate request) {

        return service.updateStock(id, request.delta());
    }

    public record StockUpdate(int delta) {}

    //DELETE
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }







}
