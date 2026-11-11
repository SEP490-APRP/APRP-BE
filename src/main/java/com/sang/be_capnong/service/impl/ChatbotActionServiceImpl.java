package com.sang.be_capnong.service.impl;

import com.sang.be_capnong.dto.request.AiDetectedIntent;
import com.sang.be_capnong.dto.request.CartAddRequest;
import com.sang.be_capnong.dto.response.CartItemResponse;
import com.sang.be_capnong.dto.response.CartResponse;
import com.sang.be_capnong.dto.response.ChatbotMessageResponse;
import com.sang.be_capnong.dto.response.ReturnRequestResponse;
import com.sang.be_capnong.dto.response.ShopCartResponse;
import com.sang.be_capnong.entity.*;
import com.sang.be_capnong.enums.AiAccountActionType;
import com.sang.be_capnong.enums.AiItemType;
import com.sang.be_capnong.enums.AiResponseType;
import com.sang.be_capnong.enums.OrderStatus;
import com.sang.be_capnong.enums.ReturnStatus;
import com.sang.be_capnong.enums.WalletStatus;
import com.sang.be_capnong.exception.AppException;
import com.sang.be_capnong.exception.ErrorCode;
import com.sang.be_capnong.mapper.ReturnRequestMapper;
import com.sang.be_capnong.repository.*;
import com.sang.be_capnong.repository.specification.ProductSpecification;
import com.sang.be_capnong.service.CartService;
import com.sang.be_capnong.service.ChatbotAiClient;
import com.sang.be_capnong.service.ChatbotActionService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import com.sang.be_capnong.enums.VoucherType;
import com.sang.be_capnong.enums.VoucherScope;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class ChatbotActionServiceImpl implements ChatbotActionService {

    ProductRepository productRepository;
    MysteryBoxRepository mysteryBoxRepository;
    CartService cartService;
    OrderRepository orderRepository;
    OrderDetailRepository orderDetailRepository;
    UserRepository userRepository;
    VoucherRepository voucherRepository;
    VoucherUserRepository voucherUserRepository;
    UserAllergyRepository userAllergyRepository;
    PaymentRepository paymentRepository;
    SavedAddressRepository savedAddressRepository;
    BuildComboRepository buildComboRepository;
    ChatbotAiClient chatbotAiClient;
    ChatbotSupportProperties supportProperties;
    WalletRepository walletRepository;
    WalletTransactionRepository walletTransactionRepository;
    ReturnRequestRepository returnRequestRepository;
    ReturnRequestMapper returnRequestMapper;

    /** Cùng điều kiện với OrderServiceImpl.resolveProductPrice: giảm giá chỉ có hiệu lực trong khoảng ngày khuyến mãi. */
    private boolean isOnSale(Product p) {
        LocalDateTime now = LocalDateTime.now();
        return p.getSellingPrice() != null
                && p.getDiscountPercent() != null && p.getDiscountPercent() > 0
                && p.getDiscountStartDate() != null && p.getDiscountEndDate() != null
                && now.isAfter(p.getDiscountStartDate()) && now.isBefore(p.getDiscountEndDate());
    }

    /** Giá khách thực trả khi tạo đơn. */
    private BigDecimal salePriceOf(Product p) {
        if (!isOnSale(p)) {
            return p.getSellingPrice();
        }
        BigDecimal factor = BigDecimal.valueOf(100 - p.getDiscountPercent())
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        return p.getSellingPrice().multiply(factor);
    }

    private List<String> allergensOf(Integer userId) {
        if (userId == null) return List.of();
        try {
            return userAllergyRepository.findByBuyer_Id(userId).stream()
                    .filter(ua -> ua.getAllergen() != null && ua.getAllergen().getName() != null)
                    .map(ua -> ua.getAllergen().getName())
                    .toList();
        } catch (Exception e) {
            log.warn("Could not load allergies of user {}: {}", userId, e.getMessage());
            return List.of();
        }
    }

    private static String foldAccents(String text) {
        return Normalizer.normalize(text == null ? "" : text, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .replace('đ', 'd').replace('Đ', 'D')
                .toLowerCase(Locale.ROOT);
    }

    /** So theo tên nên chỉ loại được món có tên chứa chất gây dị ứng, không đảm bảo về thành phần. */
    private static boolean mentionsAllergen(String text, List<String> allergens) {
        String folded = foldAccents(text);
        return allergens.stream().anyMatch(a -> !a.isBlank() && folded.contains(foldAccents(a)));
    }

    private ChatbotMessageResponse orderNotFound(Integer orderId) {
        return ChatbotMessageResponse.builder()
                .type(AiResponseType.NAVIGATION)
                .message("Không tìm thấy đơn hàng #" + orderId + " trong tài khoản của bạn. Bạn kiểm tra lại mã đơn trong Lịch sử đơn hàng nhé!")
                .navigation(ChatbotMessageResponse.NavigationDto.builder().url("/orders").label("Xem Lịch Sử Đơn Hàng").build())
                .timestamp(LocalDateTime.now())
                .build();
    }

    private Optional<Order> ownedOrder(Integer orderId, Integer userId) {
        return orderRepository.findById(orderId).filter(o -> o.getBuyer().getId() == userId);
    }

    @Override
    public List<ChatbotMessageResponse.ItemOptionDto> searchItems(String keyword, AiItemType preferredType) {
        List<ChatbotMessageResponse.ItemOptionDto> results = new ArrayList<>();
        String cleaned = (keyword != null) ? GeminiChatbotAiClient.cleanSearchKeyword(keyword) : "";
        String query = (!cleaned.isEmpty()) ? cleaned : null;

        // 1. Search products if preferredType is null, PRODUCT, or ALL
        if (preferredType == null || preferredType == AiItemType.PRODUCT) {
            Page<Product> products = productRepository.findAll(
                    ProductSpecification.filterProducts("ACTIVE", null, query, null),
                    PageRequest.of(0, 6)
            );


            for (Product p : products.getContent()) {
                if (p.getStockQuantity() != null && p.getStockQuantity() <= 0) {
                    continue;
                }
                String img = (p.getImages() != null && !p.getImages().isEmpty())
                        ? p.getImages().get(0).getImageUrl()
                        : null;

                BigDecimal salePrice = salePriceOf(p);

                results.add(ChatbotMessageResponse.ItemOptionDto.builder()
                        .itemType(AiItemType.PRODUCT)
                        .itemId(p.getProductId())
                        .name(p.getProductName())
                        .imageUrl(img)
                        .price(p.getSellingPrice())
                        .salePrice(salePrice)
                        .unit(p.getUnit() != null ? p.getUnit() : "kg")
                        .availableQuantity(p.getStockQuantity())
                        .description(p.getDescription())
                        .build());
            }
        }

        // 2. Search mystery boxes if preferredType is null, MYSTERY_BOX
        if (preferredType == null || preferredType == AiItemType.MYSTERY_BOX) {
            List<MysteryBox> boxes = mysteryBoxRepository.findAll();
            for (MysteryBox mb : boxes) {
                if (Boolean.TRUE.equals(mb.getIsActive())) {
                    int remaining = (mb.getTotalQuantity() != null ? mb.getTotalQuantity() : 0)
                            - (mb.getSoldQuantity() != null ? mb.getSoldQuantity() : 0);
                    if (remaining <= 0) continue;

                    if (query != null) {
                        String name = mb.getBoxType() != null ? mb.getBoxType().toLowerCase() : "";
                        String desc = mb.getDescription() != null ? mb.getDescription().toLowerCase() : "";
                        if (!name.contains(query.toLowerCase()) && !desc.contains(query.toLowerCase())) {
                            continue;
                        }
                    }

                    results.add(ChatbotMessageResponse.ItemOptionDto.builder()
                            .itemType(AiItemType.MYSTERY_BOX)
                            .itemId(mb.getMysteryId())
                            .name(mb.getBoxType() != null ? mb.getBoxType() : "Hộp mù nông sản giải cứu")
                            .imageUrl(mb.getImageUrl())
                            .price(mb.getPrice())
                            .salePrice(mb.getPrice())
                            .unit("hộp")
                            .availableQuantity(remaining)
                            .description(mb.getDescription())
                            .build());
                }
            }
        }

        // 3. Search build combos if preferredType is null, BUILD_COMBO, or query contains "combo"
        if (preferredType == null || preferredType == AiItemType.BUILD_COMBO || (query != null && query.toLowerCase().contains("combo"))) {
            List<BuildCombo> combos = buildComboRepository.findAll();
            for (BuildCombo bc : combos) {
                if ("ACTIVE".equalsIgnoreCase(bc.getStatus())) {
                    int remaining = (bc.getTotalQuantity() != null ? bc.getTotalQuantity() : 0)
                            - (bc.getSoldQuantity() != null ? bc.getSoldQuantity() : 0);
                    if (remaining <= 0) continue;

                    if (query != null) {
                        String cleanQ = query.toLowerCase().replace("combo", "").trim();
                        String name = bc.getComboName() != null ? bc.getComboName().toLowerCase() : "";
                        String desc = bc.getDescription() != null ? bc.getDescription().toLowerCase() : "";
                        boolean isGeneralVeggieQuery = cleanQ.isEmpty() || cleanQ.equals("rau") || cleanQ.equals("rau cu") || cleanQ.equals("rau củ") || cleanQ.equals("củ quả") || cleanQ.equals("cu qua");

                        if (!isGeneralVeggieQuery) {
                            if (!name.contains(cleanQ) && !desc.contains(cleanQ)) {
                                continue;
                            }
                        } else {
                            // Với câu hỏi tìm combo rau chung, ưu tiên tất cả combo rau củ dinh dưỡng (loại trừ các đĩa thuần trái cây tráng miệng)
                            boolean isPureFruit = (name.contains("trái cây") || name.contains("sinh tố")) && !name.contains("rau");
                            if (isPureFruit) continue;
                        }
                    }

                    results.add(ChatbotMessageResponse.ItemOptionDto.builder()
                            .itemType(AiItemType.BUILD_COMBO)
                            .itemId(bc.getComboId())
                            .name(bc.getComboName() != null ? bc.getComboName() : "Combo rau củ quả dinh dưỡng")
                            .imageUrl(bc.getImageUrl())
                            .price(bc.getPrice())
                            .salePrice(bc.getPrice())
                            .unit("combo")
                            .availableQuantity(remaining)
                            .description(bc.getDescription())
                            .build());
                }
            }
        }

        return results;
    }

    @Override
    public ChatbotMessageResponse buyItems(AiDetectedIntent intent, Integer userId) {
        if (userId == null) {
            return ChatbotMessageResponse.builder()
                    .type(AiResponseType.NAVIGATION)
                    .message("Bạn cần đăng nhập tài khoản CapNong để thêm sản phẩm vào giỏ hàng hoặc mua sắm nhé! Bấm nút bên dưới để đăng nhập ngay nha.")
                    .navigation(ChatbotMessageResponse.NavigationDto.builder()
                            .url("/login")
                            .label("Đăng nhập tài khoản")
                            .build())
                    .timestamp(LocalDateTime.now())
                    .build();
        }

        List<AiDetectedIntent.ItemBuyCommand> commands = new ArrayList<>();
        if (intent.getBuyItems() != null) {
            intent.getBuyItems().stream()
                    .filter(c -> c.getName() != null && !c.getName().isBlank())
                    .forEach(commands::add);
        }
        if (commands.isEmpty() && intent.getKeyword() != null && !intent.getKeyword().isBlank()) {
            commands.add(AiDetectedIntent.ItemBuyCommand.builder()
                    .name(intent.getKeyword())
                    .quantity(intent.getQuantity() != null ? intent.getQuantity().doubleValue() : null)
                    .build());
        }
        if (commands.isEmpty()) {
            return ChatbotMessageResponse.text("Bạn muốn mua món nông sản nào ạ? Hãy nhập tên sản phẩm (ví dụ: 'Mua 2kg cà chua' hoặc 'Đặt hộp bí ẩn') để em hỗ trợ nhé!");
        }

        List<String> notes = new ArrayList<>();
        List<String> notFound = new ArrayList<>();
        List<ChatbotMessageResponse.ItemOptionDto> added = new ArrayList<>();
        List<ChatbotMessageResponse.ItemOptionDto> options = new ArrayList<>();

        for (AiDetectedIntent.ItemBuyCommand command : commands) {
            List<ChatbotMessageResponse.ItemOptionDto> matches = searchItems(command.getName(), intent.getItemType());
            if (matches.isEmpty()) {
                notFound.add(command.getName());
                continue;
            }
            // Giữ số lượng khách đã nói để nút Chọn trên thẻ thêm đúng số đó.
            matches.forEach(m -> m.setOrderQuantity(ChatbotQuantityResolver.resolve(command.getQuantity(), command.getUnit(), m.getUnit())));

            if (matches.size() > 1) {
                options.addAll(matches);
                notes.add("Có " + matches.size() + " loại phù hợp với '" + command.getName() + "', bạn chọn loại nào bên dưới nhé.");
                String unresolvedNote = unresolvedQuantityNote(command, matches);
                if (unresolvedNote != null) notes.add(unresolvedNote);
                continue;
            }

            ChatbotMessageResponse.ItemOptionDto target = matches.get(0);
            if (target.getOrderQuantity() == null) {
                options.add(target);
                notes.add(target.getName() + " đang bán theo " + target.getUnit() + " và chỉ thêm được số nguyên, nên mình chưa thêm vào giỏ. Bạn bấm Chọn rồi chỉnh số lượng trong giỏ hàng nhé.");
                continue;
            }

            String failure = tryAddToCart(target, target.getOrderQuantity());
            if (failure != null) {
                notes.add(failure);
            } else {
                added.add(target);
                notes.add("Đã thêm " + target.getOrderQuantity() + " " + target.getUnit() + " " + target.getName() + " vào giỏ hàng.");
            }
        }

        if (added.isEmpty() && options.isEmpty() && notes.isEmpty()) {
            List<ChatbotMessageResponse.ItemOptionDto> suggestions = searchItems(null, AiItemType.PRODUCT);
            String names = String.join(", ", notFound);
            if (suggestions.isEmpty()) {
                return ChatbotMessageResponse.text("Rất tiếc, CapNong hiện chưa tìm thấy món '" + names + "' hoặc sản phẩm đang tạm hết hàng.");
            }
            return ChatbotMessageResponse.builder()
                    .type(AiResponseType.ITEM_OPTIONS)
                    .message("Hiện tại sàn CapNong chưa có sẵn nông sản '" + names + "'. Bạn có thể tham khảo những nông sản khác đang có sẵn trên sàn hôm nay nhé:")
                    .itemOptions(suggestions.stream().limit(4).collect(Collectors.toList()))
                    .timestamp(LocalDateTime.now())
                    .build();
        }

        if (!notFound.isEmpty()) {
            notes.add("Chưa tìm thấy: " + String.join(", ", notFound) + ".");
        }

        ChatbotMessageResponse.ChatbotMessageResponseBuilder builder = ChatbotMessageResponse.builder()
                .message(String.join("\n", notes))
                .timestamp(LocalDateTime.now());
        if (!added.isEmpty()) {
            builder.cart(buildCartPreview());
            if (added.size() == 1) builder.selectedProduct(added.get(0));
        }
        if (!options.isEmpty()) {
            builder.itemOptions(options).type(AiResponseType.ITEM_OPTIONS);
        } else {
            builder.type(added.isEmpty() ? AiResponseType.ERROR : AiResponseType.CART_UPDATED);
        }
        return builder.build();
    }

    /**
     * Khách có nói số lượng nhưng một số lựa chọn không quy đổi được (vd 1.5kg, hoặc combo bán theo phần).
     * Nút Chọn khi đó chỉ thêm 1 đơn vị, nên phải báo trước để khách không tưởng đã mua đúng số mình nói.
     */
    private String unresolvedQuantityNote(AiDetectedIntent.ItemBuyCommand command,
                                          List<ChatbotMessageResponse.ItemOptionDto> matches) {
        if (command.getQuantity() == null && (command.getUnit() == null || command.getUnit().isBlank())) return null;
        long unresolved = matches.stream().filter(m -> m.getOrderQuantity() == null).count();
        if (unresolved == 0) return null;

        String requested = formatAmount(command.getQuantity() != null ? command.getQuantity() : 1d)
                + (command.getUnit() != null && !command.getUnit().isBlank() ? " " + command.getUnit().trim() : "");
        String scope = unresolved == matches.size() ? "Các loại này" : "Một số loại bên dưới";
        return scope + " không thêm được đúng " + requested + " vì giỏ hàng chỉ nhận số nguyên theo đơn vị bán. "
                + "Bấm Chọn sẽ thêm 1 đơn vị, bạn chỉnh lại số lượng trong giỏ hàng nhé.";
    }

    private static String formatAmount(double amount) {
        return amount == Math.rint(amount) ? String.valueOf((long) amount) : String.valueOf(amount);
    }

    @Override
    public ChatbotMessageResponse selectItemOption(Integer itemId, AiItemType itemType, Integer quantity, Integer userId) {
        if (userId == null) {
            return ChatbotMessageResponse.builder()
                    .type(AiResponseType.NAVIGATION)
                    .message("Vui lòng đăng nhập để chọn sản phẩm vào giỏ hàng nhé!")
                    .navigation(ChatbotMessageResponse.NavigationDto.builder()
                            .url("/login")
                            .label("Đăng nhập")
                            .build())
                    .timestamp(LocalDateTime.now())
                    .build();
        }

        if (itemId == null || itemType == null) {
            return ChatbotMessageResponse.error("Thông tin sản phẩm lựa chọn không hợp lệ.");
        }

        int qty = (quantity != null && quantity > 0) ? quantity : 1;
        ChatbotMessageResponse.ItemOptionDto itemDto = null;

        if (itemType == AiItemType.PRODUCT) {
            Product p = productRepository.findById(itemId).orElse(null);
            if (p == null) {
                return ChatbotMessageResponse.error("Sản phẩm không tồn tại hoặc đã ngừng kinh doanh.");
            }
            String img = (p.getImages() != null && !p.getImages().isEmpty()) ? p.getImages().get(0).getImageUrl() : null;
            itemDto = ChatbotMessageResponse.ItemOptionDto.builder()
                    .itemType(AiItemType.PRODUCT)
                    .itemId(p.getProductId())
                    .name(p.getProductName())
                    .imageUrl(img)
                    .price(p.getSellingPrice())
                    .salePrice(salePriceOf(p))
                    .unit(p.getUnit())
                    .availableQuantity(p.getStockQuantity())
                    .build();
        } else if (itemType == AiItemType.MYSTERY_BOX) {
            MysteryBox mb = mysteryBoxRepository.findById(itemId).orElse(null);
            if (mb == null) {
                return ChatbotMessageResponse.error("Hộp bí ẩn không tồn tại hoặc đã bán hết.");
            }
            int remaining = (mb.getTotalQuantity() != null ? mb.getTotalQuantity() : 0)
                    - (mb.getSoldQuantity() != null ? mb.getSoldQuantity() : 0);
            itemDto = ChatbotMessageResponse.ItemOptionDto.builder()
                    .itemType(AiItemType.MYSTERY_BOX)
                    .itemId(mb.getMysteryId())
                    .name(mb.getBoxType())
                    .imageUrl(mb.getImageUrl())
                    .price(mb.getPrice())
                    .salePrice(mb.getPrice())
                    .unit("hộp")
                    .availableQuantity(remaining)
                    .build();
        } else if (itemType == AiItemType.BUILD_COMBO || itemType == AiItemType.COMBO) {
            BuildCombo bc = buildComboRepository.findById(itemId).orElse(null);
            if (bc == null) {
                return ChatbotMessageResponse.error("Gói combo không tồn tại hoặc đã ngừng kinh doanh.");
            }
            int remaining = (bc.getTotalQuantity() != null ? bc.getTotalQuantity() : 0)
                    - (bc.getSoldQuantity() != null ? bc.getSoldQuantity() : 0);
            itemDto = ChatbotMessageResponse.ItemOptionDto.builder()
                    .itemType(AiItemType.BUILD_COMBO)
                    .itemId(bc.getComboId())
                    .name(bc.getComboName())
                    .imageUrl(bc.getImageUrl())
                    .price(bc.getPrice())
                    .salePrice(bc.getPrice())
                    .unit("combo")
                    .availableQuantity(remaining)
                    .description(bc.getDescription())
                    .build();
        }

        if (itemDto == null) {
            return ChatbotMessageResponse.error("Không tìm thấy thông tin sản phẩm đã chọn.");
        }

        return executeAddToCart(itemDto, qty);
    }

    /** @return null khi thêm thành công, ngược lại là lý do để hiện cho khách. */
    private String tryAddToCart(ChatbotMessageResponse.ItemOptionDto target, int qty) {
        try {
            String itemTypeStr = target.getItemType() != null ? target.getItemType().name() : "PRODUCT";
            if ("COMBO".equalsIgnoreCase(itemTypeStr)) {
                itemTypeStr = "BUILD_COMBO";
            }
            cartService.addToCart(CartAddRequest.builder()
                    .itemType(itemTypeStr)
                    .itemId(target.getItemId())
                    .quantity(qty)
                    .build());
            return null;
        } catch (AppException e) {
            if (e.getErrorCode() == ErrorCode.OUT_OF_STOCK) {
                Integer available = target.getAvailableQuantity();
                return target.getName() + " không đủ hàng" + (available != null ? " (chỉ còn " + available + " " + target.getUnit() + ")" : "") + ", mình chưa thêm vào giỏ.";
            }
            log.warn("Chatbot could not add item {} to cart: {}", target.getItemId(), e.getErrorCode());
            return target.getName() + " hiện không thể thêm vào giỏ hàng.";
        } catch (Exception e) {
            log.error("Failed to add item to cart via Chatbot: {}", e.getMessage(), e);
            return target.getName() + " hiện không thể thêm vào giỏ hàng. Bạn thử lại sau nhé!";
        }
    }

    private ChatbotMessageResponse executeAddToCart(ChatbotMessageResponse.ItemOptionDto target, int qty) {
        String failure = tryAddToCart(target, qty);
        if (failure != null) {
            return ChatbotMessageResponse.error(failure);
        }
        String unitStr = target.getUnit() != null ? target.getUnit() : "phần";
        return ChatbotMessageResponse.builder()
                .type(AiResponseType.CART_UPDATED)
                .message(String.format("Đã thêm thành công %d %s %s vào giỏ hàng của bạn! 🎉 Bạn có thể xem giỏ hàng hoặc tiến hành thanh toán nhé.",
                        qty, unitStr, target.getName()))
                .selectedProduct(target)
                .cart(buildCartPreview())
                .timestamp(LocalDateTime.now())
                .build();
    }

    private ChatbotMessageResponse.CartPreviewDto buildCartPreview() {
        try {
            CartResponse cart = cartService.getMyCart();
            if (cart == null) return null;
            List<ChatbotMessageResponse.CartItemDto> items = new ArrayList<>();
            if (cart.getShopCarts() != null) {
                for (ShopCartResponse shop : cart.getShopCarts()) {
                    if (shop.getItems() != null) {
                        for (CartItemResponse item : shop.getItems()) {
                            AiItemType type = AiItemType.PRODUCT;
                            if ("MYSTERY_BOX".equalsIgnoreCase(item.getItemType())) {
                                type = AiItemType.MYSTERY_BOX;
                            } else if ("BUILD_COMBO".equalsIgnoreCase(item.getItemType())) {
                                type = AiItemType.BUILD_COMBO;
                            }
                            BigDecimal total = (item.getUnitPrice() != null)
                                    ? item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()))
                                    : BigDecimal.ZERO;
                            items.add(ChatbotMessageResponse.CartItemDto.builder()
                                    .itemType(type)
                                    .itemId(item.getItemId())
                                    .name(item.getName())
                                    .imageUrl(item.getImageUrl())
                                    .quantity(item.getQuantity())
                                    .unitPrice(item.getUnitPrice())
                                    .totalPrice(total)
                                    .build());
                        }
                    }
                }
            }
            return ChatbotMessageResponse.CartPreviewDto.builder()
                    .items(items)
                    .totalAmount(cart.getCartTotal() != null ? cart.getCartTotal() : BigDecimal.ZERO)
                    .build();
        } catch (Exception e) {
            log.warn("Could not build cart preview: {}", e.getMessage());
            return null;
        }
    }

    @Override
    public ChatbotMessageResponse createCheckoutDraft(Integer userId) {
        if (userId == null) {
            return ChatbotMessageResponse.builder()
                    .type(AiResponseType.NAVIGATION)
                    .message("Bạn cần đăng nhập để tiến hành đặt hàng nhé!")
                    .navigation(ChatbotMessageResponse.NavigationDto.builder()
                            .url("/login")
                            .label("Đăng nhập ngay")
                            .build())
                    .timestamp(LocalDateTime.now())
                    .build();
        }

        try {
            CartResponse cart = cartService.getMyCart();
            if (cart == null || cart.getShopCarts() == null || cart.getShopCarts().isEmpty()) {
                return ChatbotMessageResponse.text("Giỏ hàng của bạn đang trống! Hãy chọn thêm một số món nông sản tươi ngon trước khi thanh toán nhé.");
            }

            return ChatbotMessageResponse.builder()
                    .type(AiResponseType.CHECKOUT_DRAFT)
                    .message("Đơn hàng của bạn đã sẵn sàng! Bạn có thể nhấn vào nút bên dưới để chuyển sang màn hình thanh toán.")
                    .checkout(ChatbotMessageResponse.CheckoutDraftDto.builder()
                            .checkoutUrl("/checkout")
                            .totalAmount(cart.getCartTotal() != null ? cart.getCartTotal() : BigDecimal.ZERO)
                            .build())
                    .timestamp(LocalDateTime.now())
                    .build();
        } catch (Exception e) {
            log.error("Error creating checkout draft: {}", e.getMessage());
            return ChatbotMessageResponse.error("Không thể tải thông tin thanh toán: " + e.getMessage());
        }
    }

    @Override
    public ChatbotMessageResponse handleAccountAction(AiAccountActionType actionType, Integer userId) {
        return handleAccountAction(actionType, null, userId);
    }

    @Override
    public ChatbotMessageResponse handleAccountAction(AiAccountActionType actionType, String userMessage, Integer userId) {
        if (actionType == null) {
            return ChatbotMessageResponse.text("Tôi có thể giúp bạn xem đơn hàng, quản lý ví tiền, cập nhật thông tin cá nhân hoặc xem giỏ hàng.");
        }

        String lower = (userMessage != null) ? userMessage.toLowerCase() : "";
        boolean isVagueReorderQuery = lower.contains("tuần trước") || lower.contains("tháng trước")
                || lower.contains("hôm trước") || lower.contains("tuần rồi") || lower.contains("tháng rồi") || lower.contains("năm ngoái")
                || (lower.contains("đơn cũ") && (lower.contains("mua lại") || lower.contains("đặt lại")));

        switch (actionType) {
            case VIEW_ORDERS:
            case VIEW_ORDER_DETAIL:
                String orderMsg = isVagueReorderQuery
                        ? "Do trong các khoảng thời gian trước (như tuần trước, tháng trước) bạn có thể đã đặt nhiều đơn hàng khác nhau, CapNong không thể tự ý chọn một đơn duy nhất giúp bạn. Bạn vui lòng bấm nút bên dưới để mở Lịch sử đơn hàng, xem lại toàn bộ các đơn và chọn mua lại theo đúng nhu cầu nhé!"
                        : "Bạn có thể theo dõi tiến độ đơn hàng và lịch sử mua sắm nông sản tại đây nhé!";
                return ChatbotMessageResponse.builder()
                        .type(AiResponseType.NAVIGATION)
                        .message(orderMsg)
                        .navigation(ChatbotMessageResponse.NavigationDto.builder()
                                .url("/orders")
                                .label("Xem lịch sử đơn hàng")
                                .build())
                        .timestamp(LocalDateTime.now())
                        .build();

            case VIEW_WALLET:
                return ChatbotMessageResponse.builder()
                        .type(AiResponseType.NAVIGATION)
                        .message("Bạn xem số dư, tiền tạm giữ và lịch sử giao dịch trong Ví của tôi nhé.")
                        .navigation(ChatbotMessageResponse.NavigationDto.builder()
                                .url("/my-wallet")
                                .label("Mở Ví của tôi")
                                .build())
                        .timestamp(LocalDateTime.now())
                        .build();

            case VIEW_PROFILE:
            case EDIT_PROFILE:
                return ChatbotMessageResponse.builder()
                        .type(AiResponseType.NAVIGATION)
                        .message("Đây là trang thông tin tài khoản cá nhân của bạn.")
                        .navigation(ChatbotMessageResponse.NavigationDto.builder()
                                .url("/tai-khoan")
                                .label("Hồ sơ cá nhân")
                                .build())
                        .timestamp(LocalDateTime.now())
                        .build();

            case VIEW_CART:
                return ChatbotMessageResponse.builder()
                        .type(AiResponseType.NAVIGATION)
                        .message("Chuyển nhanh đến giỏ hàng của bạn để kiểm tra sản phẩm.")
                        .navigation(ChatbotMessageResponse.NavigationDto.builder()
                                .url("/cart")
                                .label("Mở giỏ hàng")
                                .build())
                        .timestamp(LocalDateTime.now())
                        .build();

            case SAVED_ADDRESSES:
                return ChatbotMessageResponse.builder()
                        .type(AiResponseType.NAVIGATION)
                        .message("Quản lý danh sách địa chỉ giao nhận hàng của bạn tại đây.")
                        .navigation(ChatbotMessageResponse.NavigationDto.builder()
                                .url("/tai-khoan?tab=address")
                                .label("Sổ địa chỉ")
                                .build())
                        .timestamp(LocalDateTime.now())
                        .build();

            case LOGOUT:
                return ChatbotMessageResponse.text("Để đăng xuất, bạn mở mục Tài khoản và chọn 'Đăng xuất' nhé!");

            default:
                return ChatbotMessageResponse.text("CapNong Copilot luôn sẵn sàng hỗ trợ bạn bất kỳ thông tin nào về sản phẩm hoặc tài khoản!");
        }
    }

    // =========================================================================
    // ===== 12 ENHANCED ACTION METHODS ========================================
    // =========================================================================

    @Override
    public ChatbotMessageResponse trackOrder(Integer orderId, Integer userId) {
        if (userId == null) {
            return ChatbotMessageResponse.builder()
                    .type(AiResponseType.NAVIGATION)
                    .message("Bạn cần đăng nhập để tra cứu trạng thái đơn hàng và vị trí shipper nhé!")
                    .navigation(ChatbotMessageResponse.NavigationDto.builder().url("/login").label("Đăng nhập").build())
                    .timestamp(LocalDateTime.now())
                    .build();
        }

        Order targetOrder = null;
        if (orderId != null) {
            targetOrder = ownedOrder(orderId, userId).orElse(null);
            if (targetOrder == null) {
                return orderNotFound(orderId);
            }
        } else {
            Page<Order> orders = orderRepository.findByBuyer_Id(userId, PageRequest.of(0, 1, Sort.by(Sort.Direction.DESC, "createdAt")));
            if (orders.hasContent()) {
                targetOrder = orders.getContent().get(0);
            }
        }

        if (targetOrder == null) {
            return ChatbotMessageResponse.text("Bạn chưa có đơn hàng nào trên CapNong. Hãy dạo quanh cửa hàng và chọn nông sản giải cứu tươi ngon nhé!");
        }

        String statusDesc;
        switch (targetOrder.getStatus()) {
            case PENDING -> statusDesc = "Đang chờ shop xác nhận";
            case CONFIRMED -> statusDesc = "Nhà vườn đã xác nhận đơn hàng";
            case READY -> statusDesc = "Nhà vườn đang đóng gói / chờ tài xế";
            case SHIPPING -> statusDesc = "Tài xế đang trên đường giao hàng 🛵";
            case COMPLETED -> statusDesc = "Đã giao hàng thành công ✅";
            case CANCELLED -> statusDesc = "Đơn hàng đã hủy";
            default -> statusDesc = "Đang xử lý";
        }

        String shopName = targetOrder.getShop() != null
                ? (targetOrder.getShop().getShopName() != null ? targetOrder.getShop().getShopName() : targetOrder.getShop().getFullName())
                : "Nhà vườn đối tác";

        ChatbotMessageResponse.OrderTrackingDto dto = ChatbotMessageResponse.OrderTrackingDto.builder()
                .orderId(targetOrder.getOrderId())
                .orderStatus(targetOrder.getStatus().name())
                .statusDescription(statusDesc)
                .shopName(shopName)
                .totalAmount(targetOrder.getTotalAmount())
                .recipientName(targetOrder.getRecipientName())
                .recipientPhone(targetOrder.getRecipientPhone())
                .shippingAddress(targetOrder.getShippingAddress())
                .shipperName(targetOrder.getShipper() != null ? targetOrder.getShipper().getFullName() : "Đang điều phối tài xế")
                .shipperPhone(targetOrder.getShipper() != null ? targetOrder.getShipper().getPhoneNumber() : null)
                .trackingUrl("/orders?orderId=" + targetOrder.getOrderId())
                .createdAt(targetOrder.getCreatedAt())
                .build();

        return ChatbotMessageResponse.builder()
                .type(AiResponseType.ORDER_TRACKING)
                .message("Thông tin tiến độ đơn hàng #" + targetOrder.getOrderId() + " của bạn:")
                .orderTracking(dto)
                .timestamp(LocalDateTime.now())
                .build();
    }

    @Override
    public ChatbotMessageResponse handleReturnGuidance(Integer userId) {
        ChatbotMessageResponse.ReturnGuidanceDto.ReturnGuidanceDtoBuilder dto = ChatbotMessageResponse.ReturnGuidanceDto.builder()
                .policyTitle("Chính sách trả hàng / hoàn tiền")
                .policyContent(RETURN_POLICY);

        if (userId == null) {
            return returnGuidanceResponse("Bạn đăng nhập để gửi yêu cầu trả hàng cho đơn đã nhận nhé. Chính sách của CapNong như sau:",
                    dto.actionUrl("/login").actionLabel("Đăng nhập").build());
        }

        ChatbotMessageResponse.LatestReturnDto latest = returnRequestRepository
                .findByBuyer_Id(userId, PageRequest.of(0, 1, Sort.by(Sort.Direction.DESC, "createdAt")))
                .stream().findFirst().map(this::toLatestReturn).orElse(null);
        dto.latestReturn(latest);

        Order eligible = orderRepository
                .findByBuyer_Id(userId, PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt")))
                .stream().filter(o -> returnDeadline(o) != null).findFirst().orElse(null);

        String message;
        if (eligible != null) {
            dto.orderId(eligible.getOrderId())
                    .returnDeadline(returnDeadline(eligible))
                    .actionUrl("/orders?orderId=" + eligible.getOrderId())
                    .actionLabel("Yêu cầu trả hàng cho đơn #" + eligible.getOrderId());
            message = "Đơn #" + eligible.getOrderId() + " của bạn vẫn còn trong hạn trả hàng. Bạn mở đơn, chọn sản phẩm lỗi rồi bấm \"Trả hàng / Hoàn tiền\" nhé:";
        } else if (latest != null && latest.isActive()) {
            dto.actionUrl("/orders?tab=REFUND").actionLabel("Xem yêu cầu trả hàng");
            message = "Yêu cầu trả hàng #" + latest.getReturnId() + " của bạn đang được xử lý:";
        } else {
            dto.actionUrl("/orders").actionLabel("Xem đơn hàng");
            message = "Hiện bạn không có đơn nào còn trong hạn trả hàng (24 giờ sau khi nhận). Chính sách của CapNong như sau:";
        }
        return returnGuidanceResponse(message, dto.build());
    }

    private static final String RETURN_POLICY =
            "Bạn gửi yêu cầu trong vòng 24 giờ sau khi nhận hàng, cho từng sản phẩm trong đơn đã giao. "
                    + "Kèm lý do và ảnh sản phẩm lỗi để nhà vườn xem xét. Nếu nhà vườn từ chối, CapNong sẽ xem xét lại. "
                    + "Khi yêu cầu hoàn tất, tiền hoàn được cộng vào Ví CapNong của bạn.";

    private ChatbotMessageResponse returnGuidanceResponse(String message, ChatbotMessageResponse.ReturnGuidanceDto dto) {
        return ChatbotMessageResponse.builder()
                .type(AiResponseType.RETURN_GUIDANCE)
                .message(message)
                .returnGuidance(dto)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Hạn chót gửi yêu cầu trả hàng, cùng điều kiện với ReturnRequestServiceImpl.createReturnRequest.
     * Trả null nếu đơn chưa giao hoặc đã quá hạn.
     */
    static LocalDateTime returnDeadline(Order order) {
        if (order.getStatus() != OrderStatus.DELIVERED && order.getStatus() != OrderStatus.COMPLETED) return null;
        LocalDateTime deadline;
        if (order.getDeliveredAt() != null) {
            deadline = order.getDeliveredAt().plusHours(24);
        } else if (order.getEstimatedDeliveryDate() != null) {
            deadline = order.getEstimatedDeliveryDate().plusDays(3);
        } else {
            return null;
        }
        return LocalDateTime.now().isAfter(deadline) ? null : deadline;
    }

    private ChatbotMessageResponse.LatestReturnDto toLatestReturn(ReturnRequest request) {
        ReturnRequestResponse r = returnRequestMapper.toResponse(request);
        return ChatbotMessageResponse.LatestReturnDto.builder()
                .returnId(r.getReturnId())
                .orderId(r.getOrderId())
                .productName(r.getProductName())
                .status(r.getStatus() != null ? r.getStatus().name() : null)
                .statusLabel(returnStatusLabel(r.getStatus()))
                .refundAmount(r.getRefundAmount())
                .active(ReturnRequestServiceImpl.isReturnActive(r.getStatus()))
                .createdAt(r.getCreatedAt())
                .build();
    }

    static String returnStatusLabel(ReturnStatus status) {
        if (status == null) return "Đang xử lý";
        return switch (status) {
            case PENDING -> "Chờ nhà vườn phản hồi";
            case SHOP_APPROVED, ADMIN_APPROVED, APPROVED -> "Đã được duyệt";
            case DISPUTED -> "CapNong đang xem xét";
            case RETURN_SHIPPING -> "Chờ shipper lấy hàng hoàn";
            case RETURN_PICKED_UP -> "Shipper đã lấy hàng hoàn";
            case RETURN_DELIVERED -> "Hàng hoàn đã về nhà vườn";
            case RETURN_FAILED -> "Lấy hàng hoàn không thành công";
            case COMPLETED -> "Đã hoàn tiền";
            case REJECTED -> "Bị từ chối";
            case CANCELLED -> "Bạn đã hủy yêu cầu";
        };
    }

    @Override
    public ChatbotMessageResponse cancelOrder(Integer orderId, Integer userId) {
        if (userId == null) {
            return ChatbotMessageResponse.builder()
                    .type(AiResponseType.NAVIGATION)
                    .message("Bạn cần đăng nhập để thao tác đơn hàng nhé!")
                    .navigation(ChatbotMessageResponse.NavigationDto.builder().url("/login").label("Đăng nhập").build())
                    .timestamp(LocalDateTime.now())
                    .build();
        }

        Order targetOrder = null;
        if (orderId != null) {
            targetOrder = ownedOrder(orderId, userId).orElse(null);
            if (targetOrder == null) {
                return orderNotFound(orderId);
            }
        } else {
            Page<Order> orders = orderRepository.findByBuyer_Id(userId, PageRequest.of(0, 1, Sort.by(Sort.Direction.DESC, "createdAt")));
            if (orders.hasContent()) {
                targetOrder = orders.getContent().get(0);
            }
        }

        if (targetOrder == null) {
            return ChatbotMessageResponse.text("Bạn chưa có đơn hàng nào để hủy trên CapNong.");
        }

        boolean canCancel = targetOrder.getStatus() == OrderStatus.PENDING
                || targetOrder.getStatus() == OrderStatus.AWAITING_PAYMENT
                || targetOrder.getStatus() == OrderStatus.PAID
                || targetOrder.getStatus() == OrderStatus.CONFIRMED;

        String msg;
        if (canCancel) {
            msg = "Đơn hàng #" + targetOrder.getOrderId() + " hiện ở trạng thái " + targetOrder.getStatus() + ", shop chưa bàn giao cho shipper nên bạn có thể hủy ngay.";
        } else if (targetOrder.getStatus() == OrderStatus.SHIPPING || targetOrder.getStatus() == OrderStatus.PICKED_UP) {
            msg = "Đơn hàng #" + targetOrder.getOrderId() + " đang ở trạng thái " + targetOrder.getStatus() + ", không thể hủy trực tiếp vì tài xế đã lấy hàng và đang trên đường giao.";
        } else if (targetOrder.getStatus() == OrderStatus.CANCELLED) {
            msg = "Đơn hàng #" + targetOrder.getOrderId() + " đã được hủy trước đó.";
        } else {
            msg = "Đơn hàng #" + targetOrder.getOrderId() + " đang ở trạng thái " + targetOrder.getStatus() + ", không thể hủy trực tiếp vì tài xế đã lấy hàng hoặc đơn đã hoàn tất.";
        }

        ChatbotMessageResponse.OrderCancelDto dto = ChatbotMessageResponse.OrderCancelDto.builder()
                .orderId(targetOrder.getOrderId())
                .orderStatus(targetOrder.getStatus().name())
                .canCancel(canCancel)
                .message(msg)
                .refundAmount(targetOrder.getStatus() == OrderStatus.PAID ? targetOrder.getTotalAmount() : null)
                .build();

        return ChatbotMessageResponse.builder()
                .type(AiResponseType.ORDER_CANCEL)
                .message(msg)
                .orderCancel(dto)
                .timestamp(LocalDateTime.now())
                .build();
    }

    private int remainingOf(BuildCombo c) {
        return (c.getTotalQuantity() != null ? c.getTotalQuantity() : 0)
                - (c.getSoldQuantity() != null ? c.getSoldQuantity() : 0);
    }

    private boolean hasStock(BuildCombo c) {
        return remainingOf(c) > 0;
    }

    @Override
    public ChatbotMessageResponse buildBudgetCombo(BigDecimal budget, Integer userId) {
        return buildBudgetCombo(budget, null, userId);
    }

    @Override
    public ChatbotMessageResponse buildBudgetCombo(BigDecimal budget, String userMessage, Integer userId) {
        BigDecimal target = (budget != null && budget.compareTo(BigDecimal.valueOf(10000)) >= 0)
                ? budget
                : BigDecimal.valueOf(100000);

        boolean isComboQuery = userMessage != null && userMessage.toLowerCase().contains("combo");

        if (isComboQuery) {
            List<BuildCombo> allCombos = buildComboRepository.findAll();
            List<BuildCombo> matched = allCombos.stream()
                    .filter(c -> ("ACTIVE".equalsIgnoreCase(c.getStatus()) || c.getStatus() == null))
                    .filter(this::hasStock)
                    .filter(c -> c.getPrice() != null && c.getPrice().compareTo(target) <= 0)
                    .sorted(Comparator.comparing(BuildCombo::getPrice).reversed())
                    .limit(6)
                    .toList();

            // Nếu chưa có combo <= target, lấy các combo giá rẻ nhất hiện có
            if (matched.isEmpty()) {
                matched = allCombos.stream()
                        .filter(c -> ("ACTIVE".equalsIgnoreCase(c.getStatus()) || c.getStatus() == null))
                        .filter(this::hasStock)
                        .filter(c -> c.getPrice() != null)
                        .sorted(Comparator.comparing(BuildCombo::getPrice))
                        .limit(4)
                        .toList();
            }

            List<ChatbotMessageResponse.ItemOptionDto> comboItems = new ArrayList<>();
            for (BuildCombo c : matched) {
                int remaining = remainingOf(c);
                comboItems.add(ChatbotMessageResponse.ItemOptionDto.builder()
                        .itemType(AiItemType.COMBO)
                        .itemId(c.getComboId())
                        .name(c.getComboName())
                        .price(c.getPrice())
                        .salePrice(c.getPrice())
                        .unit("combo")
                        .imageUrl(c.getImageUrl())
                        .availableQuantity(remaining)
                        .description(c.getDescription())
                        .build());
            }

            ChatbotMessageResponse.BudgetComboDto dto = ChatbotMessageResponse.BudgetComboDto.builder()
                    .targetBudget(target)
                    .actualTotal(matched.isEmpty() ? target : matched.get(0).getPrice())
                    .title("Danh sách Combo dưới " + formatCurrency(target))
                    .description("CapNong gợi ý các gói combo rau củ quả đóng gói sẵn tiện lợi (dưới " + formatCurrency(target) + ") cho gia đình bạn. Bạn có thể chọn bất kỳ combo nào bên dưới:")
                    .items(comboItems)
                    .build();

            return ChatbotMessageResponse.builder()
                    .type(AiResponseType.BUDGET_COMBO)
                    .message("CapNong gửi bạn danh sách các combo nông sản đóng gói sẵn dưới " + formatCurrency(target) + " cho gia đình bạn:")
                    .budgetCombo(dto)
                    .timestamp(LocalDateTime.now())
                    .build();
        }

        // Trường hợp câu hỏi về ngân sách rau củ không dính keyword combo (ví dụ: "Tôi có 150k muốn mua rau củ"):
        // Tự động tính toán mâm nông sản sao cho tổng tiền <= ngân sách yêu cầu
        Page<Product> activeProducts = productRepository.findAll(
                ProductSpecification.filterProducts("ACTIVE", null, null, null),
                PageRequest.of(0, 10)
        );
        List<ChatbotMessageResponse.ItemOptionDto> selectedItems = new ArrayList<>();
        BigDecimal currentSum = BigDecimal.ZERO;

        for (Product p : activeProducts.getContent()) {
            if (p.getStockQuantity() != null && p.getStockQuantity() > 0 && p.getSellingPrice() != null) {
                BigDecimal salePrice = salePriceOf(p);
                if (currentSum.add(salePrice).compareTo(target) <= 0) {
                    String img = (p.getImages() != null && !p.getImages().isEmpty()) ? p.getImages().get(0).getImageUrl() : null;
                    selectedItems.add(ChatbotMessageResponse.ItemOptionDto.builder()
                            .itemType(AiItemType.PRODUCT)
                            .itemId(p.getProductId())
                            .name(p.getProductName())
                            .price(p.getSellingPrice())
                            .salePrice(salePrice)
                            .unit(p.getUnit() != null ? p.getUnit() : "kg")
                            .imageUrl(img)
                            .availableQuantity(p.getStockQuantity())
                            .description(p.getDescription())
                            .build());
                    currentSum = currentSum.add(salePrice);
                    if (selectedItems.size() >= 4) break;
                }
            }
        }

        ChatbotMessageResponse.BudgetComboDto dto = ChatbotMessageResponse.BudgetComboDto.builder()
                .targetBudget(target)
                .actualTotal(currentSum)
                .title("Mâm nông sản tiết kiệm " + formatCurrency(target))
                .description("Combo gồm các loại rau củ quả tươi sạch đầy đủ chất xơ, vitamin cân đối cho bữa cơm gia đình:")
                .items(selectedItems)
                .build();

        return ChatbotMessageResponse.builder()
                .type(AiResponseType.BUDGET_COMBO)
                .message("CapNong gợi ý combo rau củ giải cứu phù hợp với ngân sách " + formatCurrency(target) + " của bạn:")
                .budgetCombo(dto)
                .timestamp(LocalDateTime.now())
                .build();
    }

    @Override
    public ChatbotMessageResponse suggestRecipe(String userIngredients, Integer userId) {
        String cleanIng = (userIngredients != null && !userIngredients.isBlank()) ? userIngredients.trim() : "rau củ tươi";
        List<String> allergens = allergensOf(userId);

        ChatbotAiClient.RecipeIdea idea = chatbotAiClient.suggestRecipe(cleanIng, allergens);
        if (idea == null) {
            idea = new ChatbotAiClient.RecipeIdea(
                    "Canh nông sản & món xào đơn giản",
                    "20 phút",
                    "Từ " + cleanIng + ", bạn có thể nấu canh thanh mát hoặc xào tỏi. Bí quyết: phi thơm hành tỏi, xào nhanh trên lửa lớn để giữ màu sắc và độ giòn.",
                    List.of("cà chua", "hành", "nấm"));
        }

        Map<String, ChatbotMessageResponse.ItemOptionDto> extras = new LinkedHashMap<>();
        for (String name : idea.extraIngredients()) {
            if (mentionsAllergen(name, allergens)) continue;
            for (ChatbotMessageResponse.ItemOptionDto item : searchItems(name, AiItemType.PRODUCT)) {
                if (extras.size() < 3 && !mentionsAllergen(item.getName(), allergens)) {
                    extras.putIfAbsent(item.getItemType() + "-" + item.getItemId(), item);
                }
            }
        }

        ChatbotMessageResponse.RecipeSuggestionDto dto = ChatbotMessageResponse.RecipeSuggestionDto.builder()
                .dishName(idea.dishName())
                .cookTime(idea.cookTime())
                .description(idea.description())
                .userIngredients(List.of(cleanIng))
                .missingIngredients(new ArrayList<>(extras.values()))
                .build();

        return ChatbotMessageResponse.builder()
                .type(AiResponseType.RECIPE_SUGGESTION)
                .message("Gợi ý món ngon từ nguyên liệu bạn đang có sẵn:")
                .recipe(dto)
                .timestamp(LocalDateTime.now())
                .build();
    }

    @Override
    public ChatbotMessageResponse getMysteryBoxQa(Integer userId) {
        return getMysteryBoxQa(null, userId);
    }

    @Override
    public ChatbotMessageResponse getMysteryBoxQa(String userMessage, Integer userId) {
        List<MysteryBox> boxes = mysteryBoxRepository.findAll();
        MysteryBox activeBox = boxes.stream()
                .filter(b -> Boolean.TRUE.equals(b.getIsActive()))
                .filter(b -> (b.getTotalQuantity() != null ? b.getTotalQuantity() : 0) > (b.getSoldQuantity() != null ? b.getSoldQuantity() : 0))
                .findFirst()
                .orElse(null);

        ChatbotMessageResponse.ItemOptionDto boxDto = null;
        if (activeBox != null) {
            boxDto = ChatbotMessageResponse.ItemOptionDto.builder()
                    .itemType(AiItemType.MYSTERY_BOX)
                    .itemId(activeBox.getMysteryId())
                    .name(activeBox.getBoxType() != null ? activeBox.getBoxType() : "Hộp bí ẩn nông sản")
                    .imageUrl(activeBox.getImageUrl())
                    .price(activeBox.getPrice())
                    .salePrice(activeBox.getPrice())
                    .unit("hộp")
                    .build();
        }

        List<String> userAllergies = allergensOf(userId);

        String lowerMsg = (userMessage != null) ? userMessage.toLowerCase() : "";
        boolean asksAboutAllergy = lowerMsg.contains("dị ứng") || lowerMsg.contains("nấm")
                || lowerMsg.contains("ngứa") || lowerMsg.contains("đậu phộng")
                || lowerMsg.contains("đậu nành");

        List<String> riskAllergens = new ArrayList<>();
        if (lowerMsg.contains("nấm")) {
            riskAllergens.add("Nấm");
        }
        if (lowerMsg.contains("đậu phộng") || lowerMsg.contains("dị ứng lạc")) {
            riskAllergens.add("Đậu phộng / Lạc");
        }
        if (lowerMsg.contains("đậu nành") || lowerMsg.contains("đậu tương")) {
            riskAllergens.add("Đậu nành");
        }
        if (lowerMsg.contains("hải sản")) {
            riskAllergens.add("Hải sản");
        }

        for (String ua : userAllergies) {
            if (!riskAllergens.contains(ua)) {
                riskAllergens.add(ua);
            }
        }

        boolean isAllergyRisk = asksAboutAllergy || !riskAllergens.isEmpty();
        String riskAllergenStr = String.join(", ", riskAllergens);

        String botReply;
        String note;
        if (isAllergyRisk) {
            botReply = "⚠️ CẢNH BÁO AN TOÀN SỨC KHỎE: Vì Hộp Bí Ẩn (Mystery Box) được nhà vườn đóng ngẫu nhiên từ nông sản giải cứu trong ngày (rất dễ có nấm, các loại đậu hoặc hạt...), CapNong KHUYÊN BẠN KHÔNG NÊN MUA HỘP BÍ ẨN để tránh nguy cơ kích ứng hoặc sốc phản vệ.\n\nĐể đảm bảo an toàn tuyệt đối, bạn nên chọn mua từng loại rau củ đơn lẻ hoặc chọn các combo minh bạch thành phần để tự kiểm soát 100% nguyên liệu an toàn cho bản thân nhé!";
            note = "Phát hiện nguy cơ dị ứng với: " + (!riskAllergenStr.isBlank() ? riskAllergenStr : "Nông sản ngẫu nhiên") + ". Do thành phần trong hộp bí ẩn không cố định, nguy cơ nhiễm chéo dị nguyên là rất cao. Vui lòng chọn mua nông sản đơn lẻ.";
        } else {
            botReply = "Thông tin chi tiết về Hộp bí ẩn (Mystery Box) hôm nay:";
            note = userAllergies.isEmpty()
                    ? "Thành phần mỗi hộp do nhà vườn đóng gói nên có thể thay đổi theo từng đợt."
                    : "Hồ sơ của bạn có lưu dị ứng với: " + String.join(", ", userAllergies) + ". Bạn nhớ ghi chú cho nhà vườn khi đặt mua nhé!";
        }

        ChatbotMessageResponse.MysteryBoxQaDto dto = ChatbotMessageResponse.MysteryBoxQaDto.builder()
                .seasonalDescription(activeBox != null && activeBox.getDescription() != null && !activeBox.getDescription().isBlank()
                        ? activeBox.getDescription()
                        : "Hộp bí ẩn gồm các loại nông sản do nhà vườn chọn và đóng gói sẵn.")
                .userAllergies(userAllergies)
                .boxItem(boxDto)
                .note(note)
                .isAllergyRisk(isAllergyRisk)
                .riskAllergen(!riskAllergenStr.isBlank() ? riskAllergenStr : "Nấm / Các loại hạt")
                .safetyRecommendation("Khuyên bạn nên chọn mua từng loại rau củ đơn lẻ hoặc combo rõ ràng thành phần để đảm bảo an toàn sức khỏe.")
                .build();

        return ChatbotMessageResponse.builder()
                .type(AiResponseType.MYSTERY_BOX_QA)
                .message(botReply)
                .mysteryBoxQa(dto)
                .timestamp(LocalDateTime.now())
                .build();
    }

    @Override
    public ChatbotMessageResponse getAvailableVouchers(String filterKeyword, String rawMessage, Integer userId) {
        String query = (rawMessage != null ? rawMessage.toLowerCase() : "") + " " + (filterKeyword != null ? filterKeyword.toLowerCase() : "");
        boolean isFreeshipQuery = query.contains("freeship") || query.contains("miễn phí ship")
                || query.contains("free ship") || query.contains("phí vận chuyển") || query.contains("tiền ship") || query.contains("mã ship");

        // Collect claimed voucher IDs of current user
        Set<Integer> collectedVoucherIds = (userId != null)
                ? voucherUserRepository.findByUser_Id(userId).stream()
                        .map(vu -> vu.getVoucher().getVoucherId())
                        .collect(Collectors.toSet())
                : Collections.emptySet();

        List<Voucher> allVouchers = voucherRepository.findByActiveTrueAndAutoCreatedReasonIsNull();
        LocalDateTime now = LocalDateTime.now();

        // Filter active & non-expired
        List<Voucher> validVouchers = allVouchers.stream()
                .filter(v -> v.getExpiryDate() == null || v.getExpiryDate().isAfter(now))
                .collect(Collectors.toList());

        List<Voucher> targetList = new ArrayList<>();
        String headerMessage;

        if (isFreeshipQuery) {
            // Lọc đúng các mã FREESHIP
            targetList = validVouchers.stream()
                    .filter(v -> v.getVoucherType() == VoucherType.FREE_SHIPPING
                            || (v.getVoucherCode() != null && (v.getVoucherCode().toUpperCase().contains("SHIP") || v.getVoucherCode().toUpperCase().contains("FREESHIP"))))
                    .collect(Collectors.toList());

            if (targetList.isEmpty()) {
                headerMessage = "Hiện tại hệ thống CapNong chưa có mã miễn phí vận chuyển (Freeship) công khai hôm nay. Bạn có thể lưu các mã giảm giá mua hàng hấp dẫn khác dưới đây nhé:";
                // Trả về các mã giảm giá toàn sàn khác để khách vẫn có mã ưu đãi
                targetList = validVouchers.stream()
                        .limit(4)
                        .collect(Collectors.toList());
            } else {
                headerMessage = "Danh sách mã miễn phí vận chuyển (Freeship) CapNong hôm nay:";
            }
        } else {
            // Kiểm tra xem khách có đang hỏi về một Shop / Nhà vườn cụ thể không
            String targetShopName = null;
            Integer targetShopId = null;

            for (Voucher v : validVouchers) {
                if (v.getShop() != null) {
                    String shopName = v.getShop().getShopName();
                    if (shopName != null && !shopName.isBlank()) {
                        String sLower = shopName.toLowerCase();
                        if (query.contains(sLower) || (filterKeyword != null && !filterKeyword.equalsIgnoreCase("FREESHIP") && sLower.contains(filterKeyword.toLowerCase()))) {
                            targetShopName = shopName;
                            targetShopId = v.getShop().getId();
                            break;
                        }
                    }
                }
            }

            if (targetShopId != null) {
                final Integer sId = targetShopId;
                targetList = validVouchers.stream()
                        .filter(v -> v.getShop() != null && v.getShop().getId() == sId)
                        .collect(Collectors.toList());
                headerMessage = "Danh sách mã giảm giá của nhà vườn " + targetShopName + " hôm nay:";
            } else {
                headerMessage = "Danh sách mã giảm giá và ưu đãi CapNong đang áp dụng hôm nay:";
                targetList = validVouchers.stream().limit(6).collect(Collectors.toList());
            }
        }

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        List<ChatbotMessageResponse.VoucherItemDto> list = new ArrayList<>();

        for (Voucher v : targetList) {
            boolean isCollected = collectedVoucherIds.contains(v.getVoucherId());
            boolean isOutOfStock = v.getClaimLimit() != null && (v.getClaimedCount() != null ? v.getClaimedCount() : 0) >= v.getClaimLimit();

            String desc;
            if (v.getVoucherType() == VoucherType.FREE_SHIPPING || (v.getVoucherCode() != null && v.getVoucherCode().toUpperCase().contains("SHIP"))) {
                desc = (v.getDiscountValue() != null && v.getDiscountValue().compareTo(BigDecimal.ZERO) > 0)
                        ? "Miễn phí vận chuyển tối đa " + formatCurrency(v.getDiscountValue())
                        : "Miễn phí vận chuyển toàn quốc";
            } else if (v.getVoucherType() != null && v.getVoucherType().name().contains("PERCENT")) {
                desc = "Giảm " + v.getDiscountValue() + "% tối đa " + formatCurrency(v.getMaxDiscount());
            } else {
                desc = "Giảm trực tiếp " + formatCurrency(v.getDiscountValue());
            }

            String sName = (v.getShop() != null && v.getShop().getShopName() != null)
                    ? v.getShop().getShopName()
                    : ((v.getShop() != null && v.getShop().getFullName() != null) ? v.getShop().getFullName() : "CapNong Platform");

            list.add(ChatbotMessageResponse.VoucherItemDto.builder()
                    .voucherId(v.getVoucherId())
                    .code(v.getVoucherCode())
                    .title(desc)
                    .discountValue(v.getDiscountValue())
                    .minOrderValue(v.getMinOrderValue())
                    .maxDiscount(v.getMaxDiscount())
                    .expiryDate(v.getExpiryDate() != null ? v.getExpiryDate().format(dtf) : "Không thời hạn")
                    .voucherType(v.getVoucherType() != null ? v.getVoucherType().name() : "DISCOUNT")
                    .voucherScope(v.getVoucherScope() != null ? v.getVoucherScope().name() : "PLATFORM")
                    .shopId(v.getShop() != null ? v.getShop().getId() : null)
                    .shopName(sName)
                    .isCollected(isCollected)
                    .isOutOfStock(isOutOfStock)
                    .build());
        }

        return ChatbotMessageResponse.builder()
                .type(AiResponseType.VOUCHER_LIST)
                .message(headerMessage)
                .vouchers(list)
                .timestamp(LocalDateTime.now())
                .build();
    }

    @Override
    public ChatbotMessageResponse getShippingTips(Integer userId) {
        CartResponse cart = (userId != null) ? cartService.getMyCart() : null;
        int shopCount = (cart != null && cart.getShopCarts() != null) ? cart.getShopCarts().size() : 0;
        List<String> names = (cart != null && cart.getShopCarts() != null)
                ? cart.getShopCarts().stream().map(ShopCartResponse::getShopName).collect(Collectors.toList())
                : Collections.emptyList();

        boolean multi = shopCount > 1;
        String tip;
        if (multi) {
            tip = "Giỏ hàng của bạn đang có sản phẩm từ " + shopCount + " nhà vườn khác nhau (" + String.join(", ", names) + "). Do nông sản được thu hoạch trực tiếp tại từng vườn trồng nhằm giữ trọn độ tươi mới, mỗi nhà vườn sẽ phát sinh một chuyến xe giao riêng biệt. Mẹo: Bạn nên gom mua các món từ cùng một nhà vườn để chỉ chịu 1 lần tiền ship và tiết kiệm chi phí hơn nhé!";
        } else if (shopCount == 1) {
            String shopName = names.isEmpty() ? "nhà vườn" : names.get(0);
            tip = "Giỏ hàng của bạn hiện đã gom chung từ cùng một nhà vườn (" + shopName + "), giúp tối ưu chỉ với 1 chuyến giao hàng duy nhất! Mẹo: Bạn có thể chọn thêm nông sản cùng vườn để áp dụng thêm mã Freeship và tiết kiệm chi phí vận chuyển tối đa nhé.";
        } else {
            tip = "Phí giao hàng tại CapNong được tính theo khoảng cách thực tế từ nhà vườn tới địa chỉ nhận của bạn. Để tiết kiệm chi phí hơn, bạn nên ưu tiên gom các món cần mua từ cùng một nhà vườn gần khu vực của mình nhất, hoặc sưu tầm thêm các mã Freeship trong Kho Voucher nhé!";
        }

        ChatbotMessageResponse.ShippingTipDto dto = ChatbotMessageResponse.ShippingTipDto.builder()
                .hasMultiShops(multi)
                .shopCount(shopCount)
                .shopNames(names)
                .tipMessage(tip)
                .build();

        return ChatbotMessageResponse.builder()
                .type(AiResponseType.SHIPPING_TIPS)
                .message("Mẹo tối ưu phí giao hàng thông minh:")
                .shippingTips(dto)
                .timestamp(LocalDateTime.now())
                .build();
    }

    @Override
    public ChatbotMessageResponse getPreservationTips(String query) {
        List<ChatbotMessageResponse.PreservationTipDto> tips = List.of(
                ChatbotMessageResponse.PreservationTipDto.builder()
                        .category("Rau ăn lá (Xà lách, rau muống, cải)")
                        .storageType("Ngăn mát tủ lạnh (4-8°C)")
                        .tips("Không rửa nước trước khi cất. Nhặt bỏ lá dập úa, bọc khăn giấy ẩm hoặc giấy báo rồi cho vào túi zip có lỗ thoáng. Giữ giòn ngọt 5-7 ngày.")
                        .build(),
                ChatbotMessageResponse.PreservationTipDto.builder()
                        .category("Củ quả (Khoai tây, cà rốt, bí đỏ)")
                        .storageType("Nơi thoáng mát nhiệt độ phòng")
                        .tips("Bảo quản rổ thoáng khí nơi khô ráo, tránh ánh sáng mặt trời. Không để chung khoai tây với hành tây để tránh mọc mầm.")
                        .build(),
                ChatbotMessageResponse.PreservationTipDto.builder()
                        .category("Cà chua & Trái cây chín")
                        .storageType("Nhiệt độ phòng (khi chưa chín)")
                        .tips("Để cuống hướng lên trên ở nhiệt độ phòng để giữ trọn vị ngọt umami. Chỉ cho vào tủ lạnh khi quả đã chín mềm để tránh bị xốp bột.")
                        .build(),
                ChatbotMessageResponse.PreservationTipDto.builder()
                        .category("Nấm tươi & Rau gia vị")
                        .storageType("Túi giấy ngăn mát")
                        .tips("Cho vào túi giấy xi măng để hút ẩm, không bọc kín nilon đọng nước sẽ làm nấm nhớt.")
                        .build()
        );

        return ChatbotMessageResponse.builder()
                .type(AiResponseType.PRESERVATION_TIPS)
                .message("Cẩm nang giữ rau củ tươi giòn từ chuyên gia CapNong:")
                .preservationTips(tips)
                .timestamp(LocalDateTime.now())
                .build();
    }

    @Override
    public ChatbotMessageResponse handleHumanHandover() {
        ChatbotMessageResponse.HumanHandoverDto dto = ChatbotMessageResponse.HumanHandoverDto.builder()
                .hotline(supportProperties.getHotline())
                .zaloUrl(supportProperties.getZaloUrl())
                .email(supportProperties.getEmail())
                .triggerP2pChat(true)
                .note("Bạn có thể nhắn tin trực tiếp với chủ nhà vườn trong mục Tin nhắn.")
                .build();

        return ChatbotMessageResponse.builder()
                .type(AiResponseType.HUMAN_HANDOVER)
                .message("Bạn có thể liên hệ qua các kênh sau:")
                .humanHandover(dto)
                .timestamp(LocalDateTime.now())
                .build();
    }

    @Override
    public ChatbotMessageResponse getRoleGuidance(Integer userId) {
        return getRoleGuidance(null, userId);
    }

    @Override
    public ChatbotMessageResponse getRoleGuidance(String userMessage, Integer userId) {
        User user = (userId != null) ? userRepository.findById(userId).orElse(null) : null;
        String userRole = (user != null && user.getRole() != null) ? user.getRole().getName() : "GUEST";
        boolean isShopRole = "SHOP".equalsIgnoreCase(userRole);
        boolean isShipperRole = "SHIPPER".equalsIgnoreCase(userRole);

        List<ChatbotMessageResponse.NavigationDto> links = new ArrayList<>();
        String title;
        String desc;
        String targetRole = isShopRole ? "SHOP" : (isShipperRole ? "SHIPPER" : "GUEST");
        String lowerMsg = (userMessage != null) ? userMessage.toLowerCase() : "";

        // =========================================================================
        // 1. NHÓM TÍNH NĂNG NHÀ VƯỜN (FARMER / SHOP PORTAL)
        // =========================================================================

        // 1.1. Viết bài / Đăng tin tức / Blog / Góc nhà vườn
        if (lowerMsg.contains("viết bài") || lowerMsg.contains("đăng tin tức") || lowerMsg.contains("tin tức")
                || lowerMsg.contains("bài viết") || lowerMsg.contains("blog") || lowerMsg.contains("câu chuyện") || lowerMsg.contains("góc nhà vườn")) {
            targetRole = "SHOP";
            title = "Hướng Dẫn Đăng Tin Tức & Góc Nhà Vườn";
            desc = "1. Truy cập tab Tin Tức & Blog trên Kênh Nhà Vườn.\n"
                    + "2. Nhấn nút 'Viết bài mới', chọn chuyên mục nông sản và tải ảnh chụp thực tế tại vườn.\n"
                    + "3. Xuất bản bài viết để chia sẻ quy trình canh tác sạch, câu chuyện nông sản và tăng uy tín thương hiệu.";
            links.add(ChatbotMessageResponse.NavigationDto.builder().url("/shop?tab=blogs").label("Soạn & Đăng Tin Tức Nhà Vườn").build());
            links.add(ChatbotMessageResponse.NavigationDto.builder().url("/shop?tab=products").label("Quản lý sản phẩm đang bán").build());
            links.add(ChatbotMessageResponse.NavigationDto.builder().url("/shop?tab=overview").label("Về Kênh Nhà Vườn").build());
        }
        // 1.2. Tạo & Phát hành Voucher / Mã giảm giá shop
        else if (lowerMsg.contains("tạo voucher") || lowerMsg.contains("voucher shop") || lowerMsg.contains("mã giảm giá shop")
                || lowerMsg.contains("phát hành voucher") || lowerMsg.contains("khuyến mãi shop") || lowerMsg.contains("tạo mã")
                || (isShopRole && (lowerMsg.contains("voucher") || lowerMsg.contains("mã giảm giá")))) {
            targetRole = "SHOP";
            title = "Hướng Dẫn Tạo & Phát Hành Voucher Cho Shop";
            desc = "1. Mở tab Mã Giảm Giá trong Kênh Quản Trị Nhà Vườn.\n"
                    + "2. Nhấn 'Tạo mã voucher mới', thiết lập mức giảm (tiền mặt hoặc %), giá trị đơn tối thiểu và số lượng phát hành.\n"
                    + "3. Kích hoạt voucher để khách mua có thể thu thập và áp dụng ngay khi đặt nông sản của vườn.";
            links.add(ChatbotMessageResponse.NavigationDto.builder().url("/shop?tab=vouchers").label("Quản Lý & Tạo Mã Giảm Giá Shop").build());
            links.add(ChatbotMessageResponse.NavigationDto.builder().url("/shop?tab=products").label("Xem danh sách nông sản").build());
            links.add(ChatbotMessageResponse.NavigationDto.builder().url("/shop?tab=orders").label("Xem đơn hàng áp voucher").build());
        }
        // 1.3. Đăng bán nông sản mới / Thêm sản phẩm
        else if (lowerMsg.contains("đăng bán") || lowerMsg.contains("thêm sản phẩm") || lowerMsg.contains("thêm nông sản")
                || lowerMsg.contains("đăng nông sản") || lowerMsg.contains("bán nông sản") || lowerMsg.contains("bán rau")
                || lowerMsg.contains("bán củ") || lowerMsg.contains("thêm mặt hàng")) {
            targetRole = "SHOP";
            title = "Hướng Dẫn Đăng Bán Nông Sản Mới";
            desc = "1. Vào mục Sản Phẩm & Nông Sản trong Kênh Nhà Vườn.\n"
                    + "2. Nhấn nút '+ Thêm sản phẩm' để mở biểu mẫu đăng tải nông sản mới.\n"
                    + "3. Điền tên nông sản, danh mục, giá bán, số lượng thu hoạch và chứng chỉ VietGAP/Hữu cơ rồi lưu lại.";
            links.add(ChatbotMessageResponse.NavigationDto.builder().url("/shop?tab=products&action=add").label("Đăng Bán Nông Sản Mới Ngay").build());
            links.add(ChatbotMessageResponse.NavigationDto.builder().url("/shop?tab=products").label("Danh sách nông sản đang bán").build());
            links.add(ChatbotMessageResponse.NavigationDto.builder().url("/shop?tab=orders").label("Đơn hàng mới").build());
        }
        // 1.4. Công cụ Túi mù giải cứu / Blind Box
        else if (lowerMsg.contains("túi mù") || lowerMsg.contains("công cụ túi mù") || lowerMsg.contains("tạo túi mù")
                || lowerMsg.contains("blind box") || lowerMsg.contains("hộp bí mật shop") || (isShopRole && lowerMsg.contains("hộp bí ẩn"))) {
            targetRole = "SHOP";
            title = "Công Cụ Tạo Túi Mù Nông Sản Giải Cứu";
            desc = "1. Vào tab Sản phẩm và nhấn công cụ 'Túi Mù Nông Sản'.\n"
                    + "2. Gom các loại rau củ xấu mã hoặc cận ngày cần giải cứu vào túi mù với mức giá ưu đãi hấp dẫn.\n"
                    + "3. Đặt số lượng túi mù mở bán trong ngày để xả kho nhanh chóng và giảm lãng phí tài nguyên.";
            links.add(ChatbotMessageResponse.NavigationDto.builder().url("/shop?tab=products&action=blindbox").label("Mở Công Cụ Tạo Túi Mù").build());
            links.add(ChatbotMessageResponse.NavigationDto.builder().url("/shop?tab=products").label("Quản lý sản phẩm").build());
            links.add(ChatbotMessageResponse.NavigationDto.builder().url("/shop?tab=overview").label("Doanh thu tổng").build());
        }
        // 1.5. Công cụ Ghép Combo nông sản bữa ăn
        else if (lowerMsg.contains("ghép combo") || lowerMsg.contains("tạo combo") || lowerMsg.contains("công cụ combo")
                || lowerMsg.contains("bán combo") || (isShopRole && lowerMsg.contains("combo"))) {
            targetRole = "SHOP";
            title = "Công Cụ Ghép Combo Nông Sản Bữa Ăn";
            desc = "1. Mở tab Sản phẩm và chọn tính năng 'Ghép Combo Bữa Ăn'.\n"
                    + "2. Chọn 3-5 loại nông sản phối hợp (ví dụ: combo canh chua, combo kho quẹt, combo lẩu).\n"
                    + "3. Định giá combo tiết kiệm hơn mua lẻ để kích thích khách hàng mua trọn gói nhiều món cùng lúc.";
            links.add(ChatbotMessageResponse.NavigationDto.builder().url("/shop?tab=products&action=combo").label("Mở Trình Ghép Combo").build());
            links.add(ChatbotMessageResponse.NavigationDto.builder().url("/shop?tab=products").label("Quản lý sản phẩm").build());
            links.add(ChatbotMessageResponse.NavigationDto.builder().url("/shop?tab=orders").label("Xem đơn hàng").build());
        }
        // 1.6. Quản lý danh sách sản phẩm & tồn kho
        else if (lowerMsg.contains("danh sách sản phẩm") || lowerMsg.contains("tồn kho") || lowerMsg.contains("quản lý sản phẩm")
                || lowerMsg.contains("sửa giá") || lowerMsg.contains("hết hàng") || (isShopRole && lowerMsg.contains("sản phẩm"))) {
            targetRole = "SHOP";
            title = "Quản Lý Danh Sách & Tồn Kho Nông Sản";
            desc = "1. Truy cập tab Sản Phẩm để xem toàn bộ danh sách mặt hàng đang mở bán.\n"
                    + "2. Dễ dàng bật/tắt trạng thái kinh doanh, cập nhật giá thu hoạch hoặc bổ sung số lượng tồn kho.\n"
                    + "3. Theo dõi số lượng đã bán thực tế để có kế hoạch thu hoạch phù hợp.";
            links.add(ChatbotMessageResponse.NavigationDto.builder().url("/shop?tab=products").label("Quản Lý Danh Sách Sản Phẩm").build());
            links.add(ChatbotMessageResponse.NavigationDto.builder().url("/shop?tab=products&action=add").label("Thêm sản phẩm mới").build());
            links.add(ChatbotMessageResponse.NavigationDto.builder().url("/shop?tab=orders").label("Xem đơn hàng").build());
        }
        // 1.7. Xử lý đơn hàng & chuẩn bị hàng
        else if (lowerMsg.contains("xử lý đơn") || lowerMsg.contains("chuẩn bị hàng") || lowerMsg.contains("đóng gói")
                || lowerMsg.contains("giao shipper") || lowerMsg.contains("đơn hàng của shop") || lowerMsg.contains("đơn hàng nhà vườn")
                || (isShopRole && (lowerMsg.contains("đơn hàng") || lowerMsg.contains("chuẩn bị đơn")))) {
            targetRole = "SHOP";
            title = "Quản Lý & Chuẩn Bị Đơn Hàng Nhà Vườn";
            desc = "1. Mở tab Đơn Hàng để xem danh sách các đơn mới đang chờ chuẩn bị.\n"
                    + "2. Thu hoạch, đóng thùng giữ lạnh và bấm 'Xác nhận sẵn sàng giao'.\n"
                    + "3. Bàn giao thùng hàng cho Shipper khi tài xế đến lấy tại vườn và đối soát mã đơn.";
            links.add(ChatbotMessageResponse.NavigationDto.builder().url("/shop?tab=orders").label("Xem & Xử Lý Đơn Hàng").build());
            links.add(ChatbotMessageResponse.NavigationDto.builder().url("/shop?tab=wallet").label("Ví doanh thu").build());
            links.add(ChatbotMessageResponse.NavigationDto.builder().url("/shop?tab=disputes").label("Khiếu nại khách hàng").build());
        }
        // 1.8. Ví doanh thu & Rút tiền về ngân hàng
        else if (lowerMsg.contains("rút tiền doanh thu") || lowerMsg.contains("ví shop") || lowerMsg.contains("ví nhà vườn")
                || lowerMsg.contains("tiền bán hàng") || lowerMsg.contains("doanh thu nhà vườn")
                || (isShopRole && (lowerMsg.contains("rút tiền") || lowerMsg.contains("ví") || lowerMsg.contains("doanh thu")))) {
            targetRole = "SHOP";
            title = "Ví Doanh Thu & Rút Tiền Về Ngân Hàng";
            desc = "1. Truy cập tab Ví Tiền & Doanh Thu trong Kênh Nhà Vườn.\n"
                    + "2. Kiểm tra số dư khả dụng từ các đơn hàng đã giao thành công.\n"
                    + "3. Nhấn 'Yêu cầu rút tiền', nhập số tiền và tài khoản ngân hàng thụ hưởng để nhận tiền trong 24h.";
            links.add(ChatbotMessageResponse.NavigationDto.builder().url("/shop?tab=wallet&action=withdraw").label("Mở Form Yêu Cầu Rút Tiền").build());
            links.add(ChatbotMessageResponse.NavigationDto.builder().url("/shop?tab=wallet").label("Lịch sử giao dịch ví").build());
            links.add(ChatbotMessageResponse.NavigationDto.builder().url("/shop?tab=overview").label("Báo cáo doanh thu").build());
        }
        // 1.9. Khiếu nại & Yêu cầu hoàn tiền
        else if (lowerMsg.contains("khiếu nại shop") || lowerMsg.contains("tranh chấp shop")
                || (isShopRole && (lowerMsg.contains("khiếu nại") || lowerMsg.contains("đổi trả") || lowerMsg.contains("hoàn tiền") || lowerMsg.contains("tranh chấp")))) {
            targetRole = "SHOP";
            title = "Xử Lý Khiếu Nại & Yêu Cầu Hoàn Tiền";
            desc = "1. Vào tab Khiếu Nại để xem các yêu cầu đổi trả hoặc bồi thường từ khách mua.\n"
                    + "2. Kiểm tra hình ảnh/video bằng chứng rau củ bị hư dập trong quá trình vận chuyển.\n"
                    + "3. Phản hồi chấp thuận hoàn tiền hoặc gửi ý kiến giải trình cho ban quản trị sàn.";
            links.add(ChatbotMessageResponse.NavigationDto.builder().url("/shop?tab=disputes").label("Xem Danh Sách Khiếu Nại").build());
            links.add(ChatbotMessageResponse.NavigationDto.builder().url("/shop?tab=messages").label("Nhắn tin trao đổi với khách").build());
            links.add(ChatbotMessageResponse.NavigationDto.builder().url("/shop?tab=orders").label("Xem chi tiết đơn hàng").build());
        }
        // 1.10. Nhắn tin trực tiếp với khách mua
        else if (lowerMsg.contains("chat với khách") || lowerMsg.contains("tin nhắn khách") || lowerMsg.contains("hộp thư shop")
                || (isShopRole && (lowerMsg.contains("tin nhắn") || lowerMsg.contains("chat") || lowerMsg.contains("trò chuyện")))) {
            targetRole = "SHOP";
            title = "Hộp Thư Trực Tiếp Với Khách Hàng";
            desc = "1. Truy cập tab Tin Nhắn để kết nối trực tiếp với người mua nông sản.\n"
                    + "2. Tư vấn độ tươi, độ chín của hoa quả hoặc thông báo thời gian cắt trổ rau tại vườn.\n"
                    + "3. Giải đáp nhanh các câu hỏi về đơn hàng để tăng tỷ lệ đánh giá 5 sao cho shop.";
            links.add(ChatbotMessageResponse.NavigationDto.builder().url("/shop?tab=messages").label("Mở Khung Chat Với Khách").build());
            links.add(ChatbotMessageResponse.NavigationDto.builder().url("/shop?tab=orders").label("Xem danh sách đơn").build());
            links.add(ChatbotMessageResponse.NavigationDto.builder().url("/shop?tab=reviews").label("Xem đánh giá shop").build());
        }
        // 1.11. Đánh giá & Phản hồi khách hàng
        else if (lowerMsg.contains("đánh giá shop") || (isShopRole && (lowerMsg.contains("đánh giá") || lowerMsg.contains("review") || lowerMsg.contains("nhận xét") || lowerMsg.contains("số sao")))) {
            targetRole = "SHOP";
            title = "Đánh Giá & Nhận Xét Từ Khách Hàng";
            desc = "1. Chọn tab Đánh Giá để xem tất cả phản hồi và số sao từ người mua.\n"
                    + "2. Trả lời bình luận của khách hàng để thể hiện sự tận tâm và xây dựng uy tín nhà vườn.\n"
                    + "3. Tiếp thu ý kiến đóng góp để hoàn thiện quy trình đóng gói và chất lượng thu hoạch.";
            links.add(ChatbotMessageResponse.NavigationDto.builder().url("/shop?tab=reviews").label("Xem Đánh Giá Của Khách").build());
            links.add(ChatbotMessageResponse.NavigationDto.builder().url("/shop?tab=products").label("Quản lý sản phẩm").build());
            links.add(ChatbotMessageResponse.NavigationDto.builder().url("/shop?tab=overview").label("Dashboard tổng quan").build());
        }
        // 1.12. Hồ sơ nhà vườn & Tọa độ lấy hàng
        else if (lowerMsg.contains("tọa độ vườn") || lowerMsg.contains("vị trí lấy hàng") || lowerMsg.contains("đổi địa chỉ vườn")
                || (isShopRole && (lowerMsg.contains("địa chỉ vườn") || lowerMsg.contains("hồ sơ") || lowerMsg.contains("tọa độ") || lowerMsg.contains("gps")))) {
            targetRole = "SHOP";
            title = "Cài Đặt Hồ Sơ Nhà Vườn & Vị Trí Lấy Hàng";
            desc = "1. Truy cập tab Hồ Sơ Shop để cập nhật tên nhà vườn, câu chuyện thương hiệu và ảnh bìa.\n"
                    + "2. Xác định chính xác địa chỉ và tọa độ GPS của vườn trên bản đồ để Shipper đến lấy hàng thuận lợi.\n"
                    + "3. Cập nhật số điện thoại liên hệ trực tiếp của chủ vườn.";
            links.add(ChatbotMessageResponse.NavigationDto.builder().url("/shop?tab=profile").label("Cài Đặt Hồ Sơ & Tọa Độ Vườn").build());
            links.add(ChatbotMessageResponse.NavigationDto.builder().url("/shop?tab=overview").label("Dashboard tổng quan").build());
            links.add(ChatbotMessageResponse.NavigationDto.builder().url("/shop?tab=products").label("Quản lý sản phẩm").build());
        }

        // =========================================================================
        // 2. NHÓM TÍNH NĂNG TÀI XẾ (SHIPPER DASHBOARD)
        // =========================================================================

        // 2.1. Bật trực tuyến & Nhận đơn hàng mới
        else if (lowerMsg.contains("bật trực tuyến") || lowerMsg.contains("bật nhận đơn") || lowerMsg.contains("nhận chuyến")
                || lowerMsg.contains("ngoại tuyến") || lowerMsg.contains("tìm chuyến") || lowerMsg.contains("bật app")
                || (isShipperRole && (lowerMsg.contains("đơn mới") || lowerMsg.contains("trực tuyến") || lowerMsg.contains("nhận đơn")))) {
            targetRole = "SHIPPER";
            title = "Bật Trực Tuyến & Nhận Đơn Hàng Mới";
            desc = "1. Truy cập Cổng Tài Xế và gạt nút trạng thái sang 'Trực tuyến'.\n"
                    + "2. Hệ thống sẽ tự động quét và hiển thị các đơn hàng mới quanh bán kính vị trí của bạn.\n"
                    + "3. Bấm 'Nhận chuyến' để bắt đầu lộ trình đến nhà vườn lấy hàng.";
            links.add(ChatbotMessageResponse.NavigationDto.builder().url("/shipper?tab=new").label("Mở Danh Sách Đơn Mới & Bật Online").build());
            links.add(ChatbotMessageResponse.NavigationDto.builder().url("/shipper?tab=delivering").label("Xem đơn đang giao").build());
            links.add(ChatbotMessageResponse.NavigationDto.builder().url("/shipper?tab=profile").label("Hồ sơ & Tiền COD").build());
        }
        // 2.2. Quy trình giao nhận & Xác nhận lấy hàng
        else if (lowerMsg.contains("đơn đang giao") || lowerMsg.contains("xác nhận lấy hàng") || lowerMsg.contains("chụp ảnh giao hàng")
                || lowerMsg.contains("giao thành công") || lowerMsg.contains("giao thất bại")
                || (isShipperRole && (lowerMsg.contains("đang giao") || lowerMsg.contains("lấy hàng") || lowerMsg.contains("giao hàng")))) {
            targetRole = "SHIPPER";
            title = "Quy Trình Giao Hàng & Xác Nhận 2 Chặng";
            desc = "1. Đến nhà vườn, kiểm tra thùng hàng nông sản và bấm 'Xác nhận đã lấy hàng'.\n"
                    + "2. Di chuyển đến địa chỉ người nhận và liên hệ khách qua số điện thoại trên app.\n"
                    + "3. Thu tiền mặt (nếu đơn COD), bấm 'Hoàn thành giao hàng' và chụp ảnh xác nhận.";
            links.add(ChatbotMessageResponse.NavigationDto.builder().url("/shipper?tab=delivering").label("Xem Đơn Đang Giao & Xác Nhận").build());
            links.add(ChatbotMessageResponse.NavigationDto.builder().url("/shipper?tab=new").label("Tìm đơn hàng mới").build());
            links.add(ChatbotMessageResponse.NavigationDto.builder().url("/shipper?tab=profile").label("Kiểm tra tiền thu COD").build());
        }
        // 2.3. Bản đồ chỉ đường dẫn live
        else if (lowerMsg.contains("bản đồ tài xế") || lowerMsg.contains("lộ trình giao") || lowerMsg.contains("chỉ đường shipper")
                || (isShipperRole && (lowerMsg.contains("bản đồ") || lowerMsg.contains("lộ trình") || lowerMsg.contains("chỉ đường") || lowerMsg.contains("map")))) {
            targetRole = "SHIPPER";
            title = "Bản Đồ Lộ Trình Giao Hàng Trực Tiếp";
            desc = "1. Chọn tab 'Đang Giao' trên Cổng Tài Xế.\n"
                    + "2. Bấm vào biểu tượng Bản Đồ hoặc nút chỉ đường trên đơn hàng cần giao.\n"
                    + "3. Ứng dụng tích hợp lộ trình dẫn đường tối ưu nhất tránh kẹt xe và đo khoảng cách chính xác.";
            links.add(ChatbotMessageResponse.NavigationDto.builder().url("/shipper?tab=delivering").label("Mở Bản Đồ Chỉ Đường Trực Tiếp").build());
            links.add(ChatbotMessageResponse.NavigationDto.builder().url("/shipper?tab=new").label("Đơn hàng mới").build());
            links.add(ChatbotMessageResponse.NavigationDto.builder().url("/shipper?tab=profile").label("Hồ sơ tài xế").build());
        }
        // 2.4. Tiền COD, Ví ký quỹ & Rút tiền tài xế
        else if (lowerMsg.contains("tiền cod") || lowerMsg.contains("rút cod") || lowerMsg.contains("rút tiền cod")
                || lowerMsg.contains("ký quỹ") || lowerMsg.contains("ứng tiền")
                || (isShipperRole && (lowerMsg.contains("rút tiền") || lowerMsg.contains("ví") || lowerMsg.contains("tiền mặt")))) {
            targetRole = "SHIPPER";
            title = "Quản Lý Tiền Thu COD & Ví Ký Quỹ Tài Xế";
            desc = "1. Truy cập tab 'Hồ sơ & COD' trên Cổng Tài Xế.\n"
                    + "2. Theo dõi tổng số tiền mặt COD đã thu từ khách mua trong ngày.\n"
                    + "3. Quản lý hạn mức công nợ và rút tiền hoa hồng giao nhận về tài khoản ngân hàng.";
            links.add(ChatbotMessageResponse.NavigationDto.builder().url("/shipper?tab=profile").label("Quản Lý COD & Ví Tài Xế").build());
            links.add(ChatbotMessageResponse.NavigationDto.builder().url("/shipper?tab=delivering").label("Đơn đang giao").build());
            links.add(ChatbotMessageResponse.NavigationDto.builder().url("/shipper?tab=new").label("Bật nhận chuyến").build());
        }
        // 2.5. Hồ sơ cá nhân tài xế
        else if (lowerMsg.contains("hồ sơ tài xế") || lowerMsg.contains("biển số xe")
                || (isShipperRole && (lowerMsg.contains("hồ sơ") || lowerMsg.contains("xe") || lowerMsg.contains("bằng lái")))) {
            targetRole = "SHIPPER";
            title = "Hồ Sơ Cá Nhân & Phương Tiện Giao Hàng";
            desc = "1. Mở tab 'Hồ Sơ' để kiểm tra thông tin cá nhân và phương tiện đăng ký.\n"
                    + "2. Cập nhật biển số xe, số điện thoại liên hệ và giấy phép lái xe khi có thay đổi.\n"
                    + "3. Xem tổng số chuyến đã hoàn thành và số điểm đánh giá từ khách hàng.";
            links.add(ChatbotMessageResponse.NavigationDto.builder().url("/shipper?tab=profile").label("Xem Hồ Sơ Cá Nhân Tài Xế").build());
            links.add(ChatbotMessageResponse.NavigationDto.builder().url("/shipper?tab=new").label("Nhận đơn mới").build());
            links.add(ChatbotMessageResponse.NavigationDto.builder().url("/shipper?tab=delivering").label("Lộ trình giao hàng").build());
        }

        // =========================================================================
        // 3. NHÓM ĐĂNG KÝ VAI TRÒ HOẶC TỔNG QUAN PORTAL MẶC ĐỊNH
        // =========================================================================

        // 3.1. Khách hỏi cách đăng ký mở gian hàng Nhà vườn
        else if (lowerMsg.contains("mở shop") || lowerMsg.contains("đăng ký bán") || lowerMsg.contains("nhà vườn muốn") || lowerMsg.contains("bán hàng trên sàn")) {
            targetRole = "SHOP";
            title = "Đăng Ký Mở Gian Hàng Nhà Vườn CapNong";
            desc = "1. Đăng ký tài khoản: Chọn vai trò Nhà vườn (Shop) tại biểu mẫu đăng ký.\n"
                    + "2. Cập nhật thông tin vườn: Điền địa chỉ thu hoạch và tọa độ GPS lấy hàng.\n"
                    + "3. Đăng bán nông sản: Tiếp cận hàng nghìn khách hàng đô thị và giải cứu nông sản hiệu quả.";
            links.add(ChatbotMessageResponse.NavigationDto.builder().url("/dang-ky?role=shop").label("Đăng Ký Mở Gian Hàng Nông Dân").build());
            links.add(ChatbotMessageResponse.NavigationDto.builder().url("/shop").label("Kênh Nhà Vườn (Nếu đã có tài khoản)").build());
        }
        // 3.2. Khách hỏi cách đăng ký làm tài xế / Shipper
        else if (lowerMsg.contains("làm tài xế") || lowerMsg.contains("đăng ký shipper") || lowerMsg.contains("đăng ký giao hàng") || lowerMsg.contains("làm shipper")) {
            targetRole = "SHIPPER";
            title = "Đăng Ký Trở Thành Tài Xế Giao Hàng CapNong";
            desc = "1. Đăng ký tài khoản: Chọn vai trò Tài xế (Shipper) tại trang đăng ký.\n"
                    + "2. Cập nhật hồ sơ: Cung cấp bằng lái xe, biển số phương tiện và thông tin liên hệ.\n"
                    + "3. Bật ứng dụng nhận chuyến và giao nông sản tươi cho khách.";
            links.add(ChatbotMessageResponse.NavigationDto.builder().url("/dang-ky?role=shipper").label("Đăng Ký Làm Tài Xế Giao Hàng").build());
            links.add(ChatbotMessageResponse.NavigationDto.builder().url("/shipper").label("Cổng Tài Xế (Nếu đã có tài khoản)").build());
        }
        // 3.3. Người dùng đang đăng nhập SHOP (hỏi tổng quan)
        else if (isShopRole) {
            targetRole = "SHOP";
            title = "Kênh Quản Trị Nhà Vườn & Nông Trại Đối Tác";
            desc = "1. Quản lý danh mục: Đăng bán nông sản, tạo combo bữa ăn và phát hành túi mù giải cứu.\n"
                    + "2. Chuẩn bị đơn hàng: Theo dõi đơn mới và đóng thùng giữ lạnh bàn giao cho shipper.\n"
                    + "3. Doanh thu minh bạch: Đối soát đơn hàng và rút tiền về tài khoản ngân hàng nhanh chóng.";
            links.add(ChatbotMessageResponse.NavigationDto.builder().url("/shop?tab=overview").label("Vào Kênh Nhà Vườn (Farmer Portal)").build());
            links.add(ChatbotMessageResponse.NavigationDto.builder().url("/shop?tab=orders").label("Xử lý đơn hàng nhà vườn").build());
            links.add(ChatbotMessageResponse.NavigationDto.builder().url("/shop?tab=products").label("Quản lý nông sản đăng bán").build());
            links.add(ChatbotMessageResponse.NavigationDto.builder().url("/shop?tab=wallet").label("Ví doanh thu & Rút tiền").build());
        }
        // 3.4. Người dùng đang đăng nhập SHIPPER (hỏi tổng quan)
        else if (isShipperRole) {
            targetRole = "SHIPPER";
            title = "Cổng Thông Tin & Quản Lý Dành Cho Tài Xế";
            desc = "1. Bật trực tuyến: Nhận đơn giao hàng tươi trong bán kính hoạt động quanh bạn.\n"
                    + "2. Quy trình giao nhận: Nhận hàng tại vườn và giao tận tay khách với định vị GPS trực tiếp.\n"
                    + "3. Quản lý tài chính: Kiểm tra tiền thu COD và rút tiền hoa hồng giao nhận về tài khoản.";
            links.add(ChatbotMessageResponse.NavigationDto.builder().url("/shipper?tab=new").label("Cổng Tài Xế (Bật nhận chuyến)").build());
            links.add(ChatbotMessageResponse.NavigationDto.builder().url("/shipper?tab=delivering").label("Đơn hàng đang giao & Lộ trình").build());
            links.add(ChatbotMessageResponse.NavigationDto.builder().url("/shipper?tab=profile").label("Hồ sơ tài xế & Tiền thu COD").build());
        }
        // 3.5. Khách vãng lai / Người mua hỏi chung về hợp tác đối tác
        else {
            targetRole = "GUEST";
            title = "Hợp Tác Đồng Hành Cùng CapNong";
            desc = "1. Nhà vườn đối tác: Đăng ký mở shop để phân phối nông sản tươi và nông sản giải cứu.\n"
                    + "2. Tài xế giao vận: Đăng ký gia nhập đội ngũ giao nông sản tươi.\n"
                    + "3. Hỗ trợ: Đội ngũ CapNong đồng hành cùng đối tác trong quá trình bán hàng và giao nhận.";
            links.add(ChatbotMessageResponse.NavigationDto.builder().url("/dang-ky?role=shop").label("Đăng Ký Mở Gian Hàng Nông Dân").build());
            links.add(ChatbotMessageResponse.NavigationDto.builder().url("/dang-ky?role=shipper").label("Đăng Ký Trở Thành Tài Xế").build());
            links.add(ChatbotMessageResponse.NavigationDto.builder().url("/shop").label("Kênh Nhà Vườn (Shop)").build());
            links.add(ChatbotMessageResponse.NavigationDto.builder().url("/shipper").label("Cổng Tài Xế (Shipper)").build());
        }

        ChatbotMessageResponse.RoleGuidanceDto dto = ChatbotMessageResponse.RoleGuidanceDto.builder()
                .role(targetRole)
                .title(title)
                .guidance(desc)
                .quickLinks(links)
                .build();

        return ChatbotMessageResponse.builder()
                .type(AiResponseType.ROLE_GUIDANCE)
                .message(title + ":")
                .roleGuidance(dto)
                .timestamp(LocalDateTime.now())
                .build();
    }

    // =========================================================================
    // ===== 8 EXTENDED ACTION METHODS =========================================
    // =========================================================================

    // Case 6.1: Mua lại đơn hàng cũ 1-click
    @Override
    public ChatbotMessageResponse reorderRecent(Integer orderId, Integer userId) {
        if (userId == null) {
            return ChatbotMessageResponse.builder()
                    .type(AiResponseType.NAVIGATION)
                    .message("Bạn cần đăng nhập để xem và đặt lại các đơn hàng cũ nhé!")
                    .navigation(ChatbotMessageResponse.NavigationDto.builder().url("/dang-nhap").label("Đăng nhập ngay").build())
                    .timestamp(LocalDateTime.now())
                    .build();
        }

        Order targetOrder = null;
        if (orderId != null) {
            targetOrder = orderRepository.findById(orderId).filter(o -> o.getBuyer().getId() == userId).orElse(null);
            if (targetOrder == null) {
                return ChatbotMessageResponse.builder()
                        .type(AiResponseType.NAVIGATION)
                        .message("Không tìm thấy đơn hàng #" + orderId + " trong tài khoản của bạn (hoặc đơn hàng thuộc tài khoản khác). Bạn vui lòng bấm nút bên dưới để mở Lịch sử đơn hàng kiểm tra lại nhé!")
                        .navigation(ChatbotMessageResponse.NavigationDto.builder().url("/orders").label("Xem Lịch Sử Đơn Hàng").build())
                        .timestamp(LocalDateTime.now())
                        .build();
            }
        } else {
            Page<Order> orders = orderRepository.findByBuyer_Id(userId, PageRequest.of(0, 1, Sort.by(Sort.Direction.DESC, "createdAt")));
            if (orders.hasContent()) {
                targetOrder = orders.getContent().get(0);
            }
        }

        if (targetOrder == null) {
            return ChatbotMessageResponse.builder()
                    .type(AiResponseType.NAVIGATION)
                    .message("Tài khoản của bạn hiện chưa có đơn hàng nào trước đây để đặt lại. Hãy dạo quanh catalog và khám phá nông sản giải cứu nhé!")
                    .navigation(ChatbotMessageResponse.NavigationDto.builder().url("/san-pham").label("Khám Phá Nông Sản").build())
                    .timestamp(LocalDateTime.now())
                    .build();
        }

        List<OrderDetail> details = orderDetailRepository.findByOrder_OrderId(targetOrder.getOrderId());
        List<ChatbotMessageResponse.ItemOptionDto> reorderItems = new ArrayList<>();

        for (OrderDetail d : details) {
            int orderQty = d.getQuantity() != null ? d.getQuantity() : 1;

            if (d.getProduct() != null) {
                Product p = d.getProduct();
                String img = (p.getImages() != null && !p.getImages().isEmpty()) ? p.getImages().get(0).getImageUrl() : null;
                boolean active = "ACTIVE".equalsIgnoreCase(p.getStatus());
                int stock = p.getStockQuantity() != null ? p.getStockQuantity() : 0;
                boolean outOfStock = !active || stock <= 0;

                reorderItems.add(ChatbotMessageResponse.ItemOptionDto.builder()
                        .itemType(AiItemType.PRODUCT)
                        .itemId(p.getProductId())
                        .name(p.getProductName())
                        .price(d.getUnitPrice() != null ? d.getUnitPrice() : p.getSellingPrice())
                        .salePrice(salePriceOf(p))
                        .unit(p.getUnit() != null ? p.getUnit() : "kg")
                        .imageUrl(img)
                        .availableQuantity(stock)
                        .isOutOfStock(outOfStock)
                        .orderQuantity(orderQty)
                        .build());
            } else if (d.getMysteryBox() != null) {
                MysteryBox mb = d.getMysteryBox();
                boolean active = Boolean.TRUE.equals(mb.getIsActive());
                int total = mb.getTotalQuantity() != null ? mb.getTotalQuantity() : 0;
                int sold = mb.getSoldQuantity() != null ? mb.getSoldQuantity() : 0;
                int stock = Math.max(0, total - sold);
                boolean outOfStock = !active || stock <= 0;

                reorderItems.add(ChatbotMessageResponse.ItemOptionDto.builder()
                        .itemType(AiItemType.MYSTERY_BOX)
                        .itemId(mb.getMysteryId())
                        .name(mb.getBoxType())
                        .price(mb.getPrice())
                        .salePrice(mb.getPrice())
                        .unit("hộp")
                        .imageUrl(mb.getImageUrl())
                        .availableQuantity(stock)
                        .isOutOfStock(outOfStock)
                        .orderQuantity(orderQty)
                        .build());
            } else if (d.getBuildCombo() != null) {
                BuildCombo cb = d.getBuildCombo();
                boolean active = "ACTIVE".equalsIgnoreCase(cb.getStatus());
                int total = cb.getTotalQuantity() != null ? cb.getTotalQuantity() : 0;
                int sold = cb.getSoldQuantity() != null ? cb.getSoldQuantity() : 0;
                int stock = Math.max(0, total - sold);
                boolean outOfStock = !active || stock <= 0;

                reorderItems.add(ChatbotMessageResponse.ItemOptionDto.builder()
                        .itemType(AiItemType.BUILD_COMBO)
                        .itemId(cb.getComboId())
                        .name(cb.getComboName())
                        .price(cb.getPrice())
                        .salePrice(cb.getPrice())
                        .unit("combo")
                        .imageUrl(cb.getImageUrl())
                        .availableQuantity(stock)
                        .isOutOfStock(outOfStock)
                        .orderQuantity(orderQty)
                        .build());
            }
        }

        int totalItems = reorderItems.size();
        int availableCount = (int) reorderItems.stream().filter(i -> !Boolean.TRUE.equals(i.getIsOutOfStock())).count();
        int outOfStockCount = totalItems - availableCount;
        boolean canReorder = availableCount > 0;

        String formattedDate = targetOrder.getCreatedAt() != null
                ? targetOrder.getCreatedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
                : "";
        String shopName = (targetOrder.getShop() != null && targetOrder.getShop().getFullName() != null)
                ? targetOrder.getShop().getFullName()
                : "Nhà vườn đối tác";
        String orderStatus = targetOrder.getStatus() != null ? targetOrder.getStatus().name() : "";

        String headerMessage;
        String stockWarning = null;

        if (totalItems == 0) {
            headerMessage = "Đơn hàng #" + targetOrder.getOrderId() + " không chứa sản phẩm nào khả dụng để đặt lại.";
        } else if (outOfStockCount == 0) {
            headerMessage = "Đơn hàng #" + targetOrder.getOrderId() + " của bạn gồm " + totalItems + " món. Hiện tại tất cả " + totalItems + " món đều còn hàng tươi ngon! Bạn có muốn đặt lại không?";
        } else if (availableCount > 0) {
            headerMessage = "Đơn hàng #" + targetOrder.getOrderId() + " của bạn có " + totalItems + " món, hiện tại " + availableCount + " món vẫn còn hàng, còn " + outOfStockCount + " món tạm hết hàng. Bạn có muốn đặt lại " + availableCount + " món còn hàng này không?";
            stockWarning = "Có " + outOfStockCount + " sản phẩm trong đơn cũ này hiện đã hết hàng hoặc hết mùa vụ.";
        } else {
            headerMessage = "Rất tiếc, tất cả " + totalItems + " món trong đơn hàng #" + targetOrder.getOrderId() + " hiện tại đều đã hết hàng hoặc hết mùa vụ.";
            stockWarning = "Tất cả sản phẩm trong đơn hàng này hiện đã hết hàng.";
        }

        if ("PENDING".equalsIgnoreCase(orderStatus) || "PROCESSING".equalsIgnoreCase(orderStatus) || "SHIPPING".equalsIgnoreCase(orderStatus)) {
            headerMessage = "⚠️ Lưu ý: Đơn hàng #" + targetOrder.getOrderId() + " của bạn hiện đang được chuẩn bị/giao hàng. " + headerMessage;
        }

        ChatbotMessageResponse.ReorderDto dto = ChatbotMessageResponse.ReorderDto.builder()
                .orderId(targetOrder.getOrderId())
                .orderDate(formattedDate)
                .shopName(shopName)
                .orderStatus(orderStatus)
                .totalAmount(targetOrder.getTotalAmount())
                .items(reorderItems)
                .canReorder(canReorder)
                .availableItemsCount(availableCount)
                .outOfStockItemsCount(outOfStockCount)
                .stockWarning(stockWarning)
                .build();

        return ChatbotMessageResponse.builder()
                .type(AiResponseType.REORDER_RECENT)
                .message(headerMessage)
                .reorder(dto)
                .timestamp(LocalDateTime.now())
                .build();
    }

    // Case 6.2: Nông sản xả gấp / Flash rescue
    @Override
    public ChatbotMessageResponse getFlashRescueItems() {
        // Chỉ lấy sản phẩm đang giảm giá thật; trang lớn hơn số cần lấy vì không lọc được theo giảm giá trong truy vấn.
        Page<Product> products = productRepository.findAll(
                ProductSpecification.filterProducts("ACTIVE", null, null, null),
                PageRequest.of(0, 50)
        );
        List<ChatbotMessageResponse.ItemOptionDto> flashItems = new ArrayList<>();
        int maxDiscount = 0;

        for (Product p : products.getContent()) {
            if (isOnSale(p) && p.getStockQuantity() != null && p.getStockQuantity() > 0) {
                String img = (p.getImages() != null && !p.getImages().isEmpty()) ? p.getImages().get(0).getImageUrl() : null;
                flashItems.add(ChatbotMessageResponse.ItemOptionDto.builder()
                        .itemType(AiItemType.PRODUCT)
                        .itemId(p.getProductId())
                        .name(p.getProductName())
                        .price(p.getSellingPrice())
                        .salePrice(salePriceOf(p))
                        .discountPercent(p.getDiscountPercent())
                        .unit(p.getUnit() != null ? p.getUnit() : "kg")
                        .availableQuantity(p.getStockQuantity())
                        .imageUrl(img)
                        .build());
                maxDiscount = Math.max(maxDiscount, p.getDiscountPercent());
                if (flashItems.size() >= 5) break;
            }
        }

        if (flashItems.isEmpty()) {
            return ChatbotMessageResponse.builder()
                    .type(AiResponseType.NAVIGATION)
                    .message("Hiện chưa có nông sản nào đang giảm giá. Bạn xem các nông sản khác đang có trên sàn nhé!")
                    .navigation(ChatbotMessageResponse.NavigationDto.builder().url("/san-pham").label("Xem nông sản").build())
                    .timestamp(LocalDateTime.now())
                    .build();
        }

        ChatbotMessageResponse.FlashRescueDto dto = ChatbotMessageResponse.FlashRescueDto.builder()
                .title("Nông sản đang giảm giá")
                .description("Các nông sản đang được nhà vườn giảm giá trên CapNong:")
                .discountRate(maxDiscount)
                .items(flashItems)
                .build();

        return ChatbotMessageResponse.builder()
                .type(AiResponseType.FLASH_RESCUE)
                .message("Danh sách nông sản đang giảm giá hôm nay:")
                .flashRescue(dto)
                .timestamp(LocalDateTime.now())
                .build();
    }

    // Case 7.1: Thực đơn bệnh lý & AI Meal Plan
    // Case 7.1: Thực đơn bệnh lý & AI Meal Plan
    @Override
    public ChatbotMessageResponse bridgeMealPlan(String conditionQuery, Integer userId) {
        String cond = (conditionQuery != null && !conditionQuery.isBlank()) ? conditionQuery.toLowerCase() : "tiểu đường";
        String conditionName;
        String advice;
        List<String> sampleMeals = new ArrayList<>();
        List<String> good = new ArrayList<>();
        List<String> avoid = new ArrayList<>();
        List<String> suggestedConditions = new ArrayList<>();
        String searchKeyword = "rau";

        if (cond.contains("tiểu đường") || cond.contains("đái tháo đường") || cond.contains("đường huyết")) {
            conditionName = "Chế độ dinh dưỡng & Thực đơn cho người Tiểu Đường";
            advice = "Đối với người tiểu đường, việc kiểm soát đường huyết thông qua chế độ ăn uống là vô cùng quan trọng. Bạn nên tập trung vào các loại thực phẩm có chỉ số đường huyết (GI) thấp, giàu chất xơ hòa tan, protein nạc và chất béo lành mạnh. Hạn chế tối đa đường tinh luyện, đồ ngọt, nước ngọt có ga và tinh bột chuyển hóa nhanh.";
            sampleMeals.addAll(List.of(
                    "🌅 Bữa sáng: Yến mạch nấu hạt chia hoặc khoai lang luộc + 1 quả trứng gà luộc + 1 ly sữa hạt không đường.",
                    "☀️ Bữa trưa: 1 chén cơm gạo lứt + ức gà áp chảo / cá hồi nướng + đĩa lớn bông cải xanh và bí đao luộc.",
                    "🌤️ Bữa phụ (xế chiều): 1/2 quả bưởi da xanh hoặc vài lát bơ tươi / dưa leo giòn mát.",
                    "🌙 Bữa tối: Canh mướp đắng (khổ qua) nấu thịt nạc + đậu hũ hấp rau củ + salad xà lách trộn dầu giấm."
            ));
            good.addAll(List.of("Mướp đắng (khổ qua)", "Bông cải xanh", "Rau chân vịt (cải bó xôi)", "Bí đao", "Đậu cô ve", "Bưởi da xanh"));
            avoid.addAll(List.of("Nước mía & đồ ngọt", "Xoài chín ngọt lịm", "Mít sấy", "Sầu riêng", "Khoai tây chiên", "Bánh kẹo tinh luyện"));
            suggestedConditions.addAll(List.of("Tiểu đường type 2 (Cần giảm đường bột)", "Ăn kiêng giảm cân (Keto / Low-carb)", "Huyết áp cao (Ăn nhạt, nhiều kali)"));
            searchKeyword = "cải";
        } else if (cond.contains("dạ dày") || cond.contains("bao tử") || cond.contains("trào ngược") || cond.contains("tiêu hóa")) {
            conditionName = "Chế độ dinh dưỡng & Thực đơn cho người Đau Dạ Dày";
            advice = "Ưu tiên thực phẩm làm dịu niêm mạc dạ dày, thức ăn mềm, dễ tiêu hóa, giảm tiết acid. Tránh đồ có tính acid cao, quá nhiều dầu mỡ, cay nóng và đồ lên men chua.";
            sampleMeals.addAll(List.of(
                    "🌅 Bữa sáng: Cháo yến mạch thịt nạc bằm hoặc bánh mì nướng mềm + 1 cốc nước ấm nghệ mật ong.",
                    "☀️ Bữa trưa: Cơm dẻo mềm + cá bống kho nghệ hoặc thịt nạc hấp + canh bí đỏ hầm đậu xanh mềm ngọt.",
                    "🌤️ Bữa phụ: 1 quả chuối chín hoặc 1 hũ sữa chua ít đường để nhiệt độ phòng.",
                    "🌙 Bữa tối: Canh bắp cải tươi cuộn thịt (giàu vitamin U tái tạo niêm mạc) + cà rốt luộc mềm + đậu phụ luộc."
            ));
            good.addAll(List.of("Bắp cải tươi", "Bí đỏ hồ lô", "Khoai lang", "Cà rốt", "Rau mồng tơi", "Rau ngót", "Chuối chín"));
            avoid.addAll(List.of("Ớt cay & tiêu", "Chanh / quất / cóc / xoài xanh", "Cà muối & dưa chua", "Đồ chiên xào nhiều mỡ", "Đồ uống có ga"));
            suggestedConditions.addAll(List.of("Đau dạ dày / Tiêu hóa nhạy cảm", "Trào ngược dạ dày", "Ăn kiêng dịu tiêu hóa"));
            searchKeyword = "bí";
        } else if (cond.contains("huyết áp") || cond.contains("tim mạch") || cond.contains("mỡ máu")) {
            conditionName = "Chế độ dinh dưỡng cho người Huyết Áp Cao & Tim Mạch";
            advice = "Nguyên tắc then chốt là chế độ ăn giảm muối (natri), tăng cường kali, magie và chất béo không bão hòa tốt cho mạch máu từ nông sản tươi.";
            sampleMeals.addAll(List.of(
                    "🌅 Bữa sáng: Cháo cá hồi yến mạch rắc hành ngò tươi + 1 quả chuối chín giàu kali.",
                    "☀️ Bữa trưa: Cơm gạo lứt + thịt thăn nạc hấp + đĩa lớn rau muống luộc / cần tây xào tỏi ít dầu.",
                    "🌤️ Bữa phụ: 1 ly nước ép cần tây táo hoặc vài múi bưởi da xanh.",
                    "🌙 Bữa tối: Canh cà chua nấu đậu phụ + đĩa rau củ luộc ngũ sắc (cà rốt, bí đao, cải ngọt)."
            ));
            good.addAll(List.of("Cần tây", "Cải bó xôi (spinach)", "Cà chua bi", "Bưởi da xanh", "Chuối", "Cá hồi"));
            avoid.addAll(List.of("Dưa cà muối mặn", "Thực phẩm đóng hộp", "Thịt mỡ & da gà vịt", "Mì tôm", "Rượu bia"));
            suggestedConditions.addAll(List.of("Huyết áp cao (Ăn nhạt, nhiều kali)", "Mỡ máu cao / Tim mạch"));
            searchKeyword = "bưởi";
        } else {
            conditionName = "Chế độ ăn sạch Eat Clean & Giữ Dáng Khoa Học";
            advice = "Tăng cường rau xanh đậm, củ quả tươi nguyên bản, protein thực vật và ngũ cốc nguyên cám. Giữ mức thâm hụt calo nhẹ nhàng để cơ thể luôn thon gọn, dẻo dai.";
            sampleMeals.addAll(List.of(
                    "🌅 Bữa sáng: Salad xà lách mỡ + 2 quả trứng gà luộc + cà chua bi ngọt mát.",
                    "☀️ Bữa trưa: 1 củ khoai lang vàng Đà Lạt + ức gà hấp xé phay + súp lơ xanh & cà rốt luộc.",
                    "🌤️ Bữa phụ: 1 quả táo xanh hoặc 1 nắm hạt điều nguyên vị.",
                    "🌙 Bữa tối: Cá phi lê áp chảo + đĩa xà lách mỡ trộn dầu giấm + canh rau ngót tươi mát."
            ));
            good.addAll(List.of("Xà lách mỡ", "Cải bó xôi", "Cà rốt Đà Lạt", "Dưa leo", "Cà chua bi", "Khoai lang mật"));
            avoid.addAll(List.of("Nông sản sấy tẩm đường", "Khoai tây chiên bơ", "Nước ép lọc bã", "Trà sữa", "Thức ăn nhanh"));
            suggestedConditions.addAll(List.of("Ăn kiêng giảm cân (Keto / Low-carb)", "Chế độ Eat Clean thanh lọc"));
            searchKeyword = "xà lách";
        }

        // Bỏ các gợi ý có tên trùng chất gây dị ứng trong hồ sơ. Chỉ so được theo tên nên phải nói rõ với khách.
        List<String> allergens = allergensOf(userId);
        if (!allergens.isEmpty()) {
            sampleMeals.removeIf(meal -> mentionsAllergen(meal, allergens));
            good.removeIf(name -> mentionsAllergen(name, allergens));
            advice += " Hồ sơ của bạn ghi nhận dị ứng: " + String.join(", ", allergens)
                    + ". CapNong đã bỏ các gợi ý có tên trùng với những chất này, nhưng bạn vẫn nên kiểm tra kỹ thành phần trước khi dùng.";
        }

        // Lấy 2-3 sản phẩm thật đang có bán trên sàn phù hợp với bệnh lý/chế độ ăn này
        List<ChatbotMessageResponse.ItemOptionDto> recommendedProducts = new ArrayList<>();
        try {
            Page<Product> pPage = productRepository.findAll(
                    ProductSpecification.filterProducts("ACTIVE", null, searchKeyword, null),
                    PageRequest.of(0, 6)
            );
            if (!pPage.hasContent()) {
                pPage = productRepository.findAll(
                        ProductSpecification.filterProducts("ACTIVE", null, null, null),
                        PageRequest.of(0, 6)
                );
            }
            for (Product p : pPage.getContent()) {
                boolean available = p.getStockQuantity() != null && p.getStockQuantity() > 0;
                if (!available || mentionsAllergen(p.getProductName(), allergens) || recommendedProducts.size() >= 3) continue;
                String img = (p.getImages() != null && !p.getImages().isEmpty()) ? p.getImages().get(0).getImageUrl() : null;
                recommendedProducts.add(ChatbotMessageResponse.ItemOptionDto.builder()
                        .itemType(AiItemType.PRODUCT)
                        .itemId(p.getProductId())
                        .name(p.getProductName())
                        .price(p.getSellingPrice())
                        .salePrice(salePriceOf(p))
                        .unit(p.getUnit() != null ? p.getUnit() : "kg")
                        .availableQuantity(p.getStockQuantity())
                        .imageUrl(img)
                        .build());
            }
        } catch (Exception e) {
            log.warn("[MealPlanBridge] Failed to fetch products for bridge: {}", e.getMessage());
        }

        String ctaMessage = "💡 Gợi ý từ CapNong: Để lập thực đơn cho cả tuần và cập nhật hồ sơ sức khỏe của bạn, bạn có thể truy cập trang Kế Hoạch Bữa Ăn nhé!";

        ChatbotMessageResponse.MealPlanBridgeDto dto = ChatbotMessageResponse.MealPlanBridgeDto.builder()
                .conditionName(conditionName)
                .healthAdvice(advice)
                .sampleMeals(sampleMeals)
                .goodProduce(good)
                .avoidProduce(avoid)
                .recommendedProducts(recommendedProducts)
                .suggestedConditions(suggestedConditions)
                .ctaMessage(ctaMessage)
                .planUrl("/ke-hoach-an-uong")
                .actionLabel("Lên Kế Hoạch Bữa Ăn & Cập Nhật Hồ Sơ Sức Khỏe")
                .build();

        return ChatbotMessageResponse.builder()
                .type(AiResponseType.MEAL_PLAN_BRIDGE)
                .message("Gợi ý dinh dưỡng tham khảo từ Trợ lý CapNong:")
                .mealPlanBridge(dto)
                .timestamp(LocalDateTime.now())
                .build();
    }

    // Case 7.2: Lịch mùa vụ nông sản Việt Nam - ĐA DẠNG 12 THÁNG & CHÍNH XÁC THEO THỜI VỤ
    @Override
    public ChatbotMessageResponse getSeasonalCalendar() {
        return getSeasonalCalendar(null, null);
    }

    @Override
    public ChatbotMessageResponse getSeasonalCalendar(Integer targetMonth, String userMessage) {
        int currentMonth = LocalDate.now().getMonthValue();
        int month = (targetMonth != null && targetMonth >= 1 && targetMonth <= 12)
                ? targetMonth
                : extractMonthFromText(userMessage, currentMonth);

        boolean isCurrentMonth = (month == currentMonth);
        boolean isPastMonth = (month < currentMonth);
        boolean isFutureMonth = (month > currentMonth);

        // Nông sản đặc trưng theo từng tháng thực tế tại các vùng miền Việt Nam
        List<String> fruits;
        List<String> veggies;
        String highlight;
        List<String> upcoming;

        switch (month) {
            case 1:
                fruits = List.of("Dưa hấu ruột đỏ", "Mãng cầu (Na) Tiền Giang", "Xoài cát Hòa Lộc", "Quýt đường miền Tây", "Cam Canh");
                veggies = List.of("Bắp cải hữu cơ", "Su hào Đà Lạt", "Cà chua bi cherry", "Xà lách xoăn", "Cà rốt baby");
                highlight = "Tháng 1 là tiết trời giáp Tết, nông sản phong phú với các loại trái cây chưng Tết và rau củ mùa lạnh giòn ngọt.";
                upcoming = List.of("Dâu tây giống Nhật", "Vú sữa Lò Rèn", "Bông cải xanh");
                break;
            case 2:
                fruits = List.of("Dâu tây giống Nhật Đà Lạt", "Vú sữa Lò Rèn Tiền Giang", "Mãng cầu xiêm", "Quýt hồng Lai Vung", "Thanh long ruột đỏ");
                veggies = List.of("Bông cải xanh (Súp lơ)", "Cải bó xôi (Bina)", "Củ dền đỏ", "Măng tây xanh hữu cơ", "Đậu cô ve non");
                highlight = "Tháng 2 vào xuân tươi mới, Dâu tây Đà Lạt chín mọng đạt độ ngọt cao nhất cùng các loại rau củ xanh thanh mát bổ dưỡng.";
                upcoming = List.of("Xoài keo", "Dưa gang", "Rau muống non");
                break;
            case 3:
                fruits = List.of("Dâu tây Đà Lạt (rộ vụ)", "Vú sữa Lò Rèn", "Xoài keo ngọt lịm", "Dưa gang thanh nhiệt", "Bưởi da xanh Bến Tre");
                veggies = List.of("Rau muống non sạch", "Cải ngọt tươi giòn", "Bầu sao non", "Mướp hương ngọt thơm", "Cà tím hữu cơ");
                highlight = "Tháng 3 chuyển dần sang mùa khô ấm áp, lý tưởng để thưởng thức Dâu tây tươi và các món canh rau giải nhiệt giòn mát.";
                upcoming = List.of("Bơ sáp Đắk Lắk", "Mận hậu Mộc Châu", "Dưa lưới ruột cam");
                break;
            case 4:
                fruits = List.of("Bơ sáp đầu vụ Đắk Lắk", "Mận hậu Mộc Châu (đầu vụ)", "Xoài cát Chu", "Dưa lưới ruột cam", "Chuối già Nam Mỹ");
                veggies = List.of("Rau mồng tơi", "Rau đay", "Mướp hương", "Cải bẹ xanh", "Bí đao chanh");
                highlight = "Tháng 4 mở màn mùa quả ngọt đầu hạ với Bơ sáp béo ngậy và các loại rau giải nhiệt thanh mát ngày nắng.";
                upcoming = List.of("Vải thiều", "Mận hậu chính vụ", "Sầu riêng Ri6", "Măng cụt");
                break;
            case 5:
                fruits = List.of("Mận hậu Sơn La", "Vải thiều sớm", "Sầu riêng Ri6 miền Tây", "Măng cụt Lái Thiêu (đầu vụ)", "Chôm chôm nhãn");
                veggies = List.of("Khổ qua (mướp đắng)", "Đậu bắp non hữu cơ", "Rau ngót non", "Cà chua hữu cơ", "Rau dền đỏ");
                highlight = "Tháng 5 là thiên đường trái cây nhiệt đới Việt Nam: Mận hậu giòn rụm, Măng cụt chua ngọt và Sầu riêng Ri6 thơm phức.";
                upcoming = List.of("Vải thiều Lục Ngạn", "Chôm chôm Thái", "Bơ Booth");
                break;
            case 6:
                fruits = List.of("Vải thiều Lục Ngạn Bắc Giang (chính vụ)", "Mận hậu Sơn La", "Sầu riêng Monthong/Ri6", "Chôm chôm Thái", "Bơ Booth Đắk Lắk");
                veggies = List.of("Rau muống sạch", "Rau đay mồng tơi", "Cải thìa non", "Bầu hồ lô", "Cà pháo sạch");
                highlight = "Tháng 6 rộ mùa Vải thiều chín đỏ mọng nước cùng Sầu riêng, Chôm chôm ngọt lịm - thời điểm nông sản tươi và giá tốt nhất năm.";
                upcoming = List.of("Nhãn lồng Hưng Yên", "Mít Thái", "Thanh long Bình Thuận");
                break;
            case 7:
                fruits = List.of("Nhãn lồng Hưng Yên (chớm vụ)", "Bơ Booth Đắk Lắk", "Bưởi Năm Roi", "Thanh long ruột trắng/đỏ", "Mít Thái ruột đỏ");
                veggies = List.of("Đậu cô ve xanh", "Rau ngót hữu cơ", "Bí đỏ hạt đậu", "Măng tươi Tây Nguyên", "Bông thiên lý");
                highlight = "Tháng 7 là mùa thu hoạch rộ của Bơ booth dẻo quánh, Nhãn lồng ngọt thơm và các loại rau thanh nhiệt mùa mưa hè.";
                upcoming = List.of("Nhãn lồng chính vụ", "Bưởi Phúc Trạch", "Na dai Lạng Sơn");
                break;
            case 8:
                fruits = List.of("Nhãn lồng Hưng Yên (chính vụ)", "Bưởi Phúc Trạch Hà Tĩnh", "Na dai Đồng Mỏ", "Ổi nữ hoàng", "Bơ sáp cuối vụ");
                veggies = List.of("Rau muống ruộng sạch", "Mướp hương", "Rau lang non ngọn", "Cải ngồng", "Đậu bắp");
                highlight = "Tháng 8 đón Tết Trung thu cận kề với Bưởi Phúc Trạch, Na dai ngọt lịm và các loại trái cây thơm mát mùa thu.";
                upcoming = List.of("Hồng giòn Đà Lạt", "Cam sành Hà Giang", "Khoai môn Lệ Phố");
                break;
            case 9:
                fruits = List.of("Hồng giòn Đà Lạt (đầu vụ)", "Bưởi da xanh Bến Tre", "Na dai", "Thanh long Bình Thuận", "Cam sành Hà Giang");
                veggies = List.of("Rau cần nước non", "Cải thìa", "Rau ngót", "Bí ngô ngọt", "Khoai môn Lệ Phố");
                highlight = "Tháng 9 chớm thu mát mẻ, đặc sản Hồng giòn Đà Lạt bắt đầu vàng ươm, giòn ngọt tự nhiên cùng Bưởi da xanh thanh mát.";
                upcoming = List.of("Hồng giòn rộ vụ", "Cải thảo Đà Lạt", "Bông cải xanh", "Khoai lang mật");
                break;
            case 10:
                fruits = List.of("Hồng giòn Đà Lạt (rộ vụ)", "Cam sành miền Tây (vào vụ)", "Bưởi da xanh Bến Tre", "Nho xanh Ninh Thuận", "Thanh long ruột đỏ");
                veggies = List.of("Cải thảo hữu cơ", "Bắp cải Đà Lạt", "Súp lơ (bông cải xanh)", "Củ dền đỏ", "Khoai lang mật Đà Lạt", "Su su Đà Lạt");
                highlight = "Tháng 10 là cao điểm mùa Thu Đông, nông sản nổi bật nhất là Hồng giòn Đà Lạt ngọt lịm, Cam sành mọng nước và các loại rau xứ lạnh giòn ngọt.";
                upcoming = List.of("Quýt đường miền Tây", "Bắp cải tím", "Cải xoong", "Khoai tây vàng");
                break;
            case 11:
                fruits = List.of("Cam sành Tiền Giang", "Quýt đường miền Tây", "Bưởi Diễn / Bưởi da xanh", "Chuối tiêu hồng", "Dưa hấu ruột đỏ");
                veggies = List.of("Bắp cải tím", "Cà rốt Đà Lạt", "Xà lách xoăn", "Rau cải xoong", "Củ cải trắng", "Khoai tây vàng Đà Lạt");
                highlight = "Tháng 11 bước vào chính đông, là mùa thu hoạch rộ của Quýt đường, Cam sành ngọt mát cùng các loại rau củ hữu cơ Đà Lạt giòn ngọt nhất năm.";
                upcoming = List.of("Quýt hồng Lai Vung", "Xoài cát Chu", "Bắp cải búp", "Su hào");
                break;
            case 12:
            default:
                fruits = List.of("Quýt hồng Lai Vung Đồng Tháp", "Bưởi da xanh Tết", "Xoài cát Chu", "Dưa lưới ngọt mát", "Táo gió Ninh Thuận");
                veggies = List.of("Bắp cải búp giòn", "Cải bó xôi (Bina)", "Cà rốt baby", "Củ cải đỏ", "Su hào Đà Lạt");
                highlight = "Tháng 12 đón mùa lễ hội và chuẩn bị Tết, nông sản rực rỡ với Quýt hồng Lai Vung, Bưởi Tết và rau củ mùa lạnh đạt độ ngọt tự nhiên cao nhất.";
                upcoming = List.of("Dưa hấu ruột đỏ", "Mãng cầu chưng Tết", "Cam Canh");
                break;
        }

        // Tạo câu chào & ghi chú phân biệt rõ ràng
        String messageTitle;
        String note = null;

        if (isCurrentMonth) {
            messageTitle = "Lịch mùa vụ nông sản tươi ngon Tháng " + month + " đang có tại nhà vườn:";
        } else if (isPastMonth) {
            messageTitle = "Lịch mùa vụ nông sản tươi ngon Tháng " + month + " (thời điểm thu hoạch rộ):";
            note = "Lưu ý: Hiện tại đang là Tháng " + currentMonth + ". Nông sản Tháng " + month + " trên đã qua cao điểm mùa hoặc đang vào cuối vụ, bạn vẫn có thể bấm vào từng món để kiểm tra hàng sẵn trên sàn hoặc xem tiếp mùa vụ Tháng " + currentMonth + " nhé!";
        } else {
            messageTitle = "Lịch mùa vụ nông sản dự kiến Tháng " + month + " sắp tới:";
            note = "Lưu ý: Tháng " + month + " là vụ mùa sắp tới. Nhà vườn CapNong đang trong giai đoạn chăm sóc và chuẩn bị thu hoạch những lứa nông sản chất lượng cao.";
        }

        ChatbotMessageResponse.SeasonalCalendarDto dto = ChatbotMessageResponse.SeasonalCalendarDto.builder()
                .currentMonth(String.valueOf(month))
                .inSeasonFruits(fruits)
                .inSeasonVeggies(veggies)
                .inSeasonVegetables(veggies)
                .upcomingProduce(upcoming)
                .highlights(highlight)
                .healthBenefitNote(note != null ? note : highlight)
                .isPastMonth(isPastMonth)
                .isFutureMonth(isFutureMonth)
                .note(note)
                .build();

        return ChatbotMessageResponse.builder()
                .type(AiResponseType.SEASONAL_CALENDAR)
                .message(messageTitle + (note != null ? "\n\n*" + note + "*" : ""))
                .seasonalCalendar(dto)
                .timestamp(LocalDateTime.now())
                .build();
    }

    private int extractMonthFromText(String text, int defaultMonth) {
        if (text == null || text.isBlank()) return defaultMonth;
        String lower = text.toLowerCase().trim();
        Matcher m = Pattern.compile("tháng\\s*(\\d{1,2})").matcher(lower);
        if (m.find()) {
            try {
                int val = Integer.parseInt(m.group(1));
                if (val >= 1 && val <= 12) return val;
            } catch (Exception ignored) {}
        }
        if (lower.contains("tháng sau") || lower.contains("tháng tới")) {
            return (defaultMonth % 12) + 1;
        }
        if (lower.contains("tháng trước") || lower.contains("tháng rồi")) {
            return (defaultMonth - 2 + 12) % 12 + 1;
        }
        if (lower.contains("tháng mười hai")) return 12;
        if (lower.contains("tháng mười một")) return 11;
        if (lower.contains("tháng mười")) return 10;
        if (lower.contains("tháng chín")) return 9;
        if (lower.contains("tháng tám")) return 8;
        if (lower.contains("tháng bảy")) return 7;
        if (lower.contains("tháng sáu")) return 6;
        if (lower.contains("tháng năm")) return 5;
        if (lower.contains("tháng tư") || lower.contains("tháng bốn")) return 4;
        if (lower.contains("tháng ba")) return 3;
        if (lower.contains("tháng hai")) return 2;
        if (lower.contains("tháng một") || lower.contains("tháng giêng")) return 1;

        return defaultMonth;
    }

    // Case 8.1: Tra cứu Dấu chân xanh & kg giải cứu - DỮ LIỆU THẬT 100% CỦA TÀI KHOẢN
    @Override
    public ChatbotMessageResponse getGreenImpact(Integer userId) {
        if (userId == null) {
            return ChatbotMessageResponse.builder()
                    .type(AiResponseType.NAVIGATION)
                    .message("Bạn hãy đăng nhập tài khoản để xem thành tích giải cứu nông sản và điểm xanh của mình nhé!")
                    .navigation(ChatbotMessageResponse.NavigationDto.builder().url("/login").label("Đăng nhập").build())
                    .timestamp(LocalDateTime.now())
                    .build();
        }

        User user = userRepository.findById(userId).orElse(null);
        BigDecimal rescued = (user != null && user.getTotalRescuedWeight() != null)
                ? user.getTotalRescuedWeight()
                : BigDecimal.ZERO;

        BigDecimal points = (user != null && user.getLoyaltyPoints() != null)
                ? user.getLoyaltyPoints()
                : BigDecimal.ZERO;

        // Tính toán khí thải giảm thiểu: 1.8 kg CO2 trên mỗi 1 kg nông sản giải cứu (Chuẩn FAO)
        BigDecimal co2 = rescued.multiply(BigDecimal.valueOf(1.8)).setScale(1, RoundingMode.HALF_UP);

        // Quy đổi cây xanh tương đương: 10kg nông sản giải cứu ~ 1 cây xanh bảo vệ
        int trees = rescued.divide(BigDecimal.valueOf(10), RoundingMode.HALF_UP).intValue();

        // Cấp bậc danh hiệu xanh khớp 100% với hệ thống CapNong (như hiển thị trên Hồ sơ cá nhân)
        String ecoRank;
        String impactMsg;
        if (rescued.compareTo(BigDecimal.ZERO) == 0) {
            ecoRank = "Người Gieo Mầm 🌱";
            impactMsg = "Bạn hiện đang ở cấp bậc Người Gieo Mầm (0 kg nông sản). Hãy chọn mua nông sản xấu mã tươi ngon để cùng CapNong bắt đầu hành trình dấu chân xanh bảo vệ môi trường nhé!";
        } else if (rescued.compareTo(BigDecimal.valueOf(5)) < 0) {
            ecoRank = "Bạn Của Nông Dân 🌿";
            impactMsg = "Tuyệt vời! Bạn đã giải cứu " + rescued + "kg nông sản và bắt đầu tạo tác động xanh tích cực cho môi trường.";
        } else if (rescued.compareTo(BigDecimal.valueOf(20)) < 0) {
            ecoRank = "Chiến Binh Nông Sản Xanh 🥦";
            impactMsg = "Tuyệt đỉnh! Dấu chân xanh của bạn đã giúp giảm thải " + co2 + "kg CO₂ và hỗ trợ bà con nông dân thu hồi vốn.";
        } else {
            ecoRank = "Cứu Tinh Nông Sản 🏆";
            impactMsg = "Xuất sắc! Bạn là Cứu Tinh Nông Sản với thành tích giải cứu " + rescued + "kg thực phẩm sạch!";
        }

        String share = "Tôi đã đồng hành cùng CapNong giải cứu " + rescued + "kg nông sản, giảm thiểu " + co2 + "kg CO₂ ra môi trường!";

        ChatbotMessageResponse.GreenImpactDto dto = ChatbotMessageResponse.GreenImpactDto.builder()
                .totalRescuedWeight(rescued)
                .totalRescuedKg(rescued)
                .co2SavedKg(co2)
                .totalCo2SavedKg(co2)
                .loyaltyPoints(points)
                .treesEquivalent(trees)
                .badgeLevel(ecoRank)
                .ecoRank(ecoRank)
                .impactMessage(impactMsg)
                .shareMessage(share)
                .build();

        return ChatbotMessageResponse.builder()
                .type(AiResponseType.GREEN_IMPACT)
                .message("Thành tích 'Dấu chân xanh' của bạn cùng CapNong:")
                .greenImpact(dto)
                .timestamp(LocalDateTime.now())
                .build();
    }

    // Case 9.1: Hỗ trợ thanh toán PayOS / chuyển khoản
    @Override
    public ChatbotMessageResponse assistPayment(Integer orderId, Integer userId) {
        Order order = null;
        if (userId != null) {
            if (orderId != null) {
                order = ownedOrder(orderId, userId).orElse(null);
                if (order == null) {
                    return orderNotFound(orderId);
                }
            } else {
                Page<Order> orders = orderRepository.findByBuyer_Id(userId, PageRequest.of(0, 1, Sort.by(Sort.Direction.DESC, "createdAt")));
                order = orders.hasContent() ? orders.getContent().get(0) : null;
            }
        }

        ChatbotMessageResponse.PaymentAssistanceDto dto = ChatbotMessageResponse.PaymentAssistanceDto.builder()
                .orderId(order != null ? order.getOrderId() : null)
                .paymentMethod(order != null ? order.getPaymentMethod() : null)
                .paymentStatus(order != null && order.getStatus() != null ? order.getStatus().name() : null)
                .guidance("Đơn thanh toán qua PayOS sẽ tự cập nhật khi hệ thống nhận được xác nhận giao dịch. Nếu ngân hàng đã trừ tiền mà đơn vẫn chưa cập nhật sau một lúc, bạn hãy liên hệ bộ phận hỗ trợ và cung cấp mã đơn cùng ảnh giao dịch.")
                .supportHotline(supportProperties.getHotline())
                .build();

        return ChatbotMessageResponse.builder()
                .type(AiResponseType.PAYMENT_ASSISTANCE)
                .message("Hỗ trợ thanh toán trực tuyến PayOS & Chuyển khoản:")
                .paymentAssistance(dto)
                .timestamp(LocalDateTime.now())
                .build();
    }

    // Case 9.2: Tra cứu ví CapNong an toàn
    @Override
    public ChatbotMessageResponse getWalletInfo(Integer userId) {
        if (userId == null) {
            return ChatbotMessageResponse.builder()
                    .type(AiResponseType.NAVIGATION)
                    .message("Vui lòng đăng nhập để kiểm tra thông tin ví tiền CapNong của bạn nhé!")
                    .navigation(ChatbotMessageResponse.NavigationDto.builder().url("/login").label("Đăng nhập").build())
                    .timestamp(LocalDateTime.now())
                    .build();
        }

        User user = userRepository.findById(userId).orElse(null);
        String type = user != null ? walletTypeOf(user) : null;
        if (type == null) {
            return ChatbotMessageResponse.builder()
                    .type(AiResponseType.NAVIGATION)
                    .message("Ví của Quản trị viên là ví Sàn, bạn xem trong trang quản trị nhé.")
                    .navigation(ChatbotMessageResponse.NavigationDto.builder().url("/admin/wallet").label("Mở ví Sàn").build())
                    .timestamp(LocalDateTime.now())
                    .build();
        }

        String bank = user.getBankName() != null && !user.getBankName().isBlank() ? user.getBankName() : null;
        String acc = user.getBankAccount() != null && user.getBankAccount().length() >= 4
                ? "**** " + user.getBankAccount().substring(user.getBankAccount().length() - 4)
                : null;

        // Chỉ đọc: chưa có ví thì coi như số dư 0, không tạo ví từ chatbot.
        Wallet wallet = walletRepository.findFirstByUser_IdAndType(userId, type).orElse(null);
        BigDecimal balance = wallet != null && wallet.getTotalBalance() != null ? wallet.getTotalBalance() : BigDecimal.ZERO;
        BigDecimal frozen = wallet != null && wallet.getFrozenBalance() != null ? wallet.getFrozenBalance() : BigDecimal.ZERO;
        List<ChatbotMessageResponse.WalletTransactionDto> recent = wallet == null ? List.of()
                : walletTransactionRepository
                        .findByWallet_WalletIdOrderByCreatedAtDescIdDesc(wallet.getWalletId(), PageRequest.of(0, 3))
                        .stream()
                        .map(t -> ChatbotMessageResponse.WalletTransactionDto.builder()
                                .description(t.getDescription())
                                .amount(t.getAmount())
                                .hold(t.getType() != null && t.getType().name().contains("HOLD"))
                                .createdAt(t.getCreatedAt())
                                .build())
                        .toList();

        String message;
        if (wallet != null && wallet.getStatus() == WalletStatus.FROZEN) {
            message = "Ví đang bị tạm khóa nên chưa rút được tiền. Bạn liên hệ CapNong để được hỗ trợ nhé.";
        } else if ("SHIPPER".equals(type) && balance.signum() < 0) {
            message = "Số dư âm vì bạn đang giữ tiền mặt COD chưa nộp về sàn. Số dư trở lại bình thường khi CapNong xác nhận đã nhận tiền.";
        } else if ("BUYER".equals(type)) {
            message = "Ví nhận tiền hoàn từ các yêu cầu trả hàng. Bạn có thể rút số dư về tài khoản ngân hàng trong Ví của tôi.";
        } else {
            message = "Tiền tạm giữ được cộng vào số dư khi đơn hoàn tất. Bạn có thể rút số dư về tài khoản ngân hàng trong Ví của tôi.";
        }

        ChatbotMessageResponse.WalletInfoDto dto = ChatbotMessageResponse.WalletInfoDto.builder()
                .walletStatus(walletStatusLabel(wallet))
                .walletType(type)
                .balance(balance)
                .frozenBalance(frozen)
                .totalWithdrawn(wallet != null && wallet.getTotalWithdrawn() != null ? wallet.getTotalWithdrawn() : BigDecimal.ZERO)
                .recentTransactions(recent)
                .loyaltyPoints("BUYER".equals(type) && user.getLoyaltyPoints() != null ? user.getLoyaltyPoints() : null)
                .linkedBankName(bank)
                .linkedBankAccount(acc)
                .message(message)
                .navigationUrl("/my-wallet")
                .navigationLabel("Mở Ví của tôi")
                .build();

        return ChatbotMessageResponse.builder()
                .type(AiResponseType.WALLET_INFO)
                .message("Thông tin Ví CapNong của bạn:")
                .walletInfo(dto)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /** Cùng cách chọn loại ví với WalletServiceImpl.getMyWallet; null với Admin (ví Sàn). */
    private static String walletTypeOf(User user) {
        String role = user.getRole() != null && user.getRole().getName() != null ? user.getRole().getName().toUpperCase() : "";
        return switch (role) {
            case "SHIPPER" -> "SHIPPER";
            case "SHOP" -> "SHOP";
            case "BUYER", "USER" -> "BUYER";
            default -> null;
        };
    }

    private static String walletStatusLabel(Wallet wallet) {
        if (wallet == null) return "Chưa có giao dịch";
        if (wallet.getStatus() == WalletStatus.FROZEN) return "Tạm khóa";
        if (wallet.getStatus() == WalletStatus.INACTIVE) return "Ngừng hoạt động";
        return "Đang hoạt động";
    }

    // Case 10.1: Đổi thông tin nhận hàng khẩn cấp (Địa chỉ / Số điện thoại)
    @Override
    public ChatbotMessageResponse handleUpdateShippingInfo(Integer orderId, Integer userId) {
        return handleUpdateShippingInfo(orderId, null, userId);
    }

    @Override
    public ChatbotMessageResponse handleUpdateShippingInfo(Integer orderId, String userMessage, Integer userId) {
        if (userId == null) {
            return ChatbotMessageResponse.builder()
                    .type(AiResponseType.NAVIGATION)
                    .message("Bạn cần đăng nhập để thao tác cập nhật thông tin đơn hàng nhé!")
                    .navigation(ChatbotMessageResponse.NavigationDto.builder().url("/login").label("Đăng nhập").build())
                    .timestamp(LocalDateTime.now())
                    .build();
        }

        Order targetOrder = null;
        if (orderId != null) {
            targetOrder = ownedOrder(orderId, userId).orElse(null);
            if (targetOrder == null) {
                return orderNotFound(orderId);
            }
        } else {
            Page<Order> orders = orderRepository.findByBuyer_Id(userId, PageRequest.of(0, 1, Sort.by(Sort.Direction.DESC, "createdAt")));
            if (orders.hasContent()) {
                targetOrder = orders.getContent().get(0);
            }
        }

        if (targetOrder == null) {
            return ChatbotMessageResponse.text("Không tìm thấy đơn hàng cần đổi thông tin. Bạn hãy kiểm tra lại trong mục 'Đơn hàng của tôi' nhé!");
        }

        String msgLower = (userMessage != null) ? userMessage.toLowerCase().trim() : "";
        boolean isPhoneRequest = msgLower.contains("số điện thoại") || msgLower.contains("sđt")
                || msgLower.contains("sdt") || msgLower.contains("điện thoại") || msgLower.contains("số người nhận");

        String shipperPhone = (targetOrder.getShipper() != null) ? targetOrder.getShipper().getPhoneNumber() : null;
        String shipperName = (targetOrder.getShipper() != null) ? targetOrder.getShipper().getFullName() : null;
        String updateType = isPhoneRequest ? "PHONE" : "ADDRESS";
        String statusLabel = (targetOrder.getStatus() != null) ? targetOrder.getStatus().name() : "";

        String note;
        String messageTitle;

        if (isPhoneRequest) {
            messageTitle = "Thông tin liên hệ & Số điện thoại Đơn hàng #" + targetOrder.getOrderId() + ":";
            if (targetOrder.getShipper() != null || targetOrder.getStatus() == OrderStatus.SHIPPING) {
                note = "Hệ thống không thể sửa trực tiếp số điện thoại trên app khi đơn đã xuất vận đơn. Do đơn hàng #" + targetOrder.getOrderId() + " đang được Shipper" + (shipperName != null ? " " + shipperName : "") + " vận chuyển, bạn hãy liên hệ trực tiếp với tài xế qua SĐT " + (shipperPhone != null ? shipperPhone : "của tài xế") + " để thoả thuận số điện thoại mới khi nhận hàng nhé!";
            } else {
                note = "Hệ thống không hỗ trợ tự sửa số điện thoại trên đơn hàng đã chốt để tránh xung đột dữ liệu vận chuyển. Bạn vui lòng liên hệ bộ phận hỗ trợ để được ghi chú số điện thoại mới cho tài xế khi nhận đơn, hoặc Hủy đơn trong mục 'Đơn hàng của tôi' để đặt lại nhé!";
            }
        } else {
            messageTitle = "Thông tin giao nhận Đơn hàng #" + targetOrder.getOrderId() + ":";
            if (targetOrder.getStatus() == OrderStatus.PENDING || targetOrder.getStatus() == OrderStatus.CONFIRMED) {
                note = "Đơn hàng #" + targetOrder.getOrderId() + " đã được tính phí vận chuyển theo khoảng cách giữa nhà vườn và địa chỉ nhận. Nếu cần đổi địa chỉ, bạn có thể Hủy đơn trong mục 'Đơn hàng của tôi' để đặt lại với địa chỉ mới, hoặc liên hệ bộ phận hỗ trợ nhé!";
            } else {
                note = "Đơn hàng #" + targetOrder.getOrderId() + " đang được chuẩn bị hoặc vận chuyển. Bạn vui lòng liên hệ bộ phận hỗ trợ" + (shipperPhone != null ? " hoặc gọi trực tiếp tài xế qua SĐT " + shipperPhone : "") + " để thoả thuận nhận hàng linh hoạt nhé!";
            }
        }

        ChatbotMessageResponse.UpdateShippingInfoDto dto = ChatbotMessageResponse.UpdateShippingInfoDto.builder()
                .orderId(targetOrder.getOrderId())
                .currentAddress(targetOrder.getShippingAddress())
                .currentPhone(targetOrder.getRecipientPhone())
                .canUpdate(false)
                .updateType(updateType)
                .shipperName(shipperName)
                .shipperPhone(shipperPhone)
                .orderStatus(statusLabel)
                .guidance(note)
                .note(note)
                .build();

        return ChatbotMessageResponse.builder()
                .type(AiResponseType.UPDATE_SHIPPING_INFO)
                .message(messageTitle)
                .updateShippingInfo(dto)
                .timestamp(LocalDateTime.now())
                .build();
    }

    private String formatCurrency(BigDecimal amount) {
        if (amount == null) return "0 đ";
        return String.format("%,d đ", amount.longValue()).replace(",", ".");
    }
}
