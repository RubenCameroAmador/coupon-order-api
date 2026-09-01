package com.project.coupon_order_api.mapper;

import com.project.coupon_order_api.dto.request.ProductRequest;
import com.project.coupon_order_api.dto.response.ProductResponse;
import com.project.coupon_order_api.entity.Product;

public final class ProductMapper {

    private ProductMapper() {
    }

    public static Product toEntity(ProductRequest request) {
        return Product.builder()
                .name(request.name())
                .price(request.price())
                .stock(request.stock())
                .build();
    }

    public static void updateEntity(Product product, ProductRequest request) {
        product.setName(request.name());
        product.setPrice(request.price());
        product.setStock(request.stock());
    }

    public static ProductResponse toResponse(Product product) {
        return new ProductResponse(product.getId(), product.getName(), product.getPrice(), product.getStock());
    }
}
