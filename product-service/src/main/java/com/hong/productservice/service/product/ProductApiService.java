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

    // product 조회
    public Product getProduct(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new ProductException(ErrorCode.PRODUCT_NOT_FOUND));
    }

    // productIds 로 products 조회
    public List<ProductCommonDto> getProductsByIds(List<Long> productIds) {
        List<Product> products = productRepository.findAllByProductIds(productIds);
        if (productIds.size() != products.size()) {
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
    public Boolean decreaseStock(List<ProductStockDto> productStockDtos) {
        // ProductStockDto 로 products 조회
        Map<Long, Product> productMap = getProducts(productStockDtos);

        for (ProductStockDto dto : productStockDtos) {
            Product product = productMap.get(dto.getProductId());
            if (product == null) {
                throw new ProductException(ErrorCode.PRODUCT_NOT_FOUND);
            }
            product.decreaseStock(dto.getQuantity());
        }
        return true;

    }



    @Transactional
    public Boolean increaseStock(List<ProductStockDto> productStockDtos) {
        // ProductStockDto 로 products 조회
        Map<Long, Product> productMap = getProducts(productStockDtos);

        for (ProductStockDto dto : productStockDtos) {
            Product product = productMap.get(dto.getProductId());
            if (product == null) {
                throw new ProductException(ErrorCode.PRODUCT_NOT_FOUND);
            }
            product.increaseStock(dto.getQuantity());
        }
        return true;
    }

    // ProductStockDto 로 products 조회
    private Map<Long, Product> getProducts(List<ProductStockDto> productStockDtos) {
        // 상품 Id 추출
        List<Long> productIds = productStockDtos.stream()
                .map(ProductStockDto::getProductId)
                .collect(Collectors.toList());
        // 상품 조회
        List<Product> products = productRepository.findAllByProductIds(productIds);

        // map 변환
        Map<Long, Product> productMap = products.stream()
                .collect(Collectors.toMap(Product::getId, product -> product));
        return productMap;
    }
}
