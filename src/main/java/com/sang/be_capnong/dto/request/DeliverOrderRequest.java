package com.sang.be_capnong.dto.request;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.util.List;

/** Shipper xác nhận giao thành công: bắt buộc ít nhất 1 ảnh (upload trước qua POST /upload) */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DeliverOrderRequest {
    List<String> proofImageUrls;
}
