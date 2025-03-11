package com.hong.productservice.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hong.common.exception.ErrorCode;
import com.hong.productservice.domain.*;
import com.hong.productservice.repository.CategoryRepository;
import com.hong.productservice.repository.ProductRepository;
import com.hong.productservice.repository.WishlistRepository;
import com.hong.productservice.web.dto.wishlist.WishlistRequestDto;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.startsWith;


@SpringBootTest
@AutoConfigureMockMvc
public class WishlistIntegrationTest {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    ObjectMapper objectMapper;
    @Autowired
    WishlistRepository wishlistRepository;
    @Autowired
    ProductRepository productRepository;
    @Autowired
    CategoryRepository categoryRepository;
    @Autowired
    EntityManager em;

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

    private WishlistProduct createTestWishlistProduct(Product product, int quantity){
        return WishlistProduct.create(product, quantity);
    }

    private Wishlist createTestWishlist(Long userId){
        Wishlist wishlist = Wishlist.create(userId);
        wishlistRepository.save(wishlist);
        return wishlist;
    }

    @Test
    @Transactional
    @DisplayName("wishlist 생성_성공")
    public void WishlistIntegrationTest_success() throws Exception {
        //given
        Long userId = 1L;
        Category category = createTestCategory("category");
        CategoryProduct categoryProduct = createTestCategoryProduct(category);
        Product product = createTestProduct("product", 1000, 100, List.of(categoryProduct));

        int quantity = 5;
        WishlistRequestDto requestDto = new WishlistRequestDto(product.getId(), quantity);

        //when && then
        mockMvc.perform(post("/wishlists")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .header("X-User-Id", userId)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("위시리스트 등록 완료"))
                .andExpect(jsonPath("$.data.id").exists())
                .andExpect(jsonPath("$.data.userId").value(userId))
                .andExpect(jsonPath("$.data.productId").value(product.getId()))
                .andExpect(jsonPath("$.data.quantity").value(quantity));
    }

    @Test
    @DisplayName("wishlist 생성_실패_존재 하지 않는 product")
    public void WishlistIntegrationTest_failure_notFoundProduct() throws Exception {
        //given
        Long userId = 1L;
        Long productId = 1L;
        int quantity = 5;
        WishlistRequestDto requestDto = new WishlistRequestDto(productId, quantity);

        String expectedErrorMessage = String.format(ErrorCode.PRODUCT_NOT_FOUND.getErrorMessage(), productId);

        //when && then
        mockMvc.perform(post("/wishlists")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .header("X-User-Id", userId)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value(ErrorCode.PRODUCT_NOT_FOUND.getErrorCode()))
                .andExpect(jsonPath("$.errorMessage").value(expectedErrorMessage));
    }

    @ParameterizedTest
    @CsvSource({
            "null, '5', 'NotNull', 'productId 는 필수입니다.'",
            "1, null , 'NotNull', 'quantity 는 필수입니다.'"
    })
    @DisplayName("wishlist 생성_실패_입력 값 오류")
    public void createWishlist_failure_invalidInput(String productIdStr,
                                                    String quantityStr,
                                                    String expectedError,
                                                    String expectedValue) throws Exception {
        //given
        Long productId = "null".equals(productIdStr) ? null : Long.valueOf(productIdStr);
        Integer quantity = "null".equals(quantityStr) ? null : Integer.valueOf(quantityStr);
        Long userId = 1L;

        WishlistRequestDto requestDto = new WishlistRequestDto(productId, quantity);

        //when && then
        mockMvc.perform(post("/wishlists")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .header("X-User-Id", userId)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[?(@.code == '" + expectedError + "')].defaultMessage").value(expectedValue));
    }

    @Test
    @Transactional
    @DisplayName("wishlist 페이징 조회_성공")
    public void getWishlists_success() throws Exception {
        //given
        Long userId = 1L;
        Wishlist wishlist = createTestWishlist(userId);
        Long nextCursor = null;
        Category category = createTestCategory("category");
        for(int i = 1; i <= 10; i++){
            CategoryProduct categoryProduct = createTestCategoryProduct(category);
            Product product = createTestProduct("product" + i, 1000, 100, List.of(categoryProduct));
            WishlistProduct wishlistProduct = createTestWishlistProduct(product, 3);
            wishlist.addWishlistProducts(wishlistProduct);
            if(i == 6) nextCursor = (long) i;
        }
        em.flush();
        em.clear();

        //when && then
        mockMvc.perform(get("/wishlists")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .header("X-User-Id", userId)
                        .param("cursor", "100")
                        .param("size", "5"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("위시리스트 조회 완료"))
                .andExpect(jsonPath("$.data.wishlistId").value(wishlist.getId()))
                .andExpect(jsonPath("$.data.nextCursor").value(nextCursor))
                .andExpect(jsonPath("$.data.products[0].id").exists())
                .andExpect(jsonPath("$.data.products[0].title", startsWith("product")))
                .andExpect(jsonPath("$.data.products[0].quantity").value(3))
                .andExpect(jsonPath("$.data.products[1].id").exists())
                .andExpect(jsonPath("$.data.products[1].title", startsWith("product")))
                .andExpect(jsonPath("$.data.products[1].quantity").value(3))
                .andExpect(jsonPath("$.data.products[2].id").exists())
                .andExpect(jsonPath("$.data.products[2].title", startsWith("product")))
                .andExpect(jsonPath("$.data.products[2].quantity").value(3));
    }

    @Test
    @Transactional
    @DisplayName("wishlist 삭제_성공")
    public void deleteWishlist_success() throws Exception {
        //given
        Long userId = 1L;
        Wishlist wishlist = createTestWishlist(userId);
        Category category = createTestCategory("category");
        CategoryProduct categoryProduct = createTestCategoryProduct(category);
        Product product = createTestProduct("product", 1000, 100, List.of(categoryProduct));
        WishlistProduct wishlistProduct = createTestWishlistProduct(product, 3);
        wishlist.addWishlistProducts(wishlistProduct);
        em.flush();
        em.clear();

        //when && then
        mockMvc.perform(delete("/wishlists")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .header("X-User-Id", userId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("wishlist 삭제 성공"))
                .andExpect(jsonPath("$.data").value(wishlist.getId()));
    }
}
