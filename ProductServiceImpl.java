package com.example.productapi.service.impl;

import com.example.productapi.dto.request.ProductRequest;
import com.example.productapi.dto.response.ItemResponse;
import com.example.productapi.dto.response.ProductResponse;
import com.example.productapi.entity.Product;
import com.example.productapi.exception.ResourceNotFoundException;
import com.example.productapi.mapper.ProductMapper;
import com.example.productapi.repository.ItemRepository;
import com.example.productapi.repository.ProductRepository;
import com.example.productapi.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final ItemRepository itemRepository;
    private final ProductMapper productMapper;

    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponse> getAllProducts(Pageable pageable) {
        log.info("Fetching all products with pageable: {}", pageable);
        return productRepository.findAll(pageable)
                .map(productMapper::toProductResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponse getProductById(Long id) {
        log.info("Fetching product by id: {}", id);
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));
        return productMapper.toProductResponse(product);
    }

    @Override
    @Transactional
    public ProductResponse createProduct(ProductRequest request, String username) {
        log.info("Creating product: {} by user: {}", request.getProductName(), username);
        Product product = Product.builder()
                .productName(request.getProductName())
                .createdBy(username)
                .createdOn(LocalDateTime.now())
                .build();
        Product saved = productRepository.save(product);
        return productMapper.toProductResponse(saved);
    }

    @Override
    @Transactional
    public ProductResponse updateProduct(Long id, ProductRequest request, String username) {
        log.info("Updating product id: {} by user: {}", id, username);
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));
        product.setProductName(request.getProductName());
        product.setModifiedBy(username);
        product.setModifiedOn(LocalDateTime.now());
        Product saved = productRepository.save(product);
        return productMapper.toProductResponse(saved);
    }

    @Override
    @Transactional
    public void deleteProduct(Long id) {
        log.info("Deleting product id: {}", id);
        if (!productRepository.existsById(id)) {
            throw new ResourceNotFoundException("Product", "id", id);
        }
        productRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ItemResponse> getItemsByProductId(Long productId, Pageable pageable) {
        log.info("Fetching items for product id: {}", productId);
        if (!productRepository.existsById(productId)) {
            throw new ResourceNotFoundException("Product", "id", productId);
        }
        return itemRepository.findByProductId(productId, pageable)
                .map(productMapper::toItemResponse);
    }
}
