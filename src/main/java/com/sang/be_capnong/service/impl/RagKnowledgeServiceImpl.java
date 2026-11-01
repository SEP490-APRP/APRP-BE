package com.sang.be_capnong.service.impl;

import com.sang.be_capnong.entity.BotKnowledge;
import com.sang.be_capnong.entity.MysteryBox;
import com.sang.be_capnong.entity.Product;
import com.sang.be_capnong.repository.BotKnowledgeRepository;
import com.sang.be_capnong.repository.MysteryBoxRepository;
import com.sang.be_capnong.repository.ProductRepository;
import com.sang.be_capnong.service.RagKnowledgeService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class RagKnowledgeServiceImpl implements RagKnowledgeService {

    BotKnowledgeRepository botKnowledgeRepository;
    ProductRepository productRepository;
    MysteryBoxRepository mysteryBoxRepository;

    private static final int MAX_KNOWLEDGE_CHUNKS = 10;
    private static final List<String> STOP_WORDS = List.of(
            "la", "gi", "co", "khong", "the", "nao", "duoc", "thi", "va",
            "cua", "trong", "voi", "nhu", "cho", "khi", "ma", "hay", "se",
            "da", "roi", "biet", "hieu", "noi", "ve", "moi", "tat", "ca",
            "nhung", "cac", "mot", "hai", "ba", "do", "ay", "nay", "kia",
            "sao", "dau", "di", "len", "xuong", "vao", "ra", "tu", "ban",
            "minh", "toi", "ho", "no", "ai", "muon", "can", "het", "them"
    );

    @Override
    @Transactional(readOnly = true)
    public String buildKnowledgeContext(String userMessage) {
        if (userMessage == null || userMessage.isBlank()) {
            return "";
        }

        List<String> keywords = extractKeywords(userMessage);
        Map<Integer, BotKnowledge> matchedMap = new LinkedHashMap<>();

        if (keywords.isEmpty()) {
            botKnowledgeRepository.findByActiveTrue().forEach(k -> matchedMap.put(k.getId(), k));
        } else {
            for (String kw : keywords) {
                List<BotKnowledge> found = botKnowledgeRepository.searchByKeyword(kw);
                found.forEach(k -> matchedMap.put(k.getId(), k));
            }
        }

        List<BotKnowledge> matched = matchedMap.values().stream()
                .limit(MAX_KNOWLEDGE_CHUNKS)
                .toList();

        if (matched.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("TÀI LIỆU QUY ĐỊNH & KIẾN THỨC NỀN TẢNG (CAPNONG KNOWLEDGE):\n");
        for (BotKnowledge k : matched) {
            sb.append("[").append(k.getType()).append("] ").append(k.getTitle()).append(":\n");
            sb.append(k.getContent()).append("\n\n");
        }
        return sb.toString().trim();
    }

    @Override
    @Transactional(readOnly = true)
    public String buildCatalogContext() {
        try {
            List<Product> products = productRepository.findAll().stream()
                    .filter(p -> "ACTIVE".equalsIgnoreCase(p.getStatus()))
                    .filter(p -> p.getStockQuantity() != null && p.getStockQuantity() > 0)
                    .toList();

            List<MysteryBox> mysteryBoxes = mysteryBoxRepository.findAll().stream()
                    .filter(mb -> Boolean.TRUE.equals(mb.getIsActive()))
                    .filter(mb -> mb.getTotalQuantity() != null && mb.getSoldQuantity() != null && (mb.getTotalQuantity() - mb.getSoldQuantity() > 0))
                    .toList();

            if (products.isEmpty() && mysteryBoxes.isEmpty()) {
                return "";
            }

            StringBuilder sb = new StringBuilder();
            sb.append("DANH MỤC NÔNG SẢN & HỘP BÍ MẬT ĐANG CÓ HÀNG SẴN TRÊN SÀN:\n");

            Map<String, List<Product>> byCategory = products.stream()
                    .collect(Collectors.groupingBy(
                            p -> p.getCategory() != null ? p.getCategory().getName() : "Khác",
                            LinkedHashMap::new,
                            Collectors.toList()
                    ));

            byCategory.forEach((cat, prods) -> {
                sb.append("- Nhóm ").append(cat).append(":\n");
                prods.forEach(p -> {
                    sb.append("  + ").append(p.getProductName())
                            .append(" (Giá: ").append(p.getSellingPrice()).append(" đ/").append(p.getUnit() != null ? p.getUnit() : "kg")
                            .append(", còn: ").append(p.getStockQuantity()).append(")\n");
                });
            });

            if (!mysteryBoxes.isEmpty()) {
                sb.append("- Nhóm Hộp Bí Mật (Mystery Box):\n");
                mysteryBoxes.forEach(mb -> {
                    int remain = mb.getTotalQuantity() - mb.getSoldQuantity();
                    sb.append("  + ").append(mb.getBoxType())
                            .append(" (Giá: ").append(mb.getPrice()).append(" đ, còn: ").append(remain).append(" hộp)\n");
                });
            }

            return sb.toString().trim();
        } catch (Exception e) {
            log.warn("Failed to build catalog context for chatbot: {}", e.getMessage());
            return "";
        }
    }

    private List<String> extractKeywords(String text) {
        String normalized = normalize(text);
        String[] words = normalized.split("\\s+");

        return Arrays.stream(words)
                .filter(w -> w.length() > 2)
                .filter(w -> !STOP_WORDS.contains(w))
                .distinct()
                .toList();
    }

    private String normalize(String text) {
        if (text == null) return "";
        String temp = Normalizer.normalize(text, Normalizer.Form.NFD);
        return Pattern.compile("\\p{InCombiningDiacriticalMarks}+")
                .matcher(temp)
                .replaceAll("")
                .toLowerCase()
                .replaceAll("[^a-z0-9\\s]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }
}
