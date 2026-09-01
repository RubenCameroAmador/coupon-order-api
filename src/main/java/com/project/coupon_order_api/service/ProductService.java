package com.project.coupon_order_api.service;

import com.project.coupon_order_api.dto.request.ProductRequest;
import com.project.coupon_order_api.dto.response.ProductResponse;
import com.project.coupon_order_api.entity.Product;
import com.project.coupon_order_api.exception.ProductNotFoundException;
import com.project.coupon_order_api.mapper.ProductMapper;
import com.project.coupon_order_api.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ProductService {

    private final ProductRepository productRepository;

    public ProductResponse create(ProductRequest request) {
        Product saved = productRepository.save(ProductMapper.toEntity(request));
        return ProductMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public ProductResponse getById(Long id) {
        return ProductMapper.toResponse(findOrThrow(id));
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> getAll() {
        return productRepository.findAll().stream().map(ProductMapper::toResponse).toList();
    }

    public ProductResponse update(Long id, ProductRequest request) {
        Product product = findOrThrow(id);
        ProductMapper.updateEntity(product, request);
        return ProductMapper.toResponse(productRepository.save(product));
    }

    public void delete(Long id) {
        Product product = findOrThrow(id);
        productRepository.delete(product);
    }

    private Product findOrThrow(Long id) {
        return productRepository.findById(id).orElseThrow(() -> new ProductNotFoundException(id));
    }
}
