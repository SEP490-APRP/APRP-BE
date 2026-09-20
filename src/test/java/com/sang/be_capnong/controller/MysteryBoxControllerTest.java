package com.sang.be_capnong.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sang.be_capnong.config.CustomJwtDecoder;
import com.sang.be_capnong.dto.request.MysteryBoxCreationRequest;
import com.sang.be_capnong.dto.request.MysteryBoxUpdateRequest;
import com.sang.be_capnong.dto.request.ProductMysteryItemRequest;
import com.sang.be_capnong.dto.response.MysteryBoxResponse;
import com.sang.be_capnong.dto.response.PageResponse;
import com.sang.be_capnong.dto.response.ProductMysteryResponse;
import com.sang.be_capnong.exception.ErrorCode;
import com.sang.be_capnong.service.MysteryBoxService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MysteryBoxController.class)
@AutoConfigureMockMvc(addFilters = false)
class MysteryBoxControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockBean
    MysteryBoxService mysteryBoxService;

    @MockBean
    CustomJwtDecoder customJwtDecoder;

    MysteryBoxResponse sampleResponse;
    MysteryBoxCreationRequest sampleCreationRequest;

    @BeforeEach
    void setUp() {
        sampleResponse = MysteryBoxResponse.builder()
                .mysteryId(1)
                .shopId(10)
                .shopName("Nông trại Ba Đồi")
                .boxType("VEGETABLES")
                .price(BigDecimal.valueOf(99000))
                .totalQuantity(10)
                .soldQuantity(0)
                .isActive(true)
                .description("Hộp rau củ bất ngờ")
                .items(List.of(
                        ProductMysteryResponse.builder()
                                .productId(101)
                                .productName("Cà chua bi")
                                .unit("kg")
                                .productQuantity(2)
                                .build()
                ))
                .build();

        sampleCreationRequest = MysteryBoxCreationRequest.builder()
                .boxType("VEGETABLES")
                .price(BigDecimal.valueOf(99000))
                .totalQuantity(10)
                .description("Hộp rau củ bất ngờ")
                .items(List.of(
                        ProductMysteryItemRequest.builder().productId(101).productQuantity(2).build()
                ))
                .build();
    }

    @Test
    void createMysteryBox_valid_returnsSuccess() throws Exception {
        when(mysteryBoxService.createMysteryBox(any(MysteryBoxCreationRequest.class))).thenReturn(sampleResponse);

        mockMvc.perform(post("/mystery-boxes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleCreationRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.result.mysteryId").value(1))
                .andExpect(jsonPath("$.result.boxType").value("VEGETABLES"));
    }

    @Test
    void createMysteryBox_missingPrice_returnsBadRequest() throws Exception {
        sampleCreationRequest.setPrice(null);

        mockMvc.perform(post("/mystery-boxes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleCreationRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.MYSTERY_BOX_PRICE_REQUIRED.getCode()));
    }

    @Test
    void createMysteryBox_missingItems_returnsBadRequest() throws Exception {
        sampleCreationRequest.setItems(null);

        mockMvc.perform(post("/mystery-boxes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleCreationRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.MYSTERY_BOX_ITEMS_REQUIRED.getCode()));
    }

    @Test
    void getMysteryBoxById_returnsSuccess() throws Exception {
        when(mysteryBoxService.getMysteryBoxById(1)).thenReturn(sampleResponse);

        mockMvc.perform(get("/mystery-boxes/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.result.mysteryId").value(1));
    }

    @Test
    void getMysteryBoxes_returnsPageResponse() throws Exception {
        PageResponse<MysteryBoxResponse> pageResponse = PageResponse.<MysteryBoxResponse>builder()
                .page(0)
                .size(10)
                .totalElements(1)
                .totalPages(1)
                .content(List.of(sampleResponse))
                .build();

        when(mysteryBoxService.getMysteryBoxes(any(), any(), any(), any(), any(), anyInt(), anyInt(), any(), any()))
                .thenReturn(pageResponse);

        mockMvc.perform(get("/mystery-boxes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.result.content[0].mysteryId").value(1));
    }

    @Test
    void getMyMysteryBoxes_returnsPageResponse() throws Exception {
        PageResponse<MysteryBoxResponse> pageResponse = PageResponse.<MysteryBoxResponse>builder()
                .page(0)
                .size(10)
                .totalElements(1)
                .totalPages(1)
                .content(List.of(sampleResponse))
                .build();

        when(mysteryBoxService.getMyMysteryBoxes(anyInt(), anyInt()))
                .thenReturn(pageResponse);

        mockMvc.perform(get("/mystery-boxes/my-shop"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.result.content[0].mysteryId").value(1));
    }

    @Test
    void updateMysteryBox_valid_returnsSuccess() throws Exception {
        MysteryBoxUpdateRequest request = MysteryBoxUpdateRequest.builder()
                .price(BigDecimal.valueOf(120000))
                .build();

        when(mysteryBoxService.updateMysteryBox(eq(1), any(MysteryBoxUpdateRequest.class))).thenReturn(sampleResponse);

        mockMvc.perform(put("/mystery-boxes/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000));
    }

    @Test
    void toggleMysteryBoxStatus_returnsSuccess() throws Exception {
        when(mysteryBoxService.toggleMysteryBoxStatus(eq(1), eq(false))).thenReturn(sampleResponse);

        mockMvc.perform(patch("/mystery-boxes/1/status")
                        .param("isActive", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000));
    }

    @Test
    void deleteMysteryBox_returnsSuccess() throws Exception {
        doNothing().when(mysteryBoxService).deleteMysteryBox(1);

        mockMvc.perform(delete("/mystery-boxes/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.message").value("Mystery box deleted successfully"));
    }
}
