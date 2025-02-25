package com.hong.productservice.service.product;

import com.hong.common.exception.ErrorCode;
import com.hong.common.exception.custom.ProductException;
import com.hong.productservice.domain.Category;
import com.hong.productservice.domain.CategoryProduct;
import com.hong.productservice.domain.Product;
import com.hong.productservice.dto.category.CategoryDto;
import com.hong.productservice.dto.product.ProductCacheDto;
import com.hong.productservice.dto.product.ProductDto;
import com.hong.productservice.dto.product.ProductPagingResponseDto;
import com.hong.productservice.dto.product.ProductResponseDto;
import com.hong.productservice.repository.ProductRepository;
import com.hong.productservice.service.category.CategoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "[ProductServiceImpl]")
@Transactional(readOnly = true)
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final CategoryService categoryService;

    // product 생성
    @Transactional
    @Override
    @CacheEvict(cacheNames = "getProducts", allEntries = true)
    public ProductResponseDto createProduct(ProductDto requestDto) {
        String title = requestDto.getTitle();
        // product 가 존재 하는지 검증
        if (productRepository.existsByTitle(title)) {
            log.debug("이미 존재하는 상품 title 입니다. title = {}", title);
            throw new ProductException(ErrorCode.PRODUCT_TITLE_ALREADY_EXISTS, title);
        }

        // product 는 여러 개의 category 를 가질 수 있다.
        // category 가 존재 하는지 검증
        List<Category> categories = categoryService.getCategoriesById(requestDto.getCategoryDtos());

        // categoryProduct 생성
        List<CategoryProduct> categoryProducts = categories.stream()
                .map(CategoryProduct::create)
                .collect(Collectors.toList());

        // product 생성
        Product product = Product.create(requestDto.getTitle(), requestDto.getPrice(), requestDto.getStock(), categoryProducts);
        Product savedProduct = productRepository.save(product);

        return convertProductResponseDtoWithStock(savedProduct);
    }

    // product 커서 기반 페이징 조회
    @Override
    //cache aside 동작 => redis 먼저 확인, 없으면 아래 로직 수행
    @Cacheable(cacheNames = "getProducts"
            , key = "'products:cursor:' + #cursor + ':size:' + #size + ':categoryId:' + (#categoryId != null ? #categoryId : '') + ':search:' + (#search != null ? #search : '')"
            , cacheManager = "productCacheManager")
    public ProductPagingResponseDto getProducts(String search, Long cursor, int size, Long categoryId) {
        // cursor 가 null 이면 가장 최근 데이터 조회 처리
        if (cursor == null) cursor = Long.MAX_VALUE;

        // PageRequest 객체로 조회 size 지정
        PageRequest pageRequest = PageRequest.of(0, size);

        // 페이징 조회
        List<ProductResponseDto> page = productRepository.findProductsByCursorAndCategoryIdAndSearchAndSize(cursor, categoryId, search, pageRequest);

        // nextCursor 지정
        Long nextCursor = page.isEmpty() ? 0 : page.get(page.size() - 1).getId();
        return new ProductPagingResponseDto(nextCursor, page);
    }

    // product 단건 조회
    @Override
    @Cacheable(cacheNames = "getProduct", key = "'products:' + #productId", cacheManager = "productCacheManager")
    public ProductCacheDto getProduct(Long productId) {
        // fetch join 으로 product, categoryProduct, category 조회
        Product product = fetchProductWithCategoryAndCategoryProductsAndValidate(productId);
        return convertProductCacheDtoWithoutStock(product);
    }

    // product 수정
    @Transactional
    @Override
    @Caching(evict = {
            @CacheEvict(cacheNames = "getProducts", allEntries = true),
            @CacheEvict(cacheNames = "getProduct", key = "'products:' + #productId")
    })
    public ProductResponseDto updateProduct(Long productId, ProductDto requestDto) {
        // fetch join 으로 product, categoryProduct, category 조회
        Product product = fetchProductWithCategoryAndCategoryProductsAndValidate(productId);

        // title 수정 요청 시에 title 이 이미 존재 하는지 검증
        if (!product.getTitle().equals(requestDto.getTitle())) {
            if (productRepository.existsByTitle(requestDto.getTitle())) {
                log.debug("이미 존재하는 상품 title 입니다. title = {}", requestDto.getTitle());
                throw new ProductException(ErrorCode.PRODUCT_TITLE_ALREADY_EXISTS, requestDto.getTitle());
            }
        }
        // product 수정, 연관 관계 적용
        updateProductAndSetAssociations(requestDto, product);
        // product 명시적으로 저장
        Product updatedProduct = productRepository.save(product);
        return convertProductResponseDtoWithStock(updatedProduct);
    }


    // product 삭제
    @Transactional
    @Override
    @Caching(evict = {
            @CacheEvict(cacheNames = "getProducts", allEntries = true),
            @CacheEvict(cacheNames = "getProduct", key = "'products:' + #productId")
    })
    public ProductResponseDto deleteProduct(Long productId) {
        // fetch join 으로 product, categoryProduct, category 조회
        Product product = fetchProductWithCategoryAndCategoryProductsAndValidate(productId);

        // 상품 삭제
        // cascade, orphanRemoval 로 categoryProducts 삭제
        productRepository.delete(product);
        return convertProductResponseDtoWithStock(product);
    }

    // fetch join 으로 product, categoryProduct, category 조회
    private Product fetchProductWithCategoryAndCategoryProductsAndValidate(Long productId) {
        return productRepository.findProductByProductIdWithCategoryProducts(productId).
                orElseThrow(() -> {
                    log.error("요청된 상품이 존재하지 않습니다. productId = {}", productId);
                    return new ProductException(ErrorCode.PRODUCT_NOT_FOUND, productId);
                });
    }

    // ProductResponseDto 변환 (stock 포함)
    private ProductResponseDto convertProductResponseDtoWithStock(Product product) {
        return new ProductResponseDto(
                product.getId(),
                product.getTitle(),
                product.getPrice(),
                product.getStock(),
                product.getCategoryProducts().stream()
                        .map(cp -> new CategoryDto(
                                cp.getCategory().getId(),
                                cp.getCategory().getTitle()))
                        .collect(Collectors.toList()));
    }

    // ProductCacheDto 변환 (stock 미 포함)
    private ProductCacheDto convertProductCacheDtoWithoutStock(Product product) {
        return new ProductCacheDto(
                product.getId(),
                product.getTitle(),
                product.getPrice(),
                product.getCategoryProducts().stream()
                        .map(cp -> new CategoryDto(
                                cp.getCategory().getId(),
                                cp.getCategory().getTitle()))
                        .collect(Collectors.toList()));
    }

    // product 수정, 연관 관계 적용
    private void updateProductAndSetAssociations(ProductDto requestDto, Product product) {
        // product 수정
        product.update(requestDto.getTitle(), requestDto.getPrice(), requestDto.getStock());

        // category 수정
        // category 가 존재하는지 검증
        List<Category> requestCategories = categoryService.getCategoriesById(requestDto.getCategoryDtos());

        // 기존에 존재 하는 categoryProduct
        List<CategoryProduct> originalCategoryProducts = product.getCategoryProducts();

        // 새로 생성 해야 할 categoryProducts
        List<CategoryProduct> newCategoryProducts = requestCategories.stream()
                .filter(category -> originalCategoryProducts.stream()
                        .noneMatch(cp -> cp.getCategory().getId().equals(category.getId())))
                .map(CategoryProduct::create) // categoryProduct Entity 생성
                .collect(Collectors.toList());

        // 기존에 존재 했지만 삭제 해야할 categoryProducts
        List<CategoryProduct> removeCategoryProducts = originalCategoryProducts.stream()
                .filter(cp -> requestCategories.stream()
                        .noneMatch(category -> category.getId().equals(cp.getCategory().getId())))
                .collect(Collectors.toList());

        // product 의 연관 관계 메서드 처리
        product.addCategoryProducts(newCategoryProducts);
        product.removeCategoryProducts(removeCategoryProducts);
    }
}
