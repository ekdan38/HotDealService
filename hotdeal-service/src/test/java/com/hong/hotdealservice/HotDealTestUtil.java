package com.hong.hotdealservice;

import com.hong.hotdealservice.web.dto.HotDealProductRequestDto;
import com.hong.hotdealservice.web.dto.HotDealProductUpdateRequestDto;

import java.util.*;
import java.util.stream.Collectors;

public class HotDealTestUtil {

    public static List<HotDealProductRequestDto> createHotDealParseProductInfos(String productInfosStr) {
        if (productInfosStr == null || productInfosStr.trim().isEmpty()) {
            return List.of();
        }
        return Arrays.stream(productInfosStr.split(";"))
                .map(info -> {
                    String[] parts = info.split("\\|");
                    Long productId = "null".equalsIgnoreCase(parts[0].trim()) ? null : Long.valueOf(parts[0].trim());
                    Integer quantity = "null".equalsIgnoreCase(parts[1].trim()) ? null : Integer.valueOf(parts[1].trim());
                    Double discountRate = "null".equalsIgnoreCase(parts[2].trim()) ? null : Double.valueOf(parts[2].trim());
                    return new HotDealProductRequestDto(productId, quantity, discountRate);
                })
                .collect(Collectors.toList());
    }

    public static List<HotDealProductUpdateRequestDto> updateHotDealParseProductInfos(String productInfosStr) {
        if (productInfosStr == null || productInfosStr.trim().isEmpty()) {
            return List.of();
        }
        return Arrays.stream(productInfosStr.split(";"))
                .map(info -> {
                    String[] parts = info.split("\\|");
                    Long hotDealProductId = "null".equalsIgnoreCase(parts[0].trim()) ? null : Long.valueOf(parts[0].trim());
                    Long productId = "null".equalsIgnoreCase(parts[1].trim()) ? null : Long.valueOf(parts[1].trim());
                    Integer quantity = "null".equalsIgnoreCase(parts[2].trim()) ? null : Integer.valueOf(parts[2].trim());
                    Double discountRate = "null".equalsIgnoreCase(parts[3].trim()) ? null : Double.valueOf(parts[3].trim());
                    return new HotDealProductUpdateRequestDto(hotDealProductId, productId, quantity, discountRate);
                })
                .collect(Collectors.toList());
    }

    public static List<Map<String, Object>> updateHotDealConvertProductInfosToMap(String productInfosStr) {
        if (productInfosStr == null || productInfosStr.trim().isEmpty() || "null".equalsIgnoreCase(productInfosStr.trim())) {
            return Collections.emptyList();
        }

        List<Map<String, Object>> productList = new ArrayList<>();
        String[] products = productInfosStr.split(";");
        for (String productStr : products) {
            String[] parts = productStr.split("\\|");
            if (parts.length != 4) {
                // 잘못된 형식이면 스킵하거나 예외 처리
                continue;
            }
            Map<String, Object> productMap = new HashMap<>();

            // hotDealProductId
            String hotDealProductIdStr = parts[0].trim();
            Long hotDealProductId = "null".equalsIgnoreCase(hotDealProductIdStr) ? null : Long.valueOf(hotDealProductIdStr);
            productMap.put("hotDealProductId", hotDealProductId);

            // productId
            String productIdStr = parts[1].trim();
            Long productId = "null".equalsIgnoreCase(productIdStr) ? null : Long.valueOf(productIdStr);
            productMap.put("productId", productId);

            // quantity
            String quantityStr = parts[2].trim();
            Integer quantity = "null".equalsIgnoreCase(quantityStr) ? null : Integer.valueOf(quantityStr);
            productMap.put("quantity", quantity);

            // discountRate
            String discountRateStr = parts[3].trim();
            Double discountRate = "null".equalsIgnoreCase(discountRateStr) ? null : Double.valueOf(discountRateStr);
            productMap.put("discountRate", discountRate);

            productList.add(productMap);
        }

        return productList;
    }
}
