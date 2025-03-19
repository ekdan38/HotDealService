package com.hong.productservice.service.category;


import com.hong.common.exception.ErrorCode;
import com.hong.common.exception.custom.CategoryException;
import com.hong.productservice.domain.Category;
import com.hong.productservice.dto.category.CategoryDto;
import com.hong.productservice.dto.category.CategoryResponseDto;
import com.hong.productservice.repository.CategoryRepository;
import com.hong.productservice.web.dto.cateogry.CategoryRequestDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "[CategoryService]")
@Transactional(readOnly = true)
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;

    // 최상위 category 생성
    @Transactional
    @Override
    public CategoryResponseDto createCategory(CategoryRequestDto requestDto) {
        // 1. root category 중 중복 되는 category 검증
        validateExistsTtitle(requestDto);
        
        // 2. 최상위 category 생성
        Category category = Category.create(requestDto.getTitle());

        // 3. category 저장
        Category savedCategory = categoryRepository.save(category);
        
        // 4. 응답 Dto 변환
        return new CategoryResponseDto(savedCategory.getId(), savedCategory.getTitle());
    }

    // 자식 category 생성
    @Transactional
    @Override
    public CategoryResponseDto createChildCategory(Long parentCategoryId, CategoryRequestDto requestDto) {
        
        // 1. parentCategory 조회
        Category parentCategory = fetchParentCategoryAndValidate(parentCategoryId);

        // 2. 생성 요청 childCategory 에 대한 검증(title 중복, 이미 존재 하는지)
        validateChildCategory(parentCategoryId, parentCategory,  requestDto.getTitle());

        // 3. childCategory 생성 및 parentCategory 와 연관 관계 
        Category childCategory = Category.create( requestDto.getTitle());
        parentCategory.addChildrenCategory(childCategory);

        // 4. 명시적으로 childCategory save
        Category savedChildCategory = categoryRepository.save(childCategory);

        // 5. 응답 Dto 변환
        return convertToCategoryResponse(savedChildCategory, parentCategory);
    }
    
    // 전체 category 조회(모든 최상위 카테고리 부터 자식 카테고리 까지)
    @Override
    public List<CategoryResponseDto> getCategories() {
        // 1. 모든 category 조회 (child 까지 fetch join)
        List<Category> categories = categoryRepository.findAllCategoriesWithChildren();

        // 2. parent, child 관계 정리
        Map<Long, CategoryResponseDto> categoryMap = buildToMap(categories);

        // 3. 응답 Dto 변환
        return convertToCategoryResponse(categories, categoryMap);
    }
    
    // category 단건 조회(자식 카테고리 포함)
    @Override
    public CategoryResponseDto getCategory(Long categoryId) {
        // 1. category 조회 (child 까지 fetch join)
        List<Category> categories = fetchCategoriesWithAllChildrenAndValidate(categoryId);

        // 2. Map 으로 변환
        Map<Long, CategoryResponseDto> categoryMap = buildCategoryMap(categories);

        // 3. parent, child 관계 정리 및 응답 Dto 변환
        buildToMapAndConvertResponseDto(categories, categoryMap);

        // 4. 최상위 카테고리 반환
        return categoryMap.get(categoryId);
    }


    // category 수정(title)
    @Transactional
    @Override
    public CategoryResponseDto updateCategory(Long categoryId, CategoryRequestDto requestDto) {
        // 1. category 조회
        Category category = fetchCategoryAndValidate(categoryId);

        String newTitle = requestDto.getTitle();

        // 2. root, child category 경우에 따라 newTitle 중복 검증
        validateCategoryTitle(category, newTitle);

        // 3. category update 처리
        category.updateTitle(newTitle);
        return new CategoryResponseDto(category.getId(), newTitle);
    }

    private Category fetchCategoryAndValidate(Long categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> {
                        log.debug("요청된 카테고리가 존재하지 않습니다. categoryId = {}", categoryId);
                        return new CategoryException(ErrorCode.CATEGORY_NOT_FOUND, categoryId);
                });
        return category;
    }

    // category 삭제
    @Transactional
    @Override
    public CategoryResponseDto deleteCategory(Long categoryId) {
        // 1. category 조회
        Category category = fetchCategoryAndValidate(categoryId);

        // 2. childCategory 존재 검증
        validateHasChildren(categoryId, category);

        // 3. category 를 사용 하는 product 존재 검증
        validateHasInUseProduct(categoryId);

        // 4. 삭제
        categoryRepository.delete(category);

        // 5. 응답 Dto 변환
        return new CategoryResponseDto(category.getId(), category.getTitle());
    }

    // 외부 호출 service
    @Override
    public List<Category> getCategoriesById(List<CategoryDto> categories){
        // 1. categoryIds 로 categories 조회 및 검증
        return fetchCategoriesByIdsAndValidate(categories);
    }

    private List<Category> fetchCategoriesByIdsAndValidate(List<CategoryDto> categories) {
        List<Long> categoryIds = categories.stream()
                .map(CategoryDto::getId)
                .toList();

        // categoryId로 category 조회
        List<Category> categoryList = categoryRepository.findAllById(categoryIds);

        // 조회한 category empty 이면
        if(categoryList.isEmpty()){
            log.debug("요청된 카테고리가 존재 하지 않습니다. categoryIds = {}", categoryIds);
            throw new CategoryException(ErrorCode.CATEGORY_NOT_FOUND, categoryIds);
        }
        return categoryList;
    }

    // root category 중 중복 되는 category 검증
    private void validateExistsTtitle(CategoryRequestDto requestDto) {
        String title = requestDto.getTitle();
        if(categoryRepository.existsByParentIsNullAndTitle(title)) {
            log.debug("이미 존재 하는 최상위 카테고리 입니다. title = {}", title);
            throw new CategoryException(ErrorCode.CATEGORY_ROOT_EXISTS, title);
        }
    }

    // 생성 요청 childCategory 검증
    private void validateChildCategory(Long parentCategoryId, Category parentCategory, String title) {
        // parentCategory 와 childCategory 의 이름이 같은지 검증
        if(parentCategory.getTitle().equals(title)){
            log.debug("부모 카테고리와 자식 카테고리의 title이 같습니다. parent's Title = {}, child's Title = {}", parentCategory.getTitle(), title);
            throw new CategoryException(ErrorCode.CATEGORY_PARENT_CHILD_SAME_TITLE, parentCategory.getTitle(), title);
        }

        // parentCategory 아래 이미 존재 하는 childCategory 인지 검사
        if(categoryRepository.existsByTitleAndParentId(title, parentCategoryId)){
            log.debug("부모 카테고리에 이미 존재하는 자식 카테고리 입니다. parent's Title = {}, child's Title = {}", parentCategory.getTitle(), title);
            throw new CategoryException(ErrorCode.CATEGORY_PARENT_UNDER_CHILD_EXISTS, parentCategory.getTitle(), title);
        }
    }

    // parentCategory 조회
    private Category fetchParentCategoryAndValidate(Long parentCategoryId) {
        Category parentCategory = categoryRepository.findById(parentCategoryId)
                .orElseThrow(() -> {
                    log.debug("존재 하지 않는 부모 카테고리 입니다. parentCategoryId = {}", parentCategoryId);
                    return new CategoryException(ErrorCode.CATEGORY_PARENT_NOT_FOUND, parentCategoryId);
                });
        return parentCategory;
    }

    private void buildToMapAndConvertResponseDto(List<Category> categories, Map<Long, CategoryResponseDto> categoryMap) {
        // parent, child 관계 정리
        categories.forEach(category -> {
            if (category.getParent() != null) {
                CategoryResponseDto parentDto = categoryMap.get(category.getParent().getId());
                if (parentDto != null) { // parentDto가 null인지 확인
                    CategoryResponseDto childDto = categoryMap.get(category.getId());
                    parentDto.getChildCategories().add(childDto);
                }
            }
        });
    }

    // category 조회 (child 까지 fetch join)
    private List<Category> fetchCategoriesWithAllChildrenAndValidate(Long categoryId) {
        List<Category> categories = categoryRepository.findCategoryByCategoryIdWithChildren(categoryId);
        if (categories.isEmpty()) {
            log.debug("요청된 카테고리가 존재하지 않습니다. categoryId = {}", categoryId);
            throw new CategoryException(ErrorCode.CATEGORY_NOT_FOUND, categoryId);
        }
        return categories;
    }

    private void validateHasInUseProduct(Long categoryId) {
        if(categoryRepository.existsProductCategoryByCategoryId(categoryId)){
            log.debug("삭제하려는 카테고리를 사용하는 상품이 존재합니다. 삭제 시도 = {}", categoryId);
            throw new CategoryException(ErrorCode.CATEGORY_IN_USE_BY_PRODUCT, categoryId);
        }
    }

    private void validateHasChildren(Long categoryId, Category category) {
        if (!category.getChilds().isEmpty()) {
            log.debug("삭제하려는 카테고리에게 자식 카테고리가 존재합니다. categoryId = {}", categoryId);
            throw new CategoryException(ErrorCode.CATEGORY_HAS_CHILDREN, categoryId);
        }
    }


    private List<CategoryResponseDto> convertToCategoryResponse(List<Category> categories, Map<Long, CategoryResponseDto> categoryMap) {
        return categories.stream()
                .filter(category -> category.getParent() == null)
                .map(category -> categoryMap.get(category.getId()))
                .collect(Collectors.toList());
    }

    private Map<Long, CategoryResponseDto> buildToMap(List<Category> categories) {
        Map<Long, CategoryResponseDto> categoryMap = buildCategoryMap(categories);

        categories.forEach(category -> {
            if (category.getParent() != null) {
                CategoryResponseDto parentDto = categoryMap.get(category.getParent().getId());
                CategoryResponseDto childDto = categoryMap.get(category.getId());
                parentDto.getChildCategories().add(childDto);
            }
        });
        return categoryMap;
    }
    
    private CategoryResponseDto convertToCategoryResponse(Category savedChildCategory, Category parentCategory) {
        return new CategoryResponseDto(
                savedChildCategory.getId(),
                savedChildCategory.getTitle(),
                parentCategory.getId());
    }

    // Map 으로 변환
    private Map<Long, CategoryResponseDto> buildCategoryMap(List<Category> categories) {
        Map<Long, CategoryResponseDto> categoryMap = new HashMap<>();
        categories.forEach(category -> categoryMap.put(category.getId(), new CategoryResponseDto(
                category.getId(),
                category.getTitle(),
                new ArrayList<>()
        )));
        return categoryMap;
    }

    // root, child category 경우에 따라 newTitle 중복 검증
    private void validateCategoryTitle(Category category, String newTitle) {
        // root category 인 경우 root category 중에서 newTitle 이 중복 되는지 검증
        if(category.getParent() == null){
            if(categoryRepository.existsByParentIsNullAndTitle(newTitle)) {
                log.error("이미 존재 하는 최상위 카테고리 입니다. 등록 시도 = {}", newTitle);
                throw new CategoryException(ErrorCode.CATEGORY_ROOT_EXISTS, newTitle);
            }
        }
        else{
            // child category 경우, 동일 parent category 중에서 newTitle 이 중복 되는지 검증
            if (categoryRepository.existsByTitleAndParentId(newTitle, category.getParent().getId())) {
                log.error("부모 카테고리에 이미 존재하는 자식 카테고리 입니다.parent's Title = {}, child's Title = {}",
                        category.getParent().getTitle(), newTitle);
                throw new CategoryException(ErrorCode.CATEGORY_PARENT_UNDER_CHILD_EXISTS,
                        category.getParent().getTitle(), newTitle);
            }
        }
    }
}