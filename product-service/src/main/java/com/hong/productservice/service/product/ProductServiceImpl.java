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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.RedisTemplate;
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
    private final RedisTemplate<String, Object> redisTemplate;

    // product 생성
    @Transactional
    @Override
    @CacheEvict(cacheNames = "getProducts", allEntries = true)
    public ProductResponseDto createProduct(ProductDto requestDto) {
        // 1. 이미 존재 하는 title 인지 검증
        validateExistsTitle(requestDto.getTitle());

        // 2. category 가 존재 하는지 검증
        // product 는 여러 개의 category 를 가질 수 있다.
        List<Category> categories = categoryService.getCategoriesById(requestDto.getCategoryDtos());

        // 3. categoryProduct 생성
        List<CategoryProduct> categoryProducts = categories.stream()
                .map(CategoryProduct::create)
                .collect(Collectors.toList());

        // 4. product 생성
        Product product = Product.create(requestDto.getTitle(), requestDto.getPrice(), requestDto.getStock(), categoryProducts);

        // 5. product 저장
        Product savedProduct = productRepository.save(product);

        // 6. 응답 Dto 변환
        return convertProductResponseDtoWithStock(savedProduct);
    }

    // product 커서 기반 페이징 조회
    @Override
    @Cacheable(cacheNames = "getProducts"
            , key = "'products:cursor:' + #cursor + ':size:' + #size + ':categoryId:' + (#categoryId != null ? #categoryId : '') + ':search:' + (#search != null ? #search : '')"
            , cacheManager = "productCacheManager")
    public ProductPagingResponseDto getProducts(String search, Long cursor, int size, Long categoryId) {
        // 1. products 페이징 조회(cursor 기반)
        List<ProductResponseDto> page = fetchProductsByCursorAndCategoryId(search, cursor, size, categoryId);

        // 2. 응답 Dto 변환, cursor 지정
        return converToProductPagingResponse(page);
    }

    // product 단건 조회
    @Override
    @Cacheable(cacheNames = "getProduct", key = "'products:' + #productId", cacheManager = "productCacheManager")
    public ProductCacheDto getProduct(Long productId) {
        // 1. product 조회 (categoryProduct, category fetch join) 및 검증
        Product product = fetchProductWithCategoryAndCategoryProductsAndValidate(productId);

        // 2. 응답 Dto 변환
        return convertProductCacheDtoWithoutStock(product);
    }

    // product 수정
    @Transactional
    @Override
    @CacheEvict(cacheNames = "getProduct", key = "'products:' + #productId")
    public ProductResponseDto updateProduct(Long productId, ProductDto requestDto) {
        // 1. product 조회 (categoryProduct, category fetch join) 및 검증
        Product product = fetchProductWithCategoryAndCategoryProductsAndValidate(productId);

        // 2. product 수정 및 product 페이징 조회 전면 무효화
        Product updatedProduct = updateProductFieldsAndValidateEvict(requestDto, product);

        // 3. 응답 Dto 변환
        return convertProductResponseDtoWithStock(updatedProduct);
    }



    // product 삭제
    @Transactional
    @Override
    @CacheEvict(cacheNames = "getProduct", key = "'products:' + #productId")
    public ProductResponseDto deleteProduct(Long productId) {
        // 1. product 조회 (categoryProduct, category fetch join) 및 검증
        Product product = fetchProductWithCategoryAndCategoryProductsAndValidate(productId);

        // 2. product 삭제 및 product 페이징 조회 전면 무효화
        deleteProductAndEvict(product);

        // 3. 응답 Dto 변환
        return convertProductResponseDtoWithStock(product);
    }

    private void deleteProductAndEvict(Product product) {
        List<CategoryProduct> categoryProducts = product.getCategoryProducts();

        // 상품 삭제
        // cascade, orphanRemoval 로 categoryProducts 삭제
        productRepository.delete(product);

        // categoryId 같은 getProducts() 캐싱 삭제
        categoryProducts.forEach(cp -> {
            String pattern = "getProducts::products:*:categoryId:" + cp.getCategory().getId() + ":*";
            redisTemplate.delete(redisTemplate.keys(pattern));
        });
    }

    // 이미 존재 하는 title 인지 DB 확인
    private void validateExistsTitle(String title) {
        // product 가 존재 하는지 검증
        if (productRepository.existsByTitle(title)) {
            log.debug("이미 존재하는 상품 title 입니다. title = {}", title);
            throw new ProductException(ErrorCode.PRODUCT_TITLE_ALREADY_EXISTS, title);
        }
    }

    // fetch join 으로 product, categoryProduct, category 조회
    private Product fetchProductWithCategoryAndCategoryProductsAndValidate(Long productId) {
        return productRepository.findProductByProductIdWithCategoryProducts(productId).
                orElseThrow(() -> {
                    log.error("요청된 상품이 존재하지 않습니다. productId = {}", productId);
                    return new ProductException(ErrorCode.PRODUCT_NOT_FOUND, productId);
                });
    }

    private ProductPagingResponseDto converToProductPagingResponse(List<ProductResponseDto> page) {
        Long nextCursor = page.isEmpty() ? 0 : page.get(page.size() - 1).getId();
        return new ProductPagingResponseDto(nextCursor, page);
    }

    private List<ProductResponseDto> fetchProductsByCursorAndCategoryId(String search, Long cursor, int size, Long categoryId) {
        // cursor 가 null 이면 가장 최근 데이터 조회 처리
        if (cursor == null) cursor = Long.MAX_VALUE;

        // PageRequest 객체로 조회 size 지정
        PageRequest pageRequest = PageRequest.of(0, size);

        // 페이징 조회
        List<ProductResponseDto> page = productRepository.findProductsByCursorAndCategoryIdAndSearchAndSize(cursor, categoryId, search, pageRequest);
        return page;
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
    private Product updateProductFieldsAndValidateEvict(ProductDto requestDto, Product product) {
        List<CategoryProduct> categoryProducts = product.getCategoryProducts();
        // title 수정 요청 시에 title 이 이미 존재 하는지 검증
        if (!product.getTitle().equals(requestDto.getTitle())) {
            validateExistsTitle(requestDto.getTitle());
        }
        // product 수정, 연관 관계 적용
        updateProductAndSetAssociations(requestDto, product);
        // product 명시적으로 저장
        Product updatedProduct = productRepository.save(product);
        // categoryId 같은 getProducts() 캐싱 삭제
        categoryProducts.forEach(cp -> {
            String pattern = "getProducts::products:*:categoryId:" + cp.getCategory().getId() + ":*";
            redisTemplate.delete(redisTemplate.keys(pattern));
        });
        return updatedProduct;
    }
}
