package com.sang.be_capnong.config;

import com.sang.be_capnong.entity.User;
import com.sang.be_capnong.enums.UserStatus;
import com.sang.be_capnong.repository.UserRepository;
import com.sang.be_capnong.service.GoogleMapService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Shop cũ chưa có toạ độ: lấy toạ độ từ địa chỉ để tính "shop lân cận" khi đề xuất chiến dịch.
 * Chạy nền sau khi khởi động, tối đa 100 shop mỗi lần, lỗi thì bỏ qua (shop tự ghim lại trên bản đồ).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ShopLocationBackfill implements ApplicationRunner {

    private final UserRepository userRepository;
    private final GoogleMapService googleMapService;

    @Value("${capnong.shop-location-backfill.enabled:true}")
    private boolean enabled;

    @Override
    public void run(ApplicationArguments args) {
        if (!enabled) return;
        Thread t = new Thread(this::backfill, "shop-location-backfill");
        t.setDaemon(true);
        t.start();
    }

    private void backfill() {
        try {
            List<User> shops = userRepository.findByRole_NameInAndStatus(List.of("SHOP"), UserStatus.ACTIVE).stream()
                    .filter(u -> u.getShopLatitude() == null && u.getAddress() != null && !u.getAddress().isBlank())
                    .limit(100).toList();
            int done = 0;
            for (User u : shops) {
                try {
                    double[] ll = googleMapService.geocodeAddress(u.getAddress());
                    if (ll == null) continue;
                    User fresh = userRepository.findById(u.getId()).orElse(null);
                    if (fresh == null || fresh.getShopLatitude() != null) continue;
                    fresh.setShopLatitude(ll[0]);
                    fresh.setShopLongitude(ll[1]);
                    userRepository.save(fresh);
                    done++;
                } catch (Exception e) {
                    log.debug("[ShopLocationBackfill] shop #{}: {}", u.getId(), e.getMessage());
                }
            }
            if (done > 0) log.info("[ShopLocationBackfill] Geocoded {} shops", done);
        } catch (Exception e) {
            log.warn("[ShopLocationBackfill] skipped: {}", e.getMessage());
        }
    }
}
