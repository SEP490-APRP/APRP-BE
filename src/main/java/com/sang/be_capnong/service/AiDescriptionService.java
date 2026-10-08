package com.sang.be_capnong.service;

import com.sang.be_capnong.dto.request.AiDescriptionSuggestionRequest;
import com.sang.be_capnong.dto.response.AiDescriptionSuggestionResponse;

public interface AiDescriptionService {
    AiDescriptionSuggestionResponse suggestDescription(AiDescriptionSuggestionRequest request);
}
