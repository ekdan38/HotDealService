package com.hong.productservice.service.wishlist;

import com.hong.common.exception.custom.WishlistException;
import com.hong.productservice.domain.Product;
import com.hong.productservice.domain.Wishlist;
import com.hong.productservice.domain.WishlistProduct;
import com.hong.productservice.dto.wishlist.WishlistPagingResponseDto;
import com.hong.productservice.dto.wishlist.WishlistResponseDto;
import com.hong.productservice.repository.WishlistProductRepository;
import com.hong.productservice.repository.WishlistRepository;
import com.hong.productservice.service.product.ProductApiService;
import com.hong.productservice.web.dto.wishlist.WishlistRequestDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WishlistServiceImplUnitTest {

    @InjectMocks
    WishlistServiceImpl wishlistService;
    @Mock
    WishlistProductRepository wishlistProductRepository;
    @Mock
    WishlistRepository wishlistRepository;
    @Mock
    ProductApiService productApiService;

    private Product createTestProduct(Long id, String title, int price, int stock) {
        Product product = Product.create(title, price, stock, List.of());
        ReflectionTestUtils.setField(product, "id", id);
        return product;
    }

    private Wishlist createTestWishlist(Long wishlistId, Long userId) {
        Wishlist wishlist = Wishlist.create(userId);
        ReflectionTestUtils.setField(wishlist, "id", wishlistId);
        return wishlist;
    }

    private WishlistProduct createTestWishlistProduct(Product product, int quantity, Long wishlistId) {
        WishlistProduct wishlistProduct = WishlistProduct.create(product, quantity);
        ReflectionTestUtils.setField(wishlistProduct, "id", wishlistId);
        return wishlistProduct;
    }

    @Test
    @DisplayName("wishlist 생성_성공")
    public void createWishlist_success(){
        //given
        Long userId = 1L;
        Long productId = 1L;
        Long wishlistId = 1L;
        Product product = createTestProduct(productId, "product", 1000, 100);
        when(productApiService.getProduct(any(Long.class))).thenReturn(product);
        when(wishlistRepository.findWithProductsByUserId(userId)).thenReturn(Optional.empty());
        Wishlist wishlist = createTestWishlist(wishlistId, userId);
        when(wishlistRepository.save(any(Wishlist.class))).thenReturn(wishlist);

        WishlistRequestDto requestDto = new WishlistRequestDto(productId, 5);

        //when
        WishlistResponseDto result = wishlistService.createWishlist(userId, requestDto);

        //then
        assertThat(result.getId()).isEqualTo(wishlistId);
        assertThat(result.getUserId()).isEqualTo(userId);
        assertThat(result.getProductId()).isEqualTo(productId);
        assertThat(result.getQuantity()).isEqualTo(requestDto.getQuantity());
    }

    @Test
    @DisplayName("products 페이징 조회")
    public void getWishlists(){
        // given
        Long userId = 1L;
        Long wishlistId = 1L;
        Wishlist wishlist = createTestWishlist(wishlistId, userId);
        when(wishlistRepository.findByUserId(userId)).thenReturn(Optional.of(wishlist));

        Long cursor = null;
        int size = 3;
        List<WishlistProduct> page = List.of(
                createTestWishlistProduct(createTestProduct(1L, "product1", 100, 100), 1, 1L),
                createTestWishlistProduct(createTestProduct(2L, "product2", 200, 200), 2, 2L),
                createTestWishlistProduct(createTestProduct(3L, "product3", 300, 300), 3, 3L)
                );

        when(wishlistProductRepository.findByWishlistIdAndCursor(eq(wishlistId), any(Long.class), any(PageRequest.class))).thenReturn(page);

        // when
        WishlistPagingResponseDto result = wishlistService.getWishlists(userId, cursor, size);

        //then
        assertThat(result.getNextCursor()).isEqualTo(3);
        assertThat(result.getProducts()).hasSize(3);
    }

    @Test
    @DisplayName("wishlist 삭제_성공")
    public void deleteWishlist_success(){
        //given
        Long userId = 1L;
        Long wishlistId = 1L;
        Wishlist wishlist = createTestWishlist(wishlistId, userId);

        when(wishlistRepository.findByUserId(userId)).thenReturn(Optional.of(wishlist));

        //when
        Long result = wishlistService.deleteWishlist(userId);

        //then
        assertThat(result).isEqualTo(wishlistId);
    }

    @Test
    @DisplayName("wishlist 삭제_실패_존재 하지 않는 wishlist")
    public void deleteWishlist_failure_notFoundWishlist(){
        //given
        Long userId = 1L;
        when(wishlistRepository.findByUserId(userId)).thenReturn(Optional.empty());

        //when && then
        assertThatThrownBy(() -> wishlistService.deleteWishlist(userId)).isInstanceOf(WishlistException.class);
    }

}