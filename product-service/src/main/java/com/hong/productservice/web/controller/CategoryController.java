package com.hong.productservice.web.controller;

import com.hong.common.dto.ResponseDto;
import com.hong.productservice.dto.category.CategoryDto;
import com.hong.productservice.dto.category.CategoryResponseDto;
import com.hong.productservice.service.category.CategoryService;
import com.hong.productservice.web.dto.cateogry.CategoryRequestDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Slf4j(topic = "[CategoryController]")
@RequestMapping("/categories")
public class CategoryController {

    private final CategoryService categoryService;
    private final ModelMapper modelMapper;

    // 최상위 category 생성
    @PostMapping
    public ResponseEntity<?> createCategory(@RequestBody @Validated CategoryRequestDto requestDto,
                                            BindingResult bindingResult){
        // 요청 dto 오류 검사
        if (bindingResult.hasErrors()) {
            log.error("최상위 카테고리 생성 요청 검증 오류 = {}", bindingResult);
            return ResponseEntity.badRequest().body(bindingResult);
        }

        // reqeustDto dto로 변환
        CategoryDto categoryDto = modelMapper.map(requestDto, CategoryDto.class);

        CategoryResponseDto resultDto = categoryService.createCategory(categoryDto);

        // 응답 설정
        ResponseDto<CategoryResponseDto> responseDto = new ResponseDto<>("최상위 카테고리 생성 완료", resultDto);
        return ResponseEntity.ok().body(responseDto);
    }
    // 자식 category 생성
    @PostMapping("/{parentCategoryId}/childcategories")
    public ResponseEntity<?> createChildCategory(@PathVariable("parentCategoryId") Long parentCategoryId,
                                                 @RequestBody @Validated CategoryRequestDto requestDto,
                                                 BindingResult bindingResult){
        // 요청 dto 오류 검사
        if (bindingResult.hasErrors()) {
            log.error("자식 카테고리 생성 요청 검증 오류 = {}", bindingResult);
            return ResponseEntity.badRequest().body(bindingResult);
        }

        // reqeustDto dto로 변환
        CategoryDto categoryDto = modelMapper.map(requestDto, CategoryDto.class);

        CategoryResponseDto resultDto = categoryService.createChildCategory(parentCategoryId, categoryDto);

        // 응답 설정
        ResponseDto<CategoryResponseDto> responseDto = new ResponseDto<>("자식 카테고리 생성 완료", resultDto);
        return ResponseEntity.ok().body(responseDto);
    }

    // 전체 category 조회
    @GetMapping
    public ResponseEntity<ResponseDto<List<CategoryResponseDto>>> getCategories(){

        List<CategoryResponseDto> resultDtos = categoryService.getCategories();

        // 응답 설정
        ResponseDto<List<CategoryResponseDto>> responseDto = new ResponseDto<>("카테고리 조회 완료", resultDtos);
        return ResponseEntity.ok().body(responseDto);
    }

    // category 단건 조회(자식 카테고리 포함)
    @GetMapping("/{categoryId}")
    public ResponseEntity<?> getCategory(@PathVariable("categoryId") Long categoryId){

        CategoryResponseDto resultDto = categoryService.getCategory(categoryId);

        // 응답 설정
        ResponseDto<CategoryResponseDto> responseDto = new ResponseDto<>("카테고리 조회 완료", resultDto);
        return ResponseEntity.ok().body(responseDto);
    }
    // category 수정(title)
    @PutMapping("/{categoryId}")
    public ResponseEntity<?> updateCategory(@PathVariable("categoryId") Long categoryId,
                                            @RequestBody @Validated CategoryRequestDto requestDto,
                                            BindingResult bindingResult){
        // 요청 dto 오류 검사
        if (bindingResult.hasErrors()) {
            log.error("카테고리 수정 요청 검증 오류 = {}", bindingResult);
            return ResponseEntity.badRequest().body(bindingResult);
        }

        // reqeustDto dto로 변환
        CategoryDto categoryDto = modelMapper.map(requestDto, CategoryDto.class);

        CategoryResponseDto resultDto = categoryService.updateCategory(categoryId, categoryDto);

        // 응답 설정
        ResponseDto<CategoryResponseDto> responseDto = new ResponseDto<>("카테고리 수정 완료", resultDto);
        return ResponseEntity.ok().body(responseDto);
    }

    // category 삭제
    @DeleteMapping("/{categoryId}")
    public ResponseEntity<?> deleteCategory(@PathVariable("categoryId") Long categoryId){

        CategoryResponseDto resultDto = categoryService.deleteCategory(categoryId);

        // 응답 설정
        ResponseDto<CategoryResponseDto> responseDto = new ResponseDto<>("카테고리 삭제 완료", resultDto);
        return ResponseEntity.ok().body(responseDto);
    }


}
