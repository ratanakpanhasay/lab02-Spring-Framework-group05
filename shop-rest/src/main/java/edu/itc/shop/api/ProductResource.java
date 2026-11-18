package edu.itc.shop.api;

import java.net.URI;
import java.util.List;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;

import edu.itc.shop.api.dto.ProductDto;
import edu.itc.shop.api.dto.StockChange;
import edu.itc.shop.domain.Product;
import edu.itc.shop.domain.ProductService;

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
    public List<ProductDto> list() {
        return service.findAll().stream()
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
    public Response create(ProductDto dto, @Context UriInfo uriInfo) {
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
    public ProductDto update(@PathParam("id") long id, ProductDto dto) {
        Product updated = service.update(id, dto.toProduct(id, 0));
        return ProductDto.from(updated);
    }

    // PATCH /products/{id}/stock — adjust stock, returns 200 or 409
    @PATCH
    @Path("{id: \\d+}/stock")
    @Consumes(MediaType.APPLICATION_JSON)
    public ProductDto changeStock(@PathParam("id") long id, StockChange change) {
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
