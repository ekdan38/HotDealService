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
import org.modelmapper.ModelMapper;
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
    private final ModelMapper modelMapper;

    // 최상위 category 생성
    @Transactional
    @Override
    public CategoryResponseDto createCategory(CategoryRequestDto requestDto) {

        // root category 중 중복 되는 category 검증
        String title = requestDto.getTitle();
        if(categoryRepository.existsByParentIsNullAndTitle(title)) {
            log.error("이미 존재 하는 최상위 카테고리 입니다. 등록 시도 = {}", title);
            throw new CategoryException(ErrorCode.CATEGORY_ROOT_EXISTS);
        }

        // 최상위 category 생성 시작
        Category category = Category.create(title);

        // category 저장
        Category savedCategory = categoryRepository.save(category);
        return modelMapper.map(savedCategory, CategoryResponseDto.class);
    }

    // 자식 category 생성
    @Transactional
    @Override
    public CategoryResponseDto createChildCategory(Long parentCategoryId, CategoryRequestDto requestDto) {

        // parentCategory 조회
        Category parentCategory = categoryRepository.findById(parentCategoryId)
                .orElseThrow(() -> new CategoryException(ErrorCode.CATEGORY_PARENT_NOT_FOUND));

        String title = requestDto.getTitle();
        // parentCategory 와 childCategory 의 이름이 같은지 검증
        if(parentCategory.getTitle().equals(title)){
            log.error("부모 카테고리와 자식 카테고리의 이름이 같습니다.  parent = {}, child = {}", parentCategory.getTitle(), title);
            throw new CategoryException(ErrorCode.CATEGORY_PARENT_CHILD_SAME);
        }

        // parentCategory 아래 이미 존재 하는 childCategory 인지 검사
        if(categoryRepository.existsByTitleAndParentId(title, parentCategoryId)){
            log.error("부모 카테고리에 이미 존재하는 자식 카테고리 입니다. parent = {}, child = {}", parentCategory.getTitle(), title);
            throw new CategoryException(ErrorCode.CATEGORY_PARENT_UNDER_CHILD_EXISTS);
        }

        // childCategory 생성 시작
        Category childCategory = Category.create(title);

        // parentCategory 와 childCategory 연결
        parentCategory.addChildrenCategory(childCategory);
        // 명시적으로 childCategory save
        Category savedChildCategory = categoryRepository.save(childCategory);
        return new CategoryResponseDto(
                savedChildCategory.getId(),
                savedChildCategory.getTitle(),
                parentCategory.getId());
    }

    // 전체 category 조회(모든 최상위 카테고리 부터 자식 카테고리 까지)
    @Override
    public List<CategoryResponseDto> getCategories() {
        // 모든 category 조회 => child 까지 fetch join
        List<Category> categories = categoryRepository.findAllCategoriesWithChildren();
        // 조회 쿼리 1번으로 최적화

        // Map 으로 변환
        Map<Long, CategoryResponseDto> categoryMap = new HashMap<>();
        categories.forEach(category -> categoryMap.put(category.getId(), new CategoryResponseDto(
                category.getId(),
                category.getTitle(),
                new ArrayList<>()
        )));

        // parent, child 관계 정리
        categories.forEach(category -> {
            if (category.getParent() != null) {
                CategoryResponseDto parentDto = categoryMap.get(category.getParent().getId());
                CategoryResponseDto childDto = categoryMap.get(category.getId());
                parentDto.getChildCategories().add(childDto);
            }
        });

        // 최상위 category 만 반환
        return categories.stream()
                .filter(category -> category.getParent() == null)
                .map(category -> categoryMap.get(category.getId()))
                .collect(Collectors.toList());
    }


    // category 단건 조회(자식 카테고리 포함)
    @Override
    public CategoryResponseDto getCategory(Long categoryId) {
        // Fetch join으로 category와 모든 자식 category 조회
        List<Category> categories = categoryRepository.findCategoryByCategoryIdWithChildren(categoryId);

        if (categories.isEmpty()) {
            throw new CategoryException(ErrorCode.CATEGORY_NOT_FOUND);
        }

        // Map 으로 변환
        Map<Long, CategoryResponseDto> categoryMap = new HashMap<>();
        categories.forEach(category -> categoryMap.put(category.getId(), new CategoryResponseDto(
                category.getId(),
                category.getTitle(),
                new ArrayList<>()
        )));

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

        // 최상위 카테고리 반환
        return categoryMap.get(categoryId);
    }

    // category 수정(title)
    @Transactional
    @Override
    public CategoryResponseDto updateCategory(Long categoryId, CategoryRequestDto requestDto) {
        // category 조회
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new CategoryException(ErrorCode.CATEGORY_PARENT_NOT_FOUND));

        String newTitle = requestDto.getTitle();
        // root category 인 경우 root category 중에서 newTitle 이 중복 되는지 검증
        if(category.getParent() == null){
            if(categoryRepository.existsByParentIsNullAndTitle(newTitle)) {
                log.error("이미 존재 하는 최상위 카테고리 입니다. 등록 시도 = {}", newTitle);
                throw new CategoryException(ErrorCode.CATEGORY_ROOT_EXISTS);
            }
        }
        else{
            // child category 경우, 동일 parent category 중에서 newTitle 이 중복 되는지 검증
            if (categoryRepository.existsByTitleAndParentId(newTitle, category.getParent().getId())) {
                log.error("부모 카테고리 아래 이미 존재하는 카테고리입니다. 등록 시도 = {}", newTitle);
                throw new CategoryException(ErrorCode.CATEGORY_PARENT_UNDER_CHILD_EXISTS);
            }
        }

        category.updateTitle(newTitle);
        return new CategoryResponseDto(category.getId(), newTitle);
    }

    // category 삭제
    @Transactional
    @Override
    public CategoryResponseDto deleteCategory(Long categoryId) {
        // category 조회
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new CategoryException(ErrorCode.CATEGORY_NOT_FOUND));

        // childCategory 존재 하는지 확인
        if (!category.getChilds().isEmpty()) {
            log.error("삭제하려는 카테고리에게 자식 카테고리가 존재합니다. 삭제 시도 = {}", category);
            throw new CategoryException(ErrorCode.CATEGORY_HAS_CHILDREN);
        }

        // category 를 사용 하는 product 가 존재 하는지 확인
        if(categoryRepository.existsProductCategoryByCategoryId(categoryId)){
            log.error("삭제하려는 카테고리를 사용하는 상품이 존재합니다. 삭제 시도 = {}", category);
            throw new CategoryException(ErrorCode.CATEGORY_IN_USE_BY_PRODUCT);
        }


        categoryRepository.delete(category);
        return new CategoryResponseDto(category.getId(), category.getTitle());
    }


    // 외부 호출 service
    @Override
    public List<Category> getCategoriesById(List<CategoryDto> categories){
        // categoryId만 추출
        List<Long> categoryIds = categories.stream()
                .map(CategoryDto::getId)
                .toList();

        // categoryId로 category 조회
        List<Category> categoryList = categoryRepository.findAllById(categoryIds);

        // 조회한 category empty 이면
        if(categoryList.isEmpty()){
            throw new CategoryException(ErrorCode.CATEGORY_NOT_FOUND);
        }

        return categoryList;
    }

}