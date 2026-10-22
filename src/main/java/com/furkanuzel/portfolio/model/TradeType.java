package com.furkanuzel.portfolio.model;

public enum TradeType {
    BUY,
    SELL;

    public static TradeType parse(String value) {
        if (value != null) {
            for (TradeType type : values()) {
                if (type.name().equalsIgnoreCase(value.trim())) return type;
            }
        }
        throw new IllegalArgumentException("Geçersiz işlem türü");
    }
}
