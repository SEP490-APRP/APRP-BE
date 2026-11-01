package com.sang.be_capnong.service.impl;

import com.sang.be_capnong.dto.request.BotKnowledgeRequest;
import com.sang.be_capnong.dto.response.BotKnowledgeResponse;
import com.sang.be_capnong.entity.BotKnowledge;
import com.sang.be_capnong.exception.AppException;
import com.sang.be_capnong.exception.ErrorCode;
import com.sang.be_capnong.repository.BotKnowledgeRepository;
import com.sang.be_capnong.service.BotKnowledgeService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class BotKnowledgeServiceImpl implements BotKnowledgeService {

    BotKnowledgeRepository botKnowledgeRepository;

    @Override
    @Transactional
    public BotKnowledgeResponse create(BotKnowledgeRequest request) {
        BotKnowledge entity = BotKnowledge.builder()
                .type(request.getType() != null ? request.getType().trim().toUpperCase() : "GENERAL")
                .title(request.getTitle().trim())
                .content(request.getContent().trim())
                .keywords(request.getKeywords() != null ? request.getKeywords().trim() : "")
                .active(request.getActive() != null ? request.getActive() : true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        return toResponse(botKnowledgeRepository.save(entity));
    }

    @Override
    @Transactional
    public BotKnowledgeResponse update(int id, BotKnowledgeRequest request) {
        BotKnowledge entity = botKnowledgeRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.BOT_KNOWLEDGE_NOT_FOUND));

        if (request.getType() != null) entity.setType(request.getType().trim().toUpperCase());
        if (request.getTitle() != null) entity.setTitle(request.getTitle().trim());
        if (request.getContent() != null) entity.setContent(request.getContent().trim());
        if (request.getKeywords() != null) entity.setKeywords(request.getKeywords().trim());
        if (request.getActive() != null) entity.setActive(request.getActive());
        entity.setUpdatedAt(LocalDateTime.now());

        return toResponse(botKnowledgeRepository.save(entity));
    }

    @Override
    @Transactional
    public void delete(int id) {
        if (!botKnowledgeRepository.existsById(id)) {
            throw new AppException(ErrorCode.BOT_KNOWLEDGE_NOT_FOUND);
        }
        botKnowledgeRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public BotKnowledgeResponse getById(int id) {
        BotKnowledge entity = botKnowledgeRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.BOT_KNOWLEDGE_NOT_FOUND));
        return toResponse(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BotKnowledgeResponse> getAll() {
        return botKnowledgeRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<BotKnowledgeResponse> getByType(String type) {
        return botKnowledgeRepository.findByTypeIgnoreCaseAndActiveTrue(type).stream()
                .map(this::toResponse)
                .toList();
    }

    private BotKnowledgeResponse toResponse(BotKnowledge k) {
        return BotKnowledgeResponse.builder()
                .id(k.getId())
                .type(k.getType())
                .title(k.getTitle())
                .content(k.getContent())
                .keywords(k.getKeywords())
                .active(k.getActive())
                .createdAt(k.getCreatedAt())
                .updatedAt(k.getUpdatedAt())
                .build();
    }
}
