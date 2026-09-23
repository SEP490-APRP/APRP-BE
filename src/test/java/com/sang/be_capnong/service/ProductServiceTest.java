package com.sang.be_capnong.service;

import com.sang.be_capnong.dto.request.ProductCreationRequest;
import com.sang.be_capnong.dto.request.ProductUpdateRequest;
import com.sang.be_capnong.dto.response.PageResponse;
import com.sang.be_capnong.dto.response.ProductResponse;
import com.sang.be_capnong.entity.Category;
import com.sang.be_capnong.entity.Product;
import com.sang.be_capnong.entity.Role;
import com.sang.be_capnong.entity.User;
import com.sang.be_capnong.exception.AppException;
import com.sang.be_capnong.exception.ErrorCode;
import com.sang.be_capnong.mapper.ProductMapper;
import com.sang.be_capnong.repository.CategoryRepository;
import com.sang.be_capnong.repository.ProductRepository;
import com.sang.be_capnong.repository.UserRepository;
import com.sang.be_capnong.service.impl.ProductServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    ProductRepository productRepository;

    @Mock
    CategoryRepository categoryRepository;

    @Mock
    UserRepository userRepository;

    @Mock
    ProductMapper productMapper;

    @InjectMocks
    ProductServiceImpl productService;

    User shopUser;
    User otherShopUser;
    User adminUser;
    User buyerUser;
    Category sampleCategory;
    Product sampleProduct;
    ProductResponse sampleResponse;
    ProductCreationRequest creationRequest;
    ProductUpdateRequest updateRequest;

    @BeforeEach
    void setUp() {
        Role shopRole = Role.builder().id(2).name("SHOP").build();
        Role adminRole = Role.builder().id(4).name("ADMIN").build();
        Role buyerRole = Role.builder().id(1).name("BUYER").build();

        shopUser = User.builder()
                .id(1)
                .email("farmer@capnong.com")
                .shopName("Nông Trại Xanh")
                .role(shopRole)
                .build();

        otherShopUser = User.builder()
                .id(2)
                .email("other@capnong.com")
                .shopName("Nông Trại Đỏ")
                .role(shopRole)
                .build();

        adminUser = User.builder()
                .id(99)
                .email("admin@capnong.com")
                .role(adminRole)
                .build();

        buyerUser = User.builder()
                .id(3)
                .email("buyer@capnong.com")
                .role(buyerRole)
                .build();

        sampleCategory = Category.builder()
                .categoryId(1)
                .name("Rau củ")
                .build();

        sampleProduct = Product.builder()
                .productId(10)
                .productName("Cà rốt cứu hộ")
                .unit("kg")
                .sellingPrice(BigDecimal.valueOf(15000))
                .stockQuantity(100)
                .status("ACTIVE")
                .expiryDate(LocalDate.now().plusDays(3))
                .shop(shopUser)
                .category(sampleCategory)
                .build();

        sampleResponse = ProductResponse.builder()
                .productId(10)
                .productName("Cà rốt cứu hộ")
                .unit("kg")
                .sellingPrice(BigDecimal.valueOf(15000))
                .stockQuantity(100)
                .status("ACTIVE")
                .expiryDate(LocalDate.now().plusDays(3))
                .build();

        creationRequest = ProductCreationRequest.builder()
                .productName("Cà rốt cứu hộ")
                .categoryId(1)
                .unit("kg")
                .sellingPrice(BigDecimal.valueOf(15000))
                .stockQuantity(100)
                .expiryDate(LocalDate.now().plusDays(3))
                .build();

        updateRequest = ProductUpdateRequest.builder()
                .productName("Cà rốt Đà Lạt cứu hộ")
                .categoryId(1)
                .unit("kg")
                .sellingPrice(BigDecimal.valueOf(12000))
                .stockQuantity(80)
                .status("ACTIVE")
                .expiryDate(LocalDate.now().plusDays(2))
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
    void createProduct_success() {
        mockAuthenticatedUser(1);
        when(userRepository.findById(1)).thenReturn(Optional.of(shopUser));
        when(categoryRepository.findById(1)).thenReturn(Optional.of(sampleCategory));
        when(productMapper.toProduct(creationRequest)).thenReturn(sampleProduct);
        when(productRepository.save(any(Product.class))).thenReturn(sampleProduct);
        when(productMapper.toProductResponse(sampleProduct)).thenReturn(sampleResponse);

        ProductResponse result = productService.createProduct(creationRequest);

        assertThat(result).isNotNull();
        assertThat(result.getProductName()).isEqualTo("Cà rốt cứu hộ");
        verify(productRepository).save(any(Product.class));
    }

    @Test
    void createProduct_userNotFound_throwsAppException() {
        mockAuthenticatedUser(99);
        when(userRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.createProduct(creationRequest))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_EXISTED);

        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void createProduct_setsShopCategoryAndStatus() {
        Product freshProduct = Product.builder()
                .productId(11)
                .productName("Cà rốt cứu hộ")
                .unit("kg")
                .sellingPrice(BigDecimal.valueOf(15000))
                .stockQuantity(100)
                .expiryDate(LocalDate.now().plusDays(3))
                .build();

        mockAuthenticatedUser(1);
        when(userRepository.findById(1)).thenReturn(Optional.of(shopUser));
        when(categoryRepository.findById(1)).thenReturn(Optional.of(sampleCategory));
        when(productMapper.toProduct(creationRequest)).thenReturn(freshProduct);
        when(productRepository.save(any(Product.class))).thenReturn(freshProduct);
        when(productMapper.toProductResponse(freshProduct)).thenReturn(sampleResponse);

        productService.createProduct(creationRequest);

        assertThat(freshProduct.getShop()).isEqualTo(shopUser);
        assertThat(freshProduct.getCategory()).isEqualTo(sampleCategory);
        assertThat(freshProduct.getStatus()).isEqualTo("ACTIVE");
    }

    @Test
    void createProduct_userNotShop_throwsAppException() {
        mockAuthenticatedUser(3);
        when(userRepository.findById(3)).thenReturn(Optional.of(buyerUser));

        assertThatThrownBy(() -> productService.createProduct(creationRequest))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_A_SHOP);

        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void createProduct_categoryNotFound_throwsAppException() {
        mockAuthenticatedUser(1);
        when(userRepository.findById(1)).thenReturn(Optional.of(shopUser));
        when(categoryRepository.findById(1)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.createProduct(creationRequest))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CATEGORY_NOT_FOUND);

        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void createProduct_expiryInPast_throwsAppException() {
        mockAuthenticatedUser(1);
        when(userRepository.findById(1)).thenReturn(Optional.of(shopUser));
        when(categoryRepository.findById(1)).thenReturn(Optional.of(sampleCategory));

        creationRequest.setExpiryDate(LocalDate.now().minusDays(1));

        assertThatThrownBy(() -> productService.createProduct(creationRequest))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCT_EXPIRY_INVALID);

        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void getProductById_success() {
        when(productRepository.findById(10)).thenReturn(Optional.of(sampleProduct));
        when(productMapper.toProductResponse(sampleProduct)).thenReturn(sampleResponse);

        ProductResponse result = productService.getProductById(10);

        assertThat(result).isNotNull();
        assertThat(result.getProductId()).isEqualTo(10);
    }

    @Test
    void getProductById_notFound_throwsAppException() {
        when(productRepository.findById(999)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getProductById(999))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCT_NOT_FOUND);
    }

    @Test
    void getProducts_success() {
        Page<Product> page = new PageImpl<>(List.of(sampleProduct));
        when(productRepository.findProducts(eq("ACTIVE"), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(page);
        when(productMapper.toProductResponseList(anyList())).thenReturn(List.of(sampleResponse));

        PageResponse<ProductResponse> result = productService.getProducts(null, null, null, 1, 10, null, null);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    void getProducts_withFiltersAndAscSort() {
        Page<Product> page = new PageImpl<>(List.of(sampleProduct), PageRequest.of(1, 20), 1);
        when(productRepository.findProducts(eq("ACTIVE"), eq(1), eq("cà rốt"), any(Pageable.class)))
                .thenReturn(page);
        when(productMapper.toProductResponseList(anyList())).thenReturn(List.of(sampleResponse));

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);

        PageResponse<ProductResponse> result = productService.getProducts(1, "cà rốt", "ACTIVE", 2, 20, "sellingPrice", "asc");

        assertThat(result).isNotNull();
        assertThat(result.getPage()).isEqualTo(1);
        assertThat(result.getSize()).isEqualTo(20);
        assertThat(result.getContent()).hasSize(1);

        verify(productRepository).findProducts(eq("ACTIVE"), eq(1), eq("cà rốt"), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getPageNumber()).isEqualTo(1);
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(20);
        assertThat(pageableCaptor.getValue().getSort().getOrderFor("sellingPrice").getDirection())
                .isEqualTo(Sort.Direction.ASC);
    }

    @Test
    void getProducts_defaultStatusAndSort_whenBlank() {
        Page<Product> page = new PageImpl<>(List.of(sampleProduct));
        when(productRepository.findProducts(eq("ACTIVE"), isNull(), eq(""), any(Pageable.class)))
                .thenReturn(page);
        when(productMapper.toProductResponseList(anyList())).thenReturn(List.of(sampleResponse));

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);

        productService.getProducts(null, "", "", 1, 0, "", "desc");

        verify(productRepository).findProducts(eq("ACTIVE"), isNull(), eq(""), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(1);
        assertThat(pageableCaptor.getValue().getSort().getOrderFor("productId").getDirection())
                .isEqualTo(Sort.Direction.DESC);
    }

    @Test
    void getMyProducts_success() {
        mockAuthenticatedUser(1);
        Page<Product> page = new PageImpl<>(List.of(sampleProduct));
        when(productRepository.findByShopIdAndOptionalStatus(eq(1), isNull(), any(Pageable.class)))
                .thenReturn(page);
        when(productMapper.toProductResponseList(anyList())).thenReturn(List.of(sampleResponse));

        PageResponse<ProductResponse> result = productService.getMyProducts(null, 1, 10);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    void getMyProducts_withStatusFilter() {
        mockAuthenticatedUser(1);
        Page<Product> page = new PageImpl<>(List.of(sampleProduct));
        when(productRepository.findByShopIdAndOptionalStatus(eq(1), eq("ACTIVE"), any(Pageable.class)))
                .thenReturn(page);
        when(productMapper.toProductResponseList(anyList())).thenReturn(List.of(sampleResponse));

        PageResponse<ProductResponse> result = productService.getMyProducts("ACTIVE", 1, 10);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        verify(productRepository).findByShopIdAndOptionalStatus(eq(1), eq("ACTIVE"), any(Pageable.class));
    }

    @Test
    void updateProduct_success() {
        mockAuthenticatedUser(1);
        when(productRepository.findById(10)).thenReturn(Optional.of(sampleProduct));
        when(categoryRepository.findById(1)).thenReturn(Optional.of(sampleCategory));
        when(productRepository.save(any(Product.class))).thenReturn(sampleProduct);
        when(productMapper.toProductResponse(sampleProduct)).thenReturn(sampleResponse);

        ProductResponse result = productService.updateProduct(10, updateRequest);

        assertThat(result).isNotNull();
        verify(productMapper).updateProduct(sampleProduct, updateRequest);
        verify(productRepository).save(sampleProduct);
    }

    @Test
    void updateProduct_notOwner_throwsAppException() {
        mockAuthenticatedUser(2); // different shop user
        when(productRepository.findById(10)).thenReturn(Optional.of(sampleProduct));

        assertThatThrownBy(() -> productService.updateProduct(10, updateRequest))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOT_PRODUCT_OWNER);

        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void updateProduct_productNotFound_throwsAppException() {
        mockAuthenticatedUser(1);
        when(productRepository.findById(999)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.updateProduct(999, updateRequest))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCT_NOT_FOUND);

        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void updateProduct_categoryNotFound_throwsAppException() {
        mockAuthenticatedUser(1);
        when(productRepository.findById(10)).thenReturn(Optional.of(sampleProduct));
        when(categoryRepository.findById(1)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.updateProduct(10, updateRequest))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CATEGORY_NOT_FOUND);

        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void updateProduct_expiryInPast_throwsAppException() {
        mockAuthenticatedUser(1);
        when(productRepository.findById(10)).thenReturn(Optional.of(sampleProduct));
        when(categoryRepository.findById(1)).thenReturn(Optional.of(sampleCategory));

        updateRequest.setExpiryDate(LocalDate.now().minusDays(1));

        assertThatThrownBy(() -> productService.updateProduct(10, updateRequest))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCT_EXPIRY_INVALID);

        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void updateProduct_blankStatus_keepsOriginal() {
        mockAuthenticatedUser(1);
        when(productRepository.findById(10)).thenReturn(Optional.of(sampleProduct));
        when(categoryRepository.findById(1)).thenReturn(Optional.of(sampleCategory));
        when(productRepository.save(any(Product.class))).thenReturn(sampleProduct);
        when(productMapper.toProductResponse(sampleProduct)).thenReturn(sampleResponse);

        updateRequest.setStatus("  ");

        ProductResponse result = productService.updateProduct(10, updateRequest);

        assertThat(result).isNotNull();
        assertThat(sampleProduct.getStatus()).isEqualTo("ACTIVE");
        verify(productRepository).save(sampleProduct);
    }

    @Test
    void updateProduct_nonBlankStatus_setsTrimmedStatus() {
        mockAuthenticatedUser(1);
        when(productRepository.findById(10)).thenReturn(Optional.of(sampleProduct));
        when(categoryRepository.findById(1)).thenReturn(Optional.of(sampleCategory));
        when(productRepository.save(any(Product.class))).thenReturn(sampleProduct);
        when(productMapper.toProductResponse(sampleProduct)).thenReturn(sampleResponse);

        updateRequest.setStatus("  INACTIVE  ");

        productService.updateProduct(10, updateRequest);

        assertThat(sampleProduct.getStatus()).isEqualTo("INACTIVE");
        verify(productRepository).save(sampleProduct);
    }

    @Test
    void deleteProduct_productNotFound_throwsAppException() {
        mockAuthenticatedUser(1);
        when(productRepository.findById(999)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.deleteProduct(999))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCT_NOT_FOUND);

        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void deleteProduct_userNotFound_throwsAppException() {
        mockAuthenticatedUser(99);
        when(productRepository.findById(10)).thenReturn(Optional.of(sampleProduct));
        when(userRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.deleteProduct(10))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_EXISTED);

        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void deleteProduct_byOwner_success() {
        mockAuthenticatedUser(1);
        when(productRepository.findById(10)).thenReturn(Optional.of(sampleProduct));
        when(userRepository.findById(1)).thenReturn(Optional.of(shopUser));

        productService.deleteProduct(10);

        assertThat(sampleProduct.getStatus()).isEqualTo("INACTIVE");
        verify(productRepository).save(sampleProduct);
    }

    @Test
    void deleteProduct_byAdmin_success() {
        mockAuthenticatedUser(99);
        when(productRepository.findById(10)).thenReturn(Optional.of(sampleProduct));
        when(userRepository.findById(99)).thenReturn(Optional.of(adminUser));

        productService.deleteProduct(10);

        assertThat(sampleProduct.getStatus()).isEqualTo("INACTIVE");
        verify(productRepository).save(sampleProduct);
    }

    @Test
    void deleteProduct_notOwnerNotAdmin_throwsAppException() {
        mockAuthenticatedUser(2);
        when(productRepository.findById(10)).thenReturn(Optional.of(sampleProduct));
        when(userRepository.findById(2)).thenReturn(Optional.of(otherShopUser));

        assertThatThrownBy(() -> productService.deleteProduct(10))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOT_PRODUCT_OWNER);
    }
}
