package com.sang.be_capnong;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class BE_CapNongTests {

    @org.springframework.beans.factory.annotation.Autowired
    private com.sang.be_capnong.service.ProductService productService;

    @Test
    void testGetProducts() {
        var res = productService.getProducts(null, null, "ACTIVE", 1, 10, "productId", "desc");
        System.out.println(">>> FOUND PRODUCTS COUNT: " + res.getContent().size());
        for (var p : res.getContent()) {
            System.out.println(">>> P: " + p.getProductId() + " - " + p.getProductName() + " - " + p.getSellingPrice());
        }

        var detail = productService.getProductById(101);
        System.out.println(">>> DETAIL 101: " + detail.getProductName() + " - Shop: " + (detail.getShop() != null ? detail.getShop().getShopName() : "null") + " - Images count: " + (detail.getImages() != null ? detail.getImages().size() : 0));
    }

}
