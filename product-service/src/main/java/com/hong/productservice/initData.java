//package com.hong.productservice;
//
//
//import com.hong.productservice.domain.Category;
//import com.hong.productservice.repository.CategoryRepository;
//import lombok.RequiredArgsConstructor;
//import org.springframework.boot.context.event.ApplicationReadyEvent;
//import org.springframework.context.event.EventListener;
//import org.springframework.stereotype.Component;
//import org.springframework.transaction.annotation.Transactional;
//
////@Component
//@RequiredArgsConstructor
//public class initData {
//
//
//    private final CategoryRepository categoryRepository;
//
//    // 카테고리 생성 && 저장
////    @EventListener(ApplicationReadyEvent.class)
//    @Transactional
//    public void init(){
//        setCategory();
//    }
//
//    public void setCategory(){
//        // parents
//        Category unCategorized = Category.create("UnCategorized");
//        Category savedUnCategorized = categoryRepository.save(unCategorized);
//
//        Category smartPhone = Category.create("SmartPhone");
//        Category savedSmartPhone = categoryRepository.save(smartPhone);
//
//        // child
//        smartPhone = categoryRepository.findById(savedSmartPhone.getId()).get();
//        Category samsung = Category.create("Samsung");
//        samsung.setParent(smartPhone);
//        categoryRepository.save(samsung);
//
//        Category apple = Category.create("Apple");
//        apple.setParent(smartPhone);
//        categoryRepository.save(apple);
//    }
//
//
//}
