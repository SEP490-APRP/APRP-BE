package com.sang.be_capnong.service;

public interface ImageDownloadService {

    /**
     * Tải mảng byte của hình ảnh từ URL với cơ chế bảo vệ SSRF và giới hạn kích thước an toàn.
     *
     * @param imageUrl Đường dẫn URL của hình ảnh
     * @return Mảng byte của hình ảnh
     */
    byte[] download(String imageUrl);
}
