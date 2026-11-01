package com.sang.be_capnong.service;

public interface RagKnowledgeService {
    String buildKnowledgeContext(String userMessage);
    String buildCatalogContext();

    default String buildRagContext(String userMessage) {
        String k = buildKnowledgeContext(userMessage);
        String c = buildCatalogContext();
        return (k != null ? k : "") + "\n\n" + (c != null ? c : "");
    }
}
