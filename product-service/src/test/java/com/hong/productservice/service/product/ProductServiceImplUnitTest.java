package com.hong.productservice.service.product;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
class ProductServiceImplUnitTest {

    @InjectMocks
    ProductServiceImpl productService;
    @Mock
    ProductRepository productRepository;
    @Mock
    CategoryService categoryService;
    @Mock
    RedisTemplate<String, Object> redisTemplate;

    private Category category;
    @BeforeEach
    public void setUp() {
        category = Category.create("category");
        ReflectionTestUtils.setField(category, "id", 1L);
    }

    private Product createTestProduct(long id, String title, int price, int stock, List<CategoryProduct> cp) {
        Product product = Product.create(title, price, stock, cp);
        ReflectionTestUtils.setField(product, "id", id);
        return product;
    }

    @Test
    @DisplayName("product 생성_성공")
    public void createProduct_success(){
        //given
        String productTitle = "product";
        Long productId = 1L;
        int price = 1000;
        int stock = 1000;
        Product product = createTestProduct(productId, productTitle, price, stock, List.of(CategoryProduct.create(category)));
        List<CategoryDto> categoryDtos = List.of(new CategoryDto(1L));

        ProductDto requestDto = new ProductDto(productTitle, price, stock, categoryDtos);
        when(productRepository.existsByTitle(requestDto.getTitle())).thenReturn(false);
        when(categoryService.getCategoriesById(categoryDtos)).thenReturn(List.of(Category.create("category")));
        when(productRepository.save(any(Product.class))).thenReturn(product);

        //when
        ProductResponseDto productResponseDto = productService.createProduct(requestDto);

        //then
        assertThat(productResponseDto.getId()).isEqualTo(productId);
        assertThat(productResponseDto.getTitle()).isEqualTo(productTitle);
        assertThat(productResponseDto.getPrice()).isEqualTo(price);
        assertThat(productResponseDto.getStock()).isEqualTo(stock);
        assertThat(productResponseDto.getCategories()).hasSize(1);
    }

    @Test
    @DisplayName("product 생성_실패_이미 존재 하는 product title")
    public void createProduct_failure_exists_title(){
        //given
        String productTitle = "product";
        int price = 1000;
        int stock = 1000;
        List<CategoryDto> categoryDtos = List.of(new CategoryDto(1L));

        ProductDto requestDto = new ProductDto(productTitle, price, stock, categoryDtos);
        when(productRepository.existsByTitle(requestDto.getTitle())).thenReturn(true);

        //when && then
        assertThatThrownBy(() -> productService.createProduct(requestDto)).isInstanceOf(ProductException.class);
    }

    @Test
    @DisplayName("products 페이징 조회")
    public void getProducts(){
        // given
        Long cursor = null;
        String search = "product";
        int size = 5;
        Long categoryId = 1L;

        List<ProductResponseDto> productResponseDtos = List.of(
                new ProductResponseDto(1L, "product1", 1000, 10, new ArrayList<>()),
                new ProductResponseDto(2L, "product2", 2000, 20, new ArrayList<>()),
                new ProductResponseDto(3L, "product3", 3000, 30, new ArrayList<>()),
                new ProductResponseDto(4L, "product4", 4000, 40, new ArrayList<>()),
                new ProductResponseDto(5L, "product5", 5000, 50, new ArrayList<>())
        );
        when(productRepository.findProductsByCursorAndCategoryIdAndSearchAndSize(any(Long.class), eq(categoryId), eq(search), any(PageRequest.class)))
                .thenReturn(productResponseDtos);

        // when
        ProductPagingResponseDto response = productService.getProducts(search, cursor, size, categoryId);

        //then
        assertThat(response.getNextCursor()).isEqualTo(5);
        assertThat(response.getProductResponseDtos()).hasSize(5);
    }

    @Test
    @DisplayName("product 단건 조회_성공")
    public void getProduct_success(){
        // given
        Long productId = 1L;
        String productTitle = "product";
        int price = 1000;
        int stock = 100;
        Product product = createTestProduct(productId, productTitle, price, stock, List.of());


        when(productRepository.findProductByProductIdWithCategoryProducts(productId)).thenReturn(Optional.of(product));
        // when
        ProductCacheDto result = productService.getProduct(productId);

        //then
        assertThat(result.getId()).isEqualTo(productId);
        assertThat(result.getTitle()).isEqualTo(productTitle);
        assertThat(result.getPrice()).isEqualTo(price);
    }

    @Test
    @DisplayName("product 단건 조회_실패_존재 하지 않는 product")
    public void getProduct_failure_notFoundProduct(){
        // given
        Long productId = 1L;
        when(productRepository.findProductByProductIdWithCategoryProducts(productId)).thenReturn(Optional.empty());

        // when && then
        assertThatThrownBy(() -> productService.getProduct(productId)).isInstanceOf(ProductException.class);
    }

    @Test
    @DisplayName("product 수정_성공")
    public void updateProduct_success(){
        //given
        Long productId = 1L;
        String productTitle = "product";
        int price = 1000;
        int stock = 100;
        String categoryTitle = "category";

        Product product = createTestProduct(productId, "originalTitle", price, stock,
                List.of(CategoryProduct.create(Category.create(categoryTitle))));

        Product updatedProduct = Product.create(productTitle, price, stock,
                List.of(CategoryProduct.create(Category.create(categoryTitle))));
        ReflectionTestUtils.setField(updatedProduct, "id", productId);
        Long targetId = product.getId();

        when(productRepository.findProductByProductIdWithCategoryProducts(productId)).thenReturn(Optional.of(product));
        when(productRepository.existsByTitle(any(String.class))).thenReturn(false);
        when(productRepository.save(any(Product.class))).thenReturn(updatedProduct);
        ProductDto requestDto = new ProductDto(productTitle, price, stock, List.of());

        //when
        ProductResponseDto result = productService.updateProduct(targetId, requestDto);

        //then
        assertThat(result.getId()).isEqualTo(productId);
        assertThat(result.getTitle()).isEqualTo(productTitle);
        assertThat(result.getPrice()).isEqualTo(price);
        assertThat(result.getStock()).isEqualTo(stock);
        assertThat(result.getCategories().get(0).getTitle()).isEqualTo(categoryTitle);
    }

    @Test
    @DisplayName("product 수정_실패_존재 하지 않는 product")
    public void updateProduct_failure_notFoundProduct(){
        //given
        Long productId = 1L;
        String productTitle = "product";
        int price = 1000;
        int stock = 100;
        Product product = createTestProduct(productId, "originalProduct", 10, 10, List.of());

        ReflectionTestUtils.setField(product, "id", productId);
        Long targetId = product.getId();

        when(productRepository.findProductByProductIdWithCategoryProducts(productId)).thenReturn(Optional.empty());
        ProductDto requestDto = new ProductDto(productTitle, price, stock, List.of());

        //when && then
        assertThatThrownBy(() -> productService.updateProduct(targetId, requestDto)).isInstanceOf(ProductException.class);
    }

    @Test
    @DisplayName("product 수정_실패_이미 존재 하는 product title")
    public void updateProduct_failure_exists_product_title(){
        //given
        Long productId = 1L;
        String productTitle = "product";
        int price = 1000;
        int stock = 100;
        Product product = createTestProduct(productId, "originalProduct", 10, 10, List.of());

        Long targetId = product.getId();

        when(productRepository.findProductByProductIdWithCategoryProducts(productId)).thenReturn(Optional.of(product));
        when(productRepository.existsByTitle(any(String.class))).thenReturn(true);

        ProductDto requestDto = new ProductDto(productTitle, price, stock, List.of());

        //when && then
        assertThatThrownBy(() -> productService.updateProduct(targetId, requestDto)).isInstanceOf(ProductException.class);
    }

    @Test
    @DisplayName("product 삭제_성공")
    public void deleteProduct_success(){
        //given
        Long productId = 1L;
        String productTitle = "product";
        int price = 1000;
        int stock = 100;
        String categoryTitle = "category";

        Product product = Product.create(productTitle, price, stock, List.of(CategoryProduct.create(Category.create(categoryTitle))));
        ReflectionTestUtils.setField(product, "id", productId);
        Long targetId = product.getId();

        when(productRepository.findProductByProductIdWithCategoryProducts(productId)).thenReturn(Optional.of(product));

        //when
        ProductResponseDto result = productService.deleteProduct(targetId);

        //then
        assertThat(result.getId()).isEqualTo(productId);
        assertThat(result.getTitle()).isEqualTo(productTitle);
        assertThat(result.getPrice()).isEqualTo(price);
        assertThat(result.getStock()).isEqualTo(stock);
        assertThat(result.getCategories()).hasSize(1);
        assertThat(result.getCategories().get(0).getTitle()).isEqualTo(categoryTitle);
    }

    @Test
    @DisplayName("product 삭제_실패_존재 하지 않는 product")
    public void deleteProduct_failure_notFoundProduct(){
        //given
        Long productId = 1L;
        when(productRepository.findProductByProductIdWithCategoryProducts(productId)).thenReturn(Optional.empty());

        //when && then
        assertThatThrownBy(() -> productService.deleteProduct(productId)).isInstanceOf(ProductException.class);
    }

}