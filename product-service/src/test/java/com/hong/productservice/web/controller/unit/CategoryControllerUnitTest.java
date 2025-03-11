package com.hong.productservice.web.controller.unit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hong.productservice.domain.Category;
import com.hong.productservice.dto.category.CategoryResponseDto;
import com.hong.productservice.service.category.CategoryService;
import com.hong.productservice.web.controller.CategoryController;
import com.hong.productservice.web.dto.cateogry.CategoryRequestDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CategoryController.class)
class CategoryControllerUnitTest {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    ObjectMapper objectMapper;
    @MockitoBean
    CategoryService categoryService;


    @Test
    @DisplayName("최상위 category 생성_성공")
    public void createCategory_success() throws Exception {
        //given
        String categoryTitle = "category";
        Long categoryId = 1L;
        CategoryRequestDto requestDto = new CategoryRequestDto(categoryTitle);
        CategoryResponseDto responseDto = new CategoryResponseDto(categoryId, categoryTitle);
        when(categoryService.createCategory(requestDto)).thenReturn(responseDto);

        //when && then
        mockMvc.perform(post("/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("최상위 카테고리 생성 완료"))
                .andExpect(jsonPath("$.data.id").value(categoryId))
                .andExpect(jsonPath("$.data.title").value(categoryTitle));
    }

    @ParameterizedTest
    @CsvSource({
            "'', NotBlank, title은 필수입니다.",
            "'tooLongTitle', Size, title은 2 글자에서 10 글자입니다."
    })
    @DisplayName("최상위 category 생성_실패_입력 값 오류")
    public void createCategory_failure_invalidInput(String categoryTitle, String expectedError, String expectedValue) throws Exception {
        //given
        CategoryRequestDto requestDto = new CategoryRequestDto(categoryTitle);

        //when && then
        mockMvc.perform(post("/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[?(@.code == '" + expectedError + "')].defaultMessage").value(expectedValue));
    }

    @Test
    @DisplayName("자식 category 생성_성공")
    public void createChildCategory_success() throws Exception {
        //given
        Long parentCategoryId = 1L;

        String categoryTitle = "category";
        Long categoryId = 1L;
        CategoryRequestDto requestDto = new CategoryRequestDto(categoryTitle);
        CategoryResponseDto responseDto = new CategoryResponseDto(categoryId, categoryTitle, parentCategoryId);
        when(categoryService.createChildCategory(parentCategoryId, requestDto)).thenReturn(responseDto);

        //when && then
        mockMvc.perform(post("/categories/" + parentCategoryId + "/childcategories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("자식 카테고리 생성 완료"))
                .andExpect(jsonPath("$.data.id").value(categoryId))
                .andExpect(jsonPath("$.data.title").value(categoryTitle))
                .andExpect(jsonPath("$.data.parentId").value(parentCategoryId));
    }

    @ParameterizedTest
    @CsvSource({
            "'', NotBlank, title은 필수입니다.",
            "'tooLongTitle', Size, title은 2 글자에서 10 글자입니다."
    })
    @DisplayName("자식 category 생성_실패_입력 값 오류")
    public void createChildCategory_failure_invalidInput(String categoryTitle, String expectedError, String expectedValue) throws Exception {
        //given
        Long parentCategoryId = 1L;
        CategoryRequestDto requestDto = new CategoryRequestDto(categoryTitle);

        //when && then
        mockMvc.perform(post("/categories/" + parentCategoryId + "/childcategories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[?(@.code == '" + expectedError + "')].defaultMessage").value(expectedValue));
    }

    @Test
    @DisplayName("category 전체 조회_성공")
    public void getCategories_success() throws Exception {
        //given
        List<CategoryResponseDto> children = new ArrayList<>();
        children.add(new CategoryResponseDto(2L, "childCategory1"));
        children.add(new CategoryResponseDto(3L, "childCategory2"));
        CategoryResponseDto result = new CategoryResponseDto(1L, "parentCategory", children);
        when(categoryService.getCategories()).thenReturn(List.of(result));

        //when && then
        mockMvc.perform(get("/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("카테고리 조회 완료"))
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].title").value("parentCategory"))
                .andExpect(jsonPath("$.data[0].childCategories[0].id").value(2))
                .andExpect(jsonPath("$.data[0].childCategories[0].title").value("childCategory1"))
                .andExpect(jsonPath("$.data[0].childCategories[1].id").value(3))
                .andExpect(jsonPath("$.data[0].childCategories[1].title").value("childCategory2"));
    }

    @Test
    @DisplayName("category 수정_성공")
    public void updateCategory_success() throws Exception {
        //given
        Long categoryId = 1L;
        String newTitle = "newTitle";
        CategoryRequestDto requestDto = new CategoryRequestDto("newTitle");
        CategoryResponseDto responseDto = new CategoryResponseDto(categoryId, newTitle);
        when(categoryService.updateCategory(categoryId, requestDto)).thenReturn(responseDto);

        //when && then
        mockMvc.perform(put("/categories/" + categoryId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("카테고리 수정 완료"))
                .andExpect(jsonPath("$.data.id").value(categoryId))
                .andExpect(jsonPath("$.data.title").value(newTitle));
    }

    @ParameterizedTest
    @CsvSource({
            "'', NotBlank, title은 필수입니다.",
            "'tooLongTitle', Size, title은 2 글자에서 10 글자입니다."
    })
    @DisplayName("category 수정_실패_입력 값 오류")
    public void updateCategory_failure_invalidInput(String newCategoryTitle, String expectedError, String expectedValue) throws Exception {
        //given
        Long categoryId = 1L;
        CategoryRequestDto requestDto = new CategoryRequestDto(newCategoryTitle);

        //when && then
        mockMvc.perform(put("/categories/" + categoryId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[?(@.code == '" + expectedError + "')].defaultMessage").value(expectedValue));
    }

    @Test
    @DisplayName("category 삭제_성공")
    public void deleteCategory_success() throws Exception {
        //given
        Long categoryId = 1L;
        String categoryTitle = "category";
        CategoryResponseDto responseDto = new CategoryResponseDto(categoryId, categoryTitle);
        when(categoryService.deleteCategory(categoryId)).thenReturn(responseDto);

        //when && then
        mockMvc.perform(delete("/categories/" + categoryId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("카테고리 삭제 완료"))
                .andExpect(jsonPath("$.data.id").value(categoryId))
                .andExpect(jsonPath("$.data.title").value(categoryTitle));
    }

}