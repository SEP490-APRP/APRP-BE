package com.sang.be_capnong.mapper;

import com.sang.be_capnong.dto.response.ModerationLabelResponse;
import com.sang.be_capnong.dto.response.ModerationResultResponse;
import com.sang.be_capnong.entity.ModerationLabel;
import com.sang.be_capnong.entity.ModerationResult;
import com.sang.be_capnong.enums.AgriculturalProduceLabel;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ModerationResultMapper {

    ModerationResultResponse toModerationResultResponse(ModerationResult moderationResult);

    @Mapping(target = "labelNameVi", source = "labelName", qualifiedByName = "toVietnameseLabel")
    @Mapping(target = "parentLabelVi", source = "parentLabel", qualifiedByName = "toVietnameseLabel")
    ModerationLabelResponse toModerationLabelResponse(ModerationLabel moderationLabel);

    List<ModerationLabelResponse> toModerationLabelResponseList(List<ModerationLabel> moderationLabels);

    @Named("toVietnameseLabel")
    default String toVietnameseLabel(String labelName) {
        if (labelName == null) {
            return null;
        }
        return AgriculturalProduceLabel.getVietnameseNameOrDefault(labelName);
    }
}
