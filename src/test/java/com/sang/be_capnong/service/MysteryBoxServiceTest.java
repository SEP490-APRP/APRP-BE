package com.sang.be_capnong.service;

import com.sang.be_capnong.dto.request.MysteryBoxCreationRequest;
import com.sang.be_capnong.dto.request.MysteryBoxUpdateRequest;
import com.sang.be_capnong.dto.request.ProductMysteryItemRequest;
import com.sang.be_capnong.dto.response.MysteryBoxResponse;
import com.sang.be_capnong.dto.response.PageResponse;
import com.sang.be_capnong.dto.response.ProductMysteryResponse;
import com.sang.be_capnong.entity.*;
import com.sang.be_capnong.enums.UserStatus;
import com.sang.be_capnong.exception.AppException;
import com.sang.be_capnong.exception.ErrorCode;
import com.sang.be_capnong.mapper.MysteryBoxMapper;
import com.sang.be_capnong.repository.MysteryBoxRepository;
import com.sang.be_capnong.repository.ProductMysteryRepository;
import com.sang.be_capnong.repository.ProductRepository;
import com.sang.be_capnong.repository.UserRepository;
import com.sang.be_capnong.service.impl.MysteryBoxServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MysteryBoxServiceTest {

    @Mock
    MysteryBoxRepository mysteryBoxRepository;

    @Mock
    ProductMysteryRepository productMysteryRepository;

    @Mock
    ProductRepository productRepository;

    @Mock
    UserRepository userRepository;

    @Mock
    MysteryBoxMapper mysteryBoxMapper;

    @InjectMocks
    MysteryBoxServiceImpl mysteryBoxService;

    User sampleShop;
    Role shopRole;
    Product sampleProduct1;
    Product sampleProduct2;
    MysteryBox sampleBox;
    MysteryBoxCreationRequest validCreationRequest;
    MysteryBoxResponse sampleBoxResponse;

    @BeforeEach
    void setUp() {
        shopRole = Role.builder().name("SHOP").description("Shop Owner").build();

        sampleShop = User.builder()
                .id(10)
                .fullName("Nông trại Ba Đồi")
                .shopName("Ba Đồi Farm")
                .role(shopRole)
                .status(UserStatus.ACTIVE)
                .shopActive(true)
                .build();

        sampleProduct1 = Product.builder()
                .productId(101)
                .productName("Cà chua bi")
                .unit("kg")
                .stockQuantity(50)
                .status("ACTIVE")
                .shop(sampleShop)
                .build();

        sampleProduct2 = Product.builder()
                .productId(102)
                .productName("Dưa leo")
                .unit("kg")
                .stockQuantity(40)
                .status("ACTIVE")
                .shop(sampleShop)
                .build();

        sampleBox = MysteryBox.builder()
                .mysteryId(1)
                .shop(sampleShop)
                .boxType("VEGETABLES")
                .price(BigDecimal.valueOf(99000))
                .totalQuantity(10)
                .soldQuantity(0)
                .isActive(true)
                .build();

        validCreationRequest = MysteryBoxCreationRequest.builder()
                .boxType("VEGETABLES")
                .price(BigDecimal.valueOf(99000))
                .totalQuantity(10)
                .description("Hộp rau củ Đà Lạt")
                .note("Bảo quản ngăn mát")
                .items(List.of(
                        ProductMysteryItemRequest.builder().productId(101).productQuantity(2).build(),
                        ProductMysteryItemRequest.builder().productId(102).productQuantity(1).build()
                ))
                .build();

        sampleBoxResponse = MysteryBoxResponse.builder()
                .mysteryId(1)
                .shopId(10)
                .shopName("Ba Đồi Farm")
                .boxType("VEGETABLES")
                .price(BigDecimal.valueOf(99000))
                .totalQuantity(10)
                .soldQuantity(0)
                .isActive(true)
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void mockAuthenticatedUser(Integer userId) {
        SecurityContext securityContext = mock(SecurityContext.class);
        Authentication authentication = mock(Authentication.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn(String.valueOf(userId));
        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    void createMysteryBox_success() {
        mockAuthenticatedUser(10);
        when(userRepository.findById(10)).thenReturn(Optional.of(sampleShop));
        when(productRepository.findById(101)).thenReturn(Optional.of(sampleProduct1));
        when(productRepository.findById(102)).thenReturn(Optional.of(sampleProduct2));
        when(mysteryBoxMapper.toMysteryBox(any())).thenReturn(sampleBox);
        when(mysteryBoxRepository.save(any(MysteryBox.class))).thenReturn(sampleBox);
        when(productMysteryRepository.saveAll(anyList())).thenReturn(List.of());
        when(mysteryBoxMapper.toMysteryBoxResponse(sampleBox)).thenReturn(sampleBoxResponse);
        when(mysteryBoxMapper.toProductMysteryResponseList(anyList())).thenReturn(List.of());

        MysteryBoxResponse response = mysteryBoxService.createMysteryBox(validCreationRequest);

        assertThat(response).isNotNull();
        assertThat(response.getMysteryId()).isEqualTo(1);
        // Verify inventory deducted:
        // Product1 had 50, needed 2 * 10 = 20 -> remaining should be 30
        assertThat(sampleProduct1.getStockQuantity()).isEqualTo(30);
        // Product2 had 40, needed 1 * 10 = 10 -> remaining should be 30
        assertThat(sampleProduct2.getStockQuantity()).isEqualTo(30);
        verify(productRepository).saveAll(anyList());
        verify(mysteryBoxRepository).save(any(MysteryBox.class));
    }

    @Test
    void createMysteryBox_notShopRole_throwsException() {
        mockAuthenticatedUser(10);
        User buyerUser = User.builder()
                .id(10)
                .role(Role.builder().name("BUYER").build())
                .status(UserStatus.ACTIVE)
                .build();
        when(userRepository.findById(10)).thenReturn(Optional.of(buyerUser));

        assertThatThrownBy(() -> mysteryBoxService.createMysteryBox(validCreationRequest))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_A_SHOP);
    }

    @Test
    void createMysteryBox_shopNotActive_throwsException() {
        mockAuthenticatedUser(10);
        sampleShop.setStatus(UserStatus.PENDING);
        when(userRepository.findById(10)).thenReturn(Optional.of(sampleShop));

        assertThatThrownBy(() -> mysteryBoxService.createMysteryBox(validCreationRequest))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.SHOP_NOT_ACTIVE);
    }

    @Test
    void createMysteryBox_duplicateProducts_throwsException() {
        mockAuthenticatedUser(10);
        when(userRepository.findById(10)).thenReturn(Optional.of(sampleShop));

        MysteryBoxCreationRequest duplicateRequest = MysteryBoxCreationRequest.builder()
                .boxType("MIXED")
                .totalQuantity(5)
                .items(List.of(
                        ProductMysteryItemRequest.builder().productId(101).productQuantity(1).build(),
                        ProductMysteryItemRequest.builder().productId(101).productQuantity(2).build()
                ))
                .build();

        assertThatThrownBy(() -> mysteryBoxService.createMysteryBox(duplicateRequest))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.DUPLICATE_PRODUCT_IN_BOX);
    }

    @Test
    void createMysteryBox_productNotBelongToShop_throwsException() {
        mockAuthenticatedUser(10);
        when(userRepository.findById(10)).thenReturn(Optional.of(sampleShop));

        User otherShop = User.builder().id(99).build();
        sampleProduct1.setShop(otherShop);

        when(productRepository.findById(101)).thenReturn(Optional.of(sampleProduct1));

        assertThatThrownBy(() -> mysteryBoxService.createMysteryBox(validCreationRequest))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCT_NOT_BELONG_TO_SHOP);
    }

    @Test
    void createMysteryBox_productNotActive_throwsException() {
        mockAuthenticatedUser(10);
        when(userRepository.findById(10)).thenReturn(Optional.of(sampleShop));

        sampleProduct1.setStatus("INACTIVE");
        when(productRepository.findById(101)).thenReturn(Optional.of(sampleProduct1));

        assertThatThrownBy(() -> mysteryBoxService.createMysteryBox(validCreationRequest))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCT_NOT_ACTIVE);
    }

    @Test
    void createMysteryBox_stockInsufficient_throwsException() {
        mockAuthenticatedUser(10);
        when(userRepository.findById(10)).thenReturn(Optional.of(sampleShop));

        sampleProduct1.setStockQuantity(5); // Needed 2 * 10 = 20
        when(productRepository.findById(101)).thenReturn(Optional.of(sampleProduct1));

        assertThatThrownBy(() -> mysteryBoxService.createMysteryBox(validCreationRequest))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.STOCK_INSUFFICIENT);
    }

    @Test
    void getMysteryBoxById_asOwner_returnsItems() {
        mockAuthenticatedUser(10);
        when(mysteryBoxRepository.findById(1)).thenReturn(Optional.of(sampleBox));
        when(mysteryBoxMapper.toMysteryBoxResponse(sampleBox)).thenReturn(sampleBoxResponse);
        when(productMysteryRepository.findByMysteryBox_MysteryId(1)).thenReturn(List.of());
        when(mysteryBoxMapper.toProductMysteryResponseList(anyList()))
                .thenReturn(List.of(ProductMysteryResponse.builder().productId(101).productName("Cà chua bi").build()));

        MysteryBoxResponse response = mysteryBoxService.getMysteryBoxById(1);

        assertThat(response).isNotNull();
        assertThat(response.getItems()).isNotNull();
        assertThat(response.getItems()).hasSize(1);
    }

    @Test
    void getMysteryBoxById_asBuyer_hidesItems() {
        mockAuthenticatedUser(20); // Buyer id 20
        when(mysteryBoxRepository.findById(1)).thenReturn(Optional.of(sampleBox));
        when(mysteryBoxMapper.toMysteryBoxResponse(sampleBox)).thenReturn(sampleBoxResponse);

        MysteryBoxResponse response = mysteryBoxService.getMysteryBoxById(1);

        assertThat(response).isNotNull();
        assertThat(response.getItems()).isNull(); // Secret for buyer
    }

    @Test
    void getMysteryBoxById_notFound_throwsException() {
        when(mysteryBoxRepository.findById(999)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> mysteryBoxService.getMysteryBoxById(999))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MYSTERY_BOX_NOT_FOUND);
    }

    @Test
    void getMysteryBoxes_success() {
        Page<MysteryBox> page = new PageImpl<>(List.of(sampleBox));
        when(mysteryBoxRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class), any(Pageable.class)))
                .thenReturn(page);
        when(mysteryBoxMapper.toMysteryBoxResponseList(anyList())).thenReturn(List.of(sampleBoxResponse));

        PageResponse<MysteryBoxResponse> result = mysteryBoxService.getMysteryBoxes(
                null, null, null, null, null, 1, 10, "mysteryId", "desc");

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    void getMyMysteryBoxes_success() {
        mockAuthenticatedUser(10);
        Page<MysteryBox> page = new PageImpl<>(List.of(sampleBox));
        when(mysteryBoxRepository.findByShop_Id(eq(10), any(Pageable.class))).thenReturn(page);
        when(mysteryBoxMapper.toMysteryBoxResponse(sampleBox)).thenReturn(sampleBoxResponse);
        when(productMysteryRepository.findByMysteryBox_MysteryId(1)).thenReturn(List.of());
        when(mysteryBoxMapper.toProductMysteryResponseList(anyList())).thenReturn(List.of());

        PageResponse<MysteryBoxResponse> result = mysteryBoxService.getMyMysteryBoxes(1, 10);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    void updateMysteryBox_success() {
        mockAuthenticatedUser(10);
        when(mysteryBoxRepository.findById(1)).thenReturn(Optional.of(sampleBox));
        when(mysteryBoxRepository.save(any(MysteryBox.class))).thenReturn(sampleBox);
        when(mysteryBoxMapper.toMysteryBoxResponse(sampleBox)).thenReturn(sampleBoxResponse);
        when(productMysteryRepository.findByMysteryBox_MysteryId(1)).thenReturn(List.of());

        MysteryBoxUpdateRequest request = MysteryBoxUpdateRequest.builder()
                .boxType("FRUITS")
                .price(BigDecimal.valueOf(120000))
                .build();

        MysteryBoxResponse response = mysteryBoxService.updateMysteryBox(1, request);

        assertThat(response).isNotNull();
        verify(mysteryBoxMapper).updateMysteryBox(eq(sampleBox), eq(request));
    }

    @Test
    void updateMysteryBox_adjustStockDelta_increaseStock() {
        mockAuthenticatedUser(10);
        when(mysteryBoxRepository.findById(1)).thenReturn(Optional.of(sampleBox));

        ProductMystery pm1 = ProductMystery.builder()
                .mysteryBox(sampleBox)
                .product(sampleProduct1)
                .productQuantity(2)
                .build();

        when(productMysteryRepository.findByMysteryBox_MysteryId(1)).thenReturn(List.of(pm1));
        when(mysteryBoxRepository.save(any(MysteryBox.class))).thenReturn(sampleBox);
        when(mysteryBoxMapper.toMysteryBoxResponse(sampleBox)).thenReturn(sampleBoxResponse);

        // Current totalQuantity is 10. Increase to 15 -> delta = +5 boxes
        // Needed: 5 * 2 = 10 units of sampleProduct1 (current stock 50 -> should become 40)
        MysteryBoxUpdateRequest request = MysteryBoxUpdateRequest.builder()
                .totalQuantity(15)
                .build();

        mysteryBoxService.updateMysteryBox(1, request);

        assertThat(sampleProduct1.getStockQuantity()).isEqualTo(40);
        verify(productRepository).save(sampleProduct1);
    }

    @Test
    void toggleMysteryBoxStatus_success() {
        mockAuthenticatedUser(10);
        when(mysteryBoxRepository.findById(1)).thenReturn(Optional.of(sampleBox));
        when(mysteryBoxRepository.save(any(MysteryBox.class))).thenReturn(sampleBox);
        when(mysteryBoxMapper.toMysteryBoxResponse(sampleBox)).thenReturn(sampleBoxResponse);
        when(productMysteryRepository.findByMysteryBox_MysteryId(1)).thenReturn(List.of());

        MysteryBoxResponse response = mysteryBoxService.toggleMysteryBoxStatus(1, false);

        assertThat(response).isNotNull();
        assertThat(sampleBox.getIsActive()).isFalse();
    }



    @Test
    void deleteMysteryBox_unsold_refundsStockAndDeletes() {
        mockAuthenticatedUser(10);
        sampleBox.setSoldQuantity(0);
        sampleBox.setTotalQuantity(10);

        ProductMystery pm1 = ProductMystery.builder()
                .mysteryBox(sampleBox)
                .product(sampleProduct1) // current stock 50
                .productQuantity(2)
                .build();

        when(mysteryBoxRepository.findById(1)).thenReturn(Optional.of(sampleBox));
        when(productMysteryRepository.findByMysteryBox_MysteryId(1)).thenReturn(List.of(pm1));

        mysteryBoxService.deleteMysteryBox(1);

        // Refund: 10 unsold * 2 = 20 units added back to 50 -> 70
        assertThat(sampleProduct1.getStockQuantity()).isEqualTo(70);
        verify(productRepository).save(sampleProduct1);
        verify(mysteryBoxRepository).save(sampleBox);
    }
}
