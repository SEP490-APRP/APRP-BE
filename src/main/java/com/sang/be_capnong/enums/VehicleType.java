package com.sang.be_capnong.enums;

import java.util.List;

/**
 * Loại xe tài xế đăng ký: sức chở (lọc chuyến ra hub, giới hạn đơn nhận) và điều kiện đăng ký.
 * Hạng giấy phép theo Luật Trật tự, an toàn giao thông đường bộ 2024, kèm hạng cũ cấp trước 2025 còn hiệu lực.
 */
public enum VehicleType {
    MOTORBIKE("Xe máy", 60, Plate.MOTORBIKE, List.of("A1", "A", "A2")),
    VAN("Xe ba gác / bán tải", 500, Plate.ANY, List.of("B1", "A3", "B", "B2", "C1", "C")),
    TRUCK_1T("Xe tải 1 tấn", 1000, Plate.CAR, List.of("B", "B2", "C1", "C", "C1E", "CE")),
    // Xe tải 2 tấn trở lên có khối lượng toàn bộ trên 3.500 kg
    TRUCK_2T("Xe tải 2 tấn", 2000, Plate.CAR, List.of("C1", "C", "C1E", "CE")),
    // Xe tải 5 tấn có khối lượng toàn bộ trên 7.500 kg
    TRUCK_5T("Xe tải 5 tấn", 5000, Plate.CAR, List.of("C", "CE"));

    /** Dạng biển số (đã viết hoa), cho phép dấu gạch, dấu chấm, khoảng trắng đúng vị trí. Xe máy: 59-A1 123.45; ô tô: 51C-100.01 */
    private enum Plate {
        MOTORBIKE("\\d{2}[\\s-]*(?:[A-Z]\\d|[A-Z]{2})[\\s.-]*(?:\\d{3}[.\\s]?\\d{2}|\\d{4})"),
        CAR("\\d{2}[\\s-]*[A-Z]{1,2}[\\s.-]*(?:\\d{3}[.\\s]?\\d{2}|\\d{4})"),
        ANY("(?:" + MOTORBIKE.regex + ")|(?:\\d{2}[\\s-]*[A-Z]{1,2}[\\s.-]*(?:\\d{3}[.\\s]?\\d{2}|\\d{4}))");

        private final String regex;

        Plate(String regex) {
            this.regex = regex;
        }
    }

    private final String label;
    private final int capacityKg;
    private final Plate plate;
    private final List<String> licenseClasses;

    VehicleType(String label, int capacityKg, Plate plate, List<String> licenseClasses) {
        this.label = label;
        this.capacityKg = capacityKg;
        this.plate = plate;
        this.licenseClasses = licenseClasses;
    }

    public List<String> getLicenseClasses() {
        return licenseClasses;
    }

    public static String normalizePlate(String s) {
        return s == null ? "" : s.toUpperCase().replaceAll("[^A-Z0-9]", "");
    }

    public boolean acceptsPlate(String plateNumber) {
        return plateNumber != null && plateNumber.trim().toUpperCase().matches(plate.regex);
    }

    public boolean acceptsLicense(String licenseClass) {
        return licenseClass != null && licenseClasses.contains(licenseClass.trim().toUpperCase());
    }

    public String getLabel() {
        return label;
    }

    public int getCapacityKg() {
        return capacityKg;
    }

    /** null hoặc không hợp lệ -> null */
    public static VehicleType parse(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            return valueOf(s.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /** Sức chở của một loại xe; chưa khai báo thì coi là xe máy */
    public static int capacityKgOf(String type) {
        VehicleType t = parse(type);
        return (t != null ? t : MOTORBIKE).capacityKg;
    }
}
