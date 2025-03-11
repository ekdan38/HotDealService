package com.hong.productservice.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hong.common.exception.ErrorCode;
import com.hong.productservice.domain.Category;
import com.hong.productservice.repository.CategoryRepository;
import com.hong.productservice.web.dto.cateogry.CategoryRequestDto;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class CategoryIntegrationTest {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    ObjectMapper objectMapper;
    @Autowired
    CategoryRepository categoryRepository;
    @Autowired
    EntityManager em;

    private Category createTestCategory(String categoryTtitle){
        Category category = Category.create(categoryTtitle);
        categoryRepository.save(category);
        return category;
    }

    @Test
    @Transactional
    @DisplayName("category 생성_성공")
    public void createCategory_success() throws Exception {
        //given
        String categoryTitle = "category";
        CategoryRequestDto requestDto = new CategoryRequestDto(categoryTitle);

        //when && then
        mockMvc.perform(post("/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("최상위 카테고리 생성 완료"))
                .andExpect(jsonPath("$.data.id").exists())
                .andExpect(jsonPath("$.data.title").value(categoryTitle));
    }

    @ParameterizedTest
    @CsvSource({
            "'', NotBlank, title은 필수입니다.",
            "'tooLongTitle', Size, title은 2 글자에서 10 글자입니다."
    })
    @DisplayName("category 생성_실패_입력 값 오류")
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
    @Transactional
    @DisplayName("category 생성_실패_이미 존재 하는 category Title")
    public void createCategory_failure_exists_category_title() throws Exception {
        //given
        String existsCategoryTitle = "category";
        createTestCategory(existsCategoryTitle);
        CategoryRequestDto requestDto = new CategoryRequestDto(existsCategoryTitle);
        String expectedErrorMessage = String.format(ErrorCode.CATEGORY_ROOT_EXISTS.getErrorMessage(), existsCategoryTitle);

        //when && then
        mockMvc.perform(post("/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value(ErrorCode.CATEGORY_ROOT_EXISTS.getErrorCode()))
                .andExpect(jsonPath("$.errorMessage").value(expectedErrorMessage));
    }

    @Test
    @Transactional
    @DisplayName("child Category 생성_성공")
    public void createChildCategory_success() throws Exception {
        //given
        Category category = createTestCategory("parent");
        Long parentCategoryId = category.getId();
        String childCategoryTitle = "child";
        CategoryRequestDto requestDto = new CategoryRequestDto(childCategoryTitle);

        //when && then
        mockMvc.perform(post("/categories/" + parentCategoryId + "/childcategories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("자식 카테고리 생성 완료"))
                .andExpect(jsonPath("$.data.id").exists())
                .andExpect(jsonPath("$.data.title").value(childCategoryTitle))
                .andExpect(jsonPath("$.data.parentId").value(parentCategoryId));
    }

    @ParameterizedTest
    @CsvSource({
            "'', NotBlank, title은 필수입니다.",
            "'tooLongTitle', Size, title은 2 글자에서 10 글자입니다."
    })
    @Transactional
    @DisplayName("child Category 생성_실패_입력 값 오류")
    public void createChildCategory_failure_invalidInput(String categoryTitle, String expectedError, String expectedValue) throws Exception {
        //given
        Category category = createTestCategory("parent");
        Long parentCategoryId = category.getId();
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
    @Transactional
    @DisplayName("child Category 생성_실패_존재 하지 않는 parent Category")
    public void createChildCategory_failure_notFoundParentCategory() throws Exception {
        //given
        Long parentCategoryId = 1L;
        String childCategoryTitle = "child";
        CategoryRequestDto requestDto = new CategoryRequestDto(childCategoryTitle);
        String expectedErrorMessage = String.format(ErrorCode.CATEGORY_PARENT_NOT_FOUND.getErrorMessage(), parentCategoryId);

        //when && then
        mockMvc.perform(post("/categories/" + parentCategoryId + "/childcategories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value(ErrorCode.CATEGORY_PARENT_NOT_FOUND.getErrorCode()))
                .andExpect(jsonPath("$.errorMessage").value(expectedErrorMessage));
    }

    @Test
    @Transactional
    @DisplayName("child Category 생성_실패_parentCategory title 과 title 이 같음")
    public void createChildCategory_failure_duplicateTitle() throws Exception {
        //given
        Category category = createTestCategory("category");
        Long parentCategoryId = category.getId();

        String childCategoryTitle = "category";
        CategoryRequestDto requestDto = new CategoryRequestDto(childCategoryTitle);
        String expectedErrorMessage = String.format(ErrorCode.CATEGORY_PARENT_CHILD_SAME_TITLE.getErrorMessage(),
                category.getTitle(), childCategoryTitle);

        //when && then
        mockMvc.perform(post("/categories/" + parentCategoryId + "/childcategories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value(ErrorCode.CATEGORY_PARENT_CHILD_SAME_TITLE.getErrorCode()))
                .andExpect(jsonPath("$.errorMessage").value(expectedErrorMessage));
    }

    @Test
    @Transactional
    @DisplayName("child Category 생성_실패_parentCategory 에 이미 해당 요청 category 존재")
    public void createChildCategory_failure_exists_child_category() throws Exception {
        //given
        Category category = createTestCategory("category");
        Long parentCategoryId = category.getId();

        String childTitle = "child";
        Category childCategory = createTestCategory(childTitle);
        category.addChildrenCategory(childCategory);
        em.flush();
        em.clear();

        CategoryRequestDto requestDto = new CategoryRequestDto(childTitle);
        String expectedErrorMessage = String.format(ErrorCode.CATEGORY_PARENT_UNDER_CHILD_EXISTS.getErrorMessage(),
                category.getTitle(), childTitle);

        //when && then
        mockMvc.perform(post("/categories/" + parentCategoryId + "/childcategories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value(ErrorCode.CATEGORY_PARENT_UNDER_CHILD_EXISTS.getErrorCode()))
                .andExpect(jsonPath("$.errorMessage").value(expectedErrorMessage));
    }

    @Test
    @Transactional
    @DisplayName("category 전제 조회_성공")
    public void getCategories_success() throws Exception {
        //given
        Category category1 = createTestCategory("parent1");
        Category category2 = createTestCategory("parent2");

        String childTitle = "child";
        Category childCategory = createTestCategory(childTitle);
        category1.addChildrenCategory(childCategory);
        em.flush();
        em.clear();

        //when && then
        mockMvc.perform(get("/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("카테고리 조회 완료"))
                .andExpect(jsonPath("$.data[0].id").value(category1.getId()))
                .andExpect(jsonPath("$.data[0].title").value(category1.getTitle()))
                .andExpect(jsonPath("$.data[0].childCategories[0].id").value(childCategory.getId()))
                .andExpect(jsonPath("$.data[0].childCategories[0].title").value(childCategory.getTitle()))
                .andExpect(jsonPath("$.data[1].id").value(category2.getId()))
                .andExpect(jsonPath("$.data[1].title").value(category2.getTitle()));
    }

    @Test
    @Transactional
    @DisplayName("category 단건 조회_성공")
    public void getCategory_success() throws Exception {
        //given
        Category category = createTestCategory("parent");
        Category childCategory1 = createTestCategory("child1");
        category.addChildrenCategory(childCategory1);
        Category childCategory2 = createTestCategory("child2");
        category.addChildrenCategory(childCategory2);
        em.flush();
        em.clear();

        //when && then
        mockMvc.perform(get("/categories/" + category.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("카테고리 조회 완료"))
                .andExpect(jsonPath("$.data.id").value(category.getId()))
                .andExpect(jsonPath("$.data.title").value(category.getTitle()))
                .andExpect(jsonPath("$.data.childCategories[0].id").value(childCategory1.getId()))
                .andExpect(jsonPath("$.data.childCategories[0].title").value(childCategory1.getTitle()))
                .andExpect(jsonPath("$.data.childCategories[1].id").value(childCategory2.getId()))
                .andExpect(jsonPath("$.data.childCategories[1].title").value(childCategory2.getTitle()));
    }

    @Test
    @Transactional
    @DisplayName("category 삭제_성공")
    public void deleteCategory_success() throws Exception {
        //given
        Category category = createTestCategory("parent");
        em.flush();
        em.clear();

        //when && then
        mockMvc.perform(delete("/categories/" + category.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("카테고리 삭제 완료"))
                .andExpect(jsonPath("$.data.id").value(category.getId()))
                .andExpect(jsonPath("$.data.title").value(category.getTitle()));
    }
}
