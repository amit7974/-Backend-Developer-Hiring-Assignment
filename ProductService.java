package com.example.productapi.service;

import com.example.productapi.dto.request.ProductRequest;
import com.example.productapi.dto.response.ItemResponse;
import com.example.productapi.dto.response.ProductResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ProductService {

    Page<ProductResponse> getAllProducts(Pageable pageable);

    ProductResponse getProductById(Long id);

    ProductResponse createProduct(ProductRequest request, String username);

    ProductResponse updateProduct(Long id, ProductRequest request, String username);

    void deleteProduct(Long id);

    Page<ItemResponse> getItemsByProductId(Long productId, Pageable pageable);
}
