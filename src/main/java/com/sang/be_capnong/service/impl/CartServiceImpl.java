package com.sang.be_capnong.service.impl;

import com.sang.be_capnong.dto.request.CartAddRequest;
import com.sang.be_capnong.dto.request.CartRemoveRequest;
import com.sang.be_capnong.dto.request.CartUpdateRequest;
import com.sang.be_capnong.dto.response.ApiResponse;
import com.sang.be_capnong.dto.response.CartItemResponse;
import com.sang.be_capnong.dto.response.CartResponse;
import com.sang.be_capnong.dto.response.ShopCartResponse;
import com.sang.be_capnong.entity.*;
import com.sang.be_capnong.enums.UserStatus;
import com.sang.be_capnong.exception.AppException;
import com.sang.be_capnong.exception.ErrorCode;
import com.sang.be_capnong.repository.*;
import com.sang.be_capnong.service.CartService;
import com.sang.be_capnong.util.SecurityUtils;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CartServiceImpl implements CartService {

    UserRepository userRepository;
    ProductRepository productRepository;
    MysteryBoxRepository mysteryBoxRepository;
    BuildComboRepository buildComboRepository;
    CartRepository cartRepository;
    CartItemRepository cartItemRepository;

    @Override
    public CartResponse getMyCart() {
        User user = getCurrentUser();
        Optional<Cart> cartOpt = cartRepository.findByUser(user);

        if (cartOpt.isEmpty() || cartOpt.get().getCartItems().isEmpty()) {
            return CartResponse.builder()
                    .cartId(0)
                    .shopCarts(Collections.emptyList())
                    .cartTotal(BigDecimal.ZERO)
                    .build();
        }

        Cart cart = cartOpt.get();
        List<CartItem> validItems = new ArrayList<>();
        Map<Integer, ShopCartResponse> shopGroups = new HashMap<>();
        BigDecimal total = BigDecimal.ZERO;

        for (CartItem item : cart.getCartItems()) {
            CartItemResponse itemRes = buildCartItemResponse(item);
            if (itemRes == null) {
                // Item đã bị xóa (ví dụ Product không còn tồn tại)
                continue;
            }
            
            validItems.add(item);
            if (itemRes.isAvailable()) {
                total = total.add(itemRes.getUnitPrice().multiply(BigDecimal.valueOf(itemRes.getQuantity())));
            }

            User shop = getShopFromCartItem(item);
            if (shop == null) continue;

            ShopCartResponse shopCart = shopGroups.computeIfAbsent(shop.getId(), k -> 
                ShopCartResponse.builder()
                    .shopId(shop.getId())
                    .shopName(shop.getShopName() != null ? shop.getShopName() : shop.getFullName())
                    .shopAvatar(shop.getLogoUrl())
                    .items(new ArrayList<>())
                    .build()
            );
            
            shopCart.getItems().add(itemRes);
        }

        return CartResponse.builder()
                .cartId(cart.getId())
                .shopCarts(new ArrayList<>(shopGroups.values()))
                .cartTotal(total)
                .build();
    }

    @Transactional
    @Override
    public ApiResponse<Void> addToCart(CartAddRequest request) {
        User user = getCurrentUser();
        Cart cart = cartRepository.findByUser(user).orElseGet(() -> {
            Cart newCart = Cart.builder().user(user).build();
            return cartRepository.save(newCart);
        });

        // Tìm item hiện có trong giỏ xem đã có chưa
        CartItem existingItem = findExistingItemInCart(cart, request.getItemType(), request.getItemId());

        int newQuantity = request.getQuantity();
        if (existingItem != null) {
            newQuantity += existingItem.getQuantity();
        }

        // Chống hack: Cộng dồn không được quá 999
        if (newQuantity > 999) {
            newQuantity = 999;
        }

        // Validate stock
        validateStock(request.getItemType(), request.getItemId(), newQuantity);

        if (existingItem != null) {
            existingItem.setQuantity(newQuantity);
            cartItemRepository.save(existingItem);
        } else {
            CartItem newItem = CartItem.builder()
                    .cart(cart)
                    .quantity(newQuantity)
                    .build();
            
            setCartItemReference(newItem, request.getItemType(), request.getItemId());
            cartItemRepository.save(newItem);
        }

        return ApiResponse.<Void>builder().message("Added to cart successfully").build();
    }

    @Transactional
    @Override
    public ApiResponse<Void> updateCartItem(CartUpdateRequest request) {
        CartItem cartItem = cartItemRepository.findById(request.getCartItemId())
                .orElseThrow(() -> new AppException(ErrorCode.CART_ITEM_NOT_FOUND));

        User user = getCurrentUser();
        if (cartItem.getCart().getUser().getId() != user.getId()) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        if (request.getQuantity() == 0) {
            cartItemRepository.delete(cartItem);
            return ApiResponse.<Void>builder().message("Item removed from cart").build();
        }

        String itemType = getItemType(cartItem);
        int itemId = getItemId(cartItem);
        
        validateStock(itemType, itemId, request.getQuantity());

        cartItem.setQuantity(request.getQuantity());
        cartItemRepository.save(cartItem);

        return ApiResponse.<Void>builder().message("Cart updated successfully").build();
    }

    @Transactional
    @Override
    public ApiResponse<Void> removeCartItems(CartRemoveRequest request) {
        User user = getCurrentUser();
        
        // Lấy các item cần xóa và thuộc về user hiện tại
        List<CartItem> itemsToDelete = cartItemRepository.findAllById(request.getCartItemIds())
                .stream()
                .filter(item -> item.getCart().getUser().getId() == user.getId())
                .collect(Collectors.toList());

        if (!itemsToDelete.isEmpty()) {
            cartItemRepository.deleteAll(itemsToDelete);
        }

        return ApiResponse.<Void>builder().message("Items removed from cart").build();
    }

    @Transactional
    @Override
    public void clearCart() {
        User user = getCurrentUser();
        cartRepository.findByUser(user).ifPresent(cart -> {
            cartItemRepository.deleteAll(cart.getCartItems());
        });
    }

    // --- Helper Methods ---

    private User getCurrentUser() {
        int userId = SecurityUtils.getCurrentUserId();
        return userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
    }

    private CartItemResponse buildCartItemResponse(CartItem item) {
        if (item.getProduct() != null) {
            Product p = item.getProduct();
            User shop = p.getShop();
            String imageUrl = (p.getImages() != null && !p.getImages().isEmpty()) ? p.getImages().get(0).getImageUrl() : null;
            
            boolean isAvailable = true;
            String errorNote = null;
            
            if (shop == null || shop.getStatus() != UserStatus.ACTIVE || Boolean.FALSE.equals(shop.getShopActive())) {
                isAvailable = false;
                errorNote = "Shop đang ngừng hoạt động";
            } else if (!"ACTIVE".equalsIgnoreCase(p.getStatus())) {
                isAvailable = false;
                errorNote = "Sản phẩm hiện không còn bán";
            } else if (p.getStockQuantity() < item.getQuantity()) {
                isAvailable = false;
                errorNote = "Không đủ số lượng, kho chỉ còn " + p.getStockQuantity();
            }

            return CartItemResponse.builder()
                    .cartItemId(item.getId())
                    .itemType("PRODUCT")
                    .itemId(p.getProductId())
                    .name(p.getProductName())
                    .unitPrice(p.getSellingPrice())
                    .quantity(item.getQuantity())
                    .stockQuantity(p.getStockQuantity())
                    .imageUrl(imageUrl)
                    .isAvailable(isAvailable)
                    .errorNote(errorNote)
                    .build();
        } else if (item.getMysteryBox() != null) {
            MysteryBox m = item.getMysteryBox();
            User shop = m.getShop();
            int stock = m.getTotalQuantity() - m.getSoldQuantity();
            
            boolean isAvailable = true;
            String errorNote = null;
            
            if (shop == null || shop.getStatus() != UserStatus.ACTIVE || Boolean.FALSE.equals(shop.getShopActive())) {
                isAvailable = false;
                errorNote = "Shop đang ngừng hoạt động";
            } else if (Boolean.FALSE.equals(m.getIsActive())) {
                isAvailable = false;
                errorNote = "Sản phẩm hiện không còn bán";
            } else if (stock < item.getQuantity()) {
                isAvailable = false;
                errorNote = "Không đủ số lượng, kho chỉ còn " + stock;
            }

            return CartItemResponse.builder()
                    .cartItemId(item.getId())
                    .itemType("MYSTERY_BOX")
                    .itemId(m.getMysteryId())
                    .name(m.getBoxType())
                    .unitPrice(m.getPrice())
                    .quantity(item.getQuantity())
                    .stockQuantity(stock)
                    .imageUrl(m.getImageUrl())
                    .isAvailable(isAvailable)
                    .errorNote(errorNote)
                    .build();
        } else if (item.getBuildCombo() != null) {
            BuildCombo b = item.getBuildCombo();
            User shop = b.getShop();
            int stock = b.getTotalQuantity() - b.getSoldQuantity();

            boolean isAvailable = true;
            String errorNote = null;
            
            if (shop == null || shop.getStatus() != UserStatus.ACTIVE || Boolean.FALSE.equals(shop.getShopActive())) {
                isAvailable = false;
                errorNote = "Shop đang ngừng hoạt động";
            } else if (!"ACTIVE".equalsIgnoreCase(b.getStatus())) {
                isAvailable = false;
                errorNote = "Sản phẩm hiện không còn bán";
            } else if (stock < item.getQuantity()) {
                isAvailable = false;
                errorNote = "Không đủ số lượng, kho chỉ còn " + stock;
            }

            return CartItemResponse.builder()
                    .cartItemId(item.getId())
                    .itemType("BUILD_COMBO")
                    .itemId(b.getComboId())
                    .name(b.getComboName())
                    .unitPrice(b.getPrice())
                    .quantity(item.getQuantity())
                    .stockQuantity(stock)
                    .imageUrl(b.getImageUrl())
                    .isAvailable(isAvailable)
                    .errorNote(errorNote)
                    .build();
        }
        return null;
    }

    private User getShopFromCartItem(CartItem item) {
        if (item.getProduct() != null) return item.getProduct().getShop();
        if (item.getMysteryBox() != null) return item.getMysteryBox().getShop();
        if (item.getBuildCombo() != null) return item.getBuildCombo().getShop();
        return null;
    }

    private CartItem findExistingItemInCart(Cart cart, String itemType, int itemId) {
        if (cart.getCartItems() == null) return null;
        for (CartItem item : cart.getCartItems()) {
            if ("PRODUCT".equalsIgnoreCase(itemType) && item.getProduct() != null && item.getProduct().getProductId() == itemId) return item;
            if ("MYSTERY_BOX".equalsIgnoreCase(itemType) && item.getMysteryBox() != null && item.getMysteryBox().getMysteryId() == itemId) return item;
            if ("BUILD_COMBO".equalsIgnoreCase(itemType) && item.getBuildCombo() != null && item.getBuildCombo().getComboId() == itemId) return item;
        }
        return null;
    }

    private void validateStock(String itemType, int itemId, int requestedQuantity) {
        int availableStock = 0;
        boolean isActive = false;
        
        if ("PRODUCT".equalsIgnoreCase(itemType)) {
            Product p = productRepository.findById(itemId).orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));
            availableStock = p.getStockQuantity();
            isActive = "ACTIVE".equalsIgnoreCase(p.getStatus()) && p.getShop() != null && p.getShop().getStatus() == UserStatus.ACTIVE && Boolean.TRUE.equals(p.getShop().getShopActive());
        } else if ("MYSTERY_BOX".equalsIgnoreCase(itemType)) {
            MysteryBox m = mysteryBoxRepository.findById(itemId).orElseThrow(() -> new AppException(ErrorCode.MYSTERY_BOX_NOT_FOUND));
            availableStock = m.getTotalQuantity() - m.getSoldQuantity();
            isActive = Boolean.TRUE.equals(m.getIsActive()) && m.getShop() != null && m.getShop().getStatus() == UserStatus.ACTIVE && Boolean.TRUE.equals(m.getShop().getShopActive());
        } else if ("BUILD_COMBO".equalsIgnoreCase(itemType)) {
            BuildCombo b = buildComboRepository.findById(itemId).orElseThrow(() -> new AppException(ErrorCode.COMBO_NOT_FOUND));
            availableStock = b.getTotalQuantity() - b.getSoldQuantity();
            isActive = "ACTIVE".equalsIgnoreCase(b.getStatus()) && b.getShop() != null && b.getShop().getStatus() == UserStatus.ACTIVE && Boolean.TRUE.equals(b.getShop().getShopActive());
        } else {
            throw new AppException(ErrorCode.INVALID_ITEM_TYPE);
        }

        if (!isActive) {
            throw new AppException(ErrorCode.PRODUCT_NOT_FOUND); // Hoặc tạo mã lỗi PRODUCT_INACTIVE
        }

        if (requestedQuantity > availableStock) {
            throw new AppException(ErrorCode.OUT_OF_STOCK);
        }
    }

    private void setCartItemReference(CartItem item, String itemType, int itemId) {
        if ("PRODUCT".equalsIgnoreCase(itemType)) {
            item.setProduct(productRepository.findById(itemId).orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND)));
        } else if ("MYSTERY_BOX".equalsIgnoreCase(itemType)) {
            item.setMysteryBox(mysteryBoxRepository.findById(itemId).orElseThrow(() -> new AppException(ErrorCode.MYSTERY_BOX_NOT_FOUND)));
        } else if ("BUILD_COMBO".equalsIgnoreCase(itemType)) {
            item.setBuildCombo(buildComboRepository.findById(itemId).orElseThrow(() -> new AppException(ErrorCode.COMBO_NOT_FOUND)));
        } else {
            throw new AppException(ErrorCode.INVALID_ITEM_TYPE);
        }
    }

    private String getItemType(CartItem item) {
        if (item.getProduct() != null) return "PRODUCT";
        if (item.getMysteryBox() != null) return "MYSTERY_BOX";
        if (item.getBuildCombo() != null) return "BUILD_COMBO";
        return "UNKNOWN";
    }

    private int getItemId(CartItem item) {
        if (item.getProduct() != null) return item.getProduct().getProductId();
        if (item.getMysteryBox() != null) return item.getMysteryBox().getMysteryId();
        if (item.getBuildCombo() != null) return item.getBuildCombo().getComboId();
        return 0;
    }
}
