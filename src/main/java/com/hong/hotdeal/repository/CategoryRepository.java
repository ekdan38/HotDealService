package com.hong.hotdeal.repository;

import com.hong.hotdeal.domain.Category;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Long> {
}
