package com.toyProject.controller;

import com.toyProject.dto.PopularTravelDto;
import com.toyProject.dto.ProductDto;
import com.toyProject.entity.Product;
import com.toyProject.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/products")
public class ProductController {

    private final ProductService productService;

    //상품 등록
    @PostMapping
    public ResponseEntity<ProductDto> addProduct(@RequestBody ProductDto productDto) {
        return ResponseEntity.ok(productService.addProduct(productDto));
    }

    //상품 리스트
    @GetMapping
    public ResponseEntity<List<ProductDto>> productList() {
        return ResponseEntity.ok(productService.productListV2());
    }

    // 상품 상세
    @GetMapping("/{id}")
    public ResponseEntity<ProductDto> productDetail(@PathVariable Long id) {
        return ResponseEntity.ok(productService.productDetail(id));
    }

    // 인기 상품 캐싱
    @GetMapping("/popular")
    public ResponseEntity<List<PopularTravelDto>> getPopularProducts() {
        return ResponseEntity.ok(productService.getPopularTravels());
    }
}
