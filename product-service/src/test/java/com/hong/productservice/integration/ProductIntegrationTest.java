package com.hong.productservice.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hong.common.dto.ProductStockCheckRequestDto;
import com.hong.common.dto.ProductStockUpdateRequestDto;
import com.hong.common.exception.ErrorCode;
import com.hong.productservice.domain.Category;
import com.hong.productservice.domain.CategoryProduct;
import com.hong.productservice.domain.Product;
import com.hong.productservice.repository.CategoryRepository;
import com.hong.productservice.repository.ProductRepository;
import com.hong.productservice.web.dto.product.ProductRequestDto;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class ProductIntegrationTest {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    ObjectMapper objectMapper;
    @Autowired
    CategoryRepository categoryRepository;
    @Autowired
    ProductRepository productRepository;
    @Autowired
    EntityManager em;
    @Autowired
    RedisTemplate<String, Object> redisTemplate;

    @BeforeEach
    void setUp(){
        redisTemplate.getConnectionFactory().getConnection().serverCommands().flushAll();
    }

    private Category createTestCategory(String categoryTitle){
        Category category = Category.create(categoryTitle);
        categoryRepository.save(category);
        return category;
    }

    private CategoryProduct createTestCategoryProduct(Category category){
        return  CategoryProduct.create(category);
    }

    private Product createTestProduct(String title, int price, int stock, List<CategoryProduct> cp){
        Product product = Product.create(title, price, stock, cp);
        productRepository.save(product);
        return product;
    }

    @Test
    @Transactional
    @DisplayName("product 생성_성공")
    public void createProduct_success() throws Exception {
        //given
        String title = "product";
        int price = 1000;
        int stock = 100;
        Category category = createTestCategory("category");
        ProductRequestDto requestDto = new ProductRequestDto(title, price, stock, List.of(category.getId()));

        //when && then
        mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("상품 생성 완료"))
                .andExpect(jsonPath("$.data.id").exists())
                .andExpect(jsonPath("$.data.title").value(title))
                .andExpect(jsonPath("$.data.price").value(price))
                .andExpect(jsonPath("$.data.stock").value(stock))
                .andExpect(jsonPath("$.data.categories[0].id").value(category.getId()))
                .andExpect(jsonPath("$.data.categories[0].title").value(category.getTitle()));
    }

    @ParameterizedTest
    @CsvSource({
            "'', 100, 10, '1', 'NotBlank', 'title은 필수입니다.'",
            "'a', 100, 10, '1', 'Size', 'title은 2 글자에서 20 글자입니다.'",
            "'product', null, 10, '1', 'NotNull', 'price는 필수입니다.'",
            "'product', -1, 10, '1', 'Min', 'price는 0 이상이어야 합니다.'",
            "'product', 100, null, '1', 'NotNull', 'stock은 필수입니다.'",
            "'product', 100, -5, '1', 'Min', 'stock은 0 이상이어야 합니다.'",
            "'product', 100, 10, '', 'NotEmpty', 'categoryIds는 최소 1개 이상이어야 합니다.'"
    })
    @DisplayName("product 생성_실패_입력 값 오류")
    public void createProduct_failure_invalidInput(String title,
                                                   String priceStr,
                                                   String stockStr,
                                                   String strCategoryIds,
                                                   String expectedError,
                                                   String expectedValue) throws Exception {
        //given
        Integer price = "null".equals(priceStr) ? null : Integer.valueOf(priceStr);
        Integer stock = "null".equals(stockStr) ? null : Integer.valueOf(stockStr);
        List<Long> categoryIds;
        if (strCategoryIds == null || strCategoryIds.isEmpty()) categoryIds = new ArrayList<>();
        else categoryIds = List.of(Long.valueOf(strCategoryIds));
        ProductRequestDto requestDto = new ProductRequestDto(title, price, stock, categoryIds);

        //when && then
        mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[?(@.code == '" + expectedError + "')].defaultMessage").value(expectedValue));
    }

    @Test
    @Transactional
    @DisplayName("product 페이징 조회_성공_categoryId 필터링")
    public void getProducts_success_with_categoryId() throws Exception {
        //given
        Category category1 = createTestCategory("smartPhone");
        Category category2 = createTestCategory("computer");
        Long nextCursor = null;
        for(int i = 1; i < 10; i++){
            if(i % 2 == 0){
                CategoryProduct categoryProduct = createTestCategoryProduct(category1);
                createTestProduct("galaxy" + i, 1000, 100, List.of(categoryProduct));
                if(i == 4) nextCursor = Integer.toUnsignedLong(i);
            }
            else{
                CategoryProduct categoryProduct = createTestCategoryProduct(category2);
                createTestProduct("gram" + i, 1000, 100, List.of(categoryProduct));
            }
        }
        //when && then
        mockMvc.perform(get("/products")
                        .param("cursor", "100")
                        .param("size", "3")
                        .param("categoryId", Long.toString(category1.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("상품 조회 완료"))
                .andExpect(jsonPath("$.data.nextCursor").value(nextCursor))
                .andExpect(jsonPath("$.data.products", hasSize(3)))
                .andExpect(jsonPath("$.data.products[0].id").exists())
                .andExpect(jsonPath("$.data.products[0].title").value("galaxy8"))
                .andExpect(jsonPath("$.data.products[0].price").value(1000))
                .andExpect(jsonPath("$.data.products[1].id").exists())
                .andExpect(jsonPath("$.data.products[1].title").value("galaxy6"))
                .andExpect(jsonPath("$.data.products[1].price").value(1000))
                .andExpect(jsonPath("$.data.products[2].id").exists())
                .andExpect(jsonPath("$.data.products[2].title").value("galaxy4"))
                .andExpect(jsonPath("$.data.products[2].price").value(1000));
    }

    @Test
    @Transactional
    @DisplayName("product 페이징 조회_성공_search 필터링")
    public void getProducts_success_with_search() throws Exception {
        //given
        Category category1 = createTestCategory("smartPhone");
        Category category2 = createTestCategory("computer");
        Long nextCursor = null;
        for(int i = 1; i < 10; i++){
            if(i % 2 == 0){
                CategoryProduct categoryProduct = createTestCategoryProduct(category1);
                Product product = createTestProduct("galaxy" + i, 1000, 100, List.of(categoryProduct));
                if(i == 4) nextCursor = product.getId();
            }
            else{
                CategoryProduct categoryProduct = createTestCategoryProduct(category2);
                createTestProduct("gram" + i, 1000, 100, List.of(categoryProduct));
            }
        }

        //when && then
        mockMvc.perform(get("/products")
                        .param("cursor", "100")
                        .param("size", "3")
                        .param("search", "galaxy")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("상품 조회 완료"))
                .andExpect(jsonPath("$.data.nextCursor").value(nextCursor))
                .andExpect(jsonPath("$.data.products", hasSize(3)))
                .andExpect(jsonPath("$.data.products[0].id").exists())
                .andExpect(jsonPath("$.data.products[0].title").value("galaxy8"))
                .andExpect(jsonPath("$.data.products[0].price").value(1000))
                .andExpect(jsonPath("$.data.products[1].id").exists())
                .andExpect(jsonPath("$.data.products[1].title").value("galaxy6"))
                .andExpect(jsonPath("$.data.products[1].price").value(1000))
                .andExpect(jsonPath("$.data.products[2].id").exists())
                .andExpect(jsonPath("$.data.products[2].title").value("galaxy4"))
                .andExpect(jsonPath("$.data.products[2].price").value(1000));
    }

    @Test
    @Transactional
    @DisplayName("product 페이징 조회_성공_search, categoryId 필터링")
    public void getProducts_success_with_searchAndCategoryId() throws Exception {
        //given
        Category category1 = createTestCategory("smartPhone");
        Category category2 = createTestCategory("computer");
        Long nextCursor = null;
        for(int i = 1; i < 10; i++){
            if(i % 2 == 0){
                CategoryProduct categoryProduct = createTestCategoryProduct(category1);
                Product product = createTestProduct("galaxy" + i, 1000, 100, List.of(categoryProduct));
                if(i == 4) nextCursor = product.getId();
            }
            else{
                CategoryProduct categoryProduct = createTestCategoryProduct(category2);
                createTestProduct("gram" + i, 1000, 100, List.of(categoryProduct));
            }
        }
        //when && then
        mockMvc.perform(get("/products")
                        .param("cursor", "100")
                        .param("size", "3")
                        .param("search", "4")
                        .param("categoryId", Long.toString(category1.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("상품 조회 완료"))
                .andExpect(jsonPath("$.data.nextCursor").value(nextCursor))
                .andExpect(jsonPath("$.data.products[0].id").exists())
                .andExpect(jsonPath("$.data.products[0].title").value("galaxy4"))
                .andExpect(jsonPath("$.data.products[0].price").value(1000));
    }

    @Test
    @Transactional
    @DisplayName("product 단건 조회_성공")
    public void getProduct_success() throws Exception {
        //given
        Category category = createTestCategory("category");
        CategoryProduct categoryProduct = createTestCategoryProduct(category);
        Product product = createTestProduct("product", 1000, 100, List.of(categoryProduct));

        //when && then
        mockMvc.perform(get("/products/" + product.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("상품 조회 완료"))
                .andExpect(jsonPath("$.data.id").value(product.getId()))
                .andExpect(jsonPath("$.data.title").value(product.getTitle()))
                .andExpect(jsonPath("$.data.price").value(product.getPrice()))
                .andExpect(jsonPath("$.data.categories[0].id").value(category.getId()))
                .andExpect(jsonPath("$.data.categories[0].title").value(category.getTitle()));
    }

    @Test
    @DisplayName("product 단건 조회_실패_존재 하지 않는 product")
    public void getProduct_failure_notFoundProduct() throws Exception {
        //given
        Long productId = 1L;
        String expectedErrorMessage = String.format(ErrorCode.PRODUCT_NOT_FOUND.getErrorMessage(), productId);

        //when && then
        mockMvc.perform(get("/products/" + productId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value(ErrorCode.PRODUCT_NOT_FOUND.getErrorCode()))
                .andExpect(jsonPath("$.errorMessage").value(expectedErrorMessage));
    }

    @Test
    @Transactional
    @DisplayName("product 조회(재고 포함) 조회_성공")
    public void getProductStock_success() throws Exception {
        //given
        ArrayList<Long> productIds = new ArrayList<>();
        Category category = createTestCategory("category");
        for(int i = 1; i <= 2; i++){
            CategoryProduct categoryProduct = createTestCategoryProduct(category);
            Product product = createTestProduct("product" + i, 1000, 100, List.of(categoryProduct));
            productIds.add(product.getId());
        }
        String[] productIdsArray = productIds.stream()
                .map(String::valueOf)
                .toArray(String[]::new);

        //when && then
        mockMvc.perform(get("/products/stock")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .param("productIds", productIdsArray))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("상품 재고 조회 완료"))
                .andExpect(jsonPath("$.data[0].productId").exists())
                .andExpect(jsonPath("$.data[0].title", startsWith("product")))
                .andExpect(jsonPath("$.data[0].price").value(1000))
                .andExpect(jsonPath("$.data[0].stock").value(100))
                .andExpect(jsonPath("$.data[1].productId").exists())
                .andExpect(jsonPath("$.data[1].title", startsWith("product")))
                .andExpect(jsonPath("$.data[1].price").value(1000))
                .andExpect(jsonPath("$.data[1].stock").value(100));
    }

    @Test
    @DisplayName("product 조회(재고 포함) 조회_실패_존재 하지 않는 상품")
    public void getProductStock_failure_notFoundProduct() throws Exception {
        //given
        ArrayList<Long> productIds = new ArrayList<>();
        productIds.add(1L);
        productIds.add(2L);
        String[] productIdsArray = productIds.stream()
                .map(String::valueOf)
                .toArray(String[]::new);
        String expectedErrorMessage = String.format(ErrorCode.PRODUCT_NOT_FOUND.getErrorMessage(), productIds);

        //when && then
        mockMvc.perform(get("/products/stock")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .param("productIds", productIdsArray))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value(ErrorCode.PRODUCT_NOT_FOUND.getErrorCode()))
                .andExpect(jsonPath("$.errorMessage").value(expectedErrorMessage));
    }

    @Test
    @Transactional
    @DisplayName("product 수정_성공")
    public void updateProduct_success() throws Exception {
        //given
        Category category = createTestCategory("category");
        CategoryProduct categoryProduct = createTestCategoryProduct(category);
        Product product = createTestProduct("product", 1000, 100, List.of(categoryProduct));

        String newTitle = "new";
        int newPrice = 100000;
        int newStock = 10000;
        ProductRequestDto requestDto = new ProductRequestDto(newTitle, newPrice, newStock, List.of(category.getId()));

        //when && then
        mockMvc.perform(put("/products/" + product.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("상품 수정 완료"))
                .andExpect(jsonPath("$.data.id").exists())
                .andExpect(jsonPath("$.data.title").value(newTitle))
                .andExpect(jsonPath("$.data.price").value(newPrice))
                .andExpect(jsonPath("$.data.stock").value(newStock))
                .andExpect(jsonPath("$.data.categories[0].id").value(category.getId()))
                .andExpect(jsonPath("$.data.categories[0].title").value(category.getTitle()));
    }

    @Test
    @Transactional
    @DisplayName("product 수정_실패_존재 하지 않는 product")
    public void updateProduct_failure_notFoundProduct() throws Exception {
        //given
        Long categoryId = 1L;
        Long productId = 1L;

        String newTitle = "new";
        int newPrice = 100000;
        int newStock = 10000;
        ProductRequestDto requestDto = new ProductRequestDto(newTitle, newPrice, newStock, List.of(categoryId));
        String expectedErrorMessage = String.format(ErrorCode.PRODUCT_NOT_FOUND.getErrorMessage(), productId);

        //when && then
        mockMvc.perform(put("/products/" + productId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value(ErrorCode.PRODUCT_NOT_FOUND.getErrorCode()))
                .andExpect(jsonPath("$.errorMessage").value(expectedErrorMessage));
    }

    @Test
    @Transactional
    @DisplayName("product 수정_실패_이미 존재 하는 product Title")
    public void updateProduct_failure_already_exists_title() throws Exception {
        //given
        Category category = createTestCategory("category");
        CategoryProduct categoryProduct1 = createTestCategoryProduct(category);
        Product product1 = createTestProduct("product" + 1, 1000, 100, List.of(categoryProduct1));
        CategoryProduct categoryProduct2 = createTestCategoryProduct(category);
        createTestProduct("product" + 2, 1000, 100, List.of(categoryProduct2));

        String newTitle = "product2";
        int newPrice = 100000;
        int newStock = 10000;
        ProductRequestDto requestDto = new ProductRequestDto(newTitle, newPrice, newStock, List.of(category.getId()));
        String expectedErrorMessage = String.format(ErrorCode.PRODUCT_TITLE_ALREADY_EXISTS.getErrorMessage(), newTitle);

        //when && then
        mockMvc.perform(put("/products/" + product1.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value(ErrorCode.PRODUCT_TITLE_ALREADY_EXISTS.getErrorCode()))
                .andExpect(jsonPath("$.errorMessage").value(expectedErrorMessage));
    }

    @ParameterizedTest
    @CsvSource({
            "'', 100, 10, '1', 'NotBlank', 'title은 필수입니다.'",
            "'a', 100, 10, '1', 'Size', 'title은 2 글자에서 20 글자입니다.'",
            "'product', null, 10, '1', 'NotNull', 'price는 필수입니다.'",
            "'product', -1, 10, '1', 'Min', 'price는 0 이상이어야 합니다.'",
            "'product', 100, null, '1', 'NotNull', 'stock은 필수입니다.'",
            "'product', 100, -5, '1', 'Min', 'stock은 0 이상이어야 합니다.'",
            "'product', 100, 10, '', 'NotEmpty', 'categoryIds는 최소 1개 이상이어야 합니다.'"
    })
    @Transactional
    @DisplayName("product 수정_실패_입력 값 오류")
    public void updateProduct_failure_invalidInput(String title,
                                                   String priceStr,
                                                   String stockStr,
                                                   String strCategoryIds,
                                                   String expectedError,
                                                   String expectedValue) throws Exception {
        //given
        Category category = createTestCategory("category");
        CategoryProduct categoryProduct = createTestCategoryProduct(category);
        Product product = createTestProduct("product", 1000, 100, List.of(categoryProduct));

        Integer price = "null".equals(priceStr) ? null : Integer.valueOf(priceStr);
        Integer stock = "null".equals(stockStr) ? null : Integer.valueOf(stockStr);

        List<Long> categoryIds;
        if (strCategoryIds == null || strCategoryIds.isEmpty()) categoryIds = new ArrayList<>();
        else categoryIds = List.of(Long.valueOf(strCategoryIds));
        ProductRequestDto requestDto = new ProductRequestDto(title, price, stock, categoryIds);

        //when && then
        mockMvc.perform(put("/products/" + product.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[?(@.code == '" + expectedError + "')].defaultMessage").value(expectedValue));
    }

    @Test
    @DisplayName("product 삭제_성공")
    public void deleteProduct_success() throws Exception {
        //given
        Category category = createTestCategory("category");
        CategoryProduct categoryProduct = createTestCategoryProduct(category);
        Product product = createTestProduct("product", 1000, 100, List.of(categoryProduct));

        //when && then
        mockMvc.perform(delete("/products/" + product.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("상품 삭제 완료"))
                .andExpect(jsonPath("$.data.id").value(product.getId()))
                .andExpect(jsonPath("$.data.title").value(product.getTitle()))
                .andExpect(jsonPath("$.data.price").value(product.getPrice()))
                .andExpect(jsonPath("$.data.stock").value(product.getStock()))
                .andExpect(jsonPath("$.data.categories[0].id").value(category.getId()))
                .andExpect(jsonPath("$.data.categories[0].title").value(category.getTitle()));
    }

    @Test
    @DisplayName("product 삭제_실패_존재 하지 않는 product")
    public void deleteProduct_failure_notFoundProduct() throws Exception {
        //given
        Long productId = 1L;
        String expectedErrorMessage = String.format(ErrorCode.PRODUCT_NOT_FOUND.getErrorMessage(), productId);

        //when && then
        mockMvc.perform(delete("/products/" + productId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value(ErrorCode.PRODUCT_NOT_FOUND.getErrorCode()))
                .andExpect(jsonPath("$.errorMessage").value(expectedErrorMessage));
    }
//???
    @Test
    @Transactional
    @DisplayName("products 조회(재고 포함) 및 (요청 수량 < 재고)검증_성공")
    public void fetchProducts_success() throws Exception {
        //given
        ArrayList<ProductStockCheckRequestDto> requestDtos = new ArrayList<>();
        Category category = createTestCategory("category");
        for(int i = 1; i <= 2; i++){
            CategoryProduct categoryProduct = createTestCategoryProduct(category);
            Product product = createTestProduct("product" + i, 1000, 100, List.of(categoryProduct));
            requestDtos.add(new ProductStockCheckRequestDto(product.getId(), 10));
        }

        //when && then
        mockMvc.perform(post("/product-service/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDtos)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.[0].productId").exists())
                .andExpect(jsonPath("$.[0].title", startsWith("product")))
                .andExpect(jsonPath("$.[0].quantity").value(10))
                .andExpect(jsonPath("$.[0].price").value(1000))
                .andExpect(jsonPath("$.[1].productId").exists())
                .andExpect(jsonPath("$.[1].title", startsWith("product")))
                .andExpect(jsonPath("$.[1].quantity").value(10))
                .andExpect(jsonPath("$.[1].price").value(1000));
    }

    @Test
    @DisplayName("products 조회(재고 포함) 및 (요청 수량 < 재고)검증_실패_존재 하지 않는 product")
    public void fetchProducts_failure_notFoundProduct() throws Exception {
        //given
        ArrayList<ProductStockCheckRequestDto> requestDtos = new ArrayList<>();
        requestDtos.add(new ProductStockCheckRequestDto(1L, 10));
        requestDtos.add(new ProductStockCheckRequestDto(2L, 10));
        List<Long> productIds = requestDtos.stream()
                .map(ProductStockCheckRequestDto::getProductId)
                .toList();
        String expectedErrorMessage = String.format(ErrorCode.PRODUCT_NOT_FOUND.getErrorMessage(), productIds);

        //when && then
        mockMvc.perform(post("/product-service/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDtos)))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value(ErrorCode.PRODUCT_NOT_FOUND.getErrorCode()))
                .andExpect(jsonPath("$.errorMessage").value(expectedErrorMessage));;
    }

    @Test
    @Transactional
    @DisplayName("products 조회(재고 포함) 및 (요청 수량 < 재고)검증_실패_재고 부족")
    public void fetchProducts_failure_notEnoughStock() throws Exception {
        //given
        ArrayList<Long> productIds = new ArrayList<>();
        Category category = createTestCategory("category");
        for(int i = 1; i <= 2; i++){
            CategoryProduct categoryProduct = createTestCategoryProduct(category);
            Product product = createTestProduct("product" + i, 1000, 100, List.of(categoryProduct));
            productIds.add(product.getId());
        }

        ArrayList<ProductStockCheckRequestDto> requestDtos = new ArrayList<>();
        requestDtos.add(new ProductStockCheckRequestDto(productIds.get(0), 200));
        requestDtos.add(new ProductStockCheckRequestDto(productIds.get(1), 200));

        String expectedErrorMessage = String.format(ErrorCode.PRODUCT_STOCK_NOT_ENOUGH.getErrorMessage(), productIds);

        //when && then
        mockMvc.perform(post("/product-service/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDtos)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value(ErrorCode.PRODUCT_STOCK_NOT_ENOUGH.getErrorCode()))
                .andExpect(jsonPath("$.errorMessage").value(expectedErrorMessage));
    }

    @Test
    @Transactional
    @DisplayName("products 재고 감소_성공")
    public void decreaseStock_success() throws Exception {
        //given
        ArrayList<Long> productIds = new ArrayList<>();
        Category category = createTestCategory("category");
        for(int i = 1; i <= 2; i++){
            CategoryProduct categoryProduct = createTestCategoryProduct(category);
            Product product = createTestProduct("product" + i, 1000, 100, List.of(categoryProduct));
            productIds.add(product.getId());
        }

        ArrayList<ProductStockUpdateRequestDto> requestDtos = new ArrayList<>();
        requestDtos.add(new ProductStockUpdateRequestDto(productIds.get(0), 5));
        requestDtos.add(new ProductStockUpdateRequestDto(productIds.get(1), 10));

        //when && then
        mockMvc.perform(post("/product-service/decrease-stock")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDtos)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].productId").exists())
                .andExpect(jsonPath("$[0].title", startsWith("product")))
                .andExpect(jsonPath("$[0].requestedQuantity").value(5))
                .andExpect(jsonPath("$[1].productId").exists())
                .andExpect(jsonPath("$[1].title", startsWith("product")))
                .andExpect(jsonPath("$[1].requestedQuantity").value(10));
    }

    @Test
    @DisplayName("products 재고 감소_실패_존재 하지 않는 상품")
    public void decreaseStock_failure_notFoundProduct() throws Exception {
        //given
        ArrayList<ProductStockUpdateRequestDto> requestDtos = new ArrayList<>();
        requestDtos.add(new ProductStockUpdateRequestDto(1L, 5));
        requestDtos.add(new ProductStockUpdateRequestDto(2L, 10));
        String expectedErrorMessage = String.format(ErrorCode.PRODUCT_NOT_FOUND.getErrorMessage(), List.of(1L, 2L));

        //when && then
        mockMvc.perform(post("/product-service/decrease-stock")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDtos)))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value(ErrorCode.PRODUCT_NOT_FOUND.getErrorCode()))
                .andExpect(jsonPath("$.errorMessage").value(expectedErrorMessage));
    }

    @Test
    @Transactional
    @DisplayName("products 재고 증가_성공")
    public void increase_success() throws Exception {
        //given
        ArrayList<Long> productIds = new ArrayList<>();
        Category category = createTestCategory("category");
        for(int i = 1; i <= 2; i++){
            CategoryProduct categoryProduct = createTestCategoryProduct(category);
            Product product = createTestProduct("product" + i, 1000, 100, List.of(categoryProduct));
            productIds.add(product.getId());
        }

        ArrayList<ProductStockUpdateRequestDto> requestDtos = new ArrayList<>();
        requestDtos.add(new ProductStockUpdateRequestDto(productIds.get(0), 5));
        requestDtos.add(new ProductStockUpdateRequestDto(productIds.get(1), 10));

        //when && then
        mockMvc.perform(post("/product-service/increase-stock")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDtos)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].productId").exists())
                .andExpect(jsonPath("$[0].title", startsWith("product")))
                .andExpect(jsonPath("$[0].requestedQuantity").value(5))
                .andExpect(jsonPath("$[1].productId").exists())
                .andExpect(jsonPath("$[1].title", startsWith("product")))
                .andExpect(jsonPath("$[1].requestedQuantity").value(10));
    }

    @Test
    @DisplayName("products 재고 증가_실패_존재 하지 않는 상품")
    public void increaseStock_failure_notFoundProduct() throws Exception {
        //given
        ArrayList<ProductStockUpdateRequestDto> requestDtos = new ArrayList<>();
        requestDtos.add(new ProductStockUpdateRequestDto(1L, 5));
        requestDtos.add(new ProductStockUpdateRequestDto(2L, 10));
        String expectedErrorMessage = String.format(ErrorCode.PRODUCT_NOT_FOUND.getErrorMessage(), List.of(1L, 2L));

        //when && then
        mockMvc.perform(post("/product-service/increase-stock")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDtos)))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value(ErrorCode.PRODUCT_NOT_FOUND.getErrorCode()))
                .andExpect(jsonPath("$.errorMessage").value(expectedErrorMessage));
    }
}
