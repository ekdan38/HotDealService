package com.hong.productservice.web.controller.unit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hong.productservice.dto.category.CategoryDto;
import com.hong.productservice.dto.product.ProductDto;
import com.hong.productservice.dto.product.ProductPagingResponseDto;
import com.hong.productservice.dto.product.ProductResponseDto;
import com.hong.productservice.dto.product.ProductStockDto;
import com.hong.productservice.service.product.ProductApiService;
import com.hong.productservice.service.product.ProductService;
import com.hong.productservice.web.controller.ProductController;
import com.hong.productservice.web.dto.product.ProductRequestDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@WebMvcTest(ProductController.class)
class ProductControllerUnitTest {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    ObjectMapper objectMapper;
    @MockitoBean
    ProductService productService;
    @MockitoBean
    ProductApiService productApiService;

    @Test
    @DisplayName("product 생성_성공")
    public void createProduct_success() throws Exception {
        //given
        Long productId = 1L;
        String title = "product";
        int price = 1000;
        int stock = 100;
        Long categoryId = 1L;
        ProductRequestDto requestDto = new ProductRequestDto(title, price, stock, List.of(categoryId));
        ProductResponseDto responseDto = new ProductResponseDto(productId, title, price, stock, List.of(new CategoryDto(categoryId)));
        when(productService.createProduct(any(ProductDto.class))).thenReturn(responseDto);

        //when && then
        mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("상품 생성 완료"))
                .andExpect(jsonPath("$.data.id").value(productId))
                .andExpect(jsonPath("$.data.title").value(title))
                .andExpect(jsonPath("$.data.price").value(price))
                .andExpect(jsonPath("$.data.stock").value(stock))
                .andExpect(jsonPath("$.data.categories[0].id").value(categoryId));
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
    @DisplayName("product 페이징 조회_성공")
    public void getProducts_success() throws Exception {
        //given
        ArrayList<ProductResponseDto> productResponseDtos = new ArrayList<>();
        productResponseDtos.add(new ProductResponseDto(10L, "product10", 100));
        productResponseDtos.add(new ProductResponseDto(9L, "product9", 100));
        productResponseDtos.add(new ProductResponseDto(8L, "product8", 100));
        ProductPagingResponseDto pagingResponse = new ProductPagingResponseDto(5L, productResponseDtos);
        when(productService.getProducts(anyString(), anyLong(), anyInt(), anyLong())).thenReturn(pagingResponse);

        //when && then
        mockMvc.perform(get("/products")
                        .param("cursor", "10")
                        .param("size", "3")
                        .param("categoryId", "1")
                        .param("search", "test")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("상품 조회 완료"))
                .andExpect(jsonPath("$.data.nextCursor").value(5))
                .andExpect(jsonPath("$.data.products", hasSize(3)))
                .andExpect(jsonPath("$.data.products[0].id").value(10))
                .andExpect(jsonPath("$.data.products[0].title").value("product10"))
                .andExpect(jsonPath("$.data.products[0].price").value(100))
                .andExpect(jsonPath("$.data.products[1].id").value(9))
                .andExpect(jsonPath("$.data.products[1].title").value("product9"))
                .andExpect(jsonPath("$.data.products[1].price").value(100))
                .andExpect(jsonPath("$.data.products[2].id").value(8))
                .andExpect(jsonPath("$.data.products[2].title").value("product8"))
                .andExpect(jsonPath("$.data.products[2].price").value(100));
    }

    @Test
    @DisplayName("product 단건 조회_성공")
    public void getProduct_success() throws Exception {
        //given
        List<ProductStockDto> stockList = List.of(
                new ProductStockDto(1L, "product1", 1000, 100),
                new ProductStockDto(2L, "product2", 1000, 100));
        when(productApiService.getProductsWithStock(anyList())).thenReturn(stockList);

        //when && then
        mockMvc.perform(get("/products/stock")
                        .param("productIds", "1", "2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("상품 재고 조회 완료"))
                .andExpect(jsonPath("$.data[0].productId").value(1))
                .andExpect(jsonPath("$.data[0].stock").value(100))
                .andExpect(jsonPath("$.data[1].productId").value(2))
                .andExpect(jsonPath("$.data[1].stock").value(100));
    }

    @Test
    @DisplayName("product 수정_성공")
    public void updateProduct_success() throws Exception {
        //given
        Long productId = 1L;
        String title = "product";
        int price = 1000;
        int stock = 100;
        Long categoryId = 1L;
        ProductRequestDto requestDto = new ProductRequestDto(title, price, stock, List.of(categoryId));
        ProductResponseDto responseDto = new ProductResponseDto(productId, title, price, stock, List.of(new CategoryDto(categoryId)));
        when(productService.updateProduct(eq(productId), any(ProductDto.class))).thenReturn(responseDto);

        //when && then
        mockMvc.perform(put("/products/" + productId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("상품 수정 완료"))
                .andExpect(jsonPath("$.data.id").value(productId))
                .andExpect(jsonPath("$.data.title").value(title))
                .andExpect(jsonPath("$.data.price").value(price))
                .andExpect(jsonPath("$.data.stock").value(stock))
                .andExpect(jsonPath("$.data.categories[0].id").value(categoryId));
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
    @DisplayName("product 수정_실패_입력 값 오류")
    public void updateProduct_failure_invalidInput(String title,
                                                   String priceStr,
                                                   String stockStr,
                                                   String strCategoryIds,
                                                   String expectedError,
                                                   String expectedValue) throws Exception {
        //given
        Long productId = 1L;
        Integer price = "null".equals(priceStr) ? null : Integer.valueOf(priceStr);
        Integer stock = "null".equals(stockStr) ? null : Integer.valueOf(stockStr);

        List<Long> categoryIds;
        if (strCategoryIds == null || strCategoryIds.isEmpty()) categoryIds = new ArrayList<>();
        else categoryIds = List.of(Long.valueOf(strCategoryIds));
        ProductRequestDto requestDto = new ProductRequestDto(title, price, stock, categoryIds);

        //when && then
        mockMvc.perform(put("/products/" + productId)
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
        Long productId = 1L;
        String title = "product";
        int price = 1000;
        int stock = 100;
        Long categoryId = 1L;
        ProductResponseDto responseDto = new ProductResponseDto(productId, title, price, stock, List.of(new CategoryDto(categoryId)));
        when(productService.deleteProduct(productId)).thenReturn(responseDto);

        //when && then
        mockMvc.perform(delete("/products/" + productId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("상품 삭제 완료"))
                .andExpect(jsonPath("$.data.id").value(productId))
                .andExpect(jsonPath("$.data.title").value(title))
                .andExpect(jsonPath("$.data.price").value(price))
                .andExpect(jsonPath("$.data.stock").value(stock))
                .andExpect(jsonPath("$.data.categories[0].id").value(categoryId));
    }
}