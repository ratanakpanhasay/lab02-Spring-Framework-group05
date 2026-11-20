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
import jakarta.ws.rs.core.EntityTag;
import jakarta.ws.rs.core.CacheControl;
import jakarta.ws.rs.core.Request;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.ClientErrorException;
import edu.itc.shop.api.dto.ProductV2;
import edu.itc.shop.domain.Money;
import edu.itc.shop.domain.ReviewRepository;

@Path("/products")
@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
public class ProductResource {

    private final ProductService service;
    private final ReviewRepository reviewRepo;

    @Inject
    public ProductResource(
            ProductService service,
            ReviewRepository reviewRepo) {

        this.service = service;
        this.reviewRepo = reviewRepo;
    }

    // GET /products — list all
    
    
    @GET
    @Compress
    public Response list(
            @BeanParam @Valid ProductFilter f,
            @Context UriInfo uriInfo) {

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

        // Step 1: Filter and sort all matching products
        List<Product> all = service.findAll().stream()
                .filter(p -> f.q == null ||
                        p.name().toLowerCase().contains(f.q.toLowerCase()))
                .filter(p -> f.minPrice == null ||
                        p.price().compareTo(f.minPrice.amount()) >= 0)
                .filter(p -> f.maxPrice == null ||
                        p.price().compareTo(f.maxPrice.amount()) <= 0)
                .filter(p -> !f.inStock || p.stock() > 0)
                .sorted(comparator)
                .toList();

        // Step 2: Count products after filtering
        int total = all.size();

        // Step 3: Calculate the last page
        int lastPage = Math.max(0, (total - 1) / f.size);

        // Step 4: Select products for the requested page
        List<ProductDto> page = all.stream()
                .skip((long) f.page * f.size)
                .limit(f.size)
                .map(ProductDto::from)
                .toList();

        // Step 5: Build the HTTP response
        Response.ResponseBuilder response = Response.ok(page)
                .header("X-Total-Count", total);

        // First and last page links
        response.link(
                uriInfo.getRequestUriBuilder()
                        .replaceQueryParam("page", 0)
                        .build(),
                "first"
        );

        response.link(
                uriInfo.getRequestUriBuilder()
                        .replaceQueryParam("page", lastPage)
                        .build(),
                "last"
        );

        // Previous page link (when available)
        if (f.page > 0) {
            response.link(
                    uriInfo.getRequestUriBuilder()
                            .replaceQueryParam("page", f.page - 1)
                            .build(),
                    "prev"
            );
        }

        // Next page link (when available)
        if (f.page < lastPage) {
            response.link(
                    uriInfo.getRequestUriBuilder()
                            .replaceQueryParam("page", f.page + 1)
                            .build(),
                    "next"
            );
        }

        return response.build();
    }

    // GET /products/{id}
    
    @GET
    @Path("{id: \\d+}")
    @Produces({
        "application/json;qs=1.0",
        "application/vnd.shop.v1+json",
        MediaType.APPLICATION_XML
    })
    public Response one(
            @PathParam("id") long id,
            @Context Request request) {

        // Get the product
        Product product = service.get(id);

        // Create an ETag using the product version
        EntityTag tag = new EntityTag(
                String.valueOf(product.version())
        );

        // Configure caching
        CacheControl cache = new CacheControl();
        cache.setPrivate(true);
        cache.setMaxAge(60);
        cache.setNoTransform(true);

        // Check whether the client's version is still current
        Response.ResponseBuilder precondition =
                request.evaluatePreconditions(tag);

        if (precondition != null) {
            return precondition
                    .tag(tag)
                    .cacheControl(cache)
                    .build();
        }

        // Return the product if the client needs its data
        return Response.ok(ProductDto.from(product))
                .tag(tag)
                .cacheControl(cache)
                .build();
    }


    @GET
    @Path("{id: \\d+}")
    @Produces("application/vnd.shop.v2+json")
    public Response oneV2(
            @PathParam("id") long id,
            @Context Request request) {

        Product product = service.get(id);

        // Generate the ETag from the product version
        EntityTag tag = new EntityTag(
                String.valueOf(product.version())
        );

        // Configure caching
        CacheControl cache = new CacheControl();
        cache.setPrivate(true);
        cache.setMaxAge(60);
        cache.setNoTransform(true);

        // Return 304 if the client already has this version
        Response.ResponseBuilder precondition =
                request.evaluatePreconditions(tag);

        if (precondition != null) {
            return precondition
                    .tag(tag)
                    .cacheControl(cache)
                    .build();
        }

        // Convert the product into the v2 representation
        ProductV2 dto = new ProductV2(
                product.id(),
                product.name(),
                product.sku(),
                new Money(product.price(), "EUR"),
                product.stock(),
                product.stock() > 0
        );

        return Response.ok(dto)
                .tag(tag)
                .cacheControl(cache)
                .build();
    }


    @Path("{id: \\d+}/reviews")
    public ReviewResource reviews(@PathParam("id") long id) {

        // Return 404 if the product does not exist
        Product product = service.get(id);

        // Forward review requests to ReviewResource
        return new ReviewResource(product, reviewRepo);
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
    public Response update(
            @PathParam("id") long id,
            @Valid @NotNull ProductDto dto,
            @HeaderParam("If-Match") String ifMatch) {

        // 428: The client must provide an If-Match header
        if (ifMatch == null || ifMatch.isBlank()) {
            throw new ClientErrorException(
                    "If-Match header is required",
                    428
            );
        }

        // Get the current product and its ETag
        Product current = service.get(id);
        EntityTag currentTag = new EntityTag(
                String.valueOf(current.version())
        );

        // 412: Reject an outdated or incorrect ETag
        if (!currentTag.toString().equals(ifMatch.trim())) {
            throw new ClientErrorException(
                    "Product version does not match",
                    412
            );
        }

        // Update the product with atomic version checking
        Product updated = service.updateIfVersionMatches(
                id,
                dto.toProduct(id, 0),
                current.version()
        );

        // Return the updated product and its new ETag
        EntityTag newTag = new EntityTag(
                String.valueOf(updated.version())
        );

        return Response.ok(ProductDto.from(updated))
                .tag(newTag)
                .build();
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
