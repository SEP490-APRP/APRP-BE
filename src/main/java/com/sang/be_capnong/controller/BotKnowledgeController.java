package com.sang.be_capnong.controller;

import com.sang.be_capnong.dto.request.BotKnowledgeRequest;
import com.sang.be_capnong.dto.response.ApiResponse;
import com.sang.be_capnong.dto.response.BotKnowledgeResponse;
import com.sang.be_capnong.service.BotKnowledgeService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"/api/v1/admin/bot-knowledge", "/admin/bot-knowledge"})
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class BotKnowledgeController {

    BotKnowledgeService botKnowledgeService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<BotKnowledgeResponse>> getAllKnowledge(
            @RequestParam(name = "type", required = false) String type
    ) {
        List<BotKnowledgeResponse> list = (type != null && !type.trim().isEmpty())
                ? botKnowledgeService.getByType(type.trim())
                : botKnowledgeService.getAll();
        return ApiResponse.<List<BotKnowledgeResponse>>builder()
                .result(list)
                .build();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<BotKnowledgeResponse> getKnowledgeById(@PathVariable("id") int id) {
        return ApiResponse.<BotKnowledgeResponse>builder()
                .result(botKnowledgeService.getById(id))
                .build();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<BotKnowledgeResponse> createKnowledge(@RequestBody @Valid BotKnowledgeRequest request) {
        return ApiResponse.<BotKnowledgeResponse>builder()
                .result(botKnowledgeService.create(request))
                .build();
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<BotKnowledgeResponse> updateKnowledge(
            @PathVariable("id") int id,
            @RequestBody @Valid BotKnowledgeRequest request
    ) {
        return ApiResponse.<BotKnowledgeResponse>builder()
                .result(botKnowledgeService.update(id, request))
                .build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> deleteKnowledge(@PathVariable("id") int id) {
        botKnowledgeService.delete(id);
        return ApiResponse.<Void>builder()
                .message("Đã xóa kiến thức thành công.")
                .build();
    }
}
