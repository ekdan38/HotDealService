package com.hong.productservice.service.product;

import com.hong.common.dto.ProductCommonDto;
import com.hong.common.dto.ProductStockDto;
import com.hong.common.exception.ErrorCode;
import com.hong.common.exception.custom.ProductException;
import com.hong.productservice.domain.Product;
import com.hong.productservice.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@Slf4j(topic = "[ProductApiService]")
@Transactional(readOnly = true)
public class ProductApiService {

    private final ProductRepository productRepository;

    public Product getProduct(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new ProductException(ErrorCode.PRODUCT_NOT_FOUND));
    }

    public List<ProductCommonDto> getProductsByIds(List<Long> productIds) {
        List<Product> products = productRepository.findAllByProductIds(productIds);
        if (productIds.isEmpty()) {
            throw new ProductException(ErrorCode.PRODUCT_NOT_FOUND);
        }

        return products.stream()
                .map(product -> new ProductCommonDto(
                        product.getId(),
                        product.getTitle(),
                        product.getPrice(),
                        product.getStock()))
                .collect(Collectors.toList()
                );

    }

    @Transactional
    public void decreaseStock(List<ProductStockDto> productStockDtos) {
        List<Long> productIds = productStockDtos.stream()
                .map(ProductStockDto::getProductId)
                .collect(Collectors.toList());

        List<Product> products = productRepository.findAllByProductIds(productIds);

        Map<Long, Product> productMap = products.stream()
                .collect(Collectors.toMap(Product::getId, product -> product));

        for (ProductStockDto dto : productStockDtos) {
            Product product = productMap.get(dto.getProductId());
            if (product == null) {
                throw new ProductException(ErrorCode.PRODUCT_NOT_FOUND);
            }

            product.decreaseStock(dto.getQuantity());
        }

    }

    @Transactional
    public void increaseStock(Long productId, Integer quantity) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductException(ErrorCode.PRODUCT_NOT_FOUND));
        product.increaseStock(quantity);
    }
}
