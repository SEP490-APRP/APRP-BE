package com.sang.be_capnong.mapper;

import com.sang.be_capnong.dto.response.ReturnRequestResponse;
import com.sang.be_capnong.entity.Image;
import com.sang.be_capnong.entity.OrderDetail;
import com.sang.be_capnong.entity.ReturnRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

@Mapper(componentModel = "spring")
public interface ReturnRequestMapper {

    @Mapping(target = "id", source = "returnId")
    @Mapping(target = "returnId", source = "returnId")
    @Mapping(target = "orderId", source = "orderDetail.order.orderId")
    @Mapping(target = "orderDetailId", source = "orderDetail.orderDetailId")
    @Mapping(target = "productName", source = "orderDetail", qualifiedByName = "mapProductName")
    @Mapping(target = "productImage", source = "orderDetail", qualifiedByName = "mapProductImage")
    @Mapping(target = "itemType", source = "orderDetail.itemType")
    @Mapping(target = "quantity", source = "orderDetail.quantity")
    @Mapping(target = "unitPrice", source = "orderDetail.unitPrice")
    @Mapping(target = "itemTotal", source = "orderDetail", qualifiedByName = "mapItemTotal")
    @Mapping(target = "buyerId", source = "buyer.id")
    @Mapping(target = "buyerName", source = "request", qualifiedByName = "mapBuyerName")
    @Mapping(target = "buyerPhone", source = "request", qualifiedByName = "mapBuyerPhone")
    @Mapping(target = "buyerAddress", source = "request", qualifiedByName = "mapBuyerAddress")
    @Mapping(target = "buyerLatitude", source = "orderDetail.order.shippingLatitude")
    @Mapping(target = "buyerLongitude", source = "orderDetail.order.shippingLongitude")
    @Mapping(target = "shopId", source = "shop.id")
    @Mapping(target = "shopName", source = "shop.shopName")
    @Mapping(target = "shopPhone", source = "shop.phoneNumber")
    @Mapping(target = "shopAddress", source = "shop.address")
    @Mapping(target = "shopLatitude", source = "orderDetail.order.shopLatitude")
    @Mapping(target = "shopLongitude", source = "orderDetail.order.shopLongitude")
    @Mapping(target = "shipperId", source = "shipper.id")
    @Mapping(target = "shipperName", source = "shipper.fullName")
    @Mapping(target = "shipperPhone", source = "shipper.phoneNumber")
    @Mapping(target = "evidence", source = "images", qualifiedByName = "mapFirstEvidence")
    @Mapping(target = "evidenceUrls", source = "images", qualifiedByName = "mapEvidenceUrls")
    ReturnRequestResponse toResponse(ReturnRequest request);

    List<ReturnRequestResponse> toResponseList(List<ReturnRequest> requests);

    @Named("mapProductName")
    default String mapProductName(OrderDetail detail) {
        if (detail == null) return null;
        if (detail.getProduct() != null) return detail.getProduct().getProductName();
        if (detail.getMysteryBox() != null) {
            return detail.getMysteryBox().getBoxType() != null ? detail.getMysteryBox().getBoxType() : "Hộp bất ngờ";
        }
        if (detail.getBuildCombo() != null) {
            return detail.getBuildCombo().getComboName() != null ? detail.getBuildCombo().getComboName() : "Combo nông sản";
        }
        return "Sản phẩm";
    }

    @Named("mapProductImage")
    default String mapProductImage(OrderDetail detail) {
        if (detail == null) return null;
        if (detail.getProduct() != null) {
            if (detail.getProduct().getImages() != null && !detail.getProduct().getImages().isEmpty()) {
                return detail.getProduct().getImages().get(0).getImageUrl();
            }
        }
        if (detail.getMysteryBox() != null) {
            return detail.getMysteryBox().getImageUrl();
        }
        if (detail.getBuildCombo() != null) {
            return detail.getBuildCombo().getImageUrl();
        }
        return null;
    }

    @Named("mapItemTotal")
    default BigDecimal mapItemTotal(OrderDetail detail) {
        if (detail == null || detail.getUnitPrice() == null || detail.getQuantity() == null) {
            return BigDecimal.ZERO;
        }
        return detail.getUnitPrice().multiply(BigDecimal.valueOf(detail.getQuantity()));
    }

    @Named("mapFirstEvidence")
    default String mapFirstEvidence(List<Image> images) {
        if (images == null || images.isEmpty()) return null;
        return images.get(0).getImageUrl();
    }

    @Named("mapEvidenceUrls")
    default List<String> mapEvidenceUrls(List<Image> images) {
        if (images == null || images.isEmpty()) return Collections.emptyList();
        return images.stream().map(Image::getImageUrl).toList();
    }

    @Named("mapBuyerName")
    default String mapBuyerName(ReturnRequest request) {
        if (request == null) return null;
        if (request.getOrderDetail() != null && request.getOrderDetail().getOrder() != null) {
            String recipientName = request.getOrderDetail().getOrder().getRecipientName();
            if (recipientName != null && !recipientName.isBlank()) {
                return recipientName;
            }
        }
        return request.getBuyer() != null ? request.getBuyer().getFullName() : null;
    }

    @Named("mapBuyerPhone")
    default String mapBuyerPhone(ReturnRequest request) {
        if (request == null) return null;
        if (request.getOrderDetail() != null && request.getOrderDetail().getOrder() != null) {
            String recipientPhone = request.getOrderDetail().getOrder().getRecipientPhone();
            if (recipientPhone != null && !recipientPhone.isBlank()) {
                return recipientPhone;
            }
        }
        return request.getBuyer() != null ? request.getBuyer().getPhoneNumber() : null;
    }

    @Named("mapBuyerAddress")
    default String mapBuyerAddress(ReturnRequest request) {
        if (request == null) return null;
        if (request.getOrderDetail() != null && request.getOrderDetail().getOrder() != null) {
            String shippingAddress = request.getOrderDetail().getOrder().getShippingAddress();
            if (shippingAddress != null && !shippingAddress.isBlank()) {
                return shippingAddress;
            }
        }
        return request.getBuyer() != null ? request.getBuyer().getAddress() : null;
    }
}
