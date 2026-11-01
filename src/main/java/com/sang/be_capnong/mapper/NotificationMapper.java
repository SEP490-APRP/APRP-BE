package com.sang.be_capnong.mapper;

import com.sang.be_capnong.dto.response.NotificationResponse;
import com.sang.be_capnong.entity.Notification;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface NotificationMapper {

    @Mapping(source = "notiId", target = "id")
    @Mapping(source = "notiId", target = "notiId")
    @Mapping(source = "admin.id", target = "adminId")
    @Mapping(source = "admin.fullName", target = "adminName")
    @Mapping(target = "isRead", ignore = true)
    NotificationResponse toResponse(Notification notification);
}
