package com.jerry.salesmanagement.common;

import java.util.List;

public final class WarehouseDocumentRules {
    public static final String TRANSFER_SOURCE = "WAREHOUSE_TRANSFER";
    public static final String PROJECT_WAREHOUSE = "CK-XMCG";
    public static final String CENTRAL_WAREHOUSE = "CK-JZCG";

    private WarehouseDocumentRules() {}

    public static boolean isTransfer(String type) {
        return "TRANSFER_JTX".equals(type) || "TRANSFER_XTJ".equals(type);
    }

    public static String numberPrefix(String type) {
        return switch (type) {
            case "INBOUND_NOTE" -> "RKD";
            case "OUTBOUND_NOTE" -> "FHD";
            case "TRANSFER_JTX" -> "HBJ";
            case "TRANSFER_XTJ" -> "HBX";
            default -> throw new IllegalArgumentException("单据类型无效");
        };
    }

    public static List<String> categories(String type) {
        return switch (type) {
            case "INBOUND_NOTE" -> List.of("PROJECT_PURCHASE", "CENTRALIZED_PURCHASE");
            case "OUTBOUND_NOTE" -> List.of("PROJECT_SALES");
            case "TRANSFER_JTX" -> List.of("WAREHOUSE_TRANSFER_TO_PROJECT");
            case "TRANSFER_XTJ" -> List.of("PROJECT_TRANSFER_TO_WAREHOUSE");
            default -> List.of();
        };
    }

    public static String sourceWarehouse(String category) {
        return switch (category) {
            case "CENTRALIZED_PURCHASE", "WAREHOUSE_TRANSFER_TO_PROJECT" -> CENTRAL_WAREHOUSE;
            default -> PROJECT_WAREHOUSE;
        };
    }

    public static String destinationWarehouse(String type) {
        return "TRANSFER_JTX".equals(type) ? PROJECT_WAREHOUSE : CENTRAL_WAREHOUSE;
    }

    public static String label(String type) {
        return switch (type) {
            case "OUTBOUND_NOTE" -> "发货单";
            case "TRANSFER_JTX" -> "划拨单JTX";
            case "TRANSFER_XTJ" -> "划拨单XTJ";
            default -> "入库单";
        };
    }
}
