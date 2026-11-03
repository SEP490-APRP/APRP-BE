package com.sang.be_capnong.service;

import com.sang.be_capnong.dto.request.AdminReturnDecisionRequest;
import com.sang.be_capnong.dto.request.ReturnCreateRequest;
import com.sang.be_capnong.dto.request.ShipperReturnActionRequest;
import com.sang.be_capnong.dto.request.ShopReturnRespondRequest;
import com.sang.be_capnong.dto.response.PageResponse;
import com.sang.be_capnong.dto.response.ReturnRequestResponse;
import com.sang.be_capnong.enums.ReturnStatus;

import java.util.List;

public interface ReturnRequestService {

    ReturnRequestResponse createReturnRequest(ReturnCreateRequest request);

    ReturnRequestResponse shopRespond(int returnId, ShopReturnRespondRequest request);

    ReturnRequestResponse adminDecision(int returnId, AdminReturnDecisionRequest request);

    ReturnRequestResponse shipperAccept(int returnId);

    ReturnRequestResponse shipperPickup(int returnId, ShipperReturnActionRequest request);

    ReturnRequestResponse shipperDeliver(int returnId, ShipperReturnActionRequest request);

    ReturnRequestResponse cancelReturnRequest(int returnId);

    ReturnRequestResponse getReturnRequestById(int returnId);

    PageResponse<ReturnRequestResponse> getMyBuyerReturns(int page, int size, ReturnStatus status);

    PageResponse<ReturnRequestResponse> getMyShopReturns(int page, int size, ReturnStatus status);

    PageResponse<ReturnRequestResponse> getMyShipperReturns(int page, int size, ReturnStatus status);

    PageResponse<ReturnRequestResponse> getAvailableReturnOrdersForShipper(int page, int size);

    PageResponse<ReturnRequestResponse> getAllDisputesForAdmin(int page, int size, ReturnStatus status, String keyword);

    List<ReturnRequestResponse> getAllDisputesList();

    boolean autoCheckPayout(int orderCode);

    ReturnRequestResponse checkPayoutStatus(int returnId);
}
