package com.sang.be_capnong.service;

import com.sang.be_capnong.dto.request.SavedAddressRequest;
import com.sang.be_capnong.dto.response.SavedAddressResponse;

import java.util.List;

public interface SavedAddressService {

    List<SavedAddressResponse> getMyAddresses();

    SavedAddressResponse getAddressById(Long id);

    SavedAddressResponse createAddress(SavedAddressRequest request);

    SavedAddressResponse updateAddress(Long id, SavedAddressRequest request);

    void deleteAddress(Long id);

    SavedAddressResponse setDefaultAddress(Long id);

    SavedAddressResponse getDefaultAddress();
}
