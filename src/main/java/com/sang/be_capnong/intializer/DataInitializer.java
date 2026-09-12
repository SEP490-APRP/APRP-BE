package com.sang.be_capnong.intializer;

import com.sang.be_capnong.entity.Role;
import com.sang.be_capnong.repository.RoleRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class DataInitializer {

    RoleRepository roleRepository;

    @Bean
    public ApplicationRunner initializeSystemRoles() {
        return args -> {
            log.info("Checking and initializing system roles...");

            createRoleIfNotFound("BUYER", "Role dành cho Người mua hàng");
            createRoleIfNotFound("SHOP", "Role dành cho Cửa hàng / Người bán");
            createRoleIfNotFound("SHIPPER", "Role dành cho Người giao hàng");
            createRoleIfNotFound("ADMIN", "Role dành cho Quản trị viên hệ thống");
        };
    }

    private void createRoleIfNotFound(String name, String description) {
        if (!roleRepository.existsByName(name)) {
            Role role = Role.builder()
                    .name(name)
                    .description(description)
                    .build();
            roleRepository.save(role);
            log.info("Initialized role: {}", name);
        }
    }
}
