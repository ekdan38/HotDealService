package com.hong.productservice.repository;

import com.hong.productservice.domain.Product;
import com.hong.productservice.domain.Wishlist;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface WishlistRepository extends JpaRepository<Wishlist, Long> {

    Optional<Wishlist>findByUserId(Long userId);

    // wishlist, wishlistProduct fetch join
    @Query("SELECT w FROM Wishlist w JOIN FETCH w.wishlistProducts WHERE w.id = :userId")
    Optional<Wishlist> findByUserIdWithProducts(@Param("userId") Long userId);

    // userId로 wishlist 조회 하면서 wishlistProducts, product fetch join
    @Query("SELECT w FROM Wishlist w " +
            "LEFT JOIN FETCH w.wishlistProducts wp " +
            "LEFT JOIN FETCH wp.product " +
            "WHERE w.userId = :userId")
    Optional<Wishlist> findWithProductsByUserId(@Param("userId") Long userId);


    // FetchJoin 으로 쿼리 최적화 하기 위해서 List 로 반환 페이징
    // jpql은 limit 미지원 => pageable 사용 해서 size 적용
    @Query("SELECT w " +
            "FROM Wishlist w " +
            "LEFT JOIN FETCH w.wishlistProducts wp " +
            "LEFT JOIN FETCH wp.product p " +
            "WHERE w.userId = :userId AND w.id < :cursor " +
            "ORDER BY w.id DESC")
    List<Wishlist> findWishlistsWithProductsByUserIdAndCursor(@Param("userId") Long userId,
                                                              @Param("cursor") Long cursor,
                                                              Pageable pageable);


    // 앞에서 wishlistId
    // 각각 상품의 productId, title, quantity
}
