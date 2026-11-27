package com.sang.be_capnong.util;

import com.sang.be_capnong.enums.VehicleType;
import com.sang.be_capnong.exception.AppException;
import com.sang.be_capnong.exception.ErrorCode;

/** Điều kiện đăng ký xe: loại xe hợp lệ, biển số đúng loại, hạng giấy phép đủ lái và số giấy phép 12 chữ số */
public final class VehicleRules {

    private VehicleRules() {
    }

    public static VehicleType require(String vehicleType, String plate, String licenseClass, String licenseNumber) {
        VehicleType type = VehicleType.parse(vehicleType);
        if (type == null) throw new AppException(ErrorCode.VEHICLE_TYPE_INVALID);
        if (!type.acceptsPlate(plate)) throw new AppException(ErrorCode.VEHICLE_PLATE_INVALID);
        if (!type.acceptsLicense(licenseClass)) throw new AppException(ErrorCode.LICENSE_CLASS_NOT_ALLOWED);
        if (licenseNumber == null || !licenseNumber.trim().matches("\\d{12}")) throw new AppException(ErrorCode.LICENSE_NUMBER_INVALID);
        return type;
    }
}
