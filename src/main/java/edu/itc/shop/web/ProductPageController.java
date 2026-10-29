
package edu.itc.shop.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import edu.itc.shop.service.ProductService;
import edu.itc.shop.web.dto.CreateProduct;
import jakarta.validation.Valid;

@Controller
@RequestMapping("/products")
public class ProductPageController {

    private final ProductService service;

    public ProductPageController(ProductService service) {
        this.service = service;
    }

    @GetMapping
    public String list(
            @RequestParam(required = false) String q,
            Model model) {

        model.addAttribute("products", service.search(q));
        model.addAttribute("q", q == null ? "" : q);

        return "products/list";
    }

    @GetMapping("/new")
    public String newProduct(Model model) {
        model.addAttribute(
                "product",
                new CreateProduct("", null, 0)
        );

        return "products/form";
    }

    @PostMapping
    public String create(
            @Valid @ModelAttribute("product") CreateProduct product,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("errors", bindingResult);
            return "products/form";
        }

        service.create(product);

        redirectAttributes.addFlashAttribute(
                "message",
                "Product created successfully!"
        );

        return "redirect:/products";
    }
}
