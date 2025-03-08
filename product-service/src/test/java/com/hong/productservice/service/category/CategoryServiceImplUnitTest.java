package com.hong.productservice.service.category;

import com.hong.common.exception.custom.CategoryException;
import com.hong.productservice.domain.Category;
import com.hong.productservice.dto.category.CategoryDto;
import com.hong.productservice.dto.category.CategoryResponseDto;
import com.hong.productservice.repository.CategoryRepository;
import com.hong.productservice.web.dto.cateogry.CategoryRequestDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
class CategoryServiceImplUnitTest {

    @InjectMocks
    CategoryServiceImpl categoryService;

    @Mock
    CategoryRepository categoryRepository;

    @Test
    @DisplayName("createCategory_성공")
    public void createCategory_success(){
        //given
        String categoryTitle = "category";
        Long categoryId = 1L;
        CategoryRequestDto categoryRequestDto = new CategoryRequestDto(categoryTitle);
        when(categoryRepository.existsByParentIsNullAndTitle(categoryTitle)).thenReturn(false);
        Category savedCategory = Category.create(categoryTitle);
        ReflectionTestUtils.setField(savedCategory, "id", categoryId);
        when(categoryRepository.save(any(Category.class))).thenReturn(savedCategory);

        //when
        CategoryResponseDto categoryResponseDto = categoryService.createCategory(categoryRequestDto);

        //then
        assertThat(categoryResponseDto.getId()).isEqualTo(categoryId);
        assertThat(categoryResponseDto.getTitle()).isEqualTo(categoryTitle);
    }

    @Test
    @DisplayName("createCategory_실패_중복된 category Title")
    public void createCategory_failure_duplicateTitle(){
        //given
        String categoryTitle = "category";
        CategoryRequestDto categoryRequestDto = new CategoryRequestDto(categoryTitle);
        when(categoryRepository.existsByParentIsNullAndTitle(categoryTitle)).thenReturn(true);

        //when && then
        assertThatThrownBy(() -> categoryService.createCategory(categoryRequestDto))
                .isInstanceOf(CategoryException.class);
    }

    @Test
    @DisplayName("createChildCategory_성공")
    public void createChildCategory_success(){
        //given
        String parentCategoryTitle = "parentCategory";
        String childCategoryTitle = "childCategory";
        Long parentCategoryId = 1L;
        Long childCategoryId = 2L;

        CategoryRequestDto categoryRequestDto = new CategoryRequestDto(childCategoryTitle);
        Category parentCategory = Category.create(parentCategoryTitle);
        ReflectionTestUtils.setField(parentCategory, "id", parentCategoryId);

        when(categoryRepository.findById(any(Long.class))).thenReturn(Optional.of(parentCategory));
        when(categoryRepository.existsByTitleAndParentId(any(String.class), any(Long.class))).thenReturn(false);

        Category savedChildCategory = Category.create(childCategoryTitle);
        ReflectionTestUtils.setField(savedChildCategory, "id", childCategoryId);
        when(categoryRepository.save(any(Category.class))).thenReturn(savedChildCategory);

        //when
        CategoryResponseDto categoryResponseDto = categoryService.createChildCategory(parentCategoryId, categoryRequestDto);

        //then
        assertThat(categoryResponseDto.getId()).isEqualTo(childCategoryId);
        assertThat(categoryResponseDto.getTitle()).isEqualTo(childCategoryTitle);
        assertThat(categoryResponseDto.getParentId()).isEqualTo(parentCategoryId);
    }

    @Test
    @DisplayName("createChildCategory_실패_parentCategory 존재 하지 않음")
    public void createChildCategory_failure_notFoundParentCategory(){
        //given
        String childCategoryTitle = "childCategory";
        Long parentCategoryId = 1L;

        CategoryRequestDto categoryRequestDto = new CategoryRequestDto(childCategoryTitle);

        when(categoryRepository.findById(any(Long.class))).thenReturn(Optional.empty());
        // when && then
        assertThatThrownBy(() -> categoryService.createChildCategory(parentCategoryId, categoryRequestDto))
                .isInstanceOf(CategoryException.class);

    }

    @Test
    @DisplayName("createChildCategory_실패_parentCategory title 과 title 이 같음")
    public void createChildCategory_failure_duplicateTitle(){
        //given
        String parentCategoryTitle = "duplicateTitle";
        String childCategoryTitle = "duplicateTitle";
        Long parentCategoryId = 1L;

        CategoryRequestDto categoryRequestDto = new CategoryRequestDto(childCategoryTitle);
        Category parentCategory = Category.create(parentCategoryTitle);
        ReflectionTestUtils.setField(parentCategory, "id", parentCategoryId);

        when(categoryRepository.findById(any(Long.class))).thenReturn(Optional.of(parentCategory));

        // when && then
        assertThatThrownBy(() -> categoryService.createChildCategory(parentCategoryId, categoryRequestDto))
                .isInstanceOf(CategoryException.class);
    }

    @Test
    @DisplayName("createChildCategory_실패_parentCategory 에 이미 해당 요청 category 존재")
    public void createChildCategory_failure_exists_child_category(){
        //given
        String parentCategoryTitle = "parentCategory";
        String childCategoryTitle = "childCategory";
        Long parentCategoryId = 1L;

        CategoryRequestDto categoryRequestDto = new CategoryRequestDto(childCategoryTitle);
        Category parentCategory = Category.create(parentCategoryTitle);
        ReflectionTestUtils.setField(parentCategory, "id", parentCategoryId);

        when(categoryRepository.findById(any(Long.class))).thenReturn(Optional.of(parentCategory));
        when(categoryRepository.existsByTitleAndParentId(any(String.class), any(Long.class))).thenReturn(true);

        // when && then
        assertThatThrownBy(() -> categoryService.createChildCategory(parentCategoryId, categoryRequestDto))
                .isInstanceOf(CategoryException.class);
    }

    @Test
    @DisplayName("categories 조회(child Category 포함)_성공")
    public void getCategories_success(){
        //given
        Category parentCategory = Category.create("parentCategory");
        Category childCategory1 = Category.create("childCategory1");
        Category childCategory2 = Category.create("childCategory2");
        ReflectionTestUtils.setField(parentCategory, "id", 1L);
        ReflectionTestUtils.setField(childCategory1, "id", 2L);
        ReflectionTestUtils.setField(childCategory2, "id", 3L);
        parentCategory.addChildrenCategory(childCategory1);
        parentCategory.addChildrenCategory(childCategory2);

        List<Category> categories = List.of(parentCategory, childCategory1, childCategory2);
        when(categoryRepository.findAllCategoriesWithChildren()).thenReturn(categories);

        // when
        List<CategoryResponseDto> categoryResponseDtos = categoryService.getCategories();

        //then
        assertThat(categoryResponseDtos).hasSize(1);
        assertThat(categoryResponseDtos.get(0).getId()).isEqualTo(1L);
        assertThat(categoryResponseDtos.get(0).getChildCategories()).hasSize(2);
        assertThat(categoryResponseDtos.get(0).getChildCategories().get(0).getId()).isEqualTo(2L);
        assertThat(categoryResponseDtos.get(0).getChildCategories().get(1).getId()).isEqualTo(3L);
    }

    @Test
    @DisplayName("category 단건 조회(child Category 포함)_성공")
    public void getCategory_success(){
        //given
        Long parentCategoryId = 1L;
        String parentCategoryTitle = "parentCategory";
        Long childCategoryId = 2L;
        String childCategoryTitle = "childCategory";
        Category parentCategory = Category.create(parentCategoryTitle);
        Category childCategory1 = Category.create(childCategoryTitle);
        ReflectionTestUtils.setField(parentCategory, "id", parentCategoryId);
        ReflectionTestUtils.setField(childCategory1, "id", childCategoryId);
        parentCategory.addChildrenCategory(childCategory1);

        Long targetCategoryId = parentCategory.getId();
        when(categoryRepository.findCategoryByCategoryIdWithChildren(targetCategoryId))
                .thenReturn(List.of(parentCategory, childCategory1));

        //when
        CategoryResponseDto categoryResponseDto = categoryService.getCategory(targetCategoryId);

        //then
        assertThat(categoryResponseDto.getId()).isEqualTo(parentCategoryId);
        assertThat(categoryResponseDto.getTitle()).isEqualTo(parentCategoryTitle);
        assertThat(categoryResponseDto.getChildCategories()).hasSize(1);
        assertThat(categoryResponseDto.getChildCategories().get(0).getId()).isEqualTo(childCategoryId);
        assertThat(categoryResponseDto.getChildCategories().get(0).getTitle()).isEqualTo(childCategoryTitle);
    }

    @Test
    @DisplayName("category 단건 조회(child Category 포함)_실패_category 없음")
    public void getCategory_failure_notFoundCategory(){
        //given
        Long parentCategoryId = 1L;
        String parentCategoryTitle = "parentCategory";
        Long childCategoryId = 2L;
        String childCategoryTitle = "childCategory";
        Category parentCategory = Category.create(parentCategoryTitle);
        Category childCategory1 = Category.create(childCategoryTitle);
        ReflectionTestUtils.setField(parentCategory, "id", parentCategoryId);
        ReflectionTestUtils.setField(childCategory1, "id", childCategoryId);
        parentCategory.addChildrenCategory(childCategory1);

        Long targetCategoryId = parentCategory.getId();
        when(categoryRepository.findCategoryByCategoryIdWithChildren(targetCategoryId))
                .thenReturn(List.of());

        //when && then
        assertThatThrownBy(() -> categoryService.getCategory(targetCategoryId))
                .isInstanceOf(CategoryException.class);
    }

    @Test
    @DisplayName("category 수정_성공")
    public void updateCategory_success(){
        //given
        Long categoryId = 1L;
        String categoryTitle = "category";
        Category category = Category.create(categoryTitle);
        ReflectionTestUtils.setField(category, "id", categoryId);

        Long targetId = categoryId;
        CategoryRequestDto categoryRequestDto = new CategoryRequestDto("newCategory");
        String newTitle = categoryRequestDto.getTitle();
        when(categoryRepository.findById(targetId)).thenReturn(Optional.of(category));
        when(categoryRepository.existsByParentIsNullAndTitle(newTitle)).thenReturn(false);

        //when
        CategoryResponseDto categoryResponseDto = categoryService.updateCategory(targetId, categoryRequestDto);

        //then
        assertThat(categoryResponseDto.getId()).isEqualTo(categoryId);
        assertThat(categoryResponseDto.getTitle()).isEqualTo(newTitle);
    }

    @Test
    @DisplayName("category 수정_실패_존재 하지 않는 category")
    public void updateCategory_failure_notFoundCategory(){
        //given
        Long categoryId = 1L;
        String categoryTitle = "category";
        Category category = Category.create(categoryTitle);
        ReflectionTestUtils.setField(category, "id", categoryId);

        Long targetId = categoryId;
        CategoryRequestDto categoryRequestDto = new CategoryRequestDto("newCategory");
        when(categoryRepository.findById(targetId)).thenReturn(Optional.empty());

        //when && then
        assertThatThrownBy(() -> categoryService.updateCategory(targetId, categoryRequestDto)).isInstanceOf(CategoryException.class);
    }

    @Test
    @DisplayName("category 수정_실패_해당 category가 parent이고 요청된 title과 중복된 parentCateogry 존재")
    public void updateCategory_failure_duplicate_parent_category_title(){
        //given
        Long categoryId = 1L;
        String categoryTitle = "category";
        Category category = Category.create(categoryTitle);
        ReflectionTestUtils.setField(category, "id", categoryId);

        Long targetId = categoryId;
        CategoryRequestDto categoryRequestDto = new CategoryRequestDto("newCategory");
        String newTitle = categoryRequestDto.getTitle();
        when(categoryRepository.findById(targetId)).thenReturn(Optional.of(category));
        when(categoryRepository.existsByParentIsNullAndTitle(newTitle)).thenReturn(true);

        //when && then
        assertThatThrownBy(() -> categoryService.updateCategory(targetId, categoryRequestDto)).isInstanceOf(CategoryException.class);
    }

    @Test
    @DisplayName("category 수정_실패_수정하려는 title이 이미 parentCategory 아래 존재")
    public void updateCategory_failure_exists_title_under_parent_category(){
        //given
        Long parentCategoryId = 1L;
        String parentCategoryTitle = "parentCategory";
        Long childCategoryId = 2L;
        String childCategoryTitle = "childCategory";
        Category parentCategory = Category.create(parentCategoryTitle);
        ReflectionTestUtils.setField(parentCategory, "id", parentCategoryId);
        Category childCategory = Category.create(childCategoryTitle);
        ReflectionTestUtils.setField(childCategory, "id", childCategoryId);
        parentCategory.addChildrenCategory(childCategory);

        Long targetId = childCategoryId;
        CategoryRequestDto categoryRequestDto = new CategoryRequestDto("childCategory");
        String newTitle = categoryRequestDto.getTitle();
        when(categoryRepository.findById(targetId)).thenReturn(Optional.of(childCategory));
        when(categoryRepository.existsByTitleAndParentId(newTitle, parentCategoryId)).thenReturn(true);

        //when && then
        assertThatThrownBy(() -> categoryService.updateCategory(targetId, categoryRequestDto)).isInstanceOf(CategoryException.class);
    }

    @Test
    @DisplayName("category 삭제_성공")
    public void deleteCategory_success(){
        //given
        Long categoryId = 1L;
        String categoryTitle = "category";
        Category category = Category.create(categoryTitle);
        ReflectionTestUtils.setField(category, "id", categoryId);
        Long targetId = category.getId();
        when(categoryRepository.findById(targetId)).thenReturn(Optional.of(category));
        when(categoryRepository.existsProductCategoryByCategoryId(targetId)).thenReturn(false);
        //when
        CategoryResponseDto categoryResponseDto = categoryService.deleteCategory(targetId);

        //then
        assertThat(categoryResponseDto.getId()).isEqualTo(categoryId);
        assertThat(categoryResponseDto.getTitle()).isEqualTo(categoryTitle);
    }
    @Test
    @DisplayName("category 삭제_실패_존재 하지 않는 category")
    public void deleteCategory_failure_notFoundCategory(){
        //given
        Long categoryId = 1L;
        String categoryTitle = "category";
        Category category = Category.create(categoryTitle);
        ReflectionTestUtils.setField(category, "id", categoryId);
        Long targetId = category.getId();
        when(categoryRepository.findById(targetId)).thenReturn(Optional.empty());

        //when && then
        assertThatThrownBy(() -> categoryService.deleteCategory(targetId)).isInstanceOf(CategoryException.class);
    }

    @Test
    @DisplayName("category 삭제_실패_childCategory 존재")
    public void deleteCategory_failure_exists_child_category(){
        //given
        Long parentCategoryId = 1L;
        String parentCategoryTitle = "parentCategory";
        Long childCategoryId = 2L;
        String childCategoryTitle = "childCategory";
        Category parentCategory = Category.create(parentCategoryTitle);
        ReflectionTestUtils.setField(parentCategory, "id", parentCategoryId);
        Category childCategory = Category.create(childCategoryTitle);
        ReflectionTestUtils.setField(childCategory, "id", childCategoryId);
        parentCategory.addChildrenCategory(childCategory);

        Long targetId = parentCategory.getId();
        when(categoryRepository.findById(targetId)).thenReturn(Optional.of(parentCategory));

        //when && then
        assertThatThrownBy(() -> categoryService.deleteCategory(targetId)).isInstanceOf(CategoryException.class);
    }

    @Test
    @DisplayName("category 삭제_실패_삭제하려는 category를 사용하는 product 존재")
    public void deleteCategory_failure_category_in_use_by_product(){
        //given
        Long categoryId = 1L;
        String categoryTitle = "category";
        Category category = Category.create(categoryTitle);
        ReflectionTestUtils.setField(category, "id", categoryId);
        Long targetId = category.getId();

        when(categoryRepository.findById(targetId)).thenReturn(Optional.of(category));
        when(categoryRepository.existsProductCategoryByCategoryId(targetId)).thenReturn(true);

        //when && then
        assertThatThrownBy(() -> categoryService.deleteCategory(targetId)).isInstanceOf(CategoryException.class);
    }

    @Test
    @DisplayName("categoryIds 로 category 조회_성공")
    public void getCategoriesById_success(){
        //given
        Category category1 = Category.create("category1");
        ReflectionTestUtils.setField(category1, "id", 1L);
        Category category2 = Category.create("category2");
        ReflectionTestUtils.setField(category2, "id", 2L);
        List<Category> categories = List.of(category1, category2);

        List<CategoryDto> categoryDtos = List.of(
                new CategoryDto(1L),
                new CategoryDto(2L));

        List<Long> categoryIds = categoryDtos.stream()
                .map(CategoryDto::getId)
                .toList();

        when(categoryRepository.findAllById(categoryIds)).thenReturn(categories);
        //when
        List<Category> categoryList = categoryService.getCategoriesById(categoryDtos);

        //then
        assertThat(categoryList).hasSize(2);
        assertThat(categoryList.get(0).getId()).isEqualTo(1L);
        assertThat(categoryList.get(0).getTitle()).isEqualTo("category1");
        assertThat(categoryList.get(1).getId()).isEqualTo(2L);
        assertThat(categoryList.get(1).getTitle()).isEqualTo("category2");
    }

    @Test
    @DisplayName("categoryIds 로 category 조회_실패_empty")
    public void getCategoriesById_failure_result_is_empty(){
        //given
        List<CategoryDto> categoryDtos = List.of(
                new CategoryDto(1L),
                new CategoryDto(2L));

        List<Long> categoryIds = categoryDtos.stream()
                .map(CategoryDto::getId)
                .toList();

        when(categoryRepository.findAllById(categoryIds)).thenReturn(List.of());
        //when && then
        assertThatThrownBy(() -> categoryService.getCategoriesById(categoryDtos)).isInstanceOf(CategoryException.class);
    }


}