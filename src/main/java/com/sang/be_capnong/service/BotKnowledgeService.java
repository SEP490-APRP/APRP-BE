package com.sang.be_capnong.service;

import com.sang.be_capnong.dto.request.BotKnowledgeRequest;
import com.sang.be_capnong.dto.response.BotKnowledgeResponse;

import java.util.List;

public interface BotKnowledgeService {
    BotKnowledgeResponse create(BotKnowledgeRequest request);
    BotKnowledgeResponse update(int id, BotKnowledgeRequest request);
    void delete(int id);
    BotKnowledgeResponse getById(int id);
    List<BotKnowledgeResponse> getAll();
    List<BotKnowledgeResponse> getByType(String type);
}
