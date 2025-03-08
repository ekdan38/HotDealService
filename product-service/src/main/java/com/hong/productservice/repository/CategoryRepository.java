package com.hong.productservice.repository;

import com.hong.productservice.domain.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    // root category 중 중복 되는 category 조회
    boolean existsByParentIsNullAndTitle(String title);


    // parentCategory 아래 이미 존재 하는 childCategory 인지 조회
    boolean existsByTitleAndParentId(String title, Long parentCategoryId);


    // 조회 하면서 fetch join 으로 childCategory 조회
    @Query("SELECT c " +
            "FROM Category c " +
            "LEFT JOIN FETCH c.childs")
    List<Category> findAllCategoriesWithChildren();


    // categoryId 로 조회 하면서 fetch join 으로 childCategory 조회
    @Query("SELECT c " +
            "FROM Category c " +
            "LEFT JOIN FETCH c.childs " +
            "WHERE c.id = :categoryId OR c.parent.id IS NOT NULL")
    List<Category> findCategoryByCategoryIdWithChildren(@Param("categoryId") Long categoryId);

    // categoryId 로 조회 하면서 join 으로 CategoryProduct 조회
    @Query("SELECT CASE WHEN COUNT(cp) > 0 THEN true ELSE false END " +
            "FROM Category c " +
            "LEFT JOIN CategoryProduct cp ON cp.category = c " +
            "WHERE c.id = :categoryId")
    boolean existsProductCategoryByCategoryId(@Param("categoryId") Long categoryId);

}
