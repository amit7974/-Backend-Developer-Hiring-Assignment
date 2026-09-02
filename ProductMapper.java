package com.example.productapi.mapper;

import com.example.productapi.dto.response.ItemResponse;
import com.example.productapi.dto.response.ProductResponse;
import com.example.productapi.entity.Item;
import com.example.productapi.entity.Product;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ProductMapper {

    ProductResponse toProductResponse(Product product);

    @Mapping(source = "product.id", target = "productId")
    ItemResponse toItemResponse(Item item);
}
