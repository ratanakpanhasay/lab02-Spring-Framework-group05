package edu.itc.shop.api;

import java.net.URI;
import java.util.Comparator;
import java.util.List;

import edu.itc.shop.api.dto.ProductDto;
import edu.itc.shop.api.dto.StockChange;
import edu.itc.shop.api.filters.Compress;
import edu.itc.shop.api.params.ProductFilter;
import edu.itc.shop.domain.Product;
import edu.itc.shop.domain.ProductService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

@Path("/products")
@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
public class ProductResource {

    private final ProductService service;

    @Inject
    public ProductResource(ProductService service) {
        this.service = service;
    }

    // GET /products — list all
    
    @GET
    @Compress
    public List<ProductDto> list(@BeanParam @Valid ProductFilter f) {
        if (f.minPrice != null && !"EUR".equals(f.minPrice.currency())) {
            throw new BadRequestException("minPrice must use EUR");
        }

        if (f.maxPrice != null && !"EUR".equals(f.maxPrice.currency())) {
            throw new BadRequestException("maxPrice must use EUR");
        }

        Comparator<Product> comparator = switch (f.sortField()) {
            case "name" -> Comparator.comparing(Product::name);
            case "price" -> Comparator.comparing(Product::price);
            case "stock" -> Comparator.comparing(Product::stock);
            default -> throw new BadRequestException(
                "Unknown sort field: " + f.sortField()
            );
        };

        if (f.descending()) {
            comparator = comparator.reversed();
        }

        return service.findAll().stream()
                .filter(p -> f.q == null ||
                        p.name().toLowerCase().contains(f.q.toLowerCase()))
                .filter(p -> f.minPrice == null ||
                        p.price().compareTo(f.minPrice.amount()) >= 0)
                .filter(p -> f.maxPrice == null ||
                        p.price().compareTo(f.maxPrice.amount()) <= 0)
                .filter(p -> !f.inStock || p.stock() > 0)
                .sorted(comparator)
                .map(ProductDto::from)
                .toList();
    }


    // GET /products/{id}
    @GET
    @Path("{id: \\d+}")
    public ProductDto one(@PathParam("id") long id) {
        return ProductDto.from(service.get(id));
    }

    // POST /products — create, returns 201 + Location
    @POST
    public Response create(@Valid @NotNull ProductDto dto, @Context UriInfo uriInfo) {
        Product saved = service.create(dto.toProduct(0, 0));
        URI location = uriInfo.getAbsolutePathBuilder()
                .path(Long.toString(saved.id()))
                .build();
        return Response.created(location)
                .entity(ProductDto.from(saved))
                .build();
    }

    // PUT /products/{id} — full replace, returns 200
    @PUT
    @Path("{id: \\d+}")
    public ProductDto update(@PathParam("id") long id, @Valid @NotNull ProductDto dto) {
        Product updated = service.update(id, dto.toProduct(id, 0));
        return ProductDto.from(updated);
    }

    // PATCH /products/{id}/stock — adjust stock, returns 200 or 409
    @PATCH
    @Path("{id: \\d+}/stock")
    @Consumes(MediaType.APPLICATION_JSON)
    public ProductDto changeStock(@PathParam("id") long id, @Valid @NotNull StockChange change) {
        Product updated = service.changeStock(id, change.delta());
        return ProductDto.from(updated);
    }

    // DELETE /products/{id} — returns 204 or 404
    @DELETE
    @Path("{id: \\d+}")
    public Response delete(@PathParam("id") long id) {
        service.delete(id);
        return Response.noContent().build();
    }
}
