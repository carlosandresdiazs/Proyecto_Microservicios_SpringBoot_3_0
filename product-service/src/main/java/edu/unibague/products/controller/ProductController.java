package edu.unibague.products.controller;
import edu.unibague.products.dto.ProductRequest;
import edu.unibague.products.model.Product;
import edu.unibague.products.repository.ProductRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;

@RestController @RequestMapping("/api/products")

public class ProductController {
 private final ProductRepository repo;
 public ProductController(ProductRepository repo){this.repo=repo;}
 @GetMapping public List<Product> all(){return repo.findAll();}
 @GetMapping("/{id}") public Product one(@PathVariable Long id){
  return repo.findById(id).orElseThrow(() ->
   new ResponseStatusException(HttpStatus.NOT_FOUND,"Producto no encontrado"));
 }

 @PostMapping @ResponseStatus(HttpStatus.CREATED)
 public Product create(@Valid @RequestBody ProductRequest request){
  Product p=new Product(); updateFields(p,request);return repo.save(p);
 }

 @PutMapping("/{id}") public Product update(@PathVariable Long id,
              @Valid @RequestBody ProductRequest request){
  Product p=one(id);updateFields(p,request);return repo.save(p);
 }

 @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
 public void delete(@PathVariable Long id){
  Product p=one(id);repo.delete(p);
 }
 
 private static void updateFields(Product p,ProductRequest r){
  p.name=r.name();p.description=r.description();
  p.price=r.price();p.stock=r.stock();
 }
}
