package com.project.coupon_order_api.service;

import com.project.coupon_order_api.dto.request.ProductRequest;
import com.project.coupon_order_api.dto.response.ProductResponse;
import com.project.coupon_order_api.entity.Product;
import com.project.coupon_order_api.exception.ProductNotFoundException;
import com.project.coupon_order_api.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductService productService;

    @Test
    void create_persistsAndReturnsResponse() {
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> {
            Product p = inv.getArgument(0);
            p.setId(1L);
            return p;
        });

        ProductResponse response = productService.create(new ProductRequest("Widget", new BigDecimal("20.00"), 10));

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("Widget");
    }

    @Test
    void getById_notFound_throws() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getById(99L))
                .isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    void update_notFound_throws() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.update(99L, new ProductRequest("X", new BigDecimal("1.00"), 1)))
                .isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    void update_modifiesExistingProduct() {
        Product existing = Product.builder().id(1L).name("Old").price(new BigDecimal("10.00")).stock(5).build();
        when(productRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        ProductResponse response = productService.update(1L, new ProductRequest("New", new BigDecimal("15.00"), 8));

        assertThat(response.name()).isEqualTo("New");
        assertThat(response.price()).isEqualByComparingTo("15.00");
        assertThat(response.stock()).isEqualTo(8);
    }

    @Test
    void delete_notFound_throws() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.delete(99L))
                .isInstanceOf(ProductNotFoundException.class);
    }
}
