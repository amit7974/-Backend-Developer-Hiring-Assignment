package com.example.productapi.mapper;

import com.example.productapi.dto.response.ItemResponse;
import com.example.productapi.dto.response.ProductResponse;
import com.example.productapi.entity.Item;
import com.example.productapi.entity.Product;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-09-02T18:04:01+0530",
    comments = "version: 1.5.5.Final, compiler: Eclipse JDT (IDE) 3.46.0.v20260407-0427, environment: Java 21.0.10 (Eclipse Adoptium)"
)
@Component
public class ProductMapperImpl implements ProductMapper {

    @Override
    public ProductResponse toProductResponse(Product product) {
        if ( product == null ) {
            return null;
        }

        ProductResponse.ProductResponseBuilder productResponse = ProductResponse.builder();

        productResponse.createdBy( product.getCreatedBy() );
        productResponse.createdOn( product.getCreatedOn() );
        productResponse.id( product.getId() );
        productResponse.modifiedBy( product.getModifiedBy() );
        productResponse.modifiedOn( product.getModifiedOn() );
        productResponse.productName( product.getProductName() );

        return productResponse.build();
    }

    @Override
    public ItemResponse toItemResponse(Item item) {
        if ( item == null ) {
            return null;
        }

        ItemResponse.ItemResponseBuilder itemResponse = ItemResponse.builder();

        itemResponse.productId( itemProductId( item ) );
        itemResponse.id( item.getId() );
        itemResponse.quantity( item.getQuantity() );

        return itemResponse.build();
    }

    private Long itemProductId(Item item) {
        if ( item == null ) {
            return null;
        }
        Product product = item.getProduct();
        if ( product == null ) {
            return null;
        }
        Long id = product.getId();
        if ( id == null ) {
            return null;
        }
        return id;
    }
}
